/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.onosproject.model.based.configurable.nat;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;

/**
 *
 * @author lara
 */
public class FlowInfo {
    private Ip4Address nattedIp;
    private Short nattedPort;
    private String connectionState;

    public FlowInfo(){}

    public FlowInfo(Ip4Address nattedIp, Short nattedPort, String connectionState){
        nattedIp = this.nattedIp;
        nattedPort = this.nattedPort;
        connectionState = this.connectionState;
    }

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

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof FlowInfo))
            return false;

        FlowInfo fi = (FlowInfo) o;
        return this.nattedIp.equals(fi.getNattedIp()) && this.nattedPort.equals(fi.getNattedPort());
    }

    @Override
    public int hashCode() {

        return 31*nattedIp.hashCode() + nattedPort.hashCode();
    }
}
