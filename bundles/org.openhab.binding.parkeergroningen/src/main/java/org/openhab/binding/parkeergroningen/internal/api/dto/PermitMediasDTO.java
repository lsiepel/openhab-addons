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

import java.util.List;

/**
 * Data Transfer Object (DTO) representing permit media details.
 * This class contains information about the type, code, balance,
 * reservations, license plates, history, and remaining upgrades/downgrades
 * associated with a permit media.
 * 
 * @author Leo Siepel - Initial contribution
 */
public class PermitMediasDTO {
    public Integer TypeID;
    public String Code;
    public Integer Balance;
    public List<Integer> RestrictedProlongReservationIDs;
    public List<ActiveReservationDTO> ActiveReservations;
    public List<LicensePlateDTO> LicensePlates;
    // class HistoryDTO History;
    public Integer RemainingUpgrades;
    public Integer RemainingDowngrades;
}
