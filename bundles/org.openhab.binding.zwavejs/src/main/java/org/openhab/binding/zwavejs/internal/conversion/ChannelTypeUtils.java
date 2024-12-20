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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.StateChannelTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author L. Siepel - Initial contribution
 */
@NonNullByDefault
public class ChannelTypeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelTypeUtils.class);

    private final Map<ChannelTypeUID, ChannelType> channelTypeCache = new ConcurrentHashMap<>();

    public static ChannelTypeUID generateChannelTypeId(ChannelDetails details) {
        StringBuilder parts = new StringBuilder();

        // parts.append(details.type);
        parts.append(details.itemType);
        parts.append(details.unit);
        parts.append(details.writable);
        if (details.statePattern != null) {
            parts.append(details.statePattern.hashCode());
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] array = messageDigest.digest(parts.toString().getBytes());
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                stringBuffer.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return new ChannelTypeUID(ZwaveJSBindingConstants.BINDING_ID, stringBuffer.toString());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("NoSuchAlgorithmException error when calculating MD5 hash");
        }
        return new ChannelTypeUID(ZwaveJSBindingConstants.BINDING_ID, "unknown");
    }

    public static @Nullable ChannelType generateChannelType(ChannelDetails details) {
        if (details.ignoreAsChannel) {
            return null;
        }
        final ChannelTypeUID channelTypeUID = generateChannelTypeId(details);
        return generateChannelType(channelTypeUID, details);
    }

    public static ChannelType generateChannelType(ChannelTypeUID channelTypeUID, ChannelDetails details) {
        StateChannelTypeBuilder builder = ChannelTypeBuilder.state(channelTypeUID, details.label, details.itemType)
                .withDescription(details.description);
        if (details.statePattern != null) {
            builder.withStateDescriptionFragment(details.statePattern);
        }

        if (details.unit != null) {
            builder.withUnitHint(details.unit);
        }

        return builder.build();
    }

    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        return channelTypeCache.values();
    }

    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        return channelTypeCache.get(channelTypeUID);
    }
}
