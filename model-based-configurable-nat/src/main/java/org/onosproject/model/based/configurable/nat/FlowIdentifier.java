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
public class FlowIdentifier {
    public IpAddress srcIp;
    public Short srcPort;
    public IpAddress dstIp;
    public Short dstPort;
    public byte protocol;
    
    public IpAddress getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(IpAddress srcIp) {
        this.srcIp = srcIp;
    }

    public Short getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(Short srcPort) {
        this.srcPort = srcPort;
    }

    public IpAddress getDstIp() {
        return dstIp;
    }

    public void setDstIp(IpAddress dstIp) {
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
}
