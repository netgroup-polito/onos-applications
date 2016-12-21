package org.onosproject.ovsdbrest;

/**
 * Custom exception class for OVSDB device.
 */
public class OvsdbRestException {

    /**
     * Thrown for problems related to a device entity representing an ovsdb node.
     */
    public static class OvsdbDeviceException extends Exception {
        public OvsdbDeviceException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when the an ovs bridge already exists with a given name.
     */
    public static class BridgeAlreadyExistsException extends Exception { }

    /**
     * Thrown when an ovs bridge is not found.
     */
    public static class BridgeNotFoundException extends Exception { }
}
