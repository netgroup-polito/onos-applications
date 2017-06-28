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

    public FlowIdentifier(){}

    public FlowIdentifier(Ip4Address srcIp, Ip4Address dstIp, Short srcPort, Short dstPort, byte protocol){
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

    public boolean equals(FlowIdentifier object){
        if(! this.srcIp.equals(object.getSrcIp()))
            return false;

        if(! this.dstIp.equals(object.getDstIp()))
            return false;

        if(! (this.srcPort == object.getSrcPort()) )
            return false;

        if(! (this.dstPort == object.getDstPort()) )
            return false;

        if(! (this.protocol == object.getProtocol()) )
            return false;

        return true;
    }
}
