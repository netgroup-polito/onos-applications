package org.onosproject.ovsdbrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Configuration info to reach Ovsdb server.
 */
public class OvsdbNodeConfig extends Config<ApplicationId> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String NODES = "nodes";
    private static final String OVSDB_PORT = "ovsdbPort";
    private static final String OVSDB_IP = "ovsdbIp";

    public Set<OvsdbNode> getNodes() {
        Set<OvsdbNode> nodes = Sets.newConcurrentHashSet();

        JsonNode jsnoNodes = object.path(NODES);
        jsnoNodes.forEach(node -> {
            IpAddress ovsdbIp = IpAddress.valueOf(node.path(OVSDB_IP).textValue());
            TpPort port = TpPort.tpPort(Integer.parseInt(node.path(OVSDB_PORT).asText()));
            log.info("Ovsdb port: " + port.toString());
            nodes.add(new OvsdbNode(ovsdbIp, port));
        });
        return nodes;
    }

    public static class OvsdbNode {
        private final IpAddress ovsdbIp;
        private final TpPort ovsdbPort;

        public OvsdbNode(IpAddress ovsdbIp, TpPort ovsdbPort) {
            this.ovsdbIp = ovsdbIp;
            this.ovsdbPort = ovsdbPort;
        }

        public IpAddress ovsdbIp() {
            return ovsdbIp;
        }

        public TpPort ovsdbPort() {
            return ovsdbPort;
        }

        public DeviceId ovsdbId() {
            return DeviceId.deviceId("ovsdb:" + ovsdbIp.toString());
        }
    }
}
