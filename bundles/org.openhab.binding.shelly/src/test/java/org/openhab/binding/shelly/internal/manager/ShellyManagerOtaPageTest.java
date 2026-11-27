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
package org.openhab.binding.shelly.internal.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.openhab.binding.shelly.internal.ShellyDevices.SHELLYDT_RGBW2;
import static org.openhab.binding.shelly.internal.manager.ShellyManagerConstants.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.shelly.internal.ShellyHandlerFactory;
import org.openhab.binding.shelly.internal.manager.ShellyManagerPage.FwArchList;
import org.openhab.binding.shelly.internal.manager.ShellyManagerPage.FwRepoEntry;
import org.openhab.binding.shelly.internal.manager.ShellyManagerPage.ShellyMgrResponse;
import org.openhab.binding.shelly.internal.provider.ShellyTranslationProvider;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Tests the sources used by the Shelly Manager firmware proxy.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
class ShellyManagerOtaPageTest {
    private static final String FIRMWARE_URL = "http://firmware.shelly.cloud/gen1/SHRGBW2.zip";
    private static final String CATALOG = """
            {"SHRGBW2":{
              "url":"http://firmware.shelly.cloud/gen1/SHRGBW2.zip",
              "beta_url":"http://firmware.shelly.cloud/gen1/rc/SHRGBW2.zip"
            }}
            """;

    private final HttpClient httpClient = mock(HttpClient.class);
    private final ShellyManagerCache<String, FwRepoEntry> firmwareRepo = new ShellyManagerCache<>();
    private final ShellyManagerCache<String, FwArchList> firmwareArchive = new ShellyManagerCache<>();
    private final ShellyTranslationProvider translations = mock(ShellyTranslationProvider.class);
    private final ShellyManagerOtaPage page = spy(new ShellyManagerOtaPage(mock(ConfigurationAdmin.class), translations,
            httpClient, "192.0.2.10", 8080, mock(ShellyHandlerFactory.class), firmwareRepo, firmwareArchive));

    @AfterEach
    void disposeCaches() {
        firmwareRepo.dispose();
        firmwareArchive.dispose();
    }

    @Test
    void rejectsADirectDownloadUrl() throws Exception {
        doReturn("Firmware not found").when(page).getMessage("fwupdate.notfound", "", "", "");
        ShellyMgrResponse response = page.generateContent(SHELLY_MGR_OTA_URI,
                Map.of(URLPARM_URL, new String[] { "https://example.invalid/firmware.zip" }));

        assertEquals(HttpStatus.BAD_REQUEST_400, response.code);
        verifyNoInteractions(httpClient);
    }

    @Test
    void doesNotProbeAnUnsupportedSplitMode() throws Exception {
        doReturn(CATALOG).when(page).httpGet(FWREPO_PROD_URL);

        assertEquals(FIRMWARE_URL, page.getFirmwareUrl(SHELLYDT_RGBW2, "unknown", FWPROD, true));
        verify(page, never()).testUrl(anyString());
    }

    @Test
    void selectsWhiteAndColorFirmwareIndependently() throws Exception {
        doReturn(CATALOG).when(page).httpGet(FWREPO_PROD_URL);
        doReturn(true).when(page).testUrl(anyString());

        assertEquals("http://firmware.shelly.cloud/gen1/SHRGBW2-white.zip",
                page.getFirmwareUrl(SHELLYDT_RGBW2, "white", FWPROD, true));
        assertEquals("http://firmware.shelly.cloud/gen1/SHRGBW2-color.zip",
                page.getFirmwareUrl(SHELLYDT_RGBW2, "color", FWPROD, true));
    }

    @Test
    void downloadsCatalogFirmwareWithoutFollowingRedirects() throws Exception {
        Request request = mock(Request.class, RETURNS_SELF);
        ContentResponse firmware = mock(ContentResponse.class);
        byte[] payload = { 1, 2, 3 };
        doReturn(CATALOG).when(page).httpGet(FWREPO_PROD_URL);
        doReturn("Firmware update").when(page).getMessage(anyString(), any(Object[].class));
        when(httpClient.newRequest(FIRMWARE_URL)).thenReturn(request);
        when(request.send()).thenReturn(firmware);
        when(firmware.getStatus()).thenReturn(HttpStatus.OK_200);
        when(firmware.getHeaders()).thenReturn(HttpFields.EMPTY);
        when(firmware.getMediaType()).thenReturn("application/zip");
        when(firmware.getContent()).thenReturn(payload);

        ShellyMgrResponse response = page.generateContent(SHELLY_MGR_OTA_URI,
                Map.of(URLPARM_DEVTYPE, new String[] { SHELLYDT_RGBW2 }, URLPARM_VERSION, new String[] { FWPROD }));

        assertEquals(HttpStatus.OK_200, response.code);
        assertArrayEquals(payload, assertInstanceOf(byte[].class, response.data));
        verify(request).method(HttpMethod.GET);
        verify(request).followRedirects(false);
    }
}
