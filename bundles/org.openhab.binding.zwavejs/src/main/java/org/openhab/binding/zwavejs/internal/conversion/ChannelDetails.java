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
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.api.dto.Event;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.UnDefType;
import org.openhab.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author L. Siepel - Initial contribution
 */
public class ChannelDetails {

    private final Logger logger = LoggerFactory.getLogger(ChannelDetails.class);
    private final Map<String, String> unitMap = new ConcurrentHashMap<>();

    public int nodeId;
    public String channelId;
    public boolean readOnly;
    public State state;
    public String itemType;
    public String unit;
    public StateDescriptionFragment statePattern;
    public String label;
    public String description;
    public boolean ignoreAsChannel;

    public ChannelDetails(int nodeId, Value data) {
        populateMap();

        this.nodeId = nodeId;
        this.channelId = generateChannelId(data);

        this.ignoreAsChannel = isIgnored(channelId);

        this.readOnly = data.metadata.writeable;
        this.itemType = itemTypeFromMetadata(data.metadata.type, data.metadata.unit);
        this.unit = normalizeUnit(data.metadata.unit);
        this.statePattern = statePatternOfItemType(data.metadata.writeable, data.metadata.min, data.metadata.max, 1);
        this.state = getStateFromValue(data.value);
        this.label = data.metadata.label;
        this.description = data.commandClassName;
    }

    private boolean isIgnored(String channelId) {
        return (channelId.startsWith("configuration") || channelId.startsWith("version")
                || channelId.startsWith("manufacturer-specific"));
    }

    public ChannelDetails(int nodeId, Event data) {
        populateMap();

        this.nodeId = nodeId;
        this.channelId = generateChannelId(data);

        this.ignoreAsChannel = isIgnored(channelId);
        // this.readOnly = data.metadata.writeable;
        // this.itemType = itemTypeFromMetadata(data.metadata);
        // this.unit = normalizeUnit(data.metadata.unit);
        // this.statePattern = statePatternOfItemType(data.metadata);

        // this.label = data.metadata.label;
        // this.description = data.commandClassName;
    }

    public void setState(Event event) {
        this.state = getStateFromValue(event.args.newValue);
    }

    private void populateMap() {
        unitMap.put("W", "Power");
        unitMap.put("kWh", "Energy");
        unitMap.put("A", "ElectricCurrent");
        unitMap.put("min", "Time");
        unitMap.put("s", "Time");
        unitMap.put("V", "ElectricPotential");
    }

    private String generateChannelId(String commandClassName, @Nullable String propertyName, @Nullable String unit) {
        // todo unit should be stripped from the method
        String id = commandClassName.toLowerCase().replaceAll(" ", "-");

        if (propertyName != null) {
            String[] splitted = StringUtils.splitByCharacterType(propertyName);
            id += "-" + splitted[splitted.length - 1].toLowerCase();
        }

        if (unit != null) {
            return id + "-" + unit.toLowerCase();
        }
        return id;
    }

    public String generateChannelId(Event event) {
        return generateChannelId(event.args.commandClassName, event.args.propertyName, null);
    }

    private String generateChannelId(Value value) {
        return generateChannelId(value.commandClassName, value.propertyName, value.metadata.unit);
    }

    private @Nullable State getStateFromValue(Object newValue) {
        if (this.ignoreAsChannel) {
            logger.debug("Node id: '{}' getStateFromValue, channelId ignored", nodeId);
            return null;
        }
        if (newValue == null) {
            return UnDefType.NULL;
        }
        State state = UnDefType.UNDEF;
        String itemTypeSplitted[] = this.itemType.split(":");
        switch (itemTypeSplitted[0]) {
            case "Number":
                if (itemTypeSplitted.length > 1) {
                    Unit<?> unit = Units.getInstance().getUnit(this.unit);
                    state = new QuantityType<>((Number) newValue, unit);
                } else {
                    state = new DecimalType((Number) newValue);
                }
                break;
            case "Switch":
                state = OnOffType.from((boolean) newValue);
                break;
            default:
                state = UnDefType.UNDEF;
                break;
        }
        return state;
    }

    public String itemTypeFromMetadata(String type, @Nullable String unitSymbol) {
        // TODO Not sure if this is the best way to parse a unit as string that returns a Unit or Dimension.
        switch (type) {
            case "number":
                if (unitSymbol != null) {
                    Unit<?> unit = Units.getInstance().getUnit(unitSymbol);
                    String symbol = unit != null && unit.getSymbol() != null ? unit.getSymbol() : unitSymbol;
                    String dimension = unitMap.getOrDefault(symbol, null);
                    if (dimension == null) {
                        logger.info("Could not parse '{}' as a unit, fallback to 'Number' itemType", unitSymbol);
                        return "Number";
                    }

                    return String.format("Number:%s", dimension);
                }
                return "Number";
            case "boolean":
                // switch (or contact ?)
                return "Switch";
            case "string":
            case "string[]":
                return "String";
            default:
                logger.error(
                        "Could not determine item type based on metadata.type: {}, fallback to 'String' please file a bug report",
                        type);
                return "String";
        }
    }

    public @Nullable String normalizeUnit(@Nullable String unit) {
        if (unit == null || itemType == null || !itemType.contains(":")) {
            return null;
        }

        String[] splitted = unit.split(" ");
        return splitted[splitted.length - 1] //
                .replace("minutes", "min") //
                .replace("seconds", "s");
    }

    public StateDescriptionFragment statePatternOfItemType(boolean writeable, Integer min, Integer max, Integer step) {
        String pattern = "";
        String itemTypeSplitted[] = this.itemType.split(":");
        switch (itemTypeSplitted[0]) {
            case "Number":
                if (itemTypeSplitted.length > 1) {
                    pattern = "%0.f %unit%"; // TODO how to determine the decimals
                } else {
                    pattern = "%0.d";
                }
                break;
            case "String":
            case "Switch":
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
