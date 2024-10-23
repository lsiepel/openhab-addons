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
import org.eclipse.jdt.annotation.Nullable;

/**
 * Configuration for a JeeLinkHandler.
 *
 * @author Volker Bier - Initial contribution
 */
@NonNullByDefault
public class JeeLinkConfig {
    public String ipAddress = "";
    public int port = 81;
    public @Nullable String serialPort;
    public int baudRate = 57600;
    public @Nullable String initCommands;
    public int initDelay = 10;
    public int reconnectInterval = 300;
}
