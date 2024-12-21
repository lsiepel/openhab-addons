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
package org.openhab.binding.zwavejs.internal.type;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.api.dto.Value;
import org.openhab.binding.zwavejs.internal.conversion.ChannelDetails;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.StateChannelTypeBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates openHAB entities (Channel, ChannelType and ConfigDescription) based on the Z-Wave JS data.
 *
 * @author Leo Siepel - Initial contribution
 */
@Component
@NonNullByDefault
public class ZwaveJSTypeGeneratorImpl implements ZwaveJSTypeGenerator {
    private final Logger logger = LoggerFactory.getLogger(ZwaveJSTypeGeneratorImpl.class);

    private ZwaveJSChannelTypeProvider channelTypeProvider;
    private ZwaveJSConfigDescriptionProvider configDescriptionProvider;

    @Activate
    public ZwaveJSTypeGeneratorImpl(@Reference ZwaveJSChannelTypeProvider channelTypeProvider,
            @Reference ZwaveJSConfigDescriptionProvider configDescriptionProvider) {
        this.channelTypeProvider = channelTypeProvider;
        this.configDescriptionProvider = configDescriptionProvider;
    }

    @Override
    public ZwaveJSTypeGeneratorResult generate(ThingUID thingUID, Node node) {
        ZwaveJSTypeGeneratorResult result = new ZwaveJSTypeGeneratorResult();
        for (Value value : node.values) {

            ChannelDetails details = new ChannelDetails(node.nodeId, value);
            if (!details.ignoreAsChannel) {
                result.channels = createChannel(thingUID, result.channels, details);
            } else if (details.ignoreAsChannel) {
                result.configDescriptions = createConfigDescriptions(result.configDescriptions, details);
            }
        }
        return result;
    }

    private List<ConfigDescription> createConfigDescriptions(List<ConfigDescription> configDescriptions,
            ChannelDetails details) {
        logger.debug("Node '{}' createConfigDescriptions with Id: {}", details.nodeId, details.channelId);
        return configDescriptions;
    }

    private Map<String, Channel> createChannel(ThingUID thingUID, Map<String, Channel> channels,
            ChannelDetails details) {
        logger.debug("Node '{}' createChannel with Id: {}", details.nodeId, details.channelId);
        logger.trace(" >> {}", details);
        ChannelUID channelUID = new ChannelUID(thingUID, details.channelId);

        Channel existingChannel = channels.get(channelUID);
        if (existingChannel != null) {
            Configuration channelConfig = existingChannel.getConfiguration();
            if (channelConfig.get(ZwaveJSBindingConstants.CONFIG_CHANNEL_WRITE_PROPERTY) == null && details.writable
                    && details.writeProperty != null) {
                channelConfig.put(ZwaveJSBindingConstants.CONFIG_CHANNEL_WRITE_PROPERTY, details.writeProperty);
                channels.put(details.channelId,
                        ChannelBuilder.create(existingChannel).withConfiguration(channelConfig).build());
                logger.debug("Node {}, channel {} existing channel updated", details.nodeId, details.channelId);
                return channels;
            } else {
                logger.warn("Node {}, channel {} already exists: ignored", details.nodeId, details.channelId);
                return channels;
            }
        }

        Configuration configuration = new Configuration();
        configuration.put(ZwaveJSBindingConstants.CONFIG_CHANNEL_INCOMING_UNIT, details.unit);
        configuration.put(ZwaveJSBindingConstants.CONFIG_CHANNEL_ITEM_TYPE, details.itemType);
        configuration.put(ZwaveJSBindingConstants.CONFIG_CHANNEL_COMMANDCLASS_ID, details.commandClassId);
        configuration.put(ZwaveJSBindingConstants.CONFIG_CHANNEL_COMMANDCLASS_NAME, details.commandClassName);
        configuration.put(ZwaveJSBindingConstants.CONFIG_CHANNEL_ENDPOINT, details.endpoint);

        if (details.writable) {
            configuration.put(ZwaveJSBindingConstants.CONFIG_CHANNEL_WRITE_PROPERTY, details.writeProperty);
        }

        ChannelTypeUID channelTypeUID = generateChannelTypeUID(details);
        ChannelType channelType = channelTypeProvider.getChannelType(channelTypeUID, null);
        if (channelType == null) {
            channelType = generateChannelType(details);
            if (channelType != null) {
                channelTypeProvider.addChannelType(channelType);
            }
        }
        channels.put(details.channelId, ChannelBuilder.create(channelUID).withConfiguration(configuration)
                .withType(channelType.getUID()).build());
        return channels;
    }

    private ChannelTypeUID generateChannelTypeUID(ChannelDetails details) {
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
            logger.warn("NoSuchAlgorithmException error when calculating MD5 hash");
        }
        return new ChannelTypeUID(ZwaveJSBindingConstants.BINDING_ID, "unknown");
    }

    private @Nullable ChannelType generateChannelType(ChannelDetails details) {
        if (details.ignoreAsChannel) {
            return null;
        }
        final ChannelTypeUID channelTypeUID = generateChannelTypeUID(details);
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

    public static ConfigDescription generateConfigDescription(List<ChannelDetails> details, URI configDescriptionURI) {
        List<ConfigDescriptionParameter> parms = new ArrayList<>();

        for (ChannelDetails detail : details) {
            ConfigDescriptionParameterBuilder builder = ConfigDescriptionParameterBuilder.create(detail.label,
                    detail.configType);

            builder.withReadOnly(!detail.writable);
            builder.withDescription(detail.description);

            // builder.withMinimum(MetadataUtils.createBigDecimal(minValue));
            // builder.withMaximum(MetadataUtils.createBigDecimal(maxValue));
            // if (detail.unit != null) {
            // builder.withUnitLabel(detail.unit);
            // }
            parms.add(builder.build());
        }

        return ConfigDescriptionBuilder.create(configDescriptionURI).withParameters(parms).build();
    }

    private @Nullable URI getConfigDescriptionURI(Node node) {
        try {
            return new URI("thing-type:zwavejs:node:" + node.nodeId);
        } catch (URISyntaxException ex) {
            logger.warn("Can't create configDescriptionURI for node {}", node.nodeId);
            return null;
        }
    }
}
