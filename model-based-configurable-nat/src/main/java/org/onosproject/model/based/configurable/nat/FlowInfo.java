/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.onosproject.model.based.configurable.nat;

import org.onlab.packet.Ip4Address;

/**
 *
 * @author lara
 */
public class FlowInfo {
    Ip4Address nattedIp;
    Short nattedPort;
    String connectionState;

    public Ip4Address getNattedIp() {
        return nattedIp;
    }

    public void setNattedIp(Ip4Address nattedIp) {
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
