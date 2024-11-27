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
package org.openhab.binding.zwavejs.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link zwavejsBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author L. Siepel - Initial contribution
 */
@NonNullByDefault
public class zwavejsBindingConstants {

    private static final String BINDING_ID = "zwavejs";

    public static final String DISCOVERY_LABEL_PATTERN = "Z-Wave JS Gateway (%s)";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_GATEWAY = new ThingTypeUID(BINDING_ID, "gateway");

    // List of all Thing Configuration Parameters
    public static final String CONFIG_HOSTNAME = "hostname";
    public static final String CONFIG_PORT = "port";

    // List of all Thing Properties
    public static final String PROPERTY_HOME_ID = "homeId";
    public static final String PROPERTY_DRIVER_VERSION = "driverVersion";
    public static final String PROPERTY_SERVER_VERSION = "serverVersion";
    public static final String PROPERTY_SCHEMA_MIN = "minSchemaVersion";
    public static final String PROPERTY_SCHEMA_MAX = "maxSchemaVersion";

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";
}
