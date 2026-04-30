/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.homematic.internal.communicator.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.homematic.internal.model.HmChannel;
import org.openhab.binding.homematic.internal.model.HmDatapoint;
import org.openhab.binding.homematic.internal.model.HmDevice;
import org.openhab.binding.homematic.internal.model.HmInterface;
import org.openhab.binding.homematic.internal.model.HmParamsetType;
import org.openhab.binding.homematic.internal.model.HmValueType;
import org.openhab.binding.homematic.internal.model.dto.TclScriptDataEntry;
import org.openhab.binding.homematic.internal.model.dto.TclScriptDataList;

/**
 * @author GitHub Copilot - Regression test for string sysvars
 */
@NonNullByDefault
class CcuVariablesAndScriptsParserTest {

    @Test
    void keepsLongNumericStringSystemVariablesAsStrings() throws IOException {
        HmChannel channel = new HmChannel(HmChannel.TYPE_GATEWAY_VARIABLE, HmChannel.CHANNEL_NUMBER_VARIABLE,
                new HmDevice(HmDevice.ADDRESS_GATEWAY_EXTRAS, HmInterface.RF, HmDevice.TYPE_GATEWAY_EXTRAS, "ccu", "",
                        "1"));

        TclScriptDataEntry entry = new TclScriptDataEntry();
        entry.name = "LongStringVariable";
        entry.description = "Long numeric string";
        entry.value = "12345678901";
        entry.valueType = HmValueType.STRING.name();

        TclScriptDataList resultList = new TclScriptDataList();
        resultList.getEntries().add(entry);

        new CcuVariablesAndScriptsParser(channel).parse(resultList);

        HmDatapoint dp = channel.getDatapoint(HmParamsetType.VALUES, entry.name);
        assertNotNull(dp);
        assertEquals(HmValueType.STRING, dp.getType());
        Object value = dp.getValue();
        assertInstanceOf(String.class, value);
        assertEquals(entry.value, value);
    }
}
