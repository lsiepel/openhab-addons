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
package org.openhab.binding.zwavejs.internal.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.UnDefType;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ChannelMetadataTest {

    private ArrayList<Node> getNodesFromStore(String filename) throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson(filename, ResultMessage.class);
        return resultMessage.result.state.nodes;
    }

    private Node getNodeFromStore(String filename, int NodeId) throws IOException {
        return getNodesFromStore(filename).stream().filter(f -> f.nodeId == NodeId).findAny().get();
    }

    @Test
    public void testChannelDetailsStore1Node3Channel1() throws IOException {
        Node node = getNodeFromStore("store_1.json", 3);

        ChannelMetadata details = new ChannelMetadata(3, node.values.get(0));

        assertEquals("binary-switch-value", details.Id);
        assertNull(details.description);
        assertEquals("Switch", details.itemType);
        assertEquals("Current Value", details.label);
        assertNull(details.description);
        assertEquals(OnOffType.ON, details.state);
        assertEquals(false, details.writable);
        assertNull(details.statePattern);
        assertNull(details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore1Node6Channel0() throws IOException {
        Node node = getNodeFromStore("store_1.json", 6);

        ChannelMetadata details = new ChannelMetadata(6, node.values.get(0));

        assertEquals("binary-switch-value", details.Id);
        assertEquals("Switch", details.itemType);
        assertEquals("Current Value", details.label);
        assertNull(details.description);
        assertEquals(OnOffType.ON, details.state);
        assertEquals(false, details.writable);
        assertNull(details.statePattern);
        assertNull(details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore1Node6Channel2() throws IOException {
        Node node = getNodeFromStore("store_1.json", 6);

        ChannelMetadata details = new ChannelMetadata(6, node.values.get(2));

        assertEquals("multilevel-sensor-power", details.Id);
        assertEquals("Number:Power", details.itemType);
        assertEquals("Power", details.label);
        assertNull(details.description);
        assertEquals(new QuantityType<>(0, Units.WATT), details.state);
        assertEquals(false, details.writable);
        assertEquals(StateDescriptionFragmentBuilder.create().withPattern("%0.f %unit%").withReadOnly(true)
                .withStep(BigDecimal.valueOf(1)).build(), details.statePattern);
        assertEquals("W", details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore1Node6Channel3() throws IOException {
        Node node = getNodeFromStore("store_1.json", 6);

        ChannelMetadata details = new ChannelMetadata(6, node.values.get(3));

        assertEquals("meter-value-65537", details.Id);
        assertEquals("Number:Energy", details.itemType);
        assertEquals("Electric Consumption", details.label);
        assertNull(details.description);
        assertEquals(new QuantityType<>(881.95, Units.KILOWATT_HOUR), details.state);
        assertEquals(false, details.writable);
        assertEquals(StateDescriptionFragmentBuilder.create().withPattern("%0.f %unit%").withReadOnly(true)
                .withStep(BigDecimal.valueOf(1)).build(), details.statePattern);
        assertEquals("kWh", details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore1Node6Channel5() throws IOException {
        Node node = getNodeFromStore("store_1.json", 6);

        ChannelMetadata details = new ChannelMetadata(6, node.values.get(5));

        assertEquals("meter-reset", details.Id);
        assertEquals("Switch", details.itemType);
        assertEquals("Reset Accumulated Values", details.label);
        assertNull(details.description);
        assertEquals(UnDefType.NULL, details.state);
        assertEquals(true, details.writable);
        assertNull(details.statePattern);
        assertNull(details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore2Node2Channel66() throws IOException {
        Node node = getNodeFromStore("store_2.json", 2);

        ChannelMetadata details = new ChannelMetadata(1, node.values.get(66));

        assertEquals("multilevel-sensor-humidity-2", details.Id);
        assertEquals("Number:Dimensionless", details.itemType);
        assertEquals("Humidity", details.label);
        assertNull(details.description);
        assertEquals(new QuantityType<>(17.04, Units.PERCENT), details.state);
        assertEquals(false, details.writable);
        assertEquals(StateDescriptionFragmentBuilder.create().withPattern("%0.f %unit%").withReadOnly(true)
                .withStep(BigDecimal.valueOf(1)).build(), details.statePattern);
        assertEquals("%", details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore2Node14Channel0() throws IOException {
        Node node = getNodeFromStore("store_2.json", 14);

        ChannelMetadata details = new ChannelMetadata(14, node.values.get(0));

        assertEquals("multilevel-switch-value", details.Id);
        assertEquals("Number", details.itemType);
        assertEquals("Current Value", details.label);
        assertNull(details.description);
        assertEquals(new DecimalType(0.0), details.state);
        assertEquals(false, details.writable);

        StateDescriptionFragment statePattern = details.statePattern;
        assertNotNull(statePattern);
        assertEquals(BigDecimal.valueOf(0), statePattern.getMinimum());
        assertEquals(BigDecimal.valueOf(99), statePattern.getMaximum());
        assertEquals(BigDecimal.valueOf(1), statePattern.getStep());
        assertEquals("%0.d", statePattern.getPattern());

        assertNull(details.unitSymbol);
    }
}
