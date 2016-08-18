package org.onosproject.ovsdbrest;

/**
 * Created by gabriele on 18/08/16.
 */
public class OvsdbRestException {

    public static class OvsdbDeviceException extends Exception {
        public OvsdbDeviceException(String message) {
            super(message);
        }
    }

    public static class BridgeAlreadyExistsException extends Exception { }

    public static class BridgeNotFoundException extends Exception { }
}
