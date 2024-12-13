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

import javax.measure.Unit;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.api.dto.Metadata;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.UnDefType;
import org.openhab.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.units.indriya.unit.Units;

/**
 * @author L. Siepel - Initial contribution
 */
public class ChannelDetails {

    private final Logger logger = LoggerFactory.getLogger(ZwaveJSChannelTypeProvider.class);

    public int nodeId;
    public String channelId;
    public boolean readOnly;
    public State state;
    public String itemType;
    public String unit;
    public StateDescriptionFragment statePattern;
    public String label;
    public String description;

    public ChannelDetails(int nodeId, Value data) {
        this.nodeId = nodeId;
        this.channelId = generateChannelId(data);
        this.readOnly = data.metadata.writeable;
        this.itemType = itemTypeFromMetadata(data.metadata);
        this.unit = normalizeUnit(data.metadata.unit);
        this.statePattern = statePatternOfItemType(data.metadata);
        this.state = getStateFromValue(data);
        this.label = data.metadata.label;
        this.description = data.commandClassName;
    }

    private String generateChannelId(Value value) {
        String id = value.commandClassName.toLowerCase().replaceAll(" ", "-");

        if (value.propertyName != null) {
            String[] splitted = StringUtils.splitByCharacterType(value.propertyName);
            id += "-" + splitted[splitted.length - 1].toLowerCase();
        }

        if (value.metadata.unit != null) {
            return id + "-" + value.metadata.unit.toLowerCase();
        }
        return id;
    }

    private @Nullable State getStateFromValue(Value value) {
        if ("Configuration".equals(value.commandClassName)) {
            logger.debug("Node id: '{}' getStateFromValue, Configuration commandClass ignored", nodeId);
            return null;
        }
        if (value.value == null) {
            return UnDefType.NULL;
        }
        State state = UnDefType.UNDEF;
        String itemTypeSplitted[] = this.itemType.split(":");
        switch (itemTypeSplitted[0]) {
            case "Number":
                if (itemTypeSplitted.length > 1) {
                    Unit<?> unit = Units.getInstance().getUnit(this.unit);
                    state = new QuantityType<>((Number) value.value, unit);
                } else {
                    state = new DecimalType((Number) value.value);
                }
                break;
            case "Switch":
                state = OnOffType.from((boolean) value.value);
            default:
                state = UnDefType.UNDEF;
                break;
        }
        return state;
    }

    public String itemTypeFromMetadata(Metadata data) {
        // TODO Not sure if this is the best way to parse a unit as string that returns a Unit or Dimension.
        switch (data.type) {
            case "number":
                if (data.unit != null) {
                    Unit<?> unit = Units.getInstance().getUnit(data.unit);
                    if (unit == null) {
                        logger.info("Could not parse '{}' as a unit, fallback to 'Number' itemType", data.unit);
                        return "Number";
                    }
                    return String.format("Number:{}", unit.getDimension().toString());
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
                        data.type);
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

    public StateDescriptionFragment statePatternOfItemType(Metadata data) {
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
        fragment.withReadOnly(!data.writeable);
        if (data.min != null) {
            fragment.withMinimum(BigDecimal.valueOf(data.min));
        }
        if (data.max != null) {
            fragment.withMaximum(BigDecimal.valueOf(data.max));
        }
        // fragment.withOptions(null);
        // TODO from states but need to find out how to properly deserialize it into a
        // key/value pair
        fragment.withStep(BigDecimal.valueOf(1));
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
