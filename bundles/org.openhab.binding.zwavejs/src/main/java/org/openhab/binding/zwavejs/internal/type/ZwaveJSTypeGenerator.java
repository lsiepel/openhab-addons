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
package org.openhab.binding.zwavejs.internal.type;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.zwavejs.internal.api.dto.Node;
import org.openhab.core.thing.ThingUID;

/**
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public interface ZwaveJSTypeGenerator {

    /**
     * Generates the ChannelTypes and ConfigDescriptions for the given node.
     * 
     * @param thingUID The ThingUID that the generated channels belong to
     * @param node The Z-Wave JS node data to proces and transform into openHAB domain
     * @return ZwaveJSTypeGeneratorResult containing the channels and configuration
     */
    ZwaveJSTypeGeneratorResult generate(ThingUID thingUID, Node node);
}
