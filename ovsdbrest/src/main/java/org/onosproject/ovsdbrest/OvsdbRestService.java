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
     * Add a port to a bridge.
     * @param bridgeName bridge identifier
     * @param portName port name
     */
    void addPort(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Remove a port from a bridge.
     * @param bridgeName bridge identifier
     * @param portName port name
     */
    void removePort(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Add a patch port to a bridge setting it as peer of an other port.
     * @param bridgeName bridge identifier
     * @param portName port name
     * @param patchPeer peer port name
     */
    void createPatchPeerPort(IpAddress ovsdbAddress, String bridgeName, String portName, String patchPeer)
            throws OvsdbRestException.OvsdbDeviceException;

    /**
     * Create a gre tunnel from a bridge port to a remote destination.
     * @param bridgeName bridge identifier
     * @param portName port name
     * @param remoteIp remote end point of gre tunnel
     * @param key the tunnel key
     */
    void createGreTunnel(IpAddress ovsdbAddress, String bridgeName, String portName, IpAddress localIp,
                         IpAddress remoteIp, String key)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Delete a gre tunnel given the port name.
     * @param bridgeName bridge identifier
     * @param portName port name
     */
    void deleteGreTunnel(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException;
}
