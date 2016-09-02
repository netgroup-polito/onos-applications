package org.onosproject.ovsdbrest;

import org.onlab.packet.IpAddress;

/**
 * APIs for ovsdb driver access.
 */
public interface OvsdbRestService {

    /**
     * Creates a new bridge.
     * @param bridgeName bridge name
     */
    void createBridge(IpAddress ipAddress, String bridgeName) throws OvsdbRestException.OvsdbDeviceException,
            OvsdbRestException.BridgeAlreadyExistsException;

    /**
     * Deletes a bridge.
     * @param bridgeName bridge identifier
     */
    void deleteBridge(IpAddress ipAddress, String bridgeName) throws OvsdbRestException.OvsdbDeviceException,
            OvsdbRestException.BridgeNotFoundException;

    /**
     * Creates a port of the bridge.
     * @param bridgeName bridge identifier
     * @param portName port name
     * @param patchPeer patch peer
     */
    void createPort(IpAddress ipAddress, String bridgeName, String portName, String patchPeer)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Delete a port of the bridge.
     * @param bridgeName bridge identifier
     * @param portName port name
     */
    void deletePort(IpAddress ipAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;
}
