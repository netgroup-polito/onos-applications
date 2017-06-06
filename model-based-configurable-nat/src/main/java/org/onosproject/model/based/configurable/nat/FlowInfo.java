/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.onosproject.model.based.configurable.nat;

import org.onlab.packet.IpAddress;

/**
 *
 * @author lara
 */
public class FlowInfo {
    IpAddress nattedIp;
    Short nattedPort;
    String connectionState;

    public IpAddress getNattedIp() {
        return nattedIp;
    }

    public void setNattedIp(IpAddress nattedIp) {
        this.nattedIp = nattedIp;
    }

    public Short getNattedPort() {
        return nattedPort;
    }

    public void setNattedPort(Short nattedPort) {
        this.nattedPort = nattedPort;
    }

    public String getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(String connectionState) {
        this.connectionState = connectionState;
    }
}
