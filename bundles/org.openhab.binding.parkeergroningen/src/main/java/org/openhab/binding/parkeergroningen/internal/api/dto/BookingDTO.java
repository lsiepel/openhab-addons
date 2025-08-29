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
package org.openhab.binding.parkeergroningen.internal.api.dto;

import java.time.ZonedDateTime;

/**
 * Represents a permit with details such as validity period, associated license plate,
 * media type, and media code.
 *
 * @author Leo Siepel - Initial contribution
 */
public class BookingDTO {
    public ZonedDateTime dateFrom;
    public ZonedDateTime dateUntil;
    public LicensePlateDTO licensePlate;
    public int permitMediaTypeID = 1;
    public String permitMediaCode;

    public BookingDTO(ZonedDateTime dateFrom, ZonedDateTime dateUntil, String licensePlate, String permitMediaCode) {
        this.dateFrom = dateFrom;
        this.dateUntil = dateUntil;
        this.licensePlate = new LicensePlateDTO(licensePlate, "openHAB automation");
        this.permitMediaCode = permitMediaCode;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = new LicensePlateDTO(licensePlate, "openHAB automation");
    }
}
