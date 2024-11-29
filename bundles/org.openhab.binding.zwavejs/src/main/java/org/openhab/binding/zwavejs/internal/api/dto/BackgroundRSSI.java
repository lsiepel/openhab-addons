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
package org.openhab.binding.zwavejs.internal.api.dto;

/**
 * @author L. Siepel - Initial contribution
 */
public class BackgroundRSSI {
    // TODO Have not yet found a way to handle the parsing of this json:
    // "backgroundRSSI":{"channel0":{"current":-99,"average":-98},"channel1":{"current":-106,"average":-104},"channel2":{"current":-106,"average":-104},"channel3":{"current":127,"average":0},"timestamp":1732742364913}}
    // public Channel0 channel0;
    // public Channel1 channel1;
    // public Channel2 channel2;
    // public Channel3 channel3;
    public long timestamp;
}
