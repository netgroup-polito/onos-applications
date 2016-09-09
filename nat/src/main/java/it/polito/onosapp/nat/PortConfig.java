package it.polito.onosapp.nat;

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.packet.Ip4Address;
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

    public ApplicationPort getPort(String portId) {

        JsonNode ports = object.path(PORTS_KEY);
        JsonNode port = ports.path(portId);
        String deviceId = port.path(DEVICE_ID).textValue();
        int portNumber = port.path(PORT_NUMBER).asInt();
        return new ApplicationPort(
                DeviceId.deviceId(deviceId),
                PortNumber.portNumber(portNumber)
        );
    }

    public static class ApplicationPort {

        private final DeviceId deviceId;
        private final PortNumber portNumber;

        public ApplicationPort(DeviceId deviceId, PortNumber portNumber) {
            this.deviceId = deviceId;
            this.portNumber = portNumber;
        }

        public DeviceId getDeviceId() {
            return deviceId;
        }

        public PortNumber getPortNumber() {
            return portNumber;
        }
    }
}
