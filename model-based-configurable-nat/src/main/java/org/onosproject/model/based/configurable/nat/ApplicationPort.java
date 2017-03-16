/*
 * Copyright 2017 lara.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.model.based.configurable.nat;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 *
 * @author lara
 */
public class ApplicationPort {

    public DeviceId deviceId;
    public PortNumber portNumber;
    public int flowPriority;
    public VlanId externalVlan;
    public Ip4Address ipAddress;

    public ApplicationPort(){
        
    }
    
    public ApplicationPort(DeviceId deviceId, PortNumber portNumber, int flowPriority, VlanId externalVlan) {
        this.deviceId = deviceId;
        this.portNumber = portNumber;
        this.flowPriority = flowPriority;
        this.externalVlan = externalVlan;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public PortNumber getPortNumber() {
        return portNumber;
    }

    public int getFlowPriority() {
        return flowPriority;
    }

    public VlanId getExternalVlan() {
        return externalVlan;
    }
    
    public Ip4Address getIpAddress(){
        return ipAddress;
    }
}
