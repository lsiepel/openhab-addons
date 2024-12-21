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
package org.openhab.binding.zwavejs.internal.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants.BINDING_ID;

import java.io.IOException;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSTypeGeneratorTest {

    @Nullable
    ZwaveJSTypeGenerator provider;

    @BeforeEach
    public void setup() {
        ZwaveJSChannelTypeProvider channelTypeProvider = mock(ZwaveJSChannelTypeProviderImpl.class);
        ZwaveJSConfigDescriptionProvider configDescriptionProvider = mock(ZwaveJSConfigDescriptionProviderImpl.class);
        ThingRegistry thingRegistry = mock(ThingRegistry.class);
        provider = new ZwaveJSTypeGeneratorImpl(channelTypeProvider, configDescriptionProvider, thingRegistry);
    }

    @Test
    public void testGenerateChannelTypeForNode3() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("initial.json", ResultMessage.class);
        Node node = resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == 3).findAny().orElse(null);

        ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));

        assertEquals(2, results.channels.values().stream().map(f -> f.getChannelTypeUID()).distinct().count());
    }

    @Test
    public void testGenerateChannelTypeNode6() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("initial.json", ResultMessage.class);
        Node node = resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == 6).findAny().orElse(null);

        ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));

        assertEquals(2, results.channels.values().stream().map(f -> f.getChannelTypeUID()).distinct().count());
    }
}
