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
package org.openhab.binding.jeelink.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Configuration for a LaCrossTemperatureSensorHandler.
 *
 * @author Volker Bier - Initial contribution
 */
@NonNullByDefault
public class LaCrosseTemperatureSensorConfig extends BufferedSensorConfig {
    public float minTemp = -100;
    public float maxTemp = 100;
    public float maxDiff = 2;
}
