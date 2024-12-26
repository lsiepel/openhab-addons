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
import java.util.Objects;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.api.dto.Event;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
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
            "seconds", "s", //
            "°(C/F)", "", // special case where Zwave JS sends °F/C as unit, but is actually dimensionless
            "°F/C", ""); // special case where Zwave JS sends °F/C as unit, but is actually dimensionless

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
        this.unitSymbol = normalizeUnit(data.metadata.unit, data.value);
        this.unit = UnitUtils.parseUnit(this.unitSymbol);
        if (unitSymbol != null && unit == null) {
            logger.warn("Node id {}, unable to parse unitSymbol '{}', this is a bug", nodeId, unitSymbol);
        }
        this.itemType = itemTypeFromMetadata(data.metadata.type, data.value);
        this.configType = configTypeFromMetadata(data.metadata.type, data.value);
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
        this.unitSymbol = normalizeUnit(unitSymbol, event.args.newValue);
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
        } else if (value instanceof Map<?, ?> treeMap) {
            if (treeMap.containsKey("value")) {
                value = Objects.requireNonNull(treeMap.get("value"));
            }
        }

        String itemTypeSplitted[] = itemType.split(":");
        switch (itemTypeSplitted[0]) {
            case CoreItemFactory.NUMBER:
                if (itemTypeSplitted.length > 1) {
                    if (unit == null) {
                        logger.warn("Node id {}, the unit is unexpectedly null, please file a bug report", nodeId);
                        return new DecimalType((Number) value);
                    }
                    return new QuantityType<>((Number) value, unit);
                } else {
                    return new DecimalType((Number) value);
                }
            case CoreItemFactory.SWITCH:
                return OnOffType.from((boolean) value);
            case CoreItemFactory.COLOR:
                if (value instanceof String colorStr) {
                    try {
                        colorStr = colorStr.startsWith("#") ? colorStr : "#" + colorStr;
                        int red = Integer.valueOf(colorStr.substring(1, 3), 16);
                        int green = Integer.valueOf(colorStr.substring(3, 5), 16);
                        int blue = Integer.valueOf(colorStr.substring(5, 7), 16);
                        return HSBType.fromRGB(red, green, blue);
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        logger.warn("Node id {}, invalid color string provided: {}", nodeId, colorStr, e);
                        return UnDefType.UNDEF;
                    }
                } else if (value instanceof Map<?, ?> map && map.containsKey("red") && map.containsKey("green")
                        && map.containsKey("blue")) {
                    int red = ((Number) Objects.requireNonNull(map.get("red"))).intValue();
                    int green = ((Number) Objects.requireNonNull(map.get("green"))).intValue();
                    int blue = ((Number) Objects.requireNonNull(map.get("blue"))).intValue();
                    return HSBType.fromRGB(red, green, blue);
                } else {
                    logger.warn("Node id {}, unexpected value type for color: {}, please file a bug report", nodeId,
                            value.getClass().getName());
                    return UnDefType.UNDEF;
                }
            default:
                return UnDefType.UNDEF;
        }
    }

    private String itemTypeFromMetadata(String type, Object value) {
        type = correctedType(type, value);

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
            case "color":
                return CoreItemFactory.COLOR;
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

    private String correctedType(String type, Object value) {
        switch (type) {
            case "any":
                // Z-Wave JS not being consistent with this, so overwrite it based on our own logic
                // Can be anything from boolean, string or complex object like RGB. So we need to check the value
                if (value instanceof Number) {
                    return "number";
                } else if (value instanceof Boolean) {
                    return "boolean";
                } else if (value instanceof Map<?, ?> treeMap) {
                    if (treeMap.size() == 3) {
                        return "color";
                    }
                }
            case "duration":
                // Z-Wave JS not being consistent with this, so overwrite it based on our own logic
                // Can be anything from plain Number to a complex object with unit and value. So we need to check the
                // value
                return "number";
            default:
                return type;
        }
    }

    private Type configTypeFromMetadata(String type, Object value) {
        type = correctedType(type, value);
        switch (type) {
            case "number":
                return Type.INTEGER;
            // return Type.DECIMAL; // depends on scale?
            case "color":
                return Type.TEXT;
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

    private @Nullable String normalizeUnit(@Nullable String unitString, @Nullable Object value) {
        if (unitString == null && value instanceof Map<?, ?> treeMap) {
            if (treeMap.containsKey("unit")) {
                unitString = (String) treeMap.get("unit");
            }
        }
        if (unitString == null) {
            return null;
        }
        String[] splitted = unitString.split(" ");
        String lastPart = splitted[splitted.length - 1];
        String output = UNIT_REPLACEMENTS.getOrDefault(lastPart, lastPart);

        return !output.isBlank() ? output : null;
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
        Map<String, String> optionList = this.optionList;
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
