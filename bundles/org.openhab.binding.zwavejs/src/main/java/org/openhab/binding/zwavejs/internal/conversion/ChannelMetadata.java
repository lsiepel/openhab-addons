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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.api.dto.Event;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ChannelMetadata} class represents channel metadata information for a Z-Wave node.
 * It contains various properties and methods to handle metadata and state information.
 * 
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class ChannelMetadata extends BaseMetadata {

    private static final Logger logger = LoggerFactory.getLogger(ChannelMetadata.class);
    private static final List<String> IGNORED_COMMANDCLASSES = List.of("Manufacturer Specific", "Version");

    public boolean writable;
    public @Nullable State state;
    public @Nullable StateDescriptionFragment statePattern;
    public @Nullable String commandClassName;
    public int commandClassId;
    public int endpoint;

    public ChannelMetadata(int nodeId, Value data) {
        super(nodeId, data);

        // confirmed
        // should be pulled up from base this.itemType = itemTypeFromMetadata(data.metadata.type, data.value);
        this.statePattern = createStatePattern(data.metadata.writeable, data.metadata.min, data.metadata.max, 1);
        this.state = toState(data.value, itemType, unit);

        // unknown
        this.commandClassName = data.commandClassName;
        this.commandClassId = data.commandClass;
        this.endpoint = data.endpoint;
    }

    public ChannelMetadata(int nodeId, Event data) {
        super(nodeId, data);
    }

    public boolean isIgnoredCommandClass(@Nullable String commandClassName) {
        if (commandClassName == null) {
            return false;
        }
        return IGNORED_COMMANDCLASSES.contains(commandClassName);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChannelMetadata [");
        sb.append(", nodeId=" + nodeId);
        sb.append(", Id=" + Id);
        sb.append(", label=" + label);
        sb.append(", description=" + description);
        sb.append(", unitSymbol=" + unitSymbol);
        sb.append(", value=" + value);
        sb.append(", itemType=" + itemType);
        sb.append(", writable=" + writable);
        sb.append(", writeProperty=" + writeProperty);
        sb.append(", state=" + state);
        sb.append(", statePattern=" + statePattern);
        sb.append(", commandClassName=" + commandClassName);
        sb.append(", commandClassId=" + commandClassId);
        sb.append(", endpoint=" + endpoint);
        sb.append("]");
        return sb.toString();
    }
}
