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
package org.openhab.binding.parkeergroningen.internal.api.dto.request;

/**
 * Data Transfer Object (DTO) for login requests.
 * This class encapsulates the necessary information for a login request,
 * including identifier, login method, password, OTP, reset code, additional identifier,
 * zip code, and permit media type ID.
 * 
 * @author Leo Siepel - Initial contribution
 */
public class LoginRequestDTO {
    public String identifier;
    public String loginMethod = "Pas";
    public String password;
    public String otp = null;
    public String resetCode = null;
    public String asIdentifier = null;
    public String zipCode = null;
    public Integer permitMediaTypeID = 1;

    public LoginRequestDTO(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }
}
