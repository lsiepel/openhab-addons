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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.StateDescriptionFragmentBuilder;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ChannelDetailsTest {

    private ArrayList<Node> nodes = new ArrayList<>();

    @BeforeEach
    public void setup() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("initial.json", ResultMessage.class);
        nodes = resultMessage.result.state.nodes;
    }

    @Test
    public void testChannelDetailsForNode3Channel1() throws IOException {
        Node node = nodes.stream().filter(f -> f.nodeId == 3).findAny().orElse(null);

        ChannelDetails details = new ChannelDetails(3, node.values.get(0));

        assertEquals("binary-switch-value", details.channelId);
        assertEquals("Binary Switch", details.description);
        assertEquals("Switch", details.itemType);
        assertEquals("Current value", details.label);
        assertEquals("Binary Switch", details.description);
        assertEquals(OnOffType.ON, details.state);
        assertEquals(false, details.writable);
        assertNull(details.statePattern);
        assertNull(details.unit);
    }

    @Test
    public void testChannelDetailsForNode6Channel1() throws IOException {
        Node node = nodes.stream().filter(f -> f.nodeId == 6).findAny().orElse(null);

        ChannelDetails details = new ChannelDetails(6, node.values.get(0));

        assertEquals("binary-switch-value", details.channelId);
        assertEquals("Binary Switch", details.description);
        assertEquals("Switch", details.itemType);
        assertEquals("Current value", details.label);
        assertEquals("Binary Switch", details.description);
        assertEquals(OnOffType.ON, details.state);
        assertEquals(false, details.writable);
        assertNull(details.statePattern);
        assertNull(details.unit);
    }

    @Test
    public void parseUnits() {

        // String x = new QuantityType<>(1,
        // Units.WATT).getUnit().getDimension().getBaseDimensions().getClass().getName();
        // assertEquals("x", x);
        /*
         * DefaultQuantityFactory.getInstance(null).create(null, null)
         * 
         * Dimension.class
         * Unit<?> unit = Units.getInstance().getUnit("W");
         * unit.getClass;
         * var dim = Units.WATT.getSystemUnit().getDimension();
         * var dim2 = Units.WATT.getSystemUnit().getName();
         * Units.WATT.getSystemUnit().
         * assertEquals(Units.WATT.getSystemUnit().getDimension(), unit.getSystemUnit().getDimension());
         * assertEquals(Units.WATT.getSystemUnit().getName() , unit.getSystemUnit().getName());
         * assertEquals(Units.WATT, unit);
         */
    }

    @Test
    public void testChannelDetailsForNode3Channel3() throws IOException {
        Node node = nodes.stream().filter(f -> f.nodeId == 6).findAny().orElse(null);

        ChannelDetails details = new ChannelDetails(6, node.values.get(2));

        assertEquals("multilevel-sensor-power-w", details.channelId);
        assertEquals("Multilevel Sensor", details.description);
        assertEquals("Number:Power", details.itemType);
        assertEquals("Power", details.label);
        assertEquals("Multilevel Sensor", details.description);
        assertEquals(new QuantityType<>(0, Units.WATT), details.state);
        assertEquals(false, details.writable);
        assertEquals(StateDescriptionFragmentBuilder.create().withPattern("%0.f %unit%").withReadOnly(true)
                .withStep(BigDecimal.valueOf(1)).build(), details.statePattern);
        assertEquals("W", details.unit);
    }

    @Test
    public void testChannelDetailsForNode6Channel4() throws IOException {
        Node node = nodes.stream().filter(f -> f.nodeId == 6).findAny().orElse(null);

        ChannelDetails details = new ChannelDetails(6, node.values.get(3));

        assertEquals("meter-value-kwh", details.channelId);
        assertEquals("Meter", details.description);
        assertEquals("Number:Energy", details.itemType);
        assertEquals("Electric Consumption [kWh]", details.label);
        assertEquals("Meter", details.description);
        assertEquals(new QuantityType<>(881.95, Units.KILOWATT_HOUR), details.state);
        assertEquals(false, details.writable);
        assertEquals(StateDescriptionFragmentBuilder.create().withPattern("%0.f %unit%").withReadOnly(true)
                .withStep(BigDecimal.valueOf(1)).build(), details.statePattern);
        assertEquals("kWh", details.unit);
    }
}
