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
package org.openhab.binding.zwavejs.internal.conversion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.api.dto.Event;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.openhab.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MetadataEntry} class represents metadata information for a Z-Wave node.
 * It contains various properties and methods to handle metadata and state information.
 * 
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class MetadataEntry {

    private static final Logger logger = LoggerFactory.getLogger(MetadataEntry.class);
    private static final String DEFAULT_DESCRIPTION = "Unknown Description";
    private static final String DEFAULT_LABEL = "Unknown Label";
    private static final Map<String, String> UNIT_REPLACEMENTS = Map.of("lux", "lx", //
            "Lux", "lx", //
            "minutes", "min", //
            "seconds", "s");

    public int nodeId;
    public String channelId;
    public boolean writable;
    public @Nullable State state;
    public String itemType = CoreItemFactory.STRING;
    public Type configType = Type.TEXT;
    public @Nullable String unitSymbol;
    public @Nullable Unit<?> unit;
    public @Nullable StateDescriptionFragment statePattern;
    public String label = DEFAULT_LABEL;
    public String description = DEFAULT_DESCRIPTION;
    public boolean isConfiguration;
    public boolean isChannel;
    public @Nullable String commandClassName;
    public int commandClassId;
    public int endpoint;
    public @Nullable Object writeProperty;
    public Object value;
    public @Nullable Map<String, String> optionList;

    public MetadataEntry(int nodeId, Value data) {
        this.nodeId = nodeId;
        this.channelId = generateChannelId(data);

        this.isConfiguration = isConfiguration(channelId);
        this.isChannel = isChannel(channelId);

        this.writable = data.metadata.writeable;
        this.unitSymbol = normalizeUnit(data.metadata.unit);
        this.unit = UnitUtils.parseUnit(this.unitSymbol);
        if (unitSymbol != null && unit == null) {
            logger.warn("Node id {}, unable to parse unitSymbol '{}', this is a bug", nodeId, unitSymbol);
        }
        this.itemType = itemTypeFromMetadata(data.metadata.type);
        this.configType = configTypeFromMetadata(data.metadata.type);
        this.optionList = data.metadata.states;

        this.statePattern = createStatePattern(data.metadata.writeable, data.metadata.min, data.metadata.max, 1);
        this.state = toState(data.value, itemType, unit);

        this.description = data.metadata.description != null ? data.metadata.description : data.commandClassName;
        this.label = data.metadata.label != null ? data.metadata.label : this.description;

        this.commandClassName = data.commandClassName;
        this.commandClassId = data.commandClass;
        this.endpoint = data.endpoint;

        this.value = data.value;

        if (writable) {
            writeProperty = data.property;
        }
    }

    public MetadataEntry(int nodeId, Event data) {
        this.nodeId = nodeId;
        this.channelId = generateChannelId(data);

        this.isConfiguration = isConfiguration(channelId);
        this.isChannel = isChannel(channelId);
        this.value = data.args.newValue;
    }

    private boolean isConfiguration(String channelId) {
        return channelId.startsWith("configuration");
    }

    private boolean isChannel(String channelId) {
        return !isConfiguration(channelId) && !channelId.startsWith("version") && !channelId.startsWith("notification")
                && !channelId.startsWith("manufacturer-specific");
    }

    /**
     * Sets the state based on the provided event, item type, and unit symbol.
     *
     * @param event The event containing the new value to set the state to.
     * @param itemType The type of the item for which the state is being set.
     * @param unitSymbol The unit symbol to be used for the state, can be null.
     * @return The new state after setting it based on the event's new value.
     */
    public @Nullable State setState(Event event, String itemType, @Nullable String unitSymbol) {
        this.unitSymbol = normalizeUnit(unitSymbol);
        this.unit = UnitUtils.parseUnit(this.unitSymbol);
        if (unitSymbol != null && unit == null) {
            logger.warn("Node id {}, unable to parse unitSymbol '{}'' from channel config, this is a bug", nodeId,
                    unitSymbol);
        }
        return this.state = toState(event.args.newValue, itemType, this.unit);
    }

    private String generateChannelId(String commandClassName, @Nullable String propertyName) {
        String id = commandClassName.toLowerCase().replaceAll(" ", "-");

        if (propertyName != null && !propertyName.contains("unknown")) {
            String[] splitted = StringUtils.splitByCharacterType(propertyName);
            id += "-" + splitted[splitted.length - 1].toLowerCase();
        }

        return id;
    }

    /**
     * Generates a channel ID based on the provided event.
     *
     * @param event the event containing the command class name and property name
     * @return the generated channel ID
     */
    private String generateChannelId(Event event) {
        return generateChannelId(event.args.commandClassName, event.args.propertyName);
    }

    private String generateChannelId(Value value) {
        return generateChannelId(value.commandClassName, value.propertyName);
    }

    private @Nullable State toState(@Nullable Object value, String itemType, @Nullable Unit<?> unit) {
        if (!this.isChannel) {
            logger.debug("Node id: '{}' getStateFromValue, channelId ignored", nodeId);
            return null;
        }
        if (value == null) {
            return UnDefType.NULL;
        }
        String itemTypeSplitted[] = itemType.split(":");
        switch (itemTypeSplitted[0]) {
            case CoreItemFactory.NUMBER:
                if (itemTypeSplitted.length > 1) {
                    if (unit == null) {
                        logger.warn("Node id {}, the unit is unexpectedly null, this is a bug", nodeId);
                        return new DecimalType((Number) value);
                    }
                    return new QuantityType<>((Number) value, unit);
                } else {
                    return new DecimalType((Number) value);
                }
            case CoreItemFactory.SWITCH:
                return OnOffType.from((boolean) value);
            default:
                return UnDefType.UNDEF;
        }
    }

    private String itemTypeFromMetadata(String type) {
        switch (type) {
            case "number":
                Unit<?> unit = this.unit;
                if (unit != null) {
                    String dimension = UnitUtils.getDimensionName(unit);
                    if (dimension == null) {
                        logger.warn("Could not parse '{}' as a unit, fallback to 'Number' itemType", unitSymbol);
                        return CoreItemFactory.NUMBER;
                    }

                    return CoreItemFactory.NUMBER + ":" + dimension;
                }
                return CoreItemFactory.NUMBER;
            case "boolean":
                // switch (or contact ?)
                return CoreItemFactory.SWITCH;
            case "string":
            case "string[]":
                return CoreItemFactory.STRING;
            default:
                logger.error(
                        "Could not determine item type based on metadata.type: {}, fallback to 'String' please file a bug report",
                        type);
                return CoreItemFactory.STRING;
        }
    }

    private Type configTypeFromMetadata(String type) {
        switch (type) {
            case "number":
                return Type.INTEGER;
            // return Type.DECIMAL; // depends on scale?
            case "boolean":
                // switch (or contact ?)
                return Type.BOOLEAN;
            case "string":
            case "string[]":
                return Type.TEXT;
            default:
                logger.error(
                        "Could not determine config type based on metadata.type: {}, fallback to 'Text' please file a bug report",
                        type);
                return Type.TEXT;
        }
    }

    private @Nullable String normalizeUnit(@Nullable String unitString) {
        if (unitString == null) {
            return null;
        }

        String[] splitted = unitString.split(" ");
        String lastPart = splitted[splitted.length - 1];

        return UNIT_REPLACEMENTS.getOrDefault(lastPart, lastPart);
    }

    private @Nullable StateDescriptionFragment createStatePattern(boolean writeable, @Nullable Integer min,
            @Nullable Integer max, @Nullable Integer step) {
        String pattern = "";
        String itemTypeSplitted[] = itemType.split(":");
        switch (itemTypeSplitted[0]) {
            case CoreItemFactory.NUMBER:
                if (itemTypeSplitted.length > 1) {
                    // TODO how to determine the decimals
                    pattern = "%0.f %unit%";
                } else {
                    pattern = "%0.d";
                }
                break;
            case CoreItemFactory.STRING:
            case CoreItemFactory.SWITCH:
            default:
                return null;
        }

        var fragment = StateDescriptionFragmentBuilder.create();
        fragment.withPattern(pattern);
        fragment.withReadOnly(!writeable);
        if (min != null) {
            fragment.withMinimum(BigDecimal.valueOf(min));
        }
        if (max != null) {
            fragment.withMaximum(BigDecimal.valueOf(max));
        }
        if (optionList != null) {
            List<StateOption> options = new ArrayList<>();
            optionList.forEach((k, v) -> options.add(new StateOption(k, v)));
            fragment.withOptions(options);
        }
        if (step != null) {
            fragment.withStep(BigDecimal.valueOf(step));
        }
        return fragment.build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChannelDetails [");
        sb.append(", nodeId=" + nodeId);
        sb.append(", channelId=" + channelId);
        sb.append(", state=" + state);
        sb.append(", itemType=" + itemType);
        sb.append(", unit=" + unitSymbol);
        sb.append(", statePattern=" + statePattern);
        sb.append(", label=" + label);
        sb.append(", description=" + description);
        sb.append("]");
        return sb.toString();
    }
}
