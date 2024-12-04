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
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.binding.zwavejs.internal.config.ZwaveJSNodeConfiguration;
import org.openhab.binding.zwavejs.internal.conversion.ZwaveJSChannelTypeProvider;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelType;
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
        ZwaveJSNodeConfiguration config = this.config = getConfigAs(ZwaveJSNodeConfiguration.class);

        if (!config.isValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "id invalid");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            internalInitialize();
        });
    }

    private void internalInitialize() {
        Bridge bridge = getBridge();
        if (bridge == null || !bridge.getStatus().equals(ThingStatus.ONLINE)) {
            // when bridge is offline, stop and wait for it to become online
            logger.info("Stopped internalInitialize");
            return;
        }
        if (bridge != null && bridge.getHandler() instanceof ZwaveJSBridgeHandler handler) {
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
        logger.info("bridge state updated to {}", bridgeStatusInfo.getStatus());
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
        logger.info("Z-Wave node id: {} state update", node.nodeId);
        return true;
    }

    @Override
    public Integer getId() {
        return this.config.id;
    }

    private boolean buildChannels(Node node) {
        logger.info("building channels for {}, containing {} values", node.nodeId, node.values.size());
        for (Value value : node.values) {
            createChannel(getThing(), value);
        }

        return true;
    }

    private String generateChannelId(Value value) {
        String id = value.commandClassName.toLowerCase().replaceAll(" ", "-");

        if (value.metadata.unit != null) {
            return id + "-" + value.metadata.unit.toLowerCase();
        }
        return id;
        /*
         * if (value.propertyKeyName != null) {
         * return value.propertyKeyName.toLowerCase().replaceAll("_[a-z]+_", "-");
         * }
         * if (value.metadata.label != null) {
         * return value.metadata.label.toLowerCase().replaceAll("[\\[[a-z]+\\]]", "").trim().replaceAll(" ", "-");
         * }
         * return value.commandClassName.toLowerCase().replaceAll(" ", "-");
         */
    }

    public void createChannel(Thing thing, Value value) {
        if ("Configuration".equals(value.commandClassName)) {
            logger.debug("Thing '{}' createChannel, Configuration commandClass ignored", thing.getLabel());
            return;
        }
        String channelId = generateChannelId(value);
        logger.info("Thing '{}' createChannel, {}, channelId: {}", thing.getLabel(), value.commandClassName, channelId);
        ChannelUID channelUID = new ChannelUID(thing.getUID(), channelId);

        if (thing.getChannel(channelUID) != null) {
            // channel already exists
            logger.info("Thing {}, channel {} already exists", thing.getLabel(), channelId);
            return;
        }

        ChannelType channelType = channelTypeProvider.generateChannelType(value);
        updateThing(editThing().withChannel(ChannelBuilder.create(channelUID).withType(channelType.getUID()).build())
                .build());
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
