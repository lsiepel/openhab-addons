/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.yamahareceiver.internal.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.yamahareceiver.internal.YamahaReceiverBindingConstants;

/**
 * Basic AVR state (name, version, available zones, etc)
 *
 * @author David Graeff - Initial contribution
 * @author Tomasz Maruszak - DAB support, Spotify support, better feature detection
 */
@NonNullByDefault
public class DeviceInformationState implements Invalidateable {
    public @Nullable String host;

    // Some AVR information
    public String name = "N/A";
    public String id = "";
    public String version = "0.0";
    public final Set<YamahaReceiverBindingConstants.Zone> zones = new HashSet<>();
    public final Set<YamahaReceiverBindingConstants.Feature> features = new HashSet<>();
    /**
     * Stores additional properties for the device (protocol specific)
     */
    public final Map<String, Object> properties = new HashMap<>();

    public DeviceInformationState() {
        invalidate();
    }

    // If we lost the connection, invalidate the state.
    @Override
    public void invalidate() {
        host = null;
        name = "N/A";
        id = "";
        version = "0.0";
        zones.clear();
        features.clear();
        properties.clear();
    }
}
