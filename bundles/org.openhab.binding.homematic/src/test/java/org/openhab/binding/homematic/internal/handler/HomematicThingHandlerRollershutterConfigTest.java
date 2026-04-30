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
package org.openhab.binding.homematic.internal.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openhab.binding.homematic.internal.misc.HomematicConstants.CHANNEL_TYPE_BLIND;
import static org.openhab.binding.homematic.internal.misc.HomematicConstants.CHANNEL_TYPE_SENSOR;
import static org.openhab.binding.homematic.internal.misc.HomematicConstants.DATAPOINT_NAME_LEVEL;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.homematic.internal.model.HmChannel;
import org.openhab.binding.homematic.internal.model.HmDatapoint;
import org.openhab.binding.homematic.internal.model.HmDatapointConfig;
import org.openhab.binding.homematic.internal.model.HmDevice;
import org.openhab.binding.homematic.internal.model.HmInterface;
import org.openhab.binding.homematic.internal.model.HmParamsetType;
import org.openhab.binding.homematic.internal.model.HmValueType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * Tests for rollershutter level inversion channel config handling.
 */
@NonNullByDefault
public class HomematicThingHandlerRollershutterConfigTest {

    @Test
    public void testAdaptRollerShutterLevelCommand() {
        HmDatapoint datapoint = createDatapoint(DATAPOINT_NAME_LEVEL, CHANNEL_TYPE_BLIND);
        HmDatapointConfig config = new HmDatapointConfig();
        config.setInvertLevel(true);

        Command percentCommand = HomematicThingHandler.adaptRollerShutterLevelCommand(new PercentType(80), datapoint,
                config);
        assertThat(percentCommand, is(new PercentType(20)));

        Command increaseCommand = HomematicThingHandler.adaptRollerShutterLevelCommand(IncreaseDecreaseType.INCREASE,
                datapoint, config);
        assertThat(increaseCommand, is(IncreaseDecreaseType.DECREASE));

        Command onCommand = HomematicThingHandler.adaptRollerShutterLevelCommand(OnOffType.ON, datapoint, config);
        assertThat(onCommand, is(OnOffType.OFF));

        Command upCommand = HomematicThingHandler.adaptRollerShutterLevelCommand(UpDownType.UP, datapoint, config);
        assertThat(upCommand, is(UpDownType.UP));
    }

    @Test
    public void testAdaptRollerShutterLevelState() {
        HmDatapoint datapoint = createDatapoint(DATAPOINT_NAME_LEVEL, CHANNEL_TYPE_BLIND);
        HmDatapointConfig config = new HmDatapointConfig();
        config.setInvertLevel(true);

        State state = HomematicThingHandler.adaptRollerShutterLevelState(new PercentType(95), datapoint, config);
        assertThat(state, is(new PercentType(5)));
    }

    @Test
    public void testAdaptRollerShutterLevelIsDisabledForOtherDatapoints() {
        HmDatapoint datapoint = createDatapoint("STATE", CHANNEL_TYPE_BLIND);
        HmDatapointConfig config = new HmDatapointConfig();
        config.setInvertLevel(true);

        Command command = HomematicThingHandler.adaptRollerShutterLevelCommand(new PercentType(42), datapoint, config);
        State state = HomematicThingHandler.adaptRollerShutterLevelState(new PercentType(42), datapoint, config);

        assertThat(command, is(new PercentType(42)));
        assertThat(state, is(new PercentType(42)));
    }

    @Test
    public void testAdaptRollerShutterLevelIsDisabledForNonRollerShutterChannels() {
        HmDatapoint datapoint = createDatapoint(DATAPOINT_NAME_LEVEL, CHANNEL_TYPE_SENSOR);
        HmDatapointConfig config = new HmDatapointConfig();
        config.setInvertLevel(true);

        Command command = HomematicThingHandler.adaptRollerShutterLevelCommand(new PercentType(42), datapoint, config);
        State state = HomematicThingHandler.adaptRollerShutterLevelState(new PercentType(42), datapoint, config);

        assertThat(command, is(new PercentType(42)));
        assertThat(state, is(new PercentType(42)));
    }

    private HmDatapoint createDatapoint(String datapointName, String channelType) {
        HmDevice device = new HmDevice("LEQ123456", HmInterface.RF, "HM-STUB-DEVICE", "", "", "");
        HmChannel channel = new HmChannel(channelType, 1, device);
        HmDatapoint datapoint = new HmDatapoint(datapointName, "", HmValueType.FLOAT, null, false,
                HmParamsetType.VALUES);
        datapoint.setChannel(channel);
        return datapoint;
    }
}
