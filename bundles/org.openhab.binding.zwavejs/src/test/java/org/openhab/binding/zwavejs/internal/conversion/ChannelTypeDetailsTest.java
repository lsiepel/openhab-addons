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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.core.thing.type.ChannelType;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ChannelTypeDetailsTest {

    @Nullable
    ChannelTypeUtils provider;

    @BeforeEach
    public void setup() {
        provider = new ChannelTypeUtils();
    }

    @Test
    public void testGenerateChannelTypeForNode3() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("initial.json", ResultMessage.class);
        Node node = resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == 3).findAny().orElse(null);

        List<ChannelType> channelTypeList = new ArrayList<>();
        for (Value value : node.values) {
            ChannelType channelType = provider.generateChannelType(new ChannelDetails(3, value));
            if (channelType != null) {
                channelTypeList.add(channelType);
            }
        }

        assertEquals(7, channelTypeList.size());
    }

    @Test
    public void testGenerateChannelTypeNode6() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("initial.json", ResultMessage.class);
        Node node = resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == 6).findAny().orElse(null);

        List<ChannelType> channelTypeList = new ArrayList<>();
        for (Value value : node.values) {
            ChannelType channelType = provider.generateChannelType(new ChannelDetails(3, value));
            if (channelType != null) {
                channelTypeList.add(channelType);
            }
        }

        assertEquals(6, channelTypeList.size());
    }
}
