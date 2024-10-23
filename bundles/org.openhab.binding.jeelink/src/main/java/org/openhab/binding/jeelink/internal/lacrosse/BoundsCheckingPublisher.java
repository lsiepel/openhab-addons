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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.jeelink.internal.ReadingPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that the given temperature is in range before passing it on to the next publisher.
 *
 * @author Volker Bier - Initial contribution
 */
@NonNullByDefault
public class BoundsCheckingPublisher implements ReadingPublisher<LaCrosseTemperatureReading> {
    private final Logger logger = LoggerFactory.getLogger(BoundsCheckingPublisher.class);

    private final ReadingPublisher<LaCrosseTemperatureReading> publisher;

    private final float minTemp;
    private final float maxTemp;

    public BoundsCheckingPublisher(float min, float max, ReadingPublisher<LaCrosseTemperatureReading> p) {
        minTemp = min;
        maxTemp = max;
        publisher = p;
    }

    @Override
    public void publish(@Nullable LaCrosseTemperatureReading reading) {
        Float temperature = reading != null ? reading.getTemperature() : null;

        if (temperature != null && temperature >= minTemp && temperature <= maxTemp) {
            publisher.publish(reading);
        } else {
            logger.debug("Ignoring out of bounds reading {}", temperature);
        }
    }

    @Override
    public void dispose() {
        publisher.dispose();
    }
}
