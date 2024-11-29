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
package org.openhab.binding.zwavejs.internal.discovery;

import static org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zwavejs.internal.ZwaveJSBindingConstants;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.binding.zwavejs.internal.handler.ZwaveJSBridgeHandler;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NodeDiscoveryService} tracks for Z-Wave nodes which are connected
 * to a Z-Wave JS webservice.
 *
 * @author Leo Siepel - Initial contribution
 */
@Component(scope = ServiceScope.PROTOTYPE, service = NodeDiscoveryService.class)
@NonNullByDefault
public class NodeDiscoveryService extends AbstractThingHandlerDiscoveryService<ZwaveJSBridgeHandler> {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream.of(THING_TYPE_NODE)
            .collect(Collectors.toUnmodifiableSet());

    private static final int SEARCH_TIME = 10;

    private final Logger logger = LoggerFactory.getLogger(NodeDiscoveryService.class);

    private @Nullable ThingUID bridgeUID;

    /**
     * Creates an NodeDiscoveryService with enabled autostart.
     */
    @Activate
    public NodeDiscoveryService() {
        super(ZwaveJSBridgeHandler.class, SUPPORTED_THING_TYPES, SEARCH_TIME);
        logger.info("Initilizing1 Z-Wave discovery");
    }

    @Reference(unbind = "-")
    public void bindTranslationProvider(TranslationProvider translationProvider) {
        this.i18nProvider = translationProvider;
    }

    @Reference(unbind = "-")
    public void bindLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    @Override
    public void initialize() {
        logger.info("Initilizing Z-Wave discovery");
        bridgeUID = thingHandler.getThing().getUID();
        thingHandler.registerDiscoveryListener(this);
        logger.info("Initialized Z-Wave discovery");
        super.initialize();
    }

    @Override
    public void dispose() {
        super.dispose();
        removeOlderResults(Instant.now().toEpochMilli(), bridgeUID);
        thingHandler.unregisterDiscoveryListener();
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES;
    }

    @Override
    public void startScan() {
        logger.info("Scanning Z-Wave discovery");
        thingHandler.getFullState();
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan(), thingHandler.getThing().getUID());
    }

    public void addNodeDiscovery(Node node) {
        logger.info("Z-Wave addNodeDiscovery id: '{}'", node.nodeId);
        ThingUID thingUID = getThingUID(node.nodeId);
        ThingTypeUID thingTypeUID = THING_TYPE_NODE;

        if (thingUID != null && thingTypeUID != null) {
            String label = String.format(DISCOVERY_NODE_LABEL_PATTERN, node.nodeId, node.deviceConfig.label);

            Map<String, Object> properties = new HashMap<>();

            properties.put(CONFIG_NODE_ID, node.nodeId);

            properties.put(PROPERTY_NODE_IS_LISTENING, node.isListening);
            properties.put(PROPERTY_NODE_IS_ROUTING, node.isRouting);
            properties.put(PROPERTY_NODE_IS_SECURE, node.isSecure);
            properties.put(PROPERTY_NODE_MANUFACTURER, node.deviceConfig.manufacturer);
            properties.put(PROPERTY_NODE_PRODUCT_ID, node.productId);
            properties.put(PROPERTY_NODE_PRODUCT_TYPE, node.productType);

            //ThingType type = ThingTypeBuilder.instance(thingTypeUID, label).withLabel(label).withDescription(label);

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                    .withProperties(properties).withBridge(bridgeUID).withRepresentationProperty(CONFIG_NODE_ID)
                    .withLabel(label).build();

            thingDiscovered(discoveryResult);
        } else {
            logger.debug("Discovered unsupported device, nodeId '{}'", node.nodeId);
        }
    }

    public void removeNodeDiscovery(int nodeId) {
        logger.info("Z-Wave removeNodeDiscovery id: '{}'", nodeId);
        ThingUID thingUID = getThingUID(nodeId);

        if (thingUID != null) {
            thingRemoved(thingUID);
        }
    }

    private @Nullable ThingUID getThingUID(int nodeId) {
        ThingUID localBridgeUID = bridgeUID;
        if (localBridgeUID != null) {
            return new ThingUID(ZwaveJSBindingConstants.THING_TYPE_NODE, localBridgeUID, "node" + nodeId);
        }
        return null;
    }
}
