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
package org.openhab.binding.parkeergroningen.internal.api.dto.response;

import java.util.List;

import org.openhab.binding.parkeergroningen.internal.api.dto.BookingDTO;

/**
 * Data Transfer Object (DTO) representing the response for a session.
 * This class contains information about the session token, the name
 * associated with the session, and a list of permits (bookings) related
 * to the session.
 * 
 * @author Leo Siepel - Initial contribution
 */
public class SessionResponseDTO {
    public String Token;
    public String Name;
    public List<BookingDTO> Permits;
}
