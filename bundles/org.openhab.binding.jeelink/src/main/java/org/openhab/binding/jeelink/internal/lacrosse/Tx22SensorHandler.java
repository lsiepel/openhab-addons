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
package org.openhab.binding.jeelink.internal.lacrosse;

import static org.openhab.binding.jeelink.internal.JeeLinkBindingConstants.*;
import static org.openhab.core.library.unit.MetricPrefix.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.jeelink.internal.JeeLinkSensorHandler;
import org.openhab.binding.jeelink.internal.ReadingPublisher;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for a TX22 Temperature/Humidity Sensor thing.
 *
 * @author Volker Bier - Initial contribution
 */
@NonNullByDefault
public class Tx22SensorHandler extends JeeLinkSensorHandler<Tx22Reading> {
    private final Logger logger = LoggerFactory.getLogger(Tx22SensorHandler.class);

    public Tx22SensorHandler(Thing thing, String sensorType) {
        super(thing, sensorType);
    }

    @Override
    public Class<Tx22Reading> getReadingClass() {
        return Tx22Reading.class;
    }

    @Override
    public ReadingPublisher<Tx22Reading> createPublisher() {
        return new ReadingPublisher<>() {
            @Override
            public void publish(@Nullable Tx22Reading reading) {
                if (reading != null && getThing().getStatus() == ThingStatus.ONLINE) {
                    logger.debug("updating states for thing {} ({}): {}", getThing().getLabel(),
                            getThing().getUID().getId(), reading);

                    updateState(BATTERY_NEW_CHANNEL, OnOffType.from(reading.isBatteryNew()));
                    updateState(BATTERY_LOW_CHANNEL, OnOffType.from(reading.isBatteryLow()));
                    Float temperature = reading.getTemperature();
                    if (temperature != null) {
                        BigDecimal temp = new BigDecimal(temperature).setScale(1, RoundingMode.HALF_UP);
                        updateState(TEMPERATURE_CHANNEL, new QuantityType<>(temp, SIUnits.CELSIUS));
                    }
                    Integer humidity = reading.getHumidity();
                    if (humidity != null) {
                        updateState(HUMIDITY_CHANNEL, new QuantityType<>(humidity, Units.PERCENT));
                    }
                    Integer rain = reading.getRain();
                    if (rain != null) {
                        updateState(RAIN_CHANNEL, new QuantityType<>(rain, MILLI(SIUnits.METRE)));
                    }
                    Integer pressure = reading.getPressure();
                    if (pressure != null) {
                        updateState(PRESSURE_CHANNEL, new QuantityType<>(pressure, HECTO(SIUnits.PASCAL)));
                    }
                    Float windDirection = reading.getWindDirection();
                    if (windDirection != null) {
                        updateState(WIND_ANGLE_CHANNEL, new QuantityType<>(windDirection, Units.DEGREE_ANGLE));
                    }
                    Float windSpeed = reading.getWindSpeed();
                    if (windSpeed != null) {
                        updateState(WIND_STENGTH_CHANNEL, new QuantityType<>(windSpeed, Units.METRE_PER_SECOND));
                    }
                    Float windGust = reading.getWindGust();
                    if (windGust != null) {
                        updateState(GUST_STRENGTH_CHANNEL, new QuantityType<>(windGust, Units.METRE_PER_SECOND));
                    }
                }
            }

            @Override
            public void dispose() {
            }
        };
    }
}
