package org.onosproject.ovsdbrest;

import org.onlab.packet.IpAddress;

/**
 * APIs for ovsdb driver access.
 */
public interface OvsdbBridgeService {

    /**
     * Creates a new bridge.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     */
    void createBridge(IpAddress ovsdbAddress, String bridgeName) throws OvsdbRestException.OvsdbDeviceException,
            OvsdbRestException.BridgeAlreadyExistsException;

    /**
     * Deletes a bridge.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     */
    void deleteBridge(IpAddress ovsdbAddress, String bridgeName) throws OvsdbRestException.OvsdbDeviceException,
            OvsdbRestException.BridgeNotFoundException;

    /**
     * Adds a port to a bridge.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the name of the port to attach to the bridge
     */
    void addPort(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Removes a port from a bridge.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the name of the port to remove from the bridge
     */
    void removePort(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Adds a patch port to a bridge setting it as peer of an other port.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the port name
     * @param patchPeer the name of the peer port
     */
    void createPatchPeerPort(IpAddress ovsdbAddress, String bridgeName, String portName, String patchPeer)
            throws OvsdbRestException.OvsdbDeviceException;

    /**
     * Creates a GRE tunnel from a bridge to a remote destination.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the name of the new GRE port
     * @param localIp local end point of the GRE tunnel
     * @param remoteIp remote end point of GRE tunnel
     * @param key the tunnel key, should represent a 32 bit hexadecimal number
     */
    void createGreTunnel(IpAddress ovsdbAddress, String bridgeName, String portName, IpAddress localIp,
                         IpAddress remoteIp, String key)
            throws OvsdbRestException.OvsdbDeviceException, OvsdbRestException.BridgeNotFoundException;

    /**
     * Deletes a GRE tunnel given the port name.
     * @param ovsdbAddress the ovsdb IP address
     * @param bridgeName the bridge identifier
     * @param portName the name of the GRE
     */
    void deleteGreTunnel(IpAddress ovsdbAddress, String bridgeName, String portName)
            throws OvsdbRestException.OvsdbDeviceException;
}
