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
package org.openhab.binding.zwavejs.internal.handler.mock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants.CONFIG_NODE_ID;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.binding.zwavejs.internal.handler.ZwaveJSNodeHandler;
import org.openhab.binding.zwavejs.internal.type.ZwaveJSChannelTypeProvider;
import org.openhab.binding.zwavejs.internal.type.ZwaveJSConfigDescriptionProvider;
import org.openhab.binding.zwavejs.internal.type.ZwaveJSTypeGenerator;
import org.openhab.binding.zwavejs.internal.type.ZwaveJSTypeGeneratorImpl;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelType;

/**
 * The {@link ZwaveJSNodeHandlerMock} is responsible for mocking {@link ZwaveJSNodeHandler}
 * 
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSNodeHandlerMock extends ZwaveJSNodeHandler {

    public static Configuration createConfig(int id) {
        final Configuration config = new Configuration();
        if (id > 0) {
            config.put(CONFIG_NODE_ID, id);
        }
        return config;
    }

    public static Thing mockThing(int id) {
        final Thing thing = mock(Thing.class);
        when(thing.getUID()).thenReturn(new ThingUID(ZwaveJSBindingConstants.BINDING_ID, "test-thing"));
        when(thing.getBridgeUID()).thenReturn(new ThingUID(ZwaveJSBindingConstants.BINDING_ID, "test-bridge"));
        when(thing.getConfiguration()).thenReturn(createConfig(id));

        return thing;
    }

    public static ZwaveJSNodeHandlerMock createAndInitHandler(final ThingHandlerCallback callback, final Thing thing) {
        ZwaveJSChannelTypeProvider channelTypeProvider = mock(ZwaveJSChannelTypeProvider.class);
        ZwaveJSConfigDescriptionProvider configDescriptionProvider = mock(ZwaveJSConfigDescriptionProvider.class);
        ThingRegistry thingRegistry = mock(ThingRegistry.class);
        doNothing().when(channelTypeProvider).addChannelType(any(ChannelType.class));

        ZwaveJSTypeGenerator typeGenerator = new ZwaveJSTypeGeneratorImpl(channelTypeProvider,
                configDescriptionProvider, thingRegistry);

        final ZwaveJSNodeHandlerMock handler = spy(new ZwaveJSNodeHandlerMock(thing, typeGenerator));

        handler.setCallback(callback);
        handler.initialize();

        return handler;
    }

    public ZwaveJSNodeHandlerMock(Thing thing, ZwaveJSTypeGenerator typeGenerator) {
        super(thing, typeGenerator);

        executorService = Mockito.mock(ScheduledExecutorService.class);
        doAnswer((InvocationOnMock invocation) -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        doAnswer((InvocationOnMock invocation) -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
    }

    public boolean isLinked(String channelId) {
        return true;
    }

    public void updateThing(Thing thing) {
        super.updateThing(thing);
    }

    public ThingBuilder editThing() {
        return super.editThing();
    }

    public @Nullable Bridge getBridge() {
        final Bridge bridge = ZwaveJSBridgeHandlerMock.mockBridge("localhost");
        final ZwaveJSBridgeHandlerMock handler = mock(ZwaveJSBridgeHandlerMock.class);
        doNothing().when(handler).initialize();
        ResultMessage resultMessage;
        try {
            resultMessage = DataUtil.fromJson("initial.json", ResultMessage.class);
        } catch (IOException e) {
            return null;
        }

        doAnswer(i -> {
            int nodeId = i.getArgument(0);
            return resultMessage.result.state.nodes.stream().filter(f -> f.nodeId == nodeId).findAny().orElse(null);
        }).when(handler).requestNodeDetails(anyInt());
        when(bridge.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(bridge.getHandler()).thenReturn(handler);
        when(handler.registerNodeListener(any())).thenReturn(true);
        return bridge;
    }
}
