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
    void createBridge(IpAddress ovsdbAddress, String bridgeName) throws OvsdbRestException.OvsdbDeviceException,
            OvsdbRestException.BridgeAlreadyExistsException;

    /**
     * Deletes a bridge.
     * @param bridgeName bridge identifier
     */
    void deleteBridge(IpAddress ovsdbAddress, String bridgeName) throws OvsdbRestException.OvsdbDeviceException,
            OvsdbRestException.BridgeNotFoundException;

    /**
     * Creates a port of the bridge.
     * @param bridgeName bridge identifier
     * @param portName port name
     * @param patchPeer patch peer
     */
    void createPort(IpAddress ovsdbAddress, String bridgeName, String portName, String patchPeer)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Delete a port of the bridge.
     * @param bridgeName bridge identifier
     * @param portName port name
     */
    void deletePort(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Create a gre tunnel from a bridge port to a remote destination.
     * @param bridgeName bridge identifier
     * @param portName port name
     * @param remoteIp remote end point of gre tunnel
     */
    void createGreTunnel(IpAddress ovsdbAddress, String bridgeName, String portName, IpAddress remoteIp)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Delete a gre tunnel given the port name.
     * @param portName port name
     */
    void deleteGreTunnel(IpAddress ovsdbAddress, String portName)
            throws OvsdbRestException.OvsdbDeviceException;
}
