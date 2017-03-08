/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.model.based.configurable.nat;

import org.apache.felix.scr.annotations.*;
import org.onlab.packet.*;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;
import org.onosproject.net.config.*;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.flow.*;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.packet.*;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.MacAddress.valueOf;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private static final int DEFAULT_TIMEOUT = 1000;
    private static final int DEFAULT_PRIORITY = 40001;

    private static final short FIRST_PORT = 10000;
    private static final short LAST_PORT = 12000;

    private static final String PRIVATE_PORT_ID = "USER:0";
    private static final String PUBLIC_PORT_ID = "WAN:0";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    private ApplicationId appId;

    private NatPacketProcessor processor = new NatPacketProcessor();

    // default configuration
    private DeviceId inputDeviceId;
    private DeviceId outputDeviceId;
    private PortNumber inputInterface;
    private PortNumber outputInterface;
    private int inputPortFlowPriority;
    private int outputPortFlowPriority;
    private VlanId externalInputVlan = VlanId.vlanId((short) 0);
    private VlanId externalOutputVlan = VlanId.vlanId((short) 0);

    private Ip4Address privateAddress;
    private Ip4Address publicAddress;
    public MacAddress privateMac = valueOf(randomMACAddress());
    public MacAddress publicMac = valueOf(randomMACAddress());
    private int flowTimeout = DEFAULT_TIMEOUT;
    private int flowPriority = DEFAULT_PRIORITY;

    /**
     *  The Nat table:
     *  Key: output port
     *  value: "inputIP:inputPort"
     */
    public Map<Short, String> natPortMap = new HashMap<>();

    /**
     *  The ARP table
     *  key: IP
     *  value: MAC
     */
    private Map<Ip4Address, MacAddress> arpTable = new HashMap<>();

    /**
     *  Map containing all pending IP packets that need to be processed after IP address resolution
     *  key: destination IP
     *  value: Queue of pending packets
     */
    private Map<Ip4Address, Queue<PacketContext>> pendingPackets = new HashMap<>();

    protected StateListenerNew sl;
    
    private final ExecutorService eventExecutor =
            newSingleThreadExecutor(groupedThreads("onos/nat-ctl", "event-handler", log));

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, PortConfig.class, "nat") {
                @Override
                public PortConfig createConfig() {
                    return new PortConfig();
                }
            };

    private final NetworkConfigListener configListener = new InternalConfigListener();

    @Activate
    protected void activate(ComponentContext context) {

        // load configuration
        loadConfiguration();

        appId = coreService.registerApplication("it.polito.onosapp.nat");
        packetService.addProcessor(processor, PacketProcessor.director(0));
        configService.addListener(configListener);
        configRegistry.registerConfigFactory(configFactory);
        requestIntercepts();
        
        sl = new StateListenerNew(this);
        
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {

        withdrawIntercepts();
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(processor);
        configService.removeListener(configListener);
        configRegistry.unregisterConfigFactory(configFactory);
        processor = null;

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        requestIntercepts();
    }

    /**
     * Load configuration from ini config file.
     */
    private void loadConfiguration() {
        try {
            log.info("Loading parameters from configuration file.");
            NatConfiguration config = new NatConfiguration();

            this.inputDeviceId = DeviceId.deviceId(config.getUserDeviceId());
            this.outputDeviceId = DeviceId.deviceId(config.getWanDeviceId());
            this.inputInterface = PortNumber.portNumber(config.getUserInterface());
            this.outputInterface = PortNumber.portNumber(config.getWanInterface());

            this.privateAddress = Ip4Address.valueOf(config.getPrivateAddress());
            this.publicAddress = Ip4Address.valueOf(config.getPublicAddress());

            log.info("Loaded parameters from configuration file.");
        } catch (IOException e) {
            log.info(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load configuration by the Network Config API
     */
    private void readConfiguration() {
        PortConfig config = configRegistry.getConfig(appId, PortConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }

        // stop current interceptor
        withdrawIntercepts();

        PortConfig.ApplicationPort privatePort = config.getPort(PRIVATE_PORT_ID);
        if (privatePort != null) {
            if (privatePort.getDeviceId() != null)
                inputDeviceId = privatePort.getDeviceId();
            if (privatePort.getPortNumber() != null)
                inputInterface = privatePort.getPortNumber();
            inputPortFlowPriority = privatePort.getFlowPriority();
            externalInputVlan = VlanId.vlanId((short) privatePort.getExternalVlan());

        }
        PortConfig.ApplicationPort publicPort = config.getPort(PUBLIC_PORT_ID);
        if (publicPort != null) {
            if (publicPort.getDeviceId() != null)
                outputDeviceId = publicPort.getDeviceId();
            if (publicPort.getPortNumber() != null)
                outputInterface = publicPort.getPortNumber();
            outputPortFlowPriority = publicPort.getFlowPriority();
            externalOutputVlan = VlanId.vlanId((short) publicPort.getExternalVlan());
        }

        log.info("Updated configuration");

        // clean old rules and restart interceptor with new configuration
        flowRuleService.removeFlowRulesById(appId);
        requestIntercepts();
    }

    /**
     * Request packet in via packet service.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchInPort(inputInterface);
        if (externalInputVlan.toShort() != 0)
            selector.matchVlanId(externalInputVlan);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(inputDeviceId));

        selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_ARP);
        selector.matchInPort(inputInterface);
        if (externalInputVlan.toShort() != 0)
            selector.matchVlanId(externalInputVlan);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(inputDeviceId));

        selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_ARP);
        selector.matchInPort(outputInterface);
        if (externalOutputVlan.toShort() != 0)
            selector.matchVlanId(externalOutputVlan);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(outputDeviceId));
    }

    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchInPort(inputInterface);
        if (externalInputVlan.toShort() != 0)
            selector.matchVlanId(externalInputVlan);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(inputDeviceId));

        selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_ARP);
        selector.matchInPort(inputInterface);
        if (externalInputVlan.toShort() != 0)
            selector.matchVlanId(externalInputVlan);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(inputDeviceId));

        selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_ARP);
        selector.matchInPort(outputInterface);
        if (externalOutputVlan.toShort() != 0)
            selector.matchVlanId(externalOutputVlan);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.of(outputDeviceId));
    }

    private class NatPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext packetContext) {

            if (packetContext.isHandled())
                return;
            Ethernet ethPkt = packetContext.inPacket().parsed();
            if (ethPkt == null) {
                return;
            }
            if (isControlPacket(ethPkt))
                return;

            log.info("New packet");
            log.info("From device {} port {}", packetContext.inPacket().receivedFrom().deviceId(), packetContext.inPacket().receivedFrom().port());

            if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {
                log.info(" - ARP packet");
                ARP arpPacket = (ARP) ethPkt.getPayload();

                if ((arpPacket.getOpCode() == ARP.OP_REQUEST)) {
                    log.info(" -- ARP request: who has {}", Ip4Address.valueOf(arpPacket.getTargetProtocolAddress()));
                    if (Objects.equals(privateAddress, Ip4Address.valueOf(arpPacket.getTargetProtocolAddress()))) {
                        log.info(" -- ARP request for nat interface");
                        processArpRequest(packetContext, ethPkt, privateMac);
                    } else if (Objects.equals(publicAddress, Ip4Address.valueOf(arpPacket.getTargetProtocolAddress()))) {
                        log.info(" -- ARP request for public interface");
                        processArpRequest(packetContext, ethPkt, publicMac);
                    }
                }
                if ((arpPacket.getOpCode() == ARP.OP_REPLY)) {
                    log.info(" -- ARP reply: {} is at {}", Ip4Address.valueOf(arpPacket.getSenderProtocolAddress()), MacAddress.valueOf(arpPacket.getSenderHardwareAddress()));
                    arpTable.put(Ip4Address.valueOf(arpPacket.getSenderProtocolAddress()), MacAddress.valueOf(arpPacket.getSenderHardwareAddress()));

                    // process enqueued packets for this destination
                    Queue<PacketContext> packetsQueue = pendingPackets.get(Ip4Address.valueOf(arpPacket.getSenderProtocolAddress()));
                    while (packetsQueue != null && !packetsQueue.isEmpty()) {
                        PacketContext pendingPacketContext = packetsQueue.remove();
                        log.info("dequeued packet {}, processing...", pendingPacketContext);
                        process(pendingPacketContext);
                    }
                }
            } else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {

                // Process only if is coming from the NAT input interface
                if (!(packetContext.inPacket().receivedFrom().deviceId().equals(inputDeviceId) && packetContext.inPacket().receivedFrom().port().equals(inputInterface)))
                    return;

                log.info(" - IP packet");
                log.info("processing {}", packetContext);
                IPv4 ipHeader = (IPv4) ethPkt.getPayload();
                if (ipHeader == null)
                    return;
                IpAddress srcAddress = IpAddress.valueOf(ipHeader.getSourceAddress());
                IpAddress dstAddress = IpAddress.valueOf(ipHeader.getDestinationAddress());

                // first we need to know the destination mac address
                MacAddress dstMac = arpTable.get(dstAddress.getIp4Address());
                if (dstMac == null) {

                    log.info(" - sending arp request to {}", dstAddress.getIp4Address());
                    sendArpRequest(dstAddress, outputDeviceId, outputInterface);

                    // add this packet to the map of the pending packets to allow future processing
                    pendingPackets.putIfAbsent(dstAddress.getIp4Address(), new LinkedBlockingQueue<>());
                    pendingPackets.get(dstAddress.getIp4Address()).add(packetContext);
                    return;
                }

                int srcPortNumber;
                int publicPort = 0;

                if (ipHeader.getProtocol() == IPv4.PROTOCOL_TCP) {

                    log.info(" - - TCP packet");
                    TCP tcpHeader = (TCP) ipHeader.getPayload();
                    if (tcpHeader == null)
                        return;
                    srcPortNumber = tcpHeader.getSourcePort();
                    publicPort = getAvailableOutputPort();

                    natPortMap.put((short) publicPort, srcAddress.toString() + ":" + srcPortNumber);

                    log.info(" - - Recieved from Device: " + packetContext.inPacket().receivedFrom().deviceId().toString() + " port: " + packetContext.inPacket().receivedFrom().port().toString());
                    log.info(" - - Src IP: " + srcAddress.toString());
                    log.info(" - - Dst IP: " + dstAddress.toString());
                    log.info(" - - Src Port: " + srcPortNumber);
                    log.info(" - - Dst Port: " + tcpHeader.getDestinationPort());


                } else if (ipHeader.getProtocol() == IPv4.PROTOCOL_ICMP) {

                    log.info(" - - ICMP packet");
                    ICMP icmpHeader = (ICMP) ipHeader.getPayload();
                    if (icmpHeader == null)
                        return;
                    srcPortNumber = icmpHeader.getIcmpCode();   // icmp query id?

                    log.info(" - - Recieved from Device: " + packetContext.inPacket().receivedFrom().deviceId().toString() + " port: " + packetContext.inPacket().receivedFrom().port().toString());
                    log.info(" - - Src IP: " + srcAddress.toString());
                    log.info(" - - Dst IP: " + dstAddress.toString());
                    log.info(" - - Src code: " + srcPortNumber);
                } else return;

                ipHeader.setSourceAddress(publicAddress.toInt());

                Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(), inputDeviceId, outputDeviceId);
                Path path = pickForwardPathIfPossible(paths, packetContext.inPacket().receivedFrom().port());

                // create flows for each link
                for (Link link : path.links()) {
                    if (link.src().deviceId().equals(inputDeviceId)) {
                        log.info("LINK: input device");
                        installIncomingNatRule(packetContext, srcAddress.getIp4Address(), dstAddress.getIp4Address(), ipHeader.getProtocol(), srcPortNumber, publicPort, dstMac, link.src().port());
                        installForwardingRule(link.src().deviceId(), inputInterface, dstAddress.getIp4Address(), srcAddress.getIp4Address());
                    } else {
                        log.info("LINK: not input device");
                        installForwardingRule(link.src().deviceId(), link.src().port(), publicAddress.getIp4Address(), dstAddress.getIp4Address());
                    }
                    if (link.dst().deviceId().equals(outputDeviceId)) {
                        log.info("LINK: output device");
                        installOutcomingNatRule(dstAddress.getIp4Address(), srcAddress.getIp4Address(), ipHeader.getProtocol(), publicPort, srcPortNumber, ethPkt.getSourceMAC(), link.dst().port());
                        installForwardingRule(link.dst().deviceId(), outputInterface, publicAddress.getIp4Address(), dstAddress.getIp4Address());
                    } else {
                        log.info("LINK: not output device");
                        installForwardingRule(link.dst().deviceId(), link.dst().port(), dstAddress.getIp4Address(), srcAddress.getIp4Address());
                    }
                }
                try { Thread.sleep(100); } catch (InterruptedException ignored) { }

                log.info("Forwarding to Table");
                packetToTable(packetContext);
            }
        }

        /**
         * Processes the ARP Payload and initiates a reply to the client.
         *
         * @param packetContext context of the incoming message
         * @param ethPkt the ethernet payload
         */
        private void processArpRequest(PacketContext packetContext, Ethernet ethPkt, MacAddress replyMac) {

            ARP arpPacket = (ARP) ethPkt.getPayload();

            ARP arpReply = (ARP) arpPacket.clone();
            arpReply.setOpCode(ARP.OP_REPLY);

            arpReply.setTargetProtocolAddress(arpPacket.getSenderProtocolAddress());
            arpReply.setTargetHardwareAddress(arpPacket.getSenderHardwareAddress());
            arpReply.setSenderProtocolAddress(arpPacket.getTargetProtocolAddress());
            arpReply.setSenderHardwareAddress(replyMac.toBytes());

            // Ethernet Frame.
            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(replyMac);
            ethReply.setDestinationMACAddress(ethPkt.getSourceMAC());
            ethReply.setEtherType(Ethernet.TYPE_ARP);
            ethReply.setVlanID(ethPkt.getVlanID());

            ethReply.setPayload(arpReply);
            sendReply(packetContext, ethReply);
        }

        private void sendArpRequest(IpAddress dstAddress, DeviceId deviceId, PortNumber portNumber) {

            // Build ARP packet
            ARP arpRequest = new ARP()
                    .setHardwareType(ARP.HW_TYPE_ETHERNET)
                    .setProtocolType(ARP.PROTO_TYPE_IP)
                    .setOpCode(ARP.OP_REQUEST)
                    .setHardwareAddressLength((byte)6)
                    .setProtocolAddressLength((byte)4)
                    .setSenderHardwareAddress(publicMac.toBytes())
                    .setSenderProtocolAddress(publicAddress.toInt())
                    .setTargetHardwareAddress(MacAddress.ZERO.toBytes())
                    .setTargetProtocolAddress(dstAddress.getIp4Address().toInt());
            arpRequest.setPayload(new Data(new byte[] {0x01}));

            // Build Ethernet frame
            Ethernet ethRequest = new Ethernet()
                    .setDestinationMACAddress(MacAddress.BROADCAST)
                    .setSourceMACAddress(publicMac)
                    .setEtherType(Ethernet.TYPE_ARP);
            if (externalOutputVlan.toShort() != 0)
                ethRequest.setVlanID(externalOutputVlan.toShort());
            ethRequest.setPayload(arpRequest);

            TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
            builder.setOutput(portNumber);

            packetService.emit(new DefaultOutboundPacket(deviceId, builder.build(), ByteBuffer.wrap(ethRequest.serialize())));
        }

        /**
         * Sends the Ethernet reply frame via the Packet Service.
         *
         * @param packetContext the context of the incoming frame
         * @param reply the Ethernet reply frame
         */
        private void sendReply(PacketContext packetContext, Ethernet reply) {
            if (reply != null) {
                TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
                ConnectPoint sourcePoint = packetContext.inPacket().receivedFrom();
                builder.setOutput(sourcePoint.port());
                packetContext.block();
                packetService.emit(new DefaultOutboundPacket(sourcePoint.deviceId(),
                        builder.build(), ByteBuffer.wrap(reply.serialize())));
            }
        }
    }

    // Install a rule in the last switch applying the NAT reverse function
    private void installOutcomingNatRule(Ip4Address srcAddress, Ip4Address dstAddress, byte protocol, int dstPort,
                                        int newDstPort, MacAddress dstMac, PortNumber portNumber) {

        log.debug(" - Install outcoming nat Rule");

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder
                .matchInPort(outputInterface)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcAddress.toIpPrefix())
                .matchIPProtocol(protocol)
                .matchIPDst(publicAddress.toIpPrefix());
        if (externalOutputVlan.toShort() != 0)
            selectorBuilder.matchVlanId(externalOutputVlan);

        switch (protocol) {
            case IPv4.PROTOCOL_TCP:
                // selectorBuilder.matchTcpDst(TpPort.tpPort(dstPort));
                break;
            case IPv4.PROTOCOL_ICMP:
                // selectorBuilder.matchIcmpCode((byte) dstPort);
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder()
                .setIpDst(dstAddress.getIp4Address())
                .setEthDst(dstMac)
                .setEthSrc(publicMac)
                .setOutput(portNumber);
        // VLAN endpoint
        if (externalOutputVlan.toShort() != 0) {
            if (externalInputVlan.toShort() != 0)
                treatmentBuilder.setVlanId(externalInputVlan);
            else
                treatmentBuilder.popVlan();
        }

        // TODO TCP port change does not work
        //if (protocol == IPv4.PROTOCOL_TCP)
        //    treatmentBuilder.setTcpDst(TpPort.tpPort(newDstPort));

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();

        log.info("Installing flow rule on device '" + outputDeviceId + "'");
        log.info("Match: " +
                "InPort " + outputInterface + " | " +
                "IpSrc " + srcAddress.getIp4Address().toString() + " | " +
                "PortDst " + dstPort + " | " +
                "Proto " + protocol + " | " +
                "IpDst " + publicAddress.getIp4Address().toString());
        String tcpLogString = "";
        if (protocol == IPv4.PROTOCOL_TCP)
            tcpLogString = "setTcpDst " + newDstPort + " | ";
        log.info("Action: " +
                "setIpDst " + dstAddress.getIp4Address().toString() + " | " +
                "setEthDst " + dstMac.toString() + " | " +
                "setEthSrc " + publicMac.toString() + " | " +
                tcpLogString +
                "outputPort " + portNumber.toString());

        // create the flow rule
        flowObjectiveService.forward(outputDeviceId, forwardingObjective);
    }

    // Install a rule in the first switch applying the NAT function
    private void installIncomingNatRule(PacketContext context, Ip4Address srcAddress, Ip4Address dstAddress, byte protocol, int srcPort,
                                        int newSrcPort, MacAddress dstMac, PortNumber portNumber) {

        log.info(" - Install incoming nat Rule");

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder
                .matchInPort(context.inPacket().receivedFrom().port())
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcAddress.toIpPrefix())
                .matchIPProtocol(protocol)
                .matchIPDst(dstAddress.toIpPrefix());
        if (externalInputVlan.toShort() != 0)
            selectorBuilder.matchVlanId(externalInputVlan);
        switch (protocol) {
            case IPv4.PROTOCOL_TCP:
                //selectorBuilder.matchTcpSrc(TpPort.tpPort(srcPort));
                break;
            case IPv4.PROTOCOL_ICMP:
                //selectorBuilder.matchIcmpCode((byte) srcPort);
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder()
                .setIpSrc(publicAddress)
                .setEthSrc(publicMac)
                .setEthDst(dstMac)
                .setOutput(portNumber);
        // VLAN endpoint
        if (externalInputVlan.toShort() != 0) {
            if (externalOutputVlan.toShort() != 0)
                treatmentBuilder.setVlanId(externalOutputVlan);
            else
                treatmentBuilder.popVlan();
        }

        // TODO TCP port change does not work
        //if (protocol == IPv4.PROTOCOL_TCP)
        //    treatmentBuilder.setTcpSrc(TpPort.tpPort(newSrcPort));

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();

        log.info("Installing flow rule on device '" + inputDeviceId + "'");
        log.info("Match: " +
                "InPort " + context.inPacket().receivedFrom().port() + " | " +
                "IpSrc " + srcAddress.getIp4Address().toString() + " | " +
                "PortSrc " + srcPort + " | " +
                "Proto " + protocol + " | " +
                "IpDst " + dstAddress.toString());
        String tcpLogString = "";
        if (protocol == IPv4.PROTOCOL_TCP)
            tcpLogString = "setTcpSrc " + newSrcPort + " | ";
        log.info("Action: " +
                "setIpSrc " + publicAddress.toString() + " | " +
                "setEthDst " + dstMac.toString() + " | " +
                "setEthSrc " + publicMac.toString() + " | " +
                tcpLogString +
                "outputPort " + portNumber.toString());

        // create the flow rule
        flowObjectiveService.forward(inputDeviceId, forwardingObjective);
    }

    // install rules to steer traffic towards the final port
    private void installForwardingRule(DeviceId deviceId, PortNumber outputPort, Ip4Address srcAddress, Ip4Address dstAddress) {

        log.info(" - Install forwarding Rule");

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcAddress.toIpPrefix())
                .matchIPDst(dstAddress.toIpPrefix());

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder()
                .setOutput(outputPort);
        // VLAN endpoint
        if (deviceId == inputDeviceId && externalInputVlan.toShort() != 0) {
            if (externalOutputVlan.toShort() != 0) {
                treatmentBuilder.pushVlan();
                treatmentBuilder.setVlanId(externalInputVlan);
            }
        } else if (deviceId == outputDeviceId && externalOutputVlan.toShort() != 0) {
            if (externalInputVlan.toShort() != 0) {
                treatmentBuilder.pushVlan();
                treatmentBuilder.setVlanId(externalOutputVlan);
            }
        }

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();

        log.info("Installing flow rule on device '" + deviceId + "'");
        log.info("Match: " +
                "IpSrc " + srcAddress.getIp4Address().toString() + " | " +
                "IpDst " + dstAddress.getIp4Address().toString());
        log.info("Action: " +
                "outputPort " + outputPort.toString());

        // create the flow rule
        flowObjectiveService.forward(deviceId, forwardingObjective);
    }

    // Sends a packet to table.
    private void packetToTable(PacketContext context) {

        context.treatmentBuilder().setOutput(PortNumber.TABLE);
        context.send();
    }

    // Selects a path from the given set that does not lead back to the specified port if possible.
    private Path pickForwardPathIfPossible(Set<Path> paths, PortNumber notToPort) {
        Path lastPath = null;
        for (Path path : paths) {
            lastPath = path;
            if (!path.src().port().equals(notToPort)) {
                return path;
            }
        }
        return lastPath;
    }

    // Indicates whether this is a control packet, e.g. LLDP, BDDP
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    private short getAvailableOutputPort() {

        Random random = new Random();
        short port;
        do {
            port = (short) (random.nextInt((LAST_PORT - FIRST_PORT) + 1) + FIRST_PORT);
        } while (this.natPortMap.containsKey(port) && this.natPortMap.size() != LAST_PORT - FIRST_PORT + 1);
        return port;
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(PortConfig.class)) {
                return;
            }
            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    eventExecutor.execute(AppComponent.this::readConfiguration);
                    break;
                default:
                    break;
            }
        }
    }

    private String randomMACAddress() {
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);

        macAddr[0] = (byte)(macAddr[0] & (byte)254);  //zeroing last 2 bytes to make it unicast and locally adminstrated

        StringBuilder sb = new StringBuilder(18);
        for(byte b : macAddr){

            if(sb.length() > 0)
                sb.append(":");

            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}
