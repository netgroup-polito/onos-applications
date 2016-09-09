/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.polito.onosapp.nat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.onlab.osgi.ComponentContextAdapter;

import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.flow.FlowRuleServiceAdapter;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.topology.TopologyServiceAdapter;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;
import java.util.Enumeration;

/**
 * Set of tests of the ONOS application natManager.
 */
public class AppComponentTest {

    private AppComponent natManager;
    private final ComponentContext context = new MockComponentContext();
    @Before
    public void setUp() {
        natManager = new AppComponent();
        natManager.coreService = new CoreServiceAdapter();
        natManager.packetService = new PacketServiceAdapter();
        natManager.cfgService = new ComponentConfigAdapter();
        natManager.flowRuleService = new FlowRuleServiceAdapter();
        natManager.topologyService = new TopologyServiceAdapter();
        natManager.configService = new NetworkConfigServiceAdapter();
        natManager.configRegistry = new NetworkConfigRegistryAdapter();
        natManager.activate(context);
    }

    @After
    public void tearDown() {
        natManager.deactivate();
    }

    @Test
    public void basics() {

    }

    private class MockComponentContext extends ComponentContextAdapter {
        @Override
        public Dictionary getProperties() {
            return new MockDictionary();
        }
    }

    private class MockDictionary extends Dictionary {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Enumeration keys() {
            return null;
        }

        @Override
        public Enumeration elements() {
            return null;
        }

        @Override
        public Object get(Object key) {
            if (key.equals("pollFrequency")) {
                return "1";
            }
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }
    }

}
