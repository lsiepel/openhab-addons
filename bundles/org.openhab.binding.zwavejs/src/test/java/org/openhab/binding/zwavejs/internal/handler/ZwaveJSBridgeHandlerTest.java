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
import org.openhab.binding.zwavejs.internal.handler.mock.ZwaveJSBridgeHandlerMock;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.net.http.WebSocketFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSBridgeHandlerTest {

    private static final Configuration CONFIG = createConfig(true);
    private static final Configuration BAD_CONFIG = createConfig(false);

    private static Configuration createConfig(boolean returnValid) {
        final Configuration config = new Configuration();
        if (returnValid) {
            config.put("id", "3");
        }
        return config;
    }

    private static Bridge mockBridge(boolean withConfiguration) {
        final Bridge bridge = mock(Bridge.class);
        when(bridge.getUID()).thenReturn(new ThingUID(ZwaveJSBindingConstants.BINDING_ID, "test-bridge"));
        when(bridge.getConfiguration()).thenReturn(withConfiguration ? CONFIG : BAD_CONFIG);

        return bridge;
    }

    private static ZwaveJSBridgeHandlerMock createAndInitHandler(final ThingHandlerCallback callback,
            final Bridge thing) {
        WebSocketFactory wsFactory = mock(WebSocketFactory.class);
        final ZwaveJSBridgeHandlerMock handler = spy(new ZwaveJSBridgeHandlerMock(thing, wsFactory));

        handler.setCallback(callback);
        handler.initialize();

        return handler;
    }

    @Test
    public void testInvalidConfiguration() {
        final Bridge thing = mockBridge(false);
        final ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        final ZwaveJSBridgeHandler handler = createAndInitHandler(callback, thing);

        try {
            verify(callback).statusUpdated(eq(thing), argThat(arg -> arg.getStatus().equals(ThingStatus.OFFLINE)
                    && arg.getStatusDetail().equals(ThingStatusDetail.CONFIGURATION_ERROR)));
        } finally {
            handler.dispose();
        }
    }
}
