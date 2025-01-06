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

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.BindingConstants;
import org.openhab.binding.zwavejs.internal.api.dto.Event;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.Status;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.binding.zwavejs.internal.api.dto.commands.NodeSetValueCommand;
import org.openhab.binding.zwavejs.internal.config.ZwaveJSChannelConfiguration;
import org.openhab.binding.zwavejs.internal.config.ZwaveJSNodeConfiguration;
import org.openhab.binding.zwavejs.internal.conversion.ChannelMetadata;
import org.openhab.binding.zwavejs.internal.conversion.ConfigMetadata;
import org.openhab.binding.zwavejs.internal.type.ZwaveJSTypeGenerator;
import org.openhab.binding.zwavejs.internal.type.ZwaveJSTypeGeneratorResult;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringListType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZwaveJSNodeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ZwaveJSNodeHandler extends BaseThingHandler implements ZwaveNodeListener {

    private final Logger logger = LoggerFactory.getLogger(ZwaveJSNodeHandler.class);
    private final ZwaveJSTypeGenerator typeGenerator;
    private ZwaveJSNodeConfiguration config = new ZwaveJSNodeConfiguration();
    protected ScheduledExecutorService executorService = scheduler;

    public ZwaveJSNodeHandler(final Thing thing, final ZwaveJSTypeGenerator typeGenerator) {
        super(thing);
        this.typeGenerator = typeGenerator;
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters)
            throws ConfigValidationException {
        super.handleConfigurationUpdate(configurationParameters);

        // TODO handle update
        // 1 determine changed parameter
        // 2 prepare command
        // 3 sendCommand
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        ZwaveJSBridgeHandler handler = getBridgeHandler();
        if (handler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
            return;
        }

        Channel channel = thing.getChannel(channelUID);
        if (channel == null) {
            logger.debug("Channel {} not found", channelUID);
            return;
        }
        ZwaveJSChannelConfiguration channelConfig = channel.getConfiguration().as(ZwaveJSChannelConfiguration.class);
        NodeSetValueCommand zwaveCommand = new NodeSetValueCommand(config.id, channelConfig);

        if (command instanceof OnOffType onOffCommand) {
            zwaveCommand.value = OnOffType.ON.equals(onOffCommand);
        } else if (command instanceof QuantityType<?> quantityCommand) {
            Unit<?> unit = UnitUtils.parseUnit(channelConfig.incomingUnit);
            if (unit == null) {
                logger.warn("Could not parse '{}' as a unit, this is a bug.", channelConfig.incomingUnit);
                return;
            }
            zwaveCommand.value = Objects.requireNonNull(quantityCommand.toUnit(unit)).doubleValue();
        } else if (command instanceof DecimalType decimalCommand) {
            zwaveCommand.value = decimalCommand.doubleValue();
        } else if (command instanceof DateTimeType dateTimeCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof HSBType hsbTypeCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof PercentType percentTypeCommand) {
            zwaveCommand.value = percentTypeCommand.doubleValue();
        } else if (command instanceof IncreaseDecreaseType increaseDecreaseCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof NextPreviousType nextPreviousCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof OpenClosedType openClosedCommand) {
            zwaveCommand.value = OpenClosedType.OPEN.equals(openClosedCommand);
        } else if (command instanceof PlayPauseType stringCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof PointType pointCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof RewindFastforwardType rewindFastforwardCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof StopMoveType stopMoveCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof StringListType stringListCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof UpDownType upDownCommand) {
            throw new UnsupportedOperationException();
        } else if (command instanceof StringType stringCommand) {
            zwaveCommand.value = stringCommand.toString();
        }
        if (zwaveCommand.value != null) {
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
        if (bridge.getHandler() instanceof ZwaveJSBridgeHandler handler) {
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
                if (Status.DEAD.equals(nodeDetails.status)) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            String.format("The Z-Wave JS state of this node is: {}", nodeDetails.status));
                    return;
                }
                if (!setupThing(nodeDetails)) {
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
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE);
    }

    @Override
    public void onNodeAdded(Node node) {
        onNodeStateChanged(node);
    }

    @Override
    public boolean onNodeStateChanged(Node node) {
        logger.debug("Z-Wave node id: {} state update", node.nodeId);
        Configuration configuration = editConfiguration();
        boolean configChanged = false;

        for (Value value : node.values) {
            if (BindingConstants.CC_CONFIGURATION.equals(value.commandClassName)) {
                ConfigMetadata details = new ConfigMetadata(getId(), value);
                configuration.put(details.Id, value.value);
                logger.debug("{}: Updated Configuration {}:{}", thing.getUID(), details.Id, value.value);
                configChanged = true;
            } else {
                ChannelMetadata metadata = new ChannelMetadata(getId(), value);
                State state = metadata.state;
                if (!metadata.isIgnoredCommandClass(metadata.commandClassName) && isLinked(metadata.Id)
                        && state != null) {
                    try {
                        updateState(metadata.Id, state);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Error updating state for channel {} with value {}. {}", metadata.Id,
                                state.toFullString(), e.getMessage());
                    }
                }
            }
        }
        if (configChanged) {
            updateConfiguration(configuration);
        }

        return true;
    }

    @Override
    public boolean onNodeStateChanged(Event event) {
        logger.debug("Z-Wave node id: {} state update", config.id);

        if (BindingConstants.CC_CONFIGURATION.equals(event.args.commandClassName)) {
            // configDescriptions.add(createConfigDescription(new ConfigMetadata(node.nodeId, value)));
        } else {
            ChannelMetadata metadata = new ChannelMetadata(getId(), event);
            if (!metadata.isIgnoredCommandClass(event.args.commandClassName) && isLinked(metadata.Id)) {
                @SuppressWarnings("null") // as we checked by isLinked the channel can't be null
                ZwaveJSChannelConfiguration channelConfig = thing.getChannel(metadata.Id).getConfiguration()
                        .as(ZwaveJSChannelConfiguration.class);

                State state = metadata.setState(event, channelConfig.itemType, channelConfig.incomingUnit);
                if (state != null) {
                    try {
                        updateState(metadata.Id, state);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Error updating state for channel {} with value {}. {}", metadata.Id,
                                state.toFullString(), e.getMessage());
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Integer getId() {
        return this.config.id;
    }

    private boolean setupThing(Node node) {
        logger.debug("Building channels and configuration for {}, containing {} values", node.nodeId,
                node.values.size());

        ZwaveJSTypeGeneratorResult result = typeGenerator.generate(thing.getUID(), node);

        ThingBuilder builder = editThing();
        if (!result.location.isBlank()) {
            builder.withLocation(result.location);
        }

        updateThing(builder
                .withChannels(new ArrayList<Channel>(result.channels.entrySet().stream()
                        .sorted(Map.Entry.<String, Channel> comparingByKey()).map(m -> m.getValue()).toList()))
                .build());

        return true;
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
