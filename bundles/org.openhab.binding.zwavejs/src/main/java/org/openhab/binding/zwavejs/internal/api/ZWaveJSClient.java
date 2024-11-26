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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;

import javax.naming.CommunicationException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.zwavejs.internal.handler.ZwaveEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author L. Siepel - Initial contribution
 */
@NonNullByDefault
public class ZWaveJSClient implements WebSocketListener {

    private final Logger logger = LoggerFactory.getLogger(ZWaveJSClient.class);
    private final WebSocketClient wsClient;
    private @Nullable volatile Session session;
    private Set<ZwaveEventListener> listeners = new CopyOnWriteArraySet<>();
    private @Nullable Future<?> sessionFuture;

    public ZWaveJSClient(WebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    public void start(String URI) throws CommunicationException, InterruptedException {
        logger.debug("Connecting to Z-Wave JS Webservice");
        try {
            sessionFuture = wsClient.connect(this, new URI(URI));
        } catch (IOException | URISyntaxException e) {
            throw new CommunicationException(e.getMessage());
        }
    }

    private void stop() {
        logger.debug("Disconnecting from Z-Wave JS Webservice");
        Session localSession = this.session;
        if (localSession != null) {
            try {
                localSession.close(StatusCode.NORMAL, "Binding shutdown");
            } catch (Exception e) {
                logger.debug("Error while closing websocket communication: {} ({})", e.getClass().getName(),
                        e.getMessage());
            }
            session = null;
        }

        Future<?> localSessionFuture = sessionFuture;
        if (localSessionFuture != null) {
            if (!localSessionFuture.isDone()) {
                localSessionFuture.cancel(true);
            }
        }
    }

    public void addEventListener(ZwaveEventListener listener) {
        // TODO use some kind of id as part of the listeners to only send event to listeners that need the event
        // The listener can provide some kind of id it listens to ?!
        listeners.add(listener);
    }

    public void removeEventListener(ZwaveEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onWebSocketClose(int statusCode, @NonNullByDefault({}) String reason) {
        logger.debug("onClose({}, '{}')", statusCode, reason);

        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, reason);
    }

    @Override
    public void onWebSocketConnect(@NonNullByDefault({}) Session session) {
        logger.debug("onWebSocketConnect('{}')", session);
        this.session = session;
        // updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void onWebSocketError(@NonNullByDefault({}) Throwable cause) {
        Throwable localThrowable = (cause != null) ? cause
                : new IllegalStateException("Null Exception passed to onWebSocketError");
        logger.warn("Error during websocket communication: {}", localThrowable.getMessage(), localThrowable);

        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, localThrowable.getMessage());

        Session localSession = session;
        if (localSession != null) {
            localSession.close(StatusCode.SERVER_ERROR, "Failure: " + localThrowable.getMessage());
            session = null;
        }
    }

    @Override
    public void onWebSocketBinary(@NonNullByDefault({}) byte[] payload, int offset, int len) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onWebSocketBinary'");
    }

    @Override
    public void onWebSocketText(@NonNullByDefault({}) String message) {
        logger.info("onWebSocketText('{}')", message);
        // TODO use some kind of id as part of the listeners to only send event to listeners that need the event
        try {
            for (ZwaveEventListener listener : listeners) {
                listener.onEvent(message);
            }
        } catch (Exception e) {
            logger.warn("Error invoking event listener", e);
        }
    }
}
