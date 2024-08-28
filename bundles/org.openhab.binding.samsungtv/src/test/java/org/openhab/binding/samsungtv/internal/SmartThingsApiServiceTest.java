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
package org.openhab.binding.samsungtv.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openhab.binding.samsungtv.internal.SamsungTvBindingConstants;
import org.openhab.binding.samsungtv.internal.config.SamsungTvConfiguration;
import org.openhab.binding.samsungtv.internal.dto.DataUtil;
import org.openhab.binding.samsungtv.internal.handler.SamsungTvHandler;
import org.openhab.binding.samsungtv.internal.service.SmartThingsApiService;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;

/**
 * Tests for the HomeWizard Handler
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class SmartThingsApiServiceTest {

    private static final Configuration CONFIG = createConfig();

    private static Configuration createConfig() {
        final Configuration config = new Configuration();
        return config;
    }

    private static Thing mockThing() {
        final Thing thing = mock(Thing.class);
        when(thing.getUID())
                .thenReturn(new ThingUID(SamsungTvBindingConstants.SAMSUNG_TV_THING_TYPE, "samsungtv-test-thing"));
        when(thing.getConfiguration()).thenReturn(CONFIG);

        final List<Channel> channelList = Arrays.asList(mockChannel(thing.getUID(), SamsungTvBindingConstants.CHANNEL));

        when(thing.getChannels()).thenReturn(channelList);
        return thing;
    }

    private static Channel mockChannel(final ThingUID thingId, final String channelId) {
        final Channel channel = Mockito.mock(Channel.class);
        when(channel.getUID()).thenReturn(new ChannelUID(thingId, channelId));
        return channel;
    }

    private static SmartThingsApiService createAndInitService(final ThingHandlerCallback callback, final Thing thing) {
        final SamsungTvHandler handler = mock(SamsungTvHandler.class);
        handler.configuration = new SamsungTvConfiguration();
        handler.configuration.smartThingsApiKey = "test";
        final SmartThingsApiService service = spy(new SmartThingsApiService("fake-host", handler));
        
        doReturn(true).when(service).checkConnection();
        
        try 
        {
            doReturn(Optional.of(DataUtil.fromFile("response.json"))).when(service).sendUrl(eq(HttpMethod.GET), any(), eq(null));
        } catch (IOException e) {
            assertFalse(true);
        }

        handler.setCallback(callback);
        handler.initialize();
        return service;
    }

    private static State getState(final int input) {
        return new DecimalType(input);
    }

    private static State getState(final int input, Unit<?> unit) {
        return new QuantityType<>(input, unit);
    }

    private static State getState(final double input, Unit<?> unit) {
        return new QuantityType<>(input, unit);
    }

    @Test
    public void testUpdateChannels() {
        final Thing thing = mockThing();
        final ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        final SmartThingsApiService service = createAndInitService(callback, thing);

        service.handleCommand(SamsungTvBindingConstants.CHANNEL, RefreshType.REFRESH);

       // verify(callback).statusUpdated(eq(thing), argThat(arg -> arg.getStatus().equals(ThingStatus.UNKNOWN)));
       // verify(callback).statusUpdated(eq(thing), argThat(arg -> arg.getStatus().equals(ThingStatus.ONLINE)));

        verify(callback).stateUpdated(new ChannelUID(thing.getUID(), SamsungTvBindingConstants.CHANNEL),
                getState(567.0, Units.AMPERE));
    }
}
