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

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.api.dto.Event;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.binding.zwavejs.internal.api.dto.commands.NodeSetValueCommand;
import org.openhab.binding.zwavejs.internal.config.ZwaveJSChannelConfiguration;
import org.openhab.binding.zwavejs.internal.config.ZwaveJSNodeConfiguration;
import org.openhab.binding.zwavejs.internal.conversion.ChannelDetails;
import org.openhab.binding.zwavejs.internal.conversion.ZwaveJSChannelTypeProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZwaveJSNodeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author L. Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSNodeHandler extends BaseThingHandler implements NodeListener {

    private final Logger logger = LoggerFactory.getLogger(ZwaveJSNodeHandler.class);
    private final ZwaveJSChannelTypeProvider channelTypeProvider;
    private @Nullable ZwaveJSNodeConfiguration config;
    protected ScheduledExecutorService executorService = scheduler;

    public ZwaveJSNodeHandler(final Thing thing, final ZwaveJSChannelTypeProvider channelTypeProvider) {
        super(thing);
        this.channelTypeProvider = channelTypeProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        ZwaveJSBridgeHandler handler = getBridgeHandler();
        if (handler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
            return;
        }

        ZwaveJSChannelConfiguration channelConfig = thing.getChannel(channelUID).getConfiguration()
                .as(ZwaveJSChannelConfiguration.class);
        NodeSetValueCommand zwaveCommand = new NodeSetValueCommand(config.id, channelConfig);

        if (command instanceof OnOffType onOffCommand) {
            zwaveCommand.value = OnOffType.ON.equals(onOffCommand);
            handler.sendCommand(zwaveCommand);
        }
    }

    @Override
    public void initialize() {
        ZwaveJSNodeConfiguration config = this.config = getConfigAs(ZwaveJSNodeConfiguration.class);

        if (!config.isValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "id invalid");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        executorService.execute(() -> {
            internalInitialize();
        });
    }

    private @Nullable ZwaveJSBridgeHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null || !bridge.getStatus().equals(ThingStatus.ONLINE)) {
            // when bridge is offline, stop and wait for it to become online
            logger.debug("Stopped internalInitialize as bridge is offline");
            return null;
        }
        if (bridge != null && bridge.getHandler() instanceof ZwaveJSBridgeHandler handler) {
            return handler;
        }
        return null;
    }

    private void internalInitialize() {
        ZwaveJSBridgeHandler handler = getBridgeHandler();
        if (handler != null) {
            if (handler.registerNodeListener(this)) {
                Node nodeDetails = handler.requestNodeDetails(config.id);
                if (nodeDetails == null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Could not obtain node details");
                    return;
                }
                if (nodeDetails.status != 4) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            String.format("The Z-Wave JS state of this node is: {}", nodeDetails.status));
                    return;
                }
                if (!buildChannels(nodeDetails)) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Initialization failed, could not build channels");
                    return;
                }
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED,
                        "Could not register node listener");
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        if (bridgeStatusInfo.getStatus().equals(ThingStatus.ONLINE)) {
            internalInitialize();
        }
    }

    @Override
    public void onNodeRemoved() {
        // (ThingStatus.OFFLINE, ThingStatusDetail.NONE, "@text/offline.node-removed");
    }

    @Override
    public void onNodeAdded(Node node) {
        onNodeStateChanged(node);
    }

    @Override
    public boolean onNodeStateChanged(Node node) {
        logger.debug("Z-Wave node id: {} state update", node.nodeId);

        for (Value value : node.values) {
            ChannelDetails details = new ChannelDetails(getId(), value);
            if (!details.ignoreAsChannel) {
                State state = details.state;
                if (isLinked(details.channelId) && state != null) {
                    updateState(details.channelId, state);
                }
            }
        }

        return true;
    }

    @Override
    public boolean onNodeStateChanged(Event event) {
        logger.debug("Z-Wave node id: {} state update", config.id);

        ChannelDetails details = new ChannelDetails(getId(), event);
        if (!details.ignoreAsChannel) {
            if (isLinked(details.channelId)) {
                ZwaveJSChannelConfiguration channelConfig = thing.getChannel(details.channelId).getConfiguration()
                        .as(ZwaveJSChannelConfiguration.class);

                details.unit = channelConfig.incomingUnit;
                details.itemType = channelConfig.itemType;
                State state = details.setState(event);
                if (state != null) {
                    updateState(details.channelId, state);
                }
            }
        }
        return true;
    }

    @Override
    public Integer getId() {
        return this.config.id;
    }

    private boolean buildChannels(Node node) {
        logger.debug("Building channels for {}, containing {} values", node.nodeId, node.values.size());
        for (Value value : node.values) {
            ChannelDetails details = new ChannelDetails(this.getId(), value);
            if (!details.ignoreAsChannel) {
                createChannel(getThing(), details);
            }
        }

        return true;
    }

    // public void updateChannel(Thing thing, )

    public void createChannel(Thing thing, ChannelDetails details) {
        // if ("Configuration".equals(value.commandClassName)) {
        // logger.debug("Thing '{}' createChannel, Configuration commandClass ignored", thing.getLabel());
        // return;
        // }

        String channelId = details.channelId;
        logger.debug("Thing '{}' createChannel with Id: {}", thing.getLabel(), channelId);
        logger.trace(" >> {}", details);
        ChannelUID channelUID = new ChannelUID(thing.getUID(), channelId);

        Channel existingChannel = thing.getChannel(channelUID);
        if (existingChannel != null) {
            logger.warn("Thing {}, channel {} already exists", thing.getLabel(), channelId);
            Configuration channelConfig = existingChannel.getConfiguration();
            if (channelConfig.get(CONFIG_CHANNEL_WRITE_PROPERTY) == null && details.writable
                    && details.writeProperty != null) {
                channelConfig.put(CONFIG_CHANNEL_WRITE_PROPERTY, details.writeProperty);
                ChannelBuilder.create(existingChannel).withConfiguration(channelConfig).build();
                logger.warn("Thing {}, channel {} updated", thing.getLabel(), channelId);
                return;

                // updateThing(editThing().withChannel();
            } else {
                logger.warn("Thing {}, channel {} already exists: Ignored", thing.getLabel(), channelId);
                return;
            }
        }

        Configuration configuration = new Configuration();
        configuration.put(CONFIG_CHANNEL_INCOMING_UNIT, details.unit);
        configuration.put(CONFIG_CHANNEL_ITEM_TYPE, details.itemType);
        configuration.put(CONFIG_CHANNEL_COMMANDCLASS_ID, details.commandClassId);
        configuration.put(CONFIG_CHANNEL_COMMANDCLASS_NAME, details.commandClassName);
        configuration.put(CONFIG_CHANNEL_ENDPOINT, details.endpoint);
        if (details.writable) {
            configuration.put(CONFIG_CHANNEL_WRITE_PROPERTY, details.writeProperty);
        }

        ChannelType channelType = channelTypeProvider.generateChannelType(details);
        updateThing(editThing().withChannel(ChannelBuilder.create(channelUID).withConfiguration(configuration)
                .withType(channelType.getUID()).build()).build());
    }

    @Override
    public void dispose() {
        Bridge bridge = getBridge();
        if (bridge != null && bridge.getHandler() instanceof ZwaveJSBridgeHandler handler) {
            handler.unregisterNodeListener(this);
        }
        super.dispose();
    }
}
