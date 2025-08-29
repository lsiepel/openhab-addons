/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.parkeergroningen.internal.api.controller;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.parkeergroningen.internal.api.dto.BookingDTO;
import org.openhab.binding.parkeergroningen.internal.api.dto.request.DeactivateRequestDTO;
import org.openhab.binding.parkeergroningen.internal.api.dto.request.LoginRequestDTO;
import org.openhab.binding.parkeergroningen.internal.api.dto.response.SessionResponseDTO;
import org.openhab.binding.parkeergroningen.internal.api.dto.response.StatusResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@code RestController} class provides methods to interact with a REST API for parking services.
 * It handles login, activation, and data refresh operations using an HTTP client.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class RestController {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private static final String LOGIN_URL = "https://aanvraagparkeren.groningen.nl/DVSWebAPI/api/login";
    private static final String ACTIVATE_URL = "https://aanvraagparkeren.groningen.nl/DVSWebAPI/api/reservation/create";
    private static final String DEACTIVATE_URL = "https://aanvraagparkeren.groningen.nl/DVSWebAPI/api/reservation/end";
    private static final String HEADER_ACCEPT = "application/json, text/plain, */*";
    private static final String HEADER_CONTENT_TYPE = "application/json;charset=UTF-8";
    private static final String HEADER_AUTHORIZATION = "authorization";
    private static final int TIMEOUT_SECONDS = 10;

    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    private volatile String token = "";
    private volatile String permitMediaCode = "";
    private volatile long activeReservationId = -1L;

    public RestController(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private <T> T sendPostRequest(String url, Object payload, String token, Class<T> responseType) throws Exception {
        String jsonBody = gson.toJson(payload);
        Request request = httpClient.newRequest(url).method(HttpMethod.POST)
                .header(HttpHeader.ACCEPT.asString(), HEADER_ACCEPT)
                .header(HttpHeader.CONTENT_TYPE.asString(), HEADER_CONTENT_TYPE)
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .content(new org.eclipse.jetty.client.util.StringContentProvider(jsonBody, StandardCharsets.UTF_8));
        if (token != null && !token.isEmpty()) {
            request.header(HEADER_AUTHORIZATION, "Token " + token);
        }
        ContentResponse response = request.send();
        if (response.getStatus() == 200) {
            return gson.fromJson(response.getContentAsString(), responseType);
        } else {
            logger.warn("HTTP {} failed: {} - {}", url, response.getStatus(), response.getContentAsString());
            throw new RuntimeException("HTTP request failed: " + response.getStatus());
        }
    }

    public void refresh() {
        // TODO: Implement refresh logic
    }

    public boolean login(String cardNumber, String pin) {
        LoginRequestDTO loginRequest = new LoginRequestDTO(cardNumber, pin);
        try {
            SessionResponseDTO session = sendPostRequest(LOGIN_URL, loginRequest, null, SessionResponseDTO.class);
            if (session != null && session.Token != null) {
                this.token = session.Token;
                return true;
            }
        } catch (Exception e) {
            logger.error("Login failed", e);
        }
        return false;
    }

    public boolean activate(BookingDTO permit) {
        try {
            StatusResponseDTO activateResponse = sendPostRequest(ACTIVATE_URL, permit, this.token,
                    StatusResponseDTO.class);
            if (activateResponse != null && activateResponse.Permit != null
                    && activateResponse.Permit.PermitMedias != null) {
                this.activeReservationId = activateResponse.Permit.PermitMedias.stream()
                        .filter(pm -> pm.ActiveReservations != null && !pm.ActiveReservations.isEmpty())
                        .flatMap(pm -> pm.ActiveReservations.stream())
                        .filter(ar -> ar != null && ar.ReservationID != null).map(ar -> ar.ReservationID).findFirst()
                        .orElse(-1L);
                return this.activeReservationId != -1L;
            }
        } catch (Exception e) {
            logger.error("Activation failed", e);
        }
        return false;
    }

    public boolean deactivate() {
        var payload = new DeactivateRequestDTO();
        payload.ReservationID = this.activeReservationId;
        payload.permitMediaCode = this.permitMediaCode;
        try {
            StatusResponseDTO deactivateResponse = sendPostRequest(DEACTIVATE_URL, payload, this.token,
                    StatusResponseDTO.class);
            // Check for lingering ActiveReservations
            boolean hasActiveReservations = false;
            if (deactivateResponse != null && deactivateResponse.Permit != null
                    && deactivateResponse.Permit.PermitMedias != null) {
                hasActiveReservations = deactivateResponse.Permit.PermitMedias.stream()
                        .anyMatch(pm -> pm.ActiveReservations != null && !pm.ActiveReservations.isEmpty());
            }
            if (hasActiveReservations) {
                logger.warn("Deactivation response still contains ActiveReservations!");
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Deactivation failed", e);
            return false;
        }
    }
}
