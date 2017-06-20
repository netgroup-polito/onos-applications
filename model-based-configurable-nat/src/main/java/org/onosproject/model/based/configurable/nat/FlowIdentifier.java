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
    private Ip4Address srcIp;
    private Short srcPort;
    private Ip4Address dstIp;
    private Short dstPort;
    private byte protocol;
    
    public Ip4Address getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(Ip4Address srcIp) {
        this.srcIp = srcIp;
    }

    public Short getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(Short srcPort) {
        this.srcPort = srcPort;
    }

    public Ip4Address getDstIp() {
        return dstIp;
    }

    public void setDstIp(Ip4Address dstIp) {
        this.dstIp = dstIp;
    }

    public Short getDstPort() {
        return dstPort;
    }

    public void setDstPort(Short dstPort) {
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

        return 31*srcIp.hashCode() + srcPort.hashCode() + dstIp.hashCode() + dstPort.hashCode() + this.protocol;
    }
}
