/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.chromecast.internal.handler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.digitalmediaserver.cast.CastDevice;
import org.digitalmediaserver.cast.CastEvent;
import org.digitalmediaserver.cast.CastEvent.CastEventType;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.chromecast.internal.ChromecastAudioSink;
import org.openhab.binding.chromecast.internal.ChromecastBindingConstants;
import org.openhab.binding.chromecast.internal.ChromecastCommander;
import org.openhab.binding.chromecast.internal.ChromecastEventReceiver;
import org.openhab.binding.chromecast.internal.ChromecastScheduler;
import org.openhab.binding.chromecast.internal.ChromecastStateDescriptionOptionProvider;
import org.openhab.binding.chromecast.internal.ChromecastStatusUpdater;
import org.openhab.binding.chromecast.internal.action.ChromecastActions;
import org.openhab.binding.chromecast.internal.config.ChromecastConfig;
import org.openhab.binding.chromecast.internal.storage.AppContainer;
import org.openhab.binding.chromecast.internal.storage.AppItem;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ChromecastHandler} is responsible for handling commands, which are sent to one of the channels. It
 * furthermore implements {@link AudioSink} support.
 *
 * @author Markus Rathgeb, Kai Kreuzer - Initial contribution
 * @author Daniel Walters - Online status fix, handle playuri channel and refactor play media code
 * @author Jason Holmes - Media Status. Refactor the monolith into separate classes.
 * @author Scott Hanson - Added Actions.
 * @author Leo Siepel - Added dynamic app channel incl. storage
 */
@NonNullByDefault
public class ChromecastHandler extends BaseThingHandler implements AudioSink {
    private final Logger logger = LoggerFactory.getLogger(ChromecastHandler.class);

    private static final Set<AudioFormat> SUPPORTED_FORMATS = Set.of(AudioFormat.MP3, AudioFormat.WAV);
    private static final Set<Class<? extends AudioStream>> SUPPORTED_STREAMS = Set.of(AudioStream.class);

    private final AudioHTTPServer audioHTTPServer;
    private final @Nullable String callbackUrl;
    private final AppContainer appContainer;
    private final ChromecastStateDescriptionOptionProvider stateDescriptionProvider;
    /**
     * The actual implementation. A new one is created each time #initialize is called.
     */
    private @Nullable Coordinator coordinator;

