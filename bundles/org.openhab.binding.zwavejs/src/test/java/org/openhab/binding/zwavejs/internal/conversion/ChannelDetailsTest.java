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
import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.core.types.UnDefType;

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
    public void testGenerateDetailsForNode3FirstChannel() throws IOException {
        Node node = nodes.stream().filter(f -> f.nodeId == 3).findAny().orElse(null);

        ChannelDetails details = new ChannelDetails(3, node.values.get(0));

        assertEquals("binary-switch-value", details.channelId);
        assertEquals("Binary Switch", details.description);
        assertEquals("Switch", details.itemType);
        assertEquals("Current value", details.label);
        assertEquals("Binary Switch", details.description);
        assertEquals(UnDefType.UNDEF, details.state);
        assertEquals(false, details.readOnly);
        assertNull(details.statePattern);
        assertNull(details.unit);
    }

    @Test
    public void testGenerateDetailsForNode6FirstChannel() throws IOException {
        Node node = nodes.stream().filter(f -> f.nodeId == 3).findAny().orElse(null);

        ChannelDetails details = new ChannelDetails(6, node.values.get(0));

        assertEquals("binary-switch-value", details.channelId);
        assertEquals("Binary Switch", details.description);
        assertEquals("Switch", details.itemType);
        assertEquals("Current value", details.label);
        assertEquals("Binary Switch", details.description);
        assertEquals(UnDefType.UNDEF, details.state);
        assertEquals(false, details.readOnly);
        assertNull(details.statePattern);
        assertNull(details.unit);
    }
}
