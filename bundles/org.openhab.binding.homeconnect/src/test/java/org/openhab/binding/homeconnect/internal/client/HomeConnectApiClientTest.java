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
package org.openhab.binding.homeconnect.internal.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.*;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.configuration.ApiBridgeConfiguration;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;

/**
 * Verifies that raw Home Connect requests stay on the selected API origin.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
class HomeConnectApiClientTest {

    private static final String HA_ID = "test-appliance";
    private static final String PATH = "/api/homeappliances/test-appliance/settings";

    private final HttpClient jettyClient = new HttpClient();
    private final HttpClient client = mock(HttpClient.class);
    private final OAuthClientService oauth = mock(OAuthClientService.class);

    @AfterEach
    void closeHttpClient() throws Exception {
        jettyClient.close();
    }

    @Test
    void rawGetPreservesThePathAndEncodedQuery() throws Exception {
        String path = PATH + "?filter=two%20words";
        Request request = stubRequest(API_BASE_URL, HttpStatus.OK_200, "{}");

        assertEquals("{}", apiClient(false).getRaw(HA_ID, path));
        assertEquals(URI.create(API_BASE_URL + path), request.getURI());
        assertEquals(HttpMethod.GET.asString(), request.getMethod());
        assertFalse(request.isFollowRedirects());
    }

    @Test
    void rawPutUsesTheSimulatorOrigin() throws Exception {
        Request request = stubRequest(API_SIMULATOR_BASE_URL, HttpStatus.NO_CONTENT_204, "");

        assertEquals("", apiClient(true).putRaw(HA_ID, PATH, "{}"));
        assertEquals(URI.create(API_SIMULATOR_BASE_URL + PATH), request.getURI());
        assertEquals(HttpMethod.PUT.asString(), request.getMethod());
        assertFalse(request.isFollowRedirects());
    }

    @ParameterizedTest
    @ValueSource(strings = { "api/homeappliances", "https://example.invalid/api/homeappliances",
            "//example.invalid/api/homeappliances", "/api/homeappliances#fragment", "/api/home appliances" })
    void rejectsInvalidPathsBeforeObtainingCredentials(String path) {
        HomeConnectApiClient apiClient = apiClient(false);

        assertThrows(CommunicationException.class, () -> apiClient.getRaw(HA_ID, path));
        assertThrows(CommunicationException.class, () -> apiClient.putRaw(HA_ID, path, "{}"));
        verifyNoInteractions(oauth, client);
    }

    private HomeConnectApiClient apiClient(boolean simulated) {
        return new HomeConnectApiClient(client, oauth, simulated, null, new ApiBridgeConfiguration());
    }

    private Request stubRequest(String origin, int status, String responseBody) throws Exception {
        AccessTokenResponse token = mock(AccessTokenResponse.class);
        when(token.getAccessToken()).thenReturn("test-token");
        when(oauth.getAccessTokenResponse()).thenReturn(token);

        Request request = spy(jettyClient.newRequest(origin));
        ContentResponse response = mock(ContentResponse.class);
        when(response.getStatus()).thenReturn(status);
        when(response.getContentAsString()).thenReturn(responseBody);
        when(response.getHeaders()).thenReturn(HttpFields.EMPTY);
        doReturn(response).when(request).send();
        when(client.newRequest(origin)).thenReturn(request);
        return request;
    }
}
