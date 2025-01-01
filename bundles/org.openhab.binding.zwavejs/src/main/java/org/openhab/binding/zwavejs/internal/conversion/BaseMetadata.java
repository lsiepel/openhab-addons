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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.api.dto.Event;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
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
 * The {@link BaseMetadata} class represents basic metadata information for a Z-Wave node.
 * It contains various properties and methods to handle metadata and state information.
 * 
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public abstract class BaseMetadata {

    private static final Logger logger = LoggerFactory.getLogger(BaseMetadata.class);
    private static final String DEFAULT_LABEL = "Unknown Label";
    private static final Map<String, String> UNIT_REPLACEMENTS = Map.of("lux", "lx", //
            "Lux", "lx", //
            "minutes", "min", //
            "seconds", "s", //
            "°(C/F)", "", // special case where Zwave JS sends °F/C as unit, but is actually dimensionless
            "°F/C", ""); // special case where Zwave JS sends °F/C as unit, but is actually dimensionless

    private static final Map<String, String> CHANNEL_ID_PROPERTY_NAME_REPLACEMENTS = Map.of("currentValue", "value", //
            "targetValue", "value"); //

    public int nodeId;
    public String Id;
    public String label = DEFAULT_LABEL;
    public @Nullable String description;
    public @Nullable String unitSymbol;
    protected @Nullable Unit<?> unit;
    protected Object value;
    public boolean writable;
    public String itemType = CoreItemFactory.STRING;
    public @Nullable Object writeProperty;
    public @Nullable Map<String, String> optionList;

    public @Nullable String commandClassName;
    public int commandClassId;
    public int endpoint;

    protected BaseMetadata(int nodeId, Value value) {
        this.nodeId = nodeId;
        this.commandClassName = value.commandClassName;
        this.commandClassId = value.commandClass;
        this.endpoint = value.endpoint;
        this.writable = value.metadata.writeable;

        this.Id = generateChannelId(value);

        this.label = normalizeLabel(value.metadata.label, value.endpoint, value.propertyName);
        this.description = value.metadata.description;
        this.unitSymbol = normalizeUnit(value.metadata.unit, value.value);
        this.unit = UnitUtils.parseUnit(this.unitSymbol);
        this.itemType = itemTypeFromMetadata(value.metadata.type, value.value, value.commandClassName,
                value.metadata.states);
        if (unitSymbol != null && unit == null) {
            logger.warn("Node id {}, unable to parse unitSymbol '{}', please file a bug report", nodeId, unitSymbol);
        }
        this.optionList = value.metadata.states;
        this.value = value.value;

        if (writable) {
            writeProperty = value.property;
        }
    }

    public BaseMetadata(int nodeId, Event data) {
        this.nodeId = nodeId;
        this.Id = generateId(data);

        this.value = data.args.newValue;
    }

    private String normalizeLabel(String label, int endpoint, String propertyName) {
        String output = "";
        if (label == null || label.isBlank()) {
            return propertyName;
        }
        output = label.replaceAll("\s\\[.*\\]", "");
        output = capitalize(output);
        if (endpoint > 0) {
            output += String.format("EP%s %s", endpoint, output);
        }
        return output;
    }

    private String capitalize(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return DEFAULT_LABEL;
        }

        return Objects
                .requireNonNullElse(
                        Arrays.stream(StringUtils.splitByCharacterType(input)).filter(f -> !f.isBlank())
                                .map(word -> StringUtils.capitalize(word)).collect(Collectors.joining(" ")),
                        DEFAULT_LABEL);
    }

    private String normalizeString(@Nullable String input) {
        return input != null && !input.isBlank()
                ? "-" + input.trim().toLowerCase().replaceAll(" ", "-").replaceAll("[^a-zA-Z0-9\\-]", "")
                : "";
    }

    private String generateId(String commandClassName, int endpoint, @Nullable String propertyName,
            @Nullable String propertyKey) {
        String id = normalizeString(commandClassName).replaceFirst("-", "");
        String[] splitted;
        if (propertyName != null && !propertyName.contains("unknown")) {
            propertyName = CHANNEL_ID_PROPERTY_NAME_REPLACEMENTS.getOrDefault(propertyName, propertyName);
            splitted = StringUtils.splitByCharacterType(propertyName);
            List<String> result = Arrays.asList(splitted).stream().filter(s -> s.matches("^[a-zA-Z]+$")).toList();
            if (!result.isEmpty()) {
                id += normalizeString(String.join("-", result));
            }
        }
        if (propertyKey != null) {
            id += normalizeString(propertyKey);
        }
        if (endpoint > 0) {
            id += "-" + endpoint;
            return id;
        }

        return id;
    }

    private String generateId(Event event) {
        return generateId(event.args.commandClassName, event.args.endpoint, event.args.propertyName,
                event.args.propertyKey);
    }

    private String generateChannelId(Value value) {
        return generateId(value.commandClassName, value.endpoint, value.propertyName, value.propertyKey);
    }

    protected @Nullable State toState(@Nullable Object value, String itemType, @Nullable Unit<?> unit) {
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
                if (value instanceof Number numberValue) {
                    return OnOffType.from(numberValue.intValue() > 0);
                }
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

    protected String correctedType(String type, Object value, String commandClassName,
            @Nullable Map<String, String> optionList) {
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
            case "number":
                if ("Notification".equals(commandClassName) && optionList != null && optionList.size() == 2) {
                    return "boolean";
                }
            default:
                return type;
        }
    }

    protected String itemTypeFromMetadata(String type, Object value, String commandClassName,
            @Nullable Map<String, String> optionList) {
        type = correctedType(type, value, commandClassName, optionList);

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

    protected @Nullable StateDescriptionFragment createStatePattern(boolean writeable, @Nullable Integer min,
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

    protected @Nullable String normalizeUnit(@Nullable String unitString, @Nullable Object value) {
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
        String output = Objects.requireNonNull(UNIT_REPLACEMENTS.getOrDefault(lastPart, lastPart));

        return !output.isBlank() ? output : null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BaseMetadata [");
        sb.append(", nodeId=" + nodeId);
        sb.append(", Id=" + Id);
        sb.append(", label=" + label);
        sb.append(", description=" + description);
        sb.append(", unitSymbol=" + unitSymbol);
        sb.append(", value=" + value);
        sb.append(", itemType=" + itemType);
        sb.append(", writable=" + writable);
        sb.append(", writeProperty=" + writeProperty);
        sb.append(", itemType=" + itemType);
        sb.append("]");
        return sb.toString();
    }
}
