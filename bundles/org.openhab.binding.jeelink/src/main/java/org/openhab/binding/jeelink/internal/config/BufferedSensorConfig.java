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
 * Configuration for a Handler that is able to buffer values.
 *
 * @author Volker Bier - Initial contribution
 */
@NonNullByDefault
public class BufferedSensorConfig extends JeeLinkSensorConfig {
    public int updateInterval = 60;
    public int bufferSize = 20;
}
