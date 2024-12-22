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
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.openhab.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ChannelDetails {

    private final Logger logger = LoggerFactory.getLogger(ChannelDetails.class);

    public int nodeId;
    public String channelId;
    public boolean writable;
    public @Nullable State state;
    public String itemType = CoreItemFactory.STRING;
    public Type configType = Type.TEXT;
    public @Nullable String unit;
    public @Nullable StateDescriptionFragment statePattern;
    public String label = "Unknown Label";
    public String description = "Unknown Description";
    public boolean isConfiguration;
    public boolean isChannel;
    public @Nullable String commandClassName;
    public int commandClassId;
    public int endpoint;
    public @Nullable Object writeProperty;
    public Object value;
    public @Nullable Map<String, String> optionList;

    public ChannelDetails(int nodeId, Value data) {
        this.nodeId = nodeId;
        this.channelId = generateChannelId(data);

        this.isConfiguration = isConfiguration(channelId);
        this.isChannel = isChannel(channelId);

        this.writable = data.metadata.writeable;
        this.unit = normalizeUnit(data.metadata.unit);
        this.itemType = itemTypeFromMetadata(data.metadata.type, this.unit);
        this.configType = configTypeFromMetadata(data.metadata.type);
        this.statePattern = createStatePattern(data.metadata.writeable, data.metadata.min, data.metadata.max, 1);
        this.state = toState(data.value);
        this.label = data.metadata.label;
        this.description = data.metadata.description != null ? data.metadata.description : data.commandClassName;
        this.commandClassName = data.commandClassName;
        this.commandClassId = data.commandClass;
        this.endpoint = data.endpoint;

        this.optionList = data.metadata.states;
        this.value = data.value;

        if (writable) {
            writeProperty = data.property;
        }
    }

    private boolean isConfiguration(String channelId) {
        return channelId.startsWith("configuration");
    }

    private boolean isChannel(String channelId) {
        return !isConfiguration(channelId) && !channelId.startsWith("version") && !channelId.startsWith("notification")
                && !channelId.startsWith("manufacturer-specific");
    }

    public ChannelDetails(int nodeId, Event data) {
        this.nodeId = nodeId;
        this.channelId = generateChannelId(data);

        this.isConfiguration = isConfiguration(channelId);
        this.isChannel = isChannel(channelId);
        this.value = data.args.newValue;
    }

    public @Nullable State setState(Event event) {
        return this.state = toState(event.args.newValue);
    }

    private String generateChannelId(String commandClassName, @Nullable String propertyName) {
        String id = commandClassName.toLowerCase().replaceAll(" ", "-");

        if (propertyName != null) {
            String[] splitted = StringUtils.splitByCharacterType(propertyName);
            id += "-" + splitted[splitted.length - 1].toLowerCase();
        }

        return id;
    }

    public String generateChannelId(Event event) {
        return generateChannelId(event.args.commandClassName, event.args.propertyName);
    }

    private String generateChannelId(Value value) {
        return generateChannelId(value.commandClassName, value.propertyName);
    }

    private @Nullable State toState(@Nullable Object value) {
        if (!this.isChannel) {
            logger.debug("Node id: '{}' getStateFromValue, channelId ignored", nodeId);
            return null;
        }
        if (value == null) {
            return UnDefType.NULL;
        }
        State state = UnDefType.UNDEF;
        String itemTypeSplitted[] = itemType.split(":");
        switch (itemTypeSplitted[0]) {
            case CoreItemFactory.NUMBER:
                if (itemTypeSplitted.length > 1) {
                    Unit<?> unit = Units.getInstance().getUnit(this.unit);
                    state = new QuantityType<>((Number) value, unit);
                } else {
                    state = new DecimalType((Number) value);
                }
                break;
            case CoreItemFactory.SWITCH:
                state = OnOffType.from((boolean) value);
                break;
            default:
                state = UnDefType.UNDEF;
                break;
        }
        return state;
    }

    public String itemTypeFromMetadata(String type, @Nullable String unitSymbol) {
        switch (type) {
            case "number":
                if (unitSymbol != null) {
                    Unit<?> unit = UnitUtils.parseUnit(unitSymbol);
                    String dimension = unit != null ? UnitUtils.getDimensionName(unit) : null;
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

    public @Nullable String normalizeUnit(@Nullable String unit) {
        if (unit == null) {
            return null;
        }

        String[] splitted = unit.split(" ");
        return splitted[splitted.length - 1] //
                .replace("minutes", "min") //
                .replace("seconds", "s");
    }

    public @Nullable StateDescriptionFragment createStatePattern(boolean writeable, @Nullable Integer min,
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
        // fragment.withOptions(null);
        // TODO from states but need to find out how to properly deserialize it into a
        // key/value pair
        if (step != null) {
            fragment.withStep(BigDecimal.valueOf(step));
        }
        // TODO there does not seem to be a property that can be used for this
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
        sb.append(", unit=" + unit);
        sb.append(", statePattern=" + statePattern);
        sb.append(", label=" + label);
        sb.append(", description=" + description);
        sb.append("]");
        return sb.toString();
    }
}
