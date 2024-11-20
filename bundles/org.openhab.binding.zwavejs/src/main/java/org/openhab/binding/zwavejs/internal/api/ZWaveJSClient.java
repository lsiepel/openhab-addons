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

package org.openhab.binding.zwavejs.internal.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import javax.naming.CommunicationException;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZWaveJSClient {

    private final Logger logger = LoggerFactory.getLogger(ZWaveJSClient.class);
    private final WebSocketClient wsClient;

    public ZWaveJSClient(WebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    public void start(String URI) throws CommunicationException, InterruptedException {
        logger.debug("Connecting to Z-Wave JS Webservice");
        try {
            wsClient.connect(this, new URI(URI)).get();
            wsClient.addEventListener(null);
        } catch (ExecutionException | IOException | URISyntaxException e) {
            throw new CommunicationException(e.getMessage());
        }
    }

    private void onEvent() {
        
    }
}
