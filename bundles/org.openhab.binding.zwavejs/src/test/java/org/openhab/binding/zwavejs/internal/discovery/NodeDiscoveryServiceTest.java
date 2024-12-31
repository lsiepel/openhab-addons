/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.zwavejs.internal.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants;
import org.openhab.binding.zwavejs.internal.api.dto.DeviceConfig;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.handler.ZwaveJSBridgeHandler;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;

/**
 * @author Leo Siepel - Initial contribution
 */
public class NodeDiscoveryServiceTest {

    @Mock
    private NodeDiscoveryService nodeDiscoveryService;
    private ZwaveJSBridgeHandler thingHandler;
    private Bridge bridge;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        nodeDiscoveryService = spy(new NodeDiscoveryService());
        thingHandler = mock(ZwaveJSBridgeHandler.class);
        bridge = mock(Bridge.class);
        when(bridge.getUID()).thenReturn(new ThingUID(ZwaveJSBindingConstants.BINDING_ID, "test-bridge"));
        when(thingHandler.getThing()).thenReturn(bridge);
        nodeDiscoveryService.setThingHandler(thingHandler);
    }

    @Test
    public void testAddNodeDiscovery() {
        Node node = new Node();
        node.nodeId = 1;
        node.deviceConfig = new DeviceConfig();
        node.deviceConfig.label = "Test Device";
        node.deviceConfig.manufacturer = "Test Manufacturer";
        node.isListening = true;
        node.isRouting = true;
        node.isSecure = true;
        node.productId = 1234;
        node.productType = 5678;
        node.lastSeen = Date.from(java.time.Instant.parse("2023-10-01T12:00:00Z"));
        node.isFrequentListening = true;

        ThingUID bridgeUID = new ThingUID("zwavejs", "bridge");
        when(thingHandler.getThing().getUID()).thenReturn(bridgeUID);

        nodeDiscoveryService.addNodeDiscovery(node);

        ArgumentCaptor<DiscoveryResult> captor = ArgumentCaptor.forClass(DiscoveryResult.class);
        verify(nodeDiscoveryService).thingDiscovered(captor.capture());

        DiscoveryResult result = captor.getValue();
        Map<String, Object> expectedProperties = new HashMap<>();
        expectedProperties.put("id", node.nodeId);
        expectedProperties.put("isListening", node.isListening);
        expectedProperties.put("isRouting", node.isRouting);
        expectedProperties.put("isSecure", node.isSecure);
        expectedProperties.put("manufacturer", node.deviceConfig.manufacturer);
        expectedProperties.put("productId", node.productId);
        expectedProperties.put("productType", node.productType);
        expectedProperties.put("lastSeen", node.lastSeen);
        expectedProperties.put("isFrequentListening", node.isFrequentListening);

        assertEquals(expectedProperties, result.getProperties());
        assertEquals(bridgeUID, result.getBridgeUID());
        assertEquals("Test Manufacturer Test Device (node 1)", result.getLabel());
    }
}
