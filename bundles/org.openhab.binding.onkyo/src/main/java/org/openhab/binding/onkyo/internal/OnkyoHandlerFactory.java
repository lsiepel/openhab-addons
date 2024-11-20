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
package org.openhab.binding.onkyo.internal;

import static org.openhab.binding.onkyo.internal.OnkyoBindingConstants.SUPPORTED_THING_TYPES_UIDS;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.onkyo.internal.handler.OnkyoAudioSink;
import org.openhab.binding.onkyo.internal.handler.OnkyoHandler;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.io.transport.upnp.UpnpIOService;
import org.openhab.core.net.HttpServiceUtil;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OnkyoHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Paul Frank - Initial contribution
 * @author Stewart Cossey - added dynamic state descriptor provider functions
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.onkyo")
public class OnkyoHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(OnkyoHandlerFactory.class);

    private final Map<String, ServiceRegistration<AudioSink>> audioSinkRegistrations = new ConcurrentHashMap<>();

    private UpnpIOService upnpIOService;
    private AudioHTTPServer audioHTTPServer;
    private NetworkAddressService networkAddressService;
    private OnkyoStateDescriptionProvider stateDescriptionProvider;

    // url (scheme+server+port) to use for playing notification sounds
    private @Nullable String callbackUrl;

    @Activate
    public OnkyoHandlerFactory(@Reference final UpnpIOService upnpIOService,
            @Reference final AudioHTTPServer audioHTTPServer,
            @Reference final NetworkAddressService networkAddressService,
            @Reference final OnkyoStateDescriptionProvider provider) {
        this.upnpIOService = upnpIOService;
        this.audioHTTPServer = audioHTTPServer;
        this.networkAddressService = networkAddressService;
        this.stateDescriptionProvider = provider;
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        Dictionary<String, Object> properties = componentContext.getProperties();
        callbackUrl = (String) properties.get("callbackUrl");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            String callbackUrl = createCallbackUrl();
            OnkyoHandler handler = new OnkyoHandler(thing, upnpIOService, stateDescriptionProvider);
            OnkyoAudioSink audioSink = new OnkyoAudioSink(handler, audioHTTPServer, callbackUrl);
            @SuppressWarnings("unchecked")
            ServiceRegistration<AudioSink> reg = (ServiceRegistration<AudioSink>) bundleContext
                    .registerService(AudioSink.class.getName(), audioSink, new Hashtable<>());
            audioSinkRegistrations.put(thing.getUID().toString(), reg);
            return handler;
        }

        return null;
    }

    private @Nullable String createCallbackUrl() {
        String callbackUrl = this.callbackUrl;
        if (callbackUrl != null) {
            return callbackUrl;
        } else {
            final String ipAddress = networkAddressService.getPrimaryIpv4HostAddress();
            if (ipAddress == null) {
                logger.warn("No primary IPv4 host address could be found.");
                return null;
            }

            // we do not use SSL as it can cause certificate validation issues.
            final int port = HttpServiceUtil.getHttpServicePort(bundleContext);
            if (port == -1) {
                logger.warn("Cannot find port of the http service.");
                return null;
            }

            return "http://" + ipAddress + ":" + port;
        }
    }

    @Override
    public void unregisterHandler(Thing thing) {
        super.unregisterHandler(thing);
        ServiceRegistration<AudioSink> reg = audioSinkRegistrations.get(thing.getUID().toString());
        if (reg != null) {
            reg.unregister();
        }
    }
}
