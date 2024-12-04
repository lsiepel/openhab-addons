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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants.BINDING_ID;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSChannelTypeProviderTest {
    @Nullable
    ZwaveJSChannelTypeProvider provider;

    @BeforeEach
    public void setup() {
        provider = new ZwaveJSChannelTypeProvider();
    }

    @Test
    public void testGenerateChannelTypeForNode3() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("initial.json", ResultMessage.class);
        Node node = resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == 3).findAny().orElse(null);

        for (Value value : node.values) {
            provider.generateChannelType(value);
        }
        ChannelType type = provider.getChannelType(new ChannelTypeUID(BINDING_ID, "01d92c4e676264dd7cffd7175e194505"),
                null);

        assertNotNull(type);
    }

    @Test
    public void testGenerateChannelTypeNode6() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("initial.json", ResultMessage.class);
        Node node = resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == 6).findAny().orElse(null);

        for (Value value : node.values) {
            provider.generateChannelType(value);
        }

        assertNotNull(
                provider.getChannelType(new ChannelTypeUID(BINDING_ID, "01d92c4e676264dd7cffd7175e194505"), null));
    }
}
