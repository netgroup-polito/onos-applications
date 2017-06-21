package org.onosproject.model.based.configurable.nat;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gabriele on 06/09/16.
 */
public class PortConfig extends Config<ApplicationId> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String PORTS_KEY = "ports";

    private static final String DEVICE_ID = "device-id";
    private static final String PORT_NUMBER = "port-number";
    private static final String FLOW_PRIORITY = "flow-priority";
    private static final String EXTERNAL_VLAN = "external-vlan";

    private static final int DEFAULT_PRIORITY = 10;

    public ApplicationPort getPort(String portId) {

        JsonNode ports = object.path(PORTS_KEY);
        JsonNode port = ports.path(portId);
        String deviceId = port.path(DEVICE_ID).textValue();
        int portNumber = port.path(PORT_NUMBER).asInt();
        int flowPriority = port.path(FLOW_PRIORITY).asInt(DEFAULT_PRIORITY);
        int externalVlan = port.path(EXTERNAL_VLAN).asInt(0);
        return new ApplicationPort(
                DeviceId.deviceId(deviceId),
                PortNumber.portNumber(portNumber),
                flowPriority,
                externalVlan);
    }

    public static class ApplicationPort {

        private final DeviceId deviceId;
        private final PortNumber portNumber;
        private final int flowPriority;
        private final int externalVlan;

        public ApplicationPort(DeviceId deviceId, PortNumber portNumber, int flowPriority, int externalVlan) {
            this.deviceId = deviceId;
            this.portNumber = portNumber;
            this.flowPriority = flowPriority;
            this.externalVlan = externalVlan;
        }

        public DeviceId getDeviceId() {
            return deviceId;
        }

        public PortNumber getPortNumber() {
            return portNumber;
        }

        public int getFlowPriority() {
            return flowPriority;
        }

        public int getExternalVlan() {
            return externalVlan;
        }
    }
}
