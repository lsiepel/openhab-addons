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
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.DataUtil;
import org.openhab.binding.zwavejs.internal.api.dto.messages.EventMessage;
import org.openhab.binding.zwavejs.internal.handler.mock.ZwaveJSNodeHandlerMock;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandlerCallback;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSNodeHandlerTest {

    @Test
    public void testInvalidConfiguration() {
        final Thing thing = ZwaveJSNodeHandlerMock.mockThing(0);
        final ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        final ZwaveJSNodeHandler handler = ZwaveJSNodeHandlerMock.createAndInitHandler(callback, thing);

        try {
            verify(callback).statusUpdated(eq(thing), argThat(arg -> arg.getStatus().equals(ThingStatus.OFFLINE)
                    && arg.getStatusDetail().equals(ThingStatusDetail.CONFIGURATION_ERROR)));
        } finally {
            handler.dispose();
        }
    }

    @Test
    public void testNode3ChannelCreation() {
        final Thing thing = ZwaveJSNodeHandlerMock.mockThing(3);
        final ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        final ZwaveJSNodeHandler handler = ZwaveJSNodeHandlerMock.createAndInitHandler(callback, thing);

        try {
            verify(callback).statusUpdated(eq(thing), argThat(arg -> arg.getStatus().equals(ThingStatus.UNKNOWN)));
        } finally {
            handler.dispose();
        }
    }

    @Test
    public void testNode6ChannelCreation() {
        final Thing thing = ZwaveJSNodeHandlerMock.mockThing(6);
        final ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        final ZwaveJSNodeHandler handler = ZwaveJSNodeHandlerMock.createAndInitHandler(callback, thing);

        // handler.getThing().getChannels()

        try {
            verify(callback).statusUpdated(eq(thing), argThat(arg -> arg.getStatus().equals(ThingStatus.UNKNOWN)));
            verify(callback).statusUpdated(argThat(arg -> arg.getUID().equals(thing.getUID())),
                    argThat(arg -> arg.getStatus().equals(ThingStatus.ONLINE)));
        } finally {
            handler.dispose();
        }
    }

    @Test
    public void testNode6ChannelUpdate() throws IOException {
        final Thing thing = ZwaveJSNodeHandlerMock.mockThing(6);
        final ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        final ZwaveJSNodeHandler handler = ZwaveJSNodeHandlerMock.createAndInitHandler(callback, thing);

        EventMessage eventMessage = DataUtil.fromJson("event_node_6.json", EventMessage.class);
        handler.onNodeStateChanged(eventMessage.event);

        ChannelUID channelid = new ChannelUID("zwavejs::test-thing:binary-switch-value");
        try {
            verify(callback).statusUpdated(eq(thing), argThat(arg -> arg.getStatus().equals(ThingStatus.UNKNOWN)));
            verify(callback).statusUpdated(argThat(arg -> arg.getUID().equals(thing.getUID())),
                    argThat(arg -> arg.getStatus().equals(ThingStatus.ONLINE)));
            verify(callback).stateUpdated(eq(channelid), eq(OnOffType.OFF));
        } finally {
            handler.dispose();
        }
    }
}
