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
public class FlowIdentifier {
    public Ip4Address srcIp;
    public Integer srcPort;
    public Ip4Address dstIp;
    public Integer dstPort;
    public byte protocol;

    public FlowIdentifier(){
	this.srcIp = null;
	this.srcPort = null;
	this.dstIp = null;
	this.dstPort = null;
	this.protocol = 0;
    }

    public FlowIdentifier(Ip4Address srcIp, Ip4Address dstIp, Integer srcPort, Integer dstPort, byte protocol){
        srcIp = this.srcIp;
        dstIp = this.dstIp;
        srcPort = this.srcPort;
        dstPort = this.dstPort;
        protocol = this.protocol;
    }

    public Ip4Address getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(Ip4Address srcIp) {
        this.srcIp = srcIp;
    }

    public Integer getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(Integer srcPort) {
        this.srcPort = srcPort;
    }

    public Ip4Address getDstIp() {
        return dstIp;
    }

    public void setDstIp(Ip4Address dstIp) {
        this.dstIp = dstIp;
    }

    public Integer getDstPort() {
        return dstPort;
    }

    public void setDstPort(Integer dstPort) {
        this.dstPort = dstPort;
    }

    public byte getProtocol() {
        return protocol;
    }

    public void setProtocol(byte protocol) {
        this.protocol = protocol;
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof FlowIdentifier))
            return false;

        FlowIdentifier fi = (FlowIdentifier) o;
        return this.srcIp.equals(fi.getSrcIp()) && this.srcPort.equals(fi.getSrcPort())
                && this.dstIp.equals(fi.getDstIp()) && this.dstPort.equals(fi.getDstPort())
                && this.protocol == fi.getProtocol();
    }

    @Override
    public int hashCode() {
	int srcIpHash = 0, srcPortHash = 0, dstIpHash = 0, dstPortHash = 0, protocolHash;
	if(this.srcIp != null)
		srcIpHash = 31 * this.srcIp.hashCode();
	if(this.srcPort != null)
		srcPortHash = srcPort.hashCode();
	if(this.dstIp != null)
		dstIpHash = dstIp.hashCode();
	if(this.dstPort != null)
		dstPortHash = this.dstPort.hashCode();

        return srcIpHash + srcPortHash + dstIpHash + dstPortHash +this.protocol;
    }
}
