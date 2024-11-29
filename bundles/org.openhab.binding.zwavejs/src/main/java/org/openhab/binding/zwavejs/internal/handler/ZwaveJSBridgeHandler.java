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

import static org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.CommunicationException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants;
import org.openhab.binding.zwavejs.internal.api.ZWaveJSClient;
import org.openhab.binding.zwavejs.internal.api.dto.Commands.ListeningCommand;
import org.openhab.binding.zwavejs.internal.api.dto.Messages.BaseMessage;
import org.openhab.binding.zwavejs.internal.api.dto.Messages.ResultMessage;
import org.openhab.binding.zwavejs.internal.api.dto.Messages.VersionMessage;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.config.ZwaveJSBridgeConfiguration;
import org.openhab.binding.zwavejs.internal.discovery.NodeDiscoveryService;
import org.openhab.core.io.net.http.WebSocketFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZwaveJSNodeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author L. Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSBridgeHandler extends BaseBridgeHandler implements ZwaveEventListener {

    private final Logger logger = LoggerFactory.getLogger(ZwaveJSBridgeHandler.class);
    private final Map<Integer, NodeListener> nodeListeners = new ConcurrentHashMap<>();
    private final Map<Integer, Node> lastNodeStates = new ConcurrentHashMap<>();

    private @Nullable NodeDiscoveryService discoveryService;
    private @Nullable ZwaveJSBridgeConfiguration config;
    private ZWaveJSClient client;

    public ZwaveJSBridgeHandler(Bridge bridge, WebSocketFactory wsFactory) {
        super(bridge);
        this.client = new ZWaveJSClient(wsFactory.getCommonWebSocketClient());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_1.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        ZwaveJSBridgeConfiguration config = this.config = getConfigAs(ZwaveJSBridgeConfiguration.class);

        if (!config.isValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Hostname or port invalid");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> {
            try {
                client.start("ws://" + config.hostname + ":" + config.port);
                client.addEventListener(this);
                updateStatus(ThingStatus.ONLINE);
            } catch (CommunicationException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } catch (InterruptedException e) {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    @Override
    public void onEvent(BaseMessage message) {
        if (message instanceof VersionMessage event) {
            Map<String, String> properties = new HashMap<>();
            properties.put(ZwaveJSBindingConstants.PROPERTY_DRIVER_VERSION, event.driverVersion);
            properties.put(ZwaveJSBindingConstants.PROPERTY_SERVER_VERSION, event.serverVersion);
            properties.put(ZwaveJSBindingConstants.PROPERTY_SCHEMA_MIN, String.valueOf(event.minSchemaVersion));
            properties.put(ZwaveJSBindingConstants.PROPERTY_SCHEMA_MAX, String.valueOf(event.maxSchemaVersion));
            properties.put(ZwaveJSBindingConstants.PROPERTY_HOME_ID, String.valueOf(event.homeId));
            this.getThing().setProperties(properties);
        }
        if (message instanceof ResultMessage result) {
            if (result.result.state == null) {
                return;
            }
            logger.info("Bridge received event with, id: {}, type: {}, holding {} nodes", result.messageId, result.type,
                    result.result.state.nodes.size());

            Map<Integer, Node> lastNodeStatesCopy = new HashMap<>(lastNodeStates);
            final NodeDiscoveryService discovery = discoveryService;
            for (Node node : result.result.state.nodes) {
                logger.info("Found node, id: {} label: {}", node.nodeId, node.label);

                final int nodeId = node.nodeId;
                final NodeListener nodeListener = nodeListeners.get(nodeId);
                if (nodeListener == null) {
                    logger.info("Z-Wave node '{}' added", nodeId);

                    if (discovery != null && !lastNodeStatesCopy.containsKey(nodeId)) {
                        discovery.addNodeDiscovery(node);
                    }

                    lastNodeStates.put(nodeId, node);
                } else {
                    logger.info("Z-Wave node '{}' updated", nodeId);
                    if (nodeListener.onNodeStateChanged(node)) {
                        lastNodeStates.put(nodeId, node);
                    }
                }
                lastNodeStatesCopy.remove(nodeId);
            }

            // Check for removed nodes
            lastNodeStatesCopy.forEach((nodeId, node) -> {
                logger.trace("Z-Wave node '{}' removed", nodeId);
                lastNodeStates.remove(nodeId);

                final NodeListener nodeListener = nodeListeners.get(nodeId);
                if (nodeListener != null) {
                    nodeListener.onNodeRemoved();
                }

                if (discovery != null) {
                    discovery.removeNodeDiscovery(nodeId);
                }
            });
        }
    }

    /**
     * Initiates a full refresh of all data from the remote service.
     * 
     */
    public void getFullState() {
        if (getThing().getStatus().equals(ThingStatus.ONLINE)) {
            client.sendCommand(new ListeningCommand());
        }
    }

    @Override
    public boolean registerNodeListener(NodeListener nodeListener) {
        final Integer id = nodeListener.getId();
        if (!nodeListeners.containsKey(id)) {
            logger.debug("Registering Z-Wave node listener");
            nodeListeners.put(id, nodeListener);
            final Node node = lastNodeStates.get(id);
            if (node != null) {
                nodeListener.onNodeAdded(node);
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean unregisterNodeListener(NodeListener nodeListener) {
        logger.debug("Unregistering Z-Wave node listener");
        return nodeListeners.remove(nodeListener.getId()) != null;
    }

    public boolean registerDiscoveryListener(NodeDiscoveryService listener) {
        logger.debug("Registering Z-Wave discovery listener");
        if (discoveryService == null) {
            discoveryService = listener;
            // getFullState.forEach(listener::adNodeDiscovery);
            getFullState();
            return true;
        }

        return false;
    }

    public boolean unregisterDiscoveryListener() {
        logger.debug("Unregistering Z-Wave discovery listener");
        if (discoveryService != null) {
            discoveryService = null;
            return true;
        }

        return false;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(NodeDiscoveryService.class);
    }
}
