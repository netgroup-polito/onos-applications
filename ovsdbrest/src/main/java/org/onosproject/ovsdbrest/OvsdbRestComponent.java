package org.onosproject.ovsdbrest;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.IpAddress;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.config.*;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.ovsdbrest.OvsdbNodeConfig.OvsdbNode;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Bridge and port controller.
 */
public class OvsdbRestComponent implements OvsdbRestService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId appId;
    private static final int DPID_BEGIN = 3;
    private static final int OFPORT = 6653;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService adminService;

    private Set<OvsdbNode> ovsdbNodes;
    // map<bridgeName - datapathId>
    private Map<String, DeviceId> bridgeIds = Maps.newConcurrentMap();

    private Map<OvsdbNode, Set<DeviceId>> ovsdbNodeDevIdsSetMap = Maps.newConcurrentMap();

    private final ExecutorService eventExecutor =
            newSingleThreadExecutor(groupedThreads("onos/ovsdb-rest-ctl", "event-handler", log));
    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final AtomicLong datapathId = new AtomicLong(0);

    private final OvsdbHandler ovsdbHandler = new OvsdbHandler();
    private final BridgeHandler bridgeHandler = new BridgeHandler();

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, OvsdbNodeConfig.class, "ovsdbrest") {
                @Override
                public OvsdbNodeConfig createConfig() {
                    return new OvsdbNodeConfig();
                }
            };


    @Activate
    protected void activate() {
        appId = coreService.getAppId("org.onosproject.ovsdbrest");
        deviceService.addListener(deviceListener);
        configService.addListener(configListener);
        configRegistry.registerConfigFactory(configFactory);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        deviceService.removeListener(deviceListener);
        configRegistry.unregisterConfigFactory(configFactory);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createBridge(IpAddress ipAddress, String bridgeName) {
        OvsdbNode ovsdbNode;
        try { //  gets the target ovsdb node
             ovsdbNode = ovsdbNodes.stream().filter(node -> !node.ovsdbIp().equals(ipAddress)).findFirst().get();
        } catch (NoSuchElementException nsee) {
            log.info(nsee.getMessage());
            return;
        }
        // construct a unique dev id
        // TODO: check uniqueness
        DeviceId dpid = DeviceId.deviceId("of:" + datapathId.getAndIncrement());
        bridgeIds.put(bridgeName, dpid);
        Set<DeviceId> dpIds = ovsdbNodeDevIdsSetMap
                .computeIfAbsent(ovsdbNode, (k) -> Sets.newConcurrentHashSet());
        dpIds.add(dpid);

        if (isBridgeCreated(bridgeName)) {
            return;
        }
        List<ControllerInfo> controllers = new ArrayList<>();
        Sets.newHashSet(clusterService.getNodes()).forEach(controller -> {
                    ControllerInfo ctrlInfo = new ControllerInfo(controller.ip(), OFPORT, "tcp");
                    controllers.add(ctrlInfo);
                });
        try {
            Device device = deviceService.getDevice(ovsdbNode.ovsdbId());
            if (device.is(BridgeConfig.class)) {
                BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
                bridgeConfig.addBridge(BridgeName.bridgeName(bridgeName),
                        dpid.toString(), controllers);
            } else {
                log.warn("The bridging behaviour is not supported in device {}", device.id());
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to create integration bridge on {}", ovsdbNode.ovsdbIp());
        }
    }

    @Override
    public void deleteBridge(IpAddress ipAddress, String bridgeName) {

    }

    @Override
    public void createPort(IpAddress ipAddress, String bridgeName, String portName, String peerPatch) {

    }

    private void connectOvsdb(OvsdbNode node) {
        if (!isOvsdbConnected(node)) {
            controller.connect(node.ovsdbIp(), node.ovsdbPort());
        }
    }

    /**
     * Checks if the bridge exists and available.
     *
     * @return true if the bridge is available, false otherwise
     */
    private boolean isBridgeCreated(String bridgeName) {
        DeviceId deviceId = bridgeIds.get(bridgeName);
        return (deviceService.getDevice(deviceId) != null
                && deviceService.isAvailable(deviceId));
    }

    /**
     * Returns connection state of OVSDB server for a given node.
     *
     * @return true if it is connected, false otherwise
     */
    private boolean isOvsdbConnected(OvsdbNode node) {

        OvsdbClientService ovsdbClient = getOvsdbClient(node);
        return deviceService.isAvailable(node.ovsdbId()) &&
                ovsdbClient != null && ovsdbClient.isConnected();
    }

    /**
     * Returns OVSDB client for a given node.
     *
     * @return OVSDB client, or null if it fails to get OVSDB client
     */
    private OvsdbClientService getOvsdbClient(OvsdbNode node) {

        OvsdbClientService ovsdbClient = controller.getOvsdbClient(
                new OvsdbNodeId(node.ovsdbIp(), node.ovsdbPort().toInt()));
        if (ovsdbClient == null) {
            log.trace("Couldn't find OVSDB client for {}", node.ovsdbId().toString());
        }
        return ovsdbClient;
    }
    /**
     * Returns cordvtn node associated with a given OVSDB device.
     *
     * @param ovsdbId OVSDB device id
     * @return cordvtn node, null if it fails to find the node
     */
    private OvsdbNode nodeByOvsdbId(DeviceId ovsdbId) {
        return ovsdbNodes.stream()
                .filter(node -> node.ovsdbId().equals(ovsdbId))
                .findFirst().orElse(null);
    }

    /**
     * Returns ovsdb node associated with a given integration bridge.
     *
     * @param bridgeId device id of the bridge
     * @return ovsdb node, null if it fails to find the node
     */
    private OvsdbNode nodeByBridgeId(DeviceId bridgeId) {
        final  Set<OvsdbNode> nodes = new HashSet<>();
        ovsdbNodeDevIdsSetMap.forEach((node, set) -> {
            if (set.contains(bridgeId)) {
                 nodes.add(node);
            }
        });
        Optional<OvsdbNode> opt = nodes.stream().findAny();
        if (opt.isPresent()) {
            return opt.get();
        } else {
            return null;
        }
    }

    private class OvsdbHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            OvsdbNode node = nodeByOvsdbId(device.id());
            if (node != null) {
                // TODO: setState
            } else {
                log.debug("{} is detected on unregistered node, ignore it.", device.id());
            }
        }

        @Override
        public void disconnected(Device device) {
            if (!deviceService.isAvailable(device.id())) {
                log.debug("Device {} is disconnected", device.id());
                adminService.removeDevice(device.id());
            }
        }
    }

    private void readConfiguration() {
        OvsdbNodeConfig config = configRegistry.getConfig(appId, OvsdbNodeConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }
        ovsdbNodes = config.getNodes();
        ovsdbNodes.forEach(node -> connectOvsdb(node));
    }

    /**
     * Returns port name.
     *
     * @param port port
     * @return port name
     */
    private String portName(Port port) {
        return port.annotations().value("portName");
    }

    private class BridgeHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            OvsdbNode node = nodeByBridgeId(device.id());
            if (node != null) {
                // TODO: set state
            } else {
                log.debug("{} is detected on unregistered node, ignore it.", device.id());
            }
        }

        @Override
        public void disconnected(Device device) {
            OvsdbNode node = nodeByBridgeId(device.id());
            if (node != null) {
                log.debug("Integration Bridge is disconnected from {}", node.ovsdbIp());
                // TODO: set state
            }
        }

        /**
         * Handles port added situation.
         *
         * @param port port
         */
        public void portAdded(Port port) {
            OvsdbNode node = nodeByBridgeId((DeviceId) port.element().id());
            String portName = portName(port);

            if (node == null) {
                log.debug("{} is added to unregistered node, ignore it.", portName);
                return;
            }

            log.info("Port {} is added to {}", portName, node.ovsdbIp());

            //TODO: set state
        }
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(OvsdbNodeConfig.class)) {
                return;
            }
            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    eventExecutor.execute(OvsdbRestComponent.this::readConfiguration);
                    break;
                default:
                    break;

            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            ConnectionHandler<Device> handler =
                    (device.type().equals(SWITCH) ? bridgeHandler : ovsdbHandler);

        }
    }
}
