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
import org.openhab.binding.zwavejs.internal.conversion.MetadataEntry;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
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
 * @see ChannelType
 * @see ChannelTypeBuilder
 * @see ChannelTypeUID
 * @see StateChannelTypeBuilder
 * @see ThingRegistry
 * @see ZwaveJSChannelTypeProvider
 * @see ZwaveJSConfigDescriptionProvider
 *
 * @author Leo Siepel - Initial contribution
 */
@Component
@NonNullByDefault
public class ZwaveJSTypeGeneratorImpl implements ZwaveJSTypeGenerator {
    private final Logger logger = LoggerFactory.getLogger(ZwaveJSTypeGeneratorImpl.class);

    private final ThingRegistry thingRegistry;
    private final ZwaveJSChannelTypeProvider channelTypeProvider;
    private final ZwaveJSConfigDescriptionProvider configDescriptionProvider;

    @Activate
    public ZwaveJSTypeGeneratorImpl(@Reference ZwaveJSChannelTypeProvider channelTypeProvider,
            @Reference ZwaveJSConfigDescriptionProvider configDescriptionProvider,
            @Reference ThingRegistry thingRegistry) {
        this.channelTypeProvider = channelTypeProvider;
        this.configDescriptionProvider = configDescriptionProvider;
        this.thingRegistry = thingRegistry;
    }

    /**
     * Retrieves a Thing by its UID.
     *
     * @param thingUID the UID of the Thing
     * @return the Thing, or null if not found
     */
    public @Nullable Thing getThing(ThingUID thingUID) {
        return thingRegistry.get(thingUID);
    }

    /**
     * Generates Z-Wave JS types based on the provided Thing UID and Node.
     *
     * @param thingUID the UID of the Thing
     * @param node the Node containing Z-Wave JS data
     * @return the result containing generated types
     */
    @Override
    public ZwaveJSTypeGeneratorResult generate(ThingUID thingUID, Node node) {
        ZwaveJSTypeGeneratorResult result = new ZwaveJSTypeGeneratorResult();
        List<ConfigDescriptionParameter> configDescriptions = new ArrayList<>();
        URI uri = getConfigDescriptionURI(thingUID, node);
        for (Value value : node.values) {
            MetadataEntry details = new MetadataEntry(node.nodeId, value);
            if (details.isChannel) {
                result.channels = createChannel(thingUID, result.channels, details);
            } else if (details.isConfiguration) {
                configDescriptions.add(createConfigDescription(details));
            }
        }
        if (uri != null) {
            configDescriptionProvider.addConfigDescription(
                    ConfigDescriptionBuilder.create(uri).withParameters(configDescriptions).build());
        }
        return result;
    }

    private ConfigDescriptionParameter createConfigDescription(MetadataEntry details) {
        logger.debug("Node '{}' createConfigDescriptions with Id: {}", details.nodeId, details.channelId);

        ConfigDescriptionParameterBuilder parameterBuilder = ConfigDescriptionParameterBuilder
                .create(details.channelId, details.configType) //
                .withRequired(true) //
                .withContext("item") //
                .withLabel(details.label) //
                .withVerify(true).withUnit(null).withDescription(details.description);

        if (details.unitSymbol != null) {
            parameterBuilder.withUnit(details.unitSymbol);
        }

        if (details.optionList != null) {
            List<ParameterOption> options = new ArrayList<>();
            details.optionList.forEach((k, v) -> options.add(new ParameterOption(k, v)));
            logger.debug("Node '{}' adding {} options for Id: {}", details.nodeId, details.optionList.size(),
                    details.channelId);
            parameterBuilder.withLimitToOptions(true);
            parameterBuilder.withMultiple(false);
            parameterBuilder.withOptions(options);
        }

        return parameterBuilder.build();
    }

    private Map<String, Channel> createChannel(ThingUID thingUID, Map<String, Channel> channels,
            MetadataEntry details) {
        logger.debug("Node '{}' createChannel with Id: {}", details.nodeId, details.channelId);
        logger.trace(" >> {}", details);
        ChannelUID channelUID = new ChannelUID(thingUID, details.channelId);

        @Nullable
        Channel existingChannel = channels.get(channelUID.getAsString());
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
        configuration.put(ZwaveJSBindingConstants.CONFIG_CHANNEL_INCOMING_UNIT, details.unitSymbol);
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
        if (channelType == null) {
            logger.warn("Node {}, channel {}, ChannelType could not be found or generated, this is a bug",
                    details.nodeId, details.channelId);
        }
        channels.put(details.channelId, ChannelBuilder.create(channelUID).withConfiguration(configuration)
                .withType(channelType.getUID()).build());

        return channels;
    }

    private ChannelTypeUID generateChannelTypeUID(MetadataEntry details) {
        StringBuilder parts = new StringBuilder();

        parts.append(details.itemType);
        parts.append(details.unitSymbol);
        parts.append(details.writable);
        if (details.statePattern != null) {
            parts.append(details.statePattern.hashCode());
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] array = messageDigest.digest(parts.toString().getBytes());
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < array.length; ++i) {
                stringBuilder.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return new ChannelTypeUID(ZwaveJSBindingConstants.BINDING_ID, stringBuilder.toString());
        } catch (NoSuchAlgorithmException e) {
            logger.warn("NoSuchAlgorithmException error when calculating MD5 hash");
        }
        return new ChannelTypeUID(ZwaveJSBindingConstants.BINDING_ID, "unknown");
    }

    private @Nullable ChannelType generateChannelType(MetadataEntry details) {
        if (!details.isChannel) {
            return null;
        }
        final ChannelTypeUID channelTypeUID = generateChannelTypeUID(details);
        return generateChannelType(channelTypeUID, details);
    }

    private ChannelType generateChannelType(ChannelTypeUID channelTypeUID, MetadataEntry details) {
        StateChannelTypeBuilder builder = ChannelTypeBuilder.state(channelTypeUID, details.label, details.itemType)
                .withDescription(details.description);

        if (details.statePattern != null) {
            builder.withStateDescriptionFragment(details.statePattern);
        }

        if (details.unitSymbol != null) {
            builder.withUnitHint(details.unitSymbol);
        }

        return builder.build();
    }

    private @Nullable URI getConfigDescriptionURI(ThingUID thingUID, Node node) {
        Thing thing = getThing(thingUID);
        if (thing == null) {
            logger.debug("Thing '{}'' not found in registry for getConfigDescriptionURI", thingUID);
            return null;
        }
        ThingUID bridgeUID = thing.getBridgeUID();
        if (bridgeUID == null) {
            logger.debug("No bridgeUID found for Thing '{}'' in getConfigDescriptionURI", thingUID);
            return null;
        }

        try {
            return new URI(String.format("thing:%s:node:%s:node%s", ZwaveJSBindingConstants.BINDING_ID,
                    bridgeUID.getId(), node.nodeId));
        } catch (URISyntaxException ex) {
            logger.warn("Can't create configDescriptionURI for node {}", node.nodeId);
            return null;
        }
    }
}
