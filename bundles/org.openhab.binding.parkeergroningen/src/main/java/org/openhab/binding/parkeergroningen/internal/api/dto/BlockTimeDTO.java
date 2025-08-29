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

/**
 * Data Transfer Object (DTO) representing a block of time with associated properties.
 * 
 * @author Leo Siepel - Initial contribution
 */
public class BlockTimeDTO {
    public String ValidFrom;
    public String ValidUntil;
    public Double Units;
    public Integer Seconds;
    public Boolean IsException;
    public Boolean IsDefined;
    public Boolean IsAllowed;
    public Boolean IsFree;
    public Integer DayOfWeek;
}
