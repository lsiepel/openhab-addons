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
package org.openhab.binding.homematic.internal.discovery;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openhab.binding.homematic.test.util.BridgeHelper.createHomematicBridge;
import static org.openhab.binding.homematic.test.util.DimmerHelper.createDimmerHmDevice;

import java.io.IOException;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.homematic.internal.communicator.HomematicGateway;
import org.openhab.binding.homematic.internal.handler.HomematicBridgeHandler;
import org.openhab.binding.homematic.internal.model.HmDevice;
import org.openhab.binding.homematic.internal.type.HomematicTypeGenerator;
import org.openhab.binding.homematic.test.util.SimpleDiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.util.SameThreadExecutorService;

/**
 * Tests for {@link HomematicDeviceDiscoveryServiceTest}.
 *
 * @author Florian Stolte - Initial Contribution
 *
 */
@NonNullByDefault
public class HomematicDeviceDiscoveryServiceTest extends JavaTest {

    private @Nullable HomematicDeviceDiscoveryService homematicDeviceDiscoveryService;
    private @Nullable HomematicBridgeHandler homematicBridgeHandler;

    @BeforeEach
    public void setup() throws IOException {
        HomematicBridgeHandler homematicBridgeHandler = mockHomematicBridgeHandler();
        this.homematicBridgeHandler = homematicBridgeHandler;
        HomematicDeviceDiscoveryService discoveryService = new HomematicDeviceDiscoveryService(
                new SameThreadExecutorService());
        discoveryService.setThingHandler(homematicBridgeHandler);
        this.homematicDeviceDiscoveryService = discoveryService;
    }

    private HomematicBridgeHandler mockHomematicBridgeHandler() throws IOException {
        HomematicBridgeHandler homematicBridgeHandler = Objects.requireNonNull(mock(HomematicBridgeHandler.class));
        Bridge bridge = createHomematicBridge();
        HomematicGateway homematicGateway = mockHomematicGateway();
        HomematicTypeGenerator homematicTypeGenerator = mockHomematicTypeGenerator();

        when(homematicBridgeHandler.getThing()).thenReturn(bridge);
        when(homematicBridgeHandler.getGateway()).thenReturn(homematicGateway);
        when(homematicBridgeHandler.getTypeGenerator()).thenReturn(homematicTypeGenerator);

        return homematicBridgeHandler;
    }

    private HomematicGateway mockHomematicGateway() throws IOException {
        HomematicGateway homematicGateway = Objects.requireNonNull(mock(HomematicGateway.class));

        when(homematicGateway.getInstallMode()).thenReturn(60);

        return homematicGateway;
    }

    private HomematicTypeGenerator mockHomematicTypeGenerator() {
        return Objects.requireNonNull(mock(HomematicTypeGenerator.class));
    }

    @Test
    public void testDiscoveryResultIsReportedForNewDevice() {
        HomematicDeviceDiscoveryService discoveryService = Objects.requireNonNull(this.homematicDeviceDiscoveryService);
        SimpleDiscoveryListener discoveryListener = new SimpleDiscoveryListener();
        discoveryService.addDiscoveryListener(discoveryListener);

        HmDevice hmDevice = createDimmerHmDevice();
        discoveryService.deviceDiscovered(hmDevice);

        assertThat(discoveryListener.discoveredResults.size(), is(1));
        discoveryResultMatchesHmDevice(discoveryListener.discoveredResults.element(), hmDevice);
    }

    @Test
    public void testDevicesAreLoadedFromBridgeDuringDiscovery() throws IOException {
        HomematicBridgeHandler homematicBridgeHandler = Objects.requireNonNull(this.homematicBridgeHandler);

        startScanAndWaitForLoadedDevices();

        Objects.requireNonNull(verify(homematicBridgeHandler.getGateway())).loadAllDeviceMetadata();
    }

    @Test
    public void testInstallModeIsNotActiveDuringInitialDiscovery() throws IOException {
        HomematicBridgeHandler homematicBridgeHandler = Objects.requireNonNull(this.homematicBridgeHandler);

        startScanAndWaitForLoadedDevices();

        Objects.requireNonNull(verify(homematicBridgeHandler.getGateway(), never())).setInstallMode(eq(true), anyInt());
    }

    @Test
    public void testInstallModeIsActiveDuringSubsequentDiscovery() throws IOException {
        HomematicBridgeHandler homematicBridgeHandler = Objects.requireNonNull(this.homematicBridgeHandler);
        homematicBridgeHandler.getThing()
                .setStatusInfo(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, ""));

        startScanAndWaitForLoadedDevices();

        Objects.requireNonNull(verify(homematicBridgeHandler.getGateway())).setInstallMode(true, 60);
    }

    @Test
    public void testStoppingDiscoveryDisablesInstallMode() throws IOException {
        HomematicDeviceDiscoveryService discoveryService = Objects.requireNonNull(this.homematicDeviceDiscoveryService);
        HomematicBridgeHandler homematicBridgeHandler = Objects.requireNonNull(this.homematicBridgeHandler);
        homematicBridgeHandler.getThing()
                .setStatusInfo(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, ""));
        discoveryService.startScan();

        discoveryService.stopScan();

        Objects.requireNonNull(verify(homematicBridgeHandler.getGateway())).setInstallMode(false, 0);
    }

    private void startScanAndWaitForLoadedDevices() {
        HomematicDeviceDiscoveryService discoveryService = Objects.requireNonNull(this.homematicDeviceDiscoveryService);
        HomematicBridgeHandler homematicBridgeHandler = Objects.requireNonNull(this.homematicBridgeHandler);
        discoveryService.startScan();
        waitForAssert(() -> Objects.requireNonNull(verify(homematicBridgeHandler)).setOfflineStatus(), 1000, 50);
    }

    private void discoveryResultMatchesHmDevice(DiscoveryResult result, HmDevice device) {
        assertThat(result.getThingTypeUID().getId(), is(device.getType()));
        assertThat(result.getThingUID().getId(), is(device.getAddress()));
        assertThat(result.getLabel(), is(device.getName()));
    }
}