    /**
     * Constructor.
     *
     * @param thing the thing the coordinator should be created for
     * @param audioHTTPServer server for hosting audio streams
     * @param callbackUrl url to be used to tell the Chromecast which host to call for audio urls
     */
    public ChromecastHandler(final Thing thing, AudioHTTPServer audioHTTPServer, AppContainer appContainer,
            ChromecastStateDescriptionOptionProvider stateDescriptionProvider, @Nullable String callbackUrl) {
        super(thing);
        this.audioHTTPServer = audioHTTPServer;
        this.callbackUrl = callbackUrl;
        this.appContainer = appContainer;
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    @Override
    public void initialize() {
        ChromecastConfig config = getConfigAs(ChromecastConfig.class);

        final String hostName = config.host;
        if (hostName.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Cannot connect to Chromecast. Host name is not valid or missing.");
            return;
        }

        InetAddress inetAddress = null;
        try {
            inetAddress = java.net.InetAddress.getByName(hostName);
        } catch (UnknownHostException e) {
            logger.debug("Could not resolve InetAddress from host name: {} with mesage: {}", hostName, e.getMessage());
        }

        if (inetAddress == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Cannot connect to Chromecast. InetAddress could not be resolved from host name");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);

        Coordinator localCoordinator = coordinator;
        if (localCoordinator != null && (!localCoordinator.chromeCast.getAddress().equals(inetAddress)
                || (localCoordinator.chromeCast.getPort() != config.port))) {
            localCoordinator.destroy();
            localCoordinator = coordinator = null;
        }

        if (localCoordinator == null) {
            CastDevice chromecast = new CastDevice(config.host, inetAddress, null, null, null, null, null, null, 1,
                    null, true);
            localCoordinator = new Coordinator(this, thing, chromecast, config.refreshRate, audioHTTPServer,
                    callbackUrl);
            coordinator = localCoordinator;

            scheduler.submit(() -> {
                Coordinator c = coordinator;
                if (c != null) {
                    c.initialize();
                }
            });
        }

        this.syncRegisteredAppsWithChannel();
    }

    @Override
    public void dispose() {
        Coordinator localCoordinator = coordinator;
        if (localCoordinator != null) {
            localCoordinator.destroy();
            coordinator = null;
        }
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        Coordinator localCoordinator = coordinator;
        if (localCoordinator != null) {
            localCoordinator.commander.handleCommand(channelUID, command);
        } else {
            logger.debug("Cannot handle command. No coordinator has been initialized");
        }
    }

    @Override // Just exposing this for ChromecastStatusUpdater.
    public void updateState(String channelId, State state) {
        super.updateState(channelId, state);
    }

    @Override // Just exposing this for ChromecastStatusUpdater.
    public void updateState(ChannelUID channelUID, State state) {
        super.updateState(channelUID, state);
    }

    @Override // Just exposing this for ChromecastStatusUpdater.
    public void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }

    @Override // Just exposing this for ChromecastStatusUpdater.
    public boolean isLinked(String channelId) {
        return super.isLinked(channelId);
    }

    @Override // Just exposing this for ChromecastStatusUpdater.
    public boolean isLinked(ChannelUID channelUID) {
        return super.isLinked(channelUID);
    }

    @Override
    public String getId() {
        return thing.getUID().toString();
    }

    @Override
    public @Nullable String getLabel(@Nullable Locale locale) {
        return thing.getLabel();
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }

    @Override
    public Set<Class<? extends AudioStream>> getSupportedStreams() {
        return SUPPORTED_STREAMS;
    }

    @Override
    public void process(@Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {
        Coordinator localCoordinator = coordinator;
        if (localCoordinator != null) {
            localCoordinator.audioSink.process(audioStream);
        } else {
            logger.debug("Cannot process audioStream. No coordinator has been initialized.");
        }
    }

    @Override
    public PercentType getVolume() throws IOException {
        Coordinator localCoordinator = coordinator;
        if (localCoordinator != null) {
            return localCoordinator.statusUpdater.getVolume();
        } else {
            throw new IOException("Cannot get volume. No coordinator has been initialized.");
        }
    }

    @Override
    public void setVolume(PercentType percentType) throws IOException {
        Coordinator localCoordinator = coordinator;
        if (localCoordinator != null) {
            localCoordinator.commander.handleVolume(percentType);
        } else {
            throw new IOException("Cannot set volume. No coordinator has been initialized.");
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return List.of(ChromecastActions.class);
    }

    public boolean playURL(String url, @Nullable String mediaType) {
        Coordinator localCoordinator = coordinator;
        if (localCoordinator != null) {
            localCoordinator.commander.playMedia(null, url, mediaType);
            return true;
        }
        return false;
    }

    private static class Coordinator {
        private final Logger logger = LoggerFactory.getLogger(Coordinator.class);

        private static final long CONNECT_DELAY = 10;

        private final CastDevice chromeCast;
        private final ChromecastAudioSink audioSink;
        private final ChromecastCommander commander;
        private final ChromecastEventReceiver eventReceiver;
        private final ChromecastStatusUpdater statusUpdater;
        private final ChromecastScheduler scheduler;

        /**
         * used internally to represent the connection state
         */
        private enum ConnectionState {
            UNKNOWN,
            CONNECTING,
            CONNECTED,
            DISCONNECTING,
            DISCONNECTED
        }

        private ConnectionState connectionState = ConnectionState.UNKNOWN;

        private Coordinator(ChromecastHandler handler, Thing thing, CastDevice chromeCast, long refreshRate,
                AudioHTTPServer audioHttpServer, @Nullable String callbackURL) {
            this.chromeCast = chromeCast;

            this.scheduler = new ChromecastScheduler(handler.scheduler, CONNECT_DELAY, this::connect, refreshRate,
                    this::refresh);
            this.statusUpdater = new ChromecastStatusUpdater(thing, handler);

            this.commander = new ChromecastCommander(chromeCast, scheduler, statusUpdater);
            this.eventReceiver = new ChromecastEventReceiver(scheduler, statusUpdater);
            this.audioSink = new ChromecastAudioSink(commander, audioHttpServer, callbackURL);
        }

        void initialize() {
            if (connectionState == ConnectionState.CONNECTED) {
                logger.debug("Already connected");
                return;
            } else if (connectionState == ConnectionState.CONNECTING) {
                logger.debug("Already connecting");
                return;
            } else if (connectionState == ConnectionState.DISCONNECTING) {
                logger.warn("Trying to re-connect while still disconnecting");
                return;
            }
            connectionState = ConnectionState.CONNECTING;

            CastEvent.CastEventType[] subscribedEvents = new CastEventType[] { CastEventType.APPLICATION_AVAILABILITY,
                    CastEventType.CLOSE, CastEventType.CONNECTED, CastEventType.CUSTOM_MESSAGE,
                    CastEventType.DEVICE_ADDED, CastEventType.DEVICE_REMOVED, CastEventType.DEVICE_UPDATED,
                    CastEventType.ERROR_RESPONSE, CastEventType.LAUNCH_ERROR, CastEventType.MEDIA_STATUS,
                    CastEventType.MULTIZONE_STATUS, CastEventType.RECEIVER_STATUS, CastEventType.UNKNOWN };

            chromeCast.addEventListener(eventReceiver, subscribedEvents);
            connect();
        }

        void destroy() {
            connectionState = ConnectionState.DISCONNECTING;

            chromeCast.removeEventListener(eventReceiver);
            scheduler.destroy();

            try {
                chromeCast.disconnect();

                connectionState = ConnectionState.DISCONNECTED;
            } catch (final IOException e) {
                logger.debug("Disconnect failed: {}", e.getMessage());
                connectionState = ConnectionState.UNKNOWN;
            }
        }

        private void connect() {
            try {
                chromeCast.connect();

                statusUpdater.updateMediaStatus(null);
                statusUpdater.updateStatus(ThingStatus.ONLINE);

                connectionState = ConnectionState.CONNECTED;
            } catch (final IOException | GeneralSecurityException e) {
                logger.debug("Connect failed, trying to reconnect: {}", e.getMessage());
                statusUpdater.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                        e.getMessage());
                scheduler.scheduleConnect();
            }
        }

        private void refresh() {
            commander.handleRefresh();
        }
    }

    private void syncRegisteredAppsWithChannel() {
        List<StateOption> options = new ArrayList<>();
        // TODO: ordering list?
        appContainer.getAllApps().forEach(f -> options.add(new StateOption(f.getId(), f.getDisplayName())));

        stateDescriptionProvider.setStateOptions(
                new ChannelUID(getThing().getUID(), ChromecastBindingConstants.CHANNEL_APP_ID), options);
    }

    public void registerApplication(String appId, String displayName) {
        if (!appContainer.contains(appId)) {
            logger.debug("Registering availability of application: {} / {}", appId, displayName);
            appContainer.put(appId, new AppItem(appId, displayName));
            syncRegisteredAppsWithChannel();
        }
    }

    public void unRegisterApplication(String appId) {
        if (appContainer.contains(appId)) {
            logger.debug("Unregistering availability of application: {}", appId);
            appContainer.remove(appId);
            syncRegisteredAppsWithChannel();
        }
    }
}
