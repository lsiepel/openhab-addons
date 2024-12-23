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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ChannelDetailsTest {

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

        ChannelDetails details = new ChannelDetails(3, node.values.get(0));

        assertEquals("binary-switch-value", details.channelId);
        assertEquals("Binary Switch", details.description);
        assertEquals("Switch", details.itemType);
        assertEquals("Current value", details.label);
        assertEquals("Binary Switch", details.description);
        assertEquals(OnOffType.ON, details.state);
        assertEquals(false, details.writable);
        assertNull(details.statePattern);
        assertNull(details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore1Node6Channel1() throws IOException {
        Node node = getNodeFromStore("store_1.json", 6);

        ChannelDetails details = new ChannelDetails(6, node.values.get(0));

        assertEquals("binary-switch-value", details.channelId);
        assertEquals("Switch", details.itemType);
        assertEquals("Current value", details.label);
        assertEquals("Binary Switch", details.description);
        assertEquals(OnOffType.ON, details.state);
        assertEquals(false, details.writable);
        assertNull(details.statePattern);
        assertNull(details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore1Node6Channel3() throws IOException {
        Node node = getNodeFromStore("store_1.json", 6);

        ChannelDetails details = new ChannelDetails(6, node.values.get(2));

        assertEquals("multilevel-sensor-power", details.channelId);
        assertEquals("Number:Power", details.itemType);
        assertEquals("Power", details.label);
        assertEquals("Multilevel Sensor", details.description);
        assertEquals(new QuantityType<>(0, Units.WATT), details.state);
        assertEquals(false, details.writable);
        assertEquals(StateDescriptionFragmentBuilder.create().withPattern("%0.f %unit%").withReadOnly(true)
                .withStep(BigDecimal.valueOf(1)).build(), details.statePattern);
        assertEquals("W", details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore1Node6Channel4() throws IOException {
        Node node = getNodeFromStore("store_1.json", 6);

        ChannelDetails details = new ChannelDetails(6, node.values.get(3));

        assertEquals("meter-value", details.channelId);
        assertEquals("Number:Energy", details.itemType);
        assertEquals("Electric Consumption [kWh]", details.label);
        assertEquals("Meter", details.description);
        assertEquals(new QuantityType<>(881.95, Units.KILOWATT_HOUR), details.state);
        assertEquals(false, details.writable);
        assertEquals(StateDescriptionFragmentBuilder.create().withPattern("%0.f %unit%").withReadOnly(true)
                .withStep(BigDecimal.valueOf(1)).build(), details.statePattern);
        assertEquals("kWh", details.unitSymbol);
    }

    @Test
    public void testChannelDetailsStore1Node6Config1() throws IOException {
        Node node = getNodeFromStore("store_1.json", 6);

        ChannelDetails details = new ChannelDetails(6, node.values.get(6));

        assertEquals("configuration-function", details.channelId);
        assertEquals(Type.INTEGER, details.configType);
        assertEquals("Always On Function", details.label);
        assertEquals("Once activated, Wall Plug will keep a connected device ...", details.description);
        assertEquals(true, details.writable);
        assertEquals(BigDecimal.valueOf(0), details.statePattern.getMinimum());
        assertEquals(BigDecimal.valueOf(1), details.statePattern.getMaximum());
        assertEquals(BigDecimal.valueOf(1), details.statePattern.getStep());
        assertEquals("%0.d", details.statePattern.getPattern());
        assertEquals(new StateOption("0", "Activated"), details.statePattern.getOptions().get(0));
        assertEquals(new StateOption("1", "Inactive"), details.statePattern.getOptions().get(1));

        assertNull(details.unitSymbol);
        assertEquals(2, details.optionList.size());
        assertEquals("Inactive", details.optionList.get("1"));
    }

    @Test
    public void testChannelDetailsStore2Node2Channel66() throws IOException {
        Node node = getNodeFromStore("store_2.json", 2);

        ChannelDetails details = new ChannelDetails(1, node.values.get(66));

        assertEquals("multilevel-sensor-humidity", details.channelId);
        assertEquals("Number:Dimensionless", details.itemType);
        assertEquals("Humidity", details.label);
        assertEquals("Multilevel Sensor", details.description);
        assertEquals(new QuantityType<>(17.04, Units.PERCENT), details.state);
        assertEquals(false, details.writable);
        assertEquals(StateDescriptionFragmentBuilder.create().withPattern("%0.f %unit%").withReadOnly(true)
                .withStep(BigDecimal.valueOf(1)).build(), details.statePattern);
        assertEquals("%", details.unitSymbol);
    }
}
