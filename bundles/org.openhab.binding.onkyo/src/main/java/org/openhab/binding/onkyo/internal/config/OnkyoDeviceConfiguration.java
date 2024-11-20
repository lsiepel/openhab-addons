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
package org.openhab.binding.onkyo.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Configuration class for Onkyo device.
 *
 * @author Pauli Anttila - Initial contribution
 */
@NonNullByDefault
public class OnkyoDeviceConfiguration {

    public String ipAddress = "";
    public int port = 60128;
    public String udn = "";
    public int refreshInterval = 0;
    public int volumeLimit = 100;
    public double volumeScale = 1.0d;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("ipAddress = {}" + ipAddress);
        builder.append(", port = " + port);
        builder.append(", udn = " + udn);
        builder.append(", refreshInterval = " + refreshInterval);
        builder.append(", volumeLimit = " + volumeLimit);
        builder.append(", volumeScale = " + volumeScale);

        return builder.toString();
    }
}
