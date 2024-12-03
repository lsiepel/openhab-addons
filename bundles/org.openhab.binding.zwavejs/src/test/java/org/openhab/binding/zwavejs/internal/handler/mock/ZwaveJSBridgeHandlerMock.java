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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.openhab.binding.zwavejs.internal.handler.ZwaveJSBridgeHandler;
import org.openhab.core.io.net.http.WebSocketFactory;
import org.openhab.core.thing.Bridge;

/**
 * The {@link ZwaveJSBridgeHandlerMock} is responsible for mocking {@link ZwaveJSBridgeHandler}
 * 
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSBridgeHandlerMock extends ZwaveJSBridgeHandler {

    public ZwaveJSBridgeHandlerMock(Bridge bridge, WebSocketFactory wsFactory) {
        super(bridge, wsFactory);

        executorService = Mockito.mock(ScheduledExecutorService.class);
        doAnswer((InvocationOnMock invocation) -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }
}
