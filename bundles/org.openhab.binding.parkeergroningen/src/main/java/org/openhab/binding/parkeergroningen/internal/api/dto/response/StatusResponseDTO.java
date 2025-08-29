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

import org.openhab.binding.parkeergroningen.internal.api.dto.PermitDTO;

/**
 * Data Transfer Object (DTO) representing the response for an activation request.
 * This class contains information about the permit associated with the activation.
 * 
 * 
 * @author Leo Siepel - Initial contribution
 */
public class StatusResponseDTO {
    public PermitDTO Permit;
}
