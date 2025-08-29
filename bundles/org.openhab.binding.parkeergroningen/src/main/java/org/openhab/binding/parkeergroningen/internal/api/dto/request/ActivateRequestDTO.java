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

import org.openhab.binding.parkeergroningen.internal.api.dto.LicensePlateDTO;

/**
 * Data Transfer Object (DTO) representing a request to activate a reservation.
 * This class is used to encapsulate the necessary information for the activation process.
 * 
 * @author Leo Siepel - Initial contribution
 */
public class ActivateRequestDTO {
    public String DateFrom;
    public String DateUntil;
    public LicensePlateDTO LicensePlate;
    public Integer permitMediaTypeID;
    public String permitMediaCode;
}
