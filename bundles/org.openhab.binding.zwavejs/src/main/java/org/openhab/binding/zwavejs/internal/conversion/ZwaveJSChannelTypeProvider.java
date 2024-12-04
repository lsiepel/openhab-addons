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

import static org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants.BINDING_ID;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.api.dto.Metadata;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.StateChannelTypeBuilder;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CharacteristicChannelTypeProvider} that provides channel types for dynamically discovered characteristics.
 *
 * @author L. Siepel - Initial contribution
 */

@NonNullByDefault
@Component(service = { ZwaveJSChannelTypeProvider.class, ChannelTypeProvider.class })
public class ZwaveJSChannelTypeProvider implements ChannelTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(ZwaveJSChannelTypeProvider.class);

    private final Map<ChannelTypeUID, ChannelType> channelTypeCache = new ConcurrentHashMap<>();

    public ChannelTypeUID generateChannelTypeId(Metadata data) {
        StringBuilder parts = new StringBuilder();
        String itemType = itemTypeFromMetadata(data);
        parts.append(data.type);
        parts.append(itemType);
        parts.append(normalizeUnit(data.unit));
        if (!"String".equals(itemType)) {
            parts.append(statePatternOfItemType(data).hashCode());
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] array = messageDigest.digest(parts.toString().getBytes());
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                stringBuffer.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return new ChannelTypeUID(BINDING_ID, stringBuffer.toString());
        } catch (NoSuchAlgorithmException e) {
            logger.warn("NoSuchAlgorithmException error when calculating MD5 hash");
        }
        return new ChannelTypeUID(BINDING_ID, "unknown");
    }

    public ChannelType generateChannelType(Value value) {
        final ChannelTypeUID channelTypeUID = generateChannelTypeId(value.metadata);
        ChannelType channelType = channelTypeCache.get(channelTypeUID);
        if (channelType != null) {
            return channelType;
        }

        String itemType = itemTypeFromMetadata(value.metadata);

        StateChannelTypeBuilder builder = ChannelTypeBuilder.state(channelTypeUID, value.metadata.label, itemType)
                .withDescription(value.commandClassName);
        if (!"String".equals(itemType) || !"Switch".equals(itemType)) {
            builder.withStateDescriptionFragment(statePatternOfItemType(value.metadata));
        }

        if (itemType.contains(":")) {
            builder.withUnitHint(normalizeUnit(value.metadata.unit));
        }

        channelType = builder.build();
        // TODO add category and tags
        channelTypeCache.put(channelTypeUID, channelType);
        return channelType;
    }

    private String itemTypeFromMetadata(Metadata data) {
        // TODO Not sure if this is the best way to parse a unit as string that returns a Unit or Dimension.
        switch (data.type) {
            case "number":
                if (data.unit != null) {
                    Unit<?> unit = Units.getInstance().getUnit(normalizeUnit(data.unit));
                    if (unit == null) {
                        logger.info("Could not parse '{}' as a unit, fallback to 'Number' itemType",
                                normalizeUnit(data.unit));
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

    private String normalizeUnit(@Nullable String unit) {
        if (unit == null) {
            return "";
        }
        String[] splitted = unit.split(" ");
        return splitted[splitted.length - 1] //
                .replace("minutes", "min") //
                .replace("seconds", "s");
    }

    private StateDescriptionFragment statePatternOfItemType(Metadata data) {
        String pattern = "";
        String itemTypeSplitted[] = itemTypeFromMetadata(data).split(":");
        switch (itemTypeSplitted[0]) {
            case "number":
                if (itemTypeSplitted.length > 1) {
                    pattern = "%0.f %unit%"; // TODO how to determine the decimals
                } else {
                    pattern = "%0.d";
                }
                break;
            case "boolean":
            default:
                pattern = "";
                break;
        }

        var fragment = StateDescriptionFragmentBuilder.create();
        fragment.withPattern(pattern);
        fragment.withReadOnly(!data.writeable);
        fragment.withMinimum(BigDecimal.valueOf(data.min));
        fragment.withMaximum(BigDecimal.valueOf(data.max));
        // fragment.withOptions(null);
        // TODO from states but need to find out how to properly deserialize it into a
        // key/value pair
        fragment.withStep(BigDecimal.valueOf(1));
        // TODO there does not seem to be a property that can be used for this
        return fragment.build();
    }

    @Override
    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        return channelTypeCache.values();
    }

    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        return channelTypeCache.get(channelTypeUID);
    }
}
