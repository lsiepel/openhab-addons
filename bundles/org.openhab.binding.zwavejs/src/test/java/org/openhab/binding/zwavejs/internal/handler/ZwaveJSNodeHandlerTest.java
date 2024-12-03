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
package org.openhab.binding.zwavejs.internal.handler;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants;
import org.openhab.binding.zwavejs.internal.conversion.ZwaveJSChannelTypeProvider;
import org.openhab.binding.zwavejs.internal.handler.mock.ZwaveJSNodeHandlerMock;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSNodeHandlerTest {

    private static final Configuration CONFIG = createConfig(true);
    private static final Configuration BAD_CONFIG = createConfig(false);

    private static Configuration createConfig(boolean returnValid) {
        final Configuration config = new Configuration();
        if (returnValid) {
            config.put("id", "3");
        }
        return config;
    }

    private static Thing mockThing(boolean withConfiguration) {
        final Thing thing = mock(Thing.class);
        when(thing.getUID()).thenReturn(new ThingUID(ZwaveJSBindingConstants.BINDING_ID, "test-thing"));
        when(thing.getConfiguration()).thenReturn(withConfiguration ? CONFIG : BAD_CONFIG);

        return thing;
    }

    private static ZwaveJSNodeHandlerMock createAndInitHandler(final ThingHandlerCallback callback, final Thing thing) {
        ZwaveJSChannelTypeProvider channelTypeProvider = mock(ZwaveJSChannelTypeProvider.class);
        final ZwaveJSNodeHandlerMock handler = spy(new ZwaveJSNodeHandlerMock(thing, channelTypeProvider));

        handler.setCallback(callback);
        handler.initialize();

        return handler;
    }

    @Test
    public void testInvalidConfiguration() {
        final Thing thing = mockThing(false);
        final ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        final ZwaveJSNodeHandler handler = createAndInitHandler(callback, thing);

        try {
            verify(callback).statusUpdated(eq(thing), argThat(arg -> arg.getStatus().equals(ThingStatus.OFFLINE)
                    && arg.getStatusDetail().equals(ThingStatusDetail.CONFIGURATION_ERROR)));
        } finally {
            handler.dispose();
        }
    }
}
