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
 * Data Transfer Object (DTO) representing a request to deactivate a reservation.
 * This class is used to encapsulate the necessary information for the deactivation process.
 * 
 * @author Leo Siepel - Initial contribution
 */
public class DeactivateRequestDTO {
    public Long ReservationID;
    public Integer permitMediaTypeID = 1;
    public String permitMediaCode;
}
