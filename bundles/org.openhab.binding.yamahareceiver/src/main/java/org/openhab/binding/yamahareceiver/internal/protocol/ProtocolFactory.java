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
package org.openhab.binding.yamahareceiver.internal.protocol;

import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.yamahareceiver.internal.config.YamahaBridgeConfig;
import org.openhab.binding.yamahareceiver.internal.config.YamahaZoneConfig;
import org.openhab.binding.yamahareceiver.internal.state.AvailableInputStateListener;
import org.openhab.binding.yamahareceiver.internal.state.DabBandStateListener;
import org.openhab.binding.yamahareceiver.internal.state.DeviceInformationState;
import org.openhab.binding.yamahareceiver.internal.state.NavigationControlState;
import org.openhab.binding.yamahareceiver.internal.state.NavigationControlStateListener;
import org.openhab.binding.yamahareceiver.internal.state.PlayInfoStateListener;
import org.openhab.binding.yamahareceiver.internal.state.PresetInfoStateListener;
import org.openhab.binding.yamahareceiver.internal.state.SystemControlStateListener;
import org.openhab.binding.yamahareceiver.internal.state.ZoneControlStateListener;

/**
 * Factory to create an {@link AbstractConnection} connection object based on a feature test.
 * Also returns implementation objects for all the protocol interfaces.
 * <p>
 * At the moment only the XML protocol is supported.
 *
 * @author David Graeff - Initial contribution
 * @author Tomasz Maruszak - Input mapping fix, refactoring
 */
@NonNullByDefault
public interface ProtocolFactory {
    /**
     * Asynchronous method to create and return a connection object. Depending
     * on the feature test it might be either an {@link XMLConnection} or a JsonConnection.
     *
     * @param host The host name
     * @param connectionStateListener
     */
    void createConnection(String host, ConnectionStateListener connectionStateListener);

    SystemControl systemControl(AbstractConnection connection, SystemControlStateListener listener,
            DeviceInformationState deviceInformationState);

    InputWithPlayControl inputWithPlayControl(AbstractConnection connection, String currentInputID,
            PlayInfoStateListener listener, YamahaBridgeConfig settings, DeviceInformationState deviceInformationState);

    InputWithPresetControl inputWithPresetControl(AbstractConnection connection, String currentInputID,
            PresetInfoStateListener listener, DeviceInformationState deviceInformationState);

    InputWithTunerBandControl inputWithDabBandControl(String currentInputID, AbstractConnection connection,
            DabBandStateListener observerForBand, PresetInfoStateListener observerForPreset,
            PlayInfoStateListener observerForPlayInfo, DeviceInformationState deviceInformationState);

    InputWithNavigationControl inputWithNavigationControl(AbstractConnection connection, NavigationControlState state,
            String inputID, NavigationControlStateListener observer, DeviceInformationState deviceInformationState);

    ZoneControl zoneControl(AbstractConnection connection, YamahaZoneConfig zoneSettings,
            ZoneControlStateListener listener, Supplier<InputConverter> inputConverterSupplier,
            DeviceInformationState deviceInformationState);

    ZoneAvailableInputs zoneAvailableInputs(AbstractConnection connection, YamahaZoneConfig zoneSettings,
            AvailableInputStateListener listener, Supplier<InputConverter> inputConverterSupplier,
            DeviceInformationState deviceInformationState);

    DeviceInformation deviceInformation(AbstractConnection connection, DeviceInformationState state);

    InputConverter inputConverter(AbstractConnection connection, String setting);
}
