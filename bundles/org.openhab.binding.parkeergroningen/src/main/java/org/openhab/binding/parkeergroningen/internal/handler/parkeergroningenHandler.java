/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.parkeergroningen.internal.handler;

import static org.openhab.binding.parkeergroningen.internal.parkeergroningenBindingConstants.*;

import java.time.ZoneId;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.parkeergroningen.internal.api.controller.RestController;
import org.openhab.binding.parkeergroningen.internal.config.parkeergroningenConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link parkeergroningenHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class parkeergroningenHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(parkeergroningenHandler.class);
    private final RestController restController;
    private @Nullable parkeergroningenConfiguration config;

    public parkeergroningenHandler(Thing thing, HttpClient client) {
        super(thing);
        this.restController = new RestController(client);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            // TODO: implement refresh task
            // updateState(channelUID, OnOffType.ON);
            // updateState(channelUID, new StringType("ABC-123"));
            // updateState(channelUID, new DateTimeType(ZonedDateTime.now()));
            // updateState(channelUID, new DateTimeType(ZonedDateTime.now().plusHours(1)));
        } else {

            // Handle other commands for each channel
            if (CHANNEL_ACTIVE.equals(channelUID.getId())) {
                // Example: Toggle active state based on command
                if (command instanceof OnOffType) {
                    if (OnOffType.ON.equals(command)) {
                        restController.activate();
                    } else {
                        restController.deactivate();
                    }
                }
            } else if (CHANNEL_LICENSE_PLATE.equals(channelUID.getId())) {
                if (command instanceof StringType strCommand) {
                    restController.data.setLicensePlate(strCommand.toString());
                }
            } else if (CHANNEL_START.equals(channelUID.getId())) {
                if (command instanceof DateTimeType datetimeTypeCommand) {
                    restController.data
                            .setDateFrom(datetimeTypeCommand.getZonedDateTime(ZoneId.of("Europe/Amsterdam")));
                }
            } else if (CHANNEL_END.equals(channelUID.getId())) {
                // Example: Accept DateTimeType command as new end time
                if (command instanceof DateTimeType datetimeTypeCommand) {
                    restController.data
                            .setDateUntil(datetimeTypeCommand.getZonedDateTime(ZoneId.of("Europe/Amsterdam")));
                }
            } else {
                logger.debug("Command {} not supported for channel {}", command, channelUID.getId());
            }
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(parkeergroningenConfiguration.class);

        if (!config.isValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid configuration");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);

        scheduler.schedule(this::validateLogin, 0, null);
    }

    public void validateLogin() {
        if (restController.login(config.cardNumber, config.pin)) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Login failed");
        }
    }
}
