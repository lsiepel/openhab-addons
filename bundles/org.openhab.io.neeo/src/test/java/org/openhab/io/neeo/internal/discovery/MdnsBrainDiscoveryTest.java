/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.io.neeo.internal.discovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.junit.jupiter.api.Test;
import org.openhab.io.neeo.internal.ServiceContext;

/**
 * Tests the destinations of manual NEEO brain discovery.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
class MdnsBrainDiscoveryTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final ServiceContext context = mock(ServiceContext.class);
    private final MdnsBrainDiscovery discovery = new MdnsBrainDiscovery(context, httpClient);

    @Test
    void probesTheBrainWithoutFollowingRedirects() throws Exception {
        String systemInfoUrl = "http://192.0.2.11:3000/v1/systeminfo";
        Request request = mock(Request.class);
        when(httpClient.newRequest(systemInfoUrl)).thenReturn(request);
        when(request.send()).thenThrow(new TimeoutException());

        assertFalse(discovery.addDiscovered("192.0.2.11"));

        verify(httpClient).newRequest(systemInfoUrl);
        verify(request).followRedirects(false);
        verify(request).send();
    }
}
