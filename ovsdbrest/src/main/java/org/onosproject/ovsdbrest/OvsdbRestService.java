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
    void createBridge(IpAddress ipAddress, String bridgeName);

    /**
     * Deletes a bridge.
     * @param bridgeName bridge identifier
     */
    void deleteBridge(IpAddress ipAddress, String bridgeName);

    /**
     * Creates a port of hte bridge.
     * @param bridgeName bridge identifier
     * @param portName port name
     * @param patchPeer patch peer
     */
    void createPort(IpAddress ipAddress, String bridgeName, String portName, String patchPeer);
}
