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
    public Ip4Address nattedIp;
    public Short nattedPort;
    public String connectionState;

    public FlowInfo(){
	this.nattedIp = null;
	this.nattedPort = 0;
	this.connectionState = null;
    }

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
	int nattedIpHash = 0, connectionStateHash = 0;
	if(this.nattedIp != null)
	 	 nattedIpHash = 31 * nattedIp.hashCode();
	if(this.connectionState != null)
		connectionStateHash = connectionState.hashCode();

        return nattedIpHash  + connectionStateHash;
    }
}
