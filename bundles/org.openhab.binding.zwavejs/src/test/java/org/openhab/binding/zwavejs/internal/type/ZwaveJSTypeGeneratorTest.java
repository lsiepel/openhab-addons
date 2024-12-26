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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants.BINDING_ID;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.binding.zwavejs.internal.handler.mock.ZwaveJSChannelTypeInMemmoryProvider;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.slf4j.LoggerFactory;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSTypeGeneratorTest {

    @Nullable
    ZwaveJSTypeGenerator provider;
    ZwaveJSChannelTypeProvider channelTypeProvider = new ZwaveJSChannelTypeInMemmoryProvider();
    ZwaveJSConfigDescriptionProvider configDescriptionProvider = new ZwaveJSConfigDescriptionProviderImpl();

    @BeforeEach
    public void setup() {
        ThingRegistry thingRegistry = mock(ThingRegistry.class);
        provider = new ZwaveJSTypeGeneratorImpl(channelTypeProvider, configDescriptionProvider, thingRegistry);
    }

    @Test
    public void testGenerateChannelTypeForNode3() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("store_1.json", ResultMessage.class);
        Node node = resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == 3).findAny().orElse(null);

        ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));

        assertEquals(5, results.channels.values().stream().map(f -> f.getChannelTypeUID()).distinct().count());
    }

    @Test
    public void testGenerateChannelTypeNode6() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("store_1.json", ResultMessage.class);
        Node node = resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == 6).findAny().orElse(null);

        ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));

        assertEquals(4, results.channels.values().stream().map(f -> f.getChannelTypeUID()).distinct().count());
    }

    @Disabled
    @Test
    public void testGeneratedChannelUIDStore2AllNodes() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("store_2.json", ResultMessage.class);
        int counter = 0;
        for (Node node : resultMessage.result.state.nodes) {
            ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                    .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));

            for (Channel channel : results.channels.values()) {
                LoggerFactory.getLogger(ZwaveJSTypeGeneratorTest.class).error("Node {} {}", node.nodeId,
                        channel.getUID());
            }
        }

        assertEquals(0, counter);
    }

    @Test
    public void testGenerateChannelStore2Node2() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("store_2.json", ResultMessage.class);
        Node node = resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == 2).findAny().orElse(null);

        ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));
        ;
        /*
         * TODO numbers dont add up
         * Currently it should result in 15 channels
         * - 49 are CC `configuration`. I moddeled as Thing configuration similar to the current openHAB zwave binding.
         * - 03 are CC `Manufacturer Specific` Filtered out seem not usable, but maybe im missing something.
         * - 03 are CC `Version`, same as previous.
         * - 09 are CC `Meter`, classify as Channel (6 in the resultset?)
         * - 04 are CC `Multilevel Sensor`, classify as Channel
         * - 02 are CC `Battery`, classify as Channel
         */
        assertEquals(12, results.channels.size());
    }

    @Test
    public void testGenerateChannelTypeStore1Node6Label() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("store_1.json", ResultMessage.class);
        Map<String, Channel> channels = new HashMap<>();

        for (Node node : resultMessage.result.state.nodes) {
            ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                    .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));
            channels.putAll(results.channels);
            if (node.nodeId == 6) {
                Channel channel = results.channels.get("meter-reset");
                assertEquals("Reset", channel.getLabel());
                assertNull(channel.getDescription());
            }
        }
        ;

        assertEquals(6, channels.values().stream().map(f -> f.getChannelTypeUID()).distinct().count());
    }

    @Test
    public void testGenerateChannelTypeStore1Node6WriteProperty() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("store_1.json", ResultMessage.class);
        Map<String, Channel> channels = new HashMap<>();

        for (Node node : resultMessage.result.state.nodes) {
            ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                    .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));
            channels.putAll(results.channels);
            if (node.nodeId == 6) {
                Channel channel = results.channels.get("binary-switch-value");

                assertEquals("targetValue",
                        channel.getConfiguration().get(ZwaveJSBindingConstants.CONFIG_CHANNEL_WRITE_PROPERTY));
            }
        }
        ;
    }

    @Test
    public void testGenerateChannelTypeStore1Node6ChannelType() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("store_1.json", ResultMessage.class);
        Map<String, Channel> channels = new HashMap<>();

        for (Node node : resultMessage.result.state.nodes) {
            ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                    .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));
            channels.putAll(results.channels);
            if (node.nodeId == 6) {
                Channel channel = results.channels.get("meter-value");
                ChannelTypeUID uid = channel.getChannelTypeUID();
                ChannelType type = channelTypeProvider.getChannelType(uid, null);

                assertNotNull(type);
            }
        }
        ;
    }

    @Test
    public void testGenerateChannelTypeStore2AllNodes() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("store_2.json", ResultMessage.class);
        Map<String, Channel> channels = new HashMap<>();

        for (Node node : resultMessage.result.state.nodes) {
            ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                    .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));
            channels.putAll(results.channels);
        }
        ;

        assertEquals(23, channels.values().stream().map(f -> f.getChannelTypeUID()).distinct().count());
    }

    @Test
    public void testGenerateChannelTypeStore3AllNodes() throws IOException {
        ResultMessage resultMessage = DataUtil.fromJson("store_3.json", ResultMessage.class);
        Map<String, Channel> channels = new HashMap<>();

        for (Node node : resultMessage.result.state.nodes) {
            ZwaveJSTypeGeneratorResult results = Objects.requireNonNull(provider)
                    .generate(new ThingUID(BINDING_ID, "test-thing"), Objects.requireNonNull(node));
            channels.putAll(results.channels);
        }
        ;

        assertEquals(36, channels.values().stream().map(f -> f.getChannelTypeUID()).distinct().count());
    }
}
