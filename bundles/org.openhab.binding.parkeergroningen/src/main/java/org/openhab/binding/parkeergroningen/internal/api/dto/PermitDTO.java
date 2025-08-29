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
 * Data Transfer Object (DTO) representing a permit with various attributes.
 * This class is used to encapsulate the details of a permit, including its
 * code, type, associated media, pricing, reservation details, and other
 * related information.
 * 
 * @author Leo Siepel - Initial contribution
 */
public class PermitDTO {
    public String Code;
    public String Type;
    public String TypeCode;
    public String ZoneCode;
    public List<PermitMediasDTO> PermitMedias;
    public Integer UnitFormat;
    public Integer StartTariff;
    public Integer ProlongMinutes;
    public List<BlockTimeDTO> BlockTimes;
    public Integer BalanceLimit;
    public Double UnitPrice;
    public List<Integer> UpgradeUnits;
    public Boolean IsLicensePlatesFixed;
    public List<LicensePlateDTO> LicensePlates;
    public Boolean PresentationDateTimeWithTime;
    public Boolean ReservationDateFromOnBlock;
    public Integer ReservationDuration;
    public Boolean ReservationDateUntilActualBlock;
    public Boolean ReservationDateUntilLastBlockOfDay;
    public Boolean ReservationDateUntilWholeDay;
    public Boolean ReservationDateUntilEndOfDay;
    public Boolean ReservationDateUntilAlmostEndOfDay;
    public Boolean ReservationDateUntilInfinite;
    public Boolean PresentationDateFromHide;
    public Boolean PresentationDateFromVariable;
    public Boolean PresentationDateUntilHide;
    public Boolean PresentationDateUntilVariable;
}
