/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.io.neeo.internal.servletservices;

import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.auth.AnonymousUserSecurityContext;
import org.openhab.core.io.rest.auth.AuthFilter;
import org.openhab.io.neeo.NeeoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Tests authorization for adding a NEEO Brain from the dashboard.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
class BrainDashboardServiceTest {

    private static final String[] ADD_BRAIN = { "addbrain" };

    private final NeeoService service = mock(NeeoService.class);
    private final AuthFilter authFilter = mock(AuthFilter.class);
    private final BrainDashboardService dashboard = new BrainDashboardService(service, authFilter);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    void rejectsPlainTextFormSubmission() throws Exception {
        when(request.getContentType()).thenReturn("text/plain");

        dashboard.handlePost(request, ADD_BRAIN, response);

        verify(response).sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        verifyNoInteractions(authFilter, service);
    }

    @Test
    void rejectsAnImplicitUser() throws Exception {
        when(request.getContentType()).thenReturn("application/json");
        when(authFilter.getSecurityContext(request, false)).thenReturn(new AnonymousUserSecurityContext());

        dashboard.handlePost(request, ADD_BRAIN, response);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(request, never()).getReader();
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidCredentials() throws Exception {
        when(request.getContentType()).thenReturn("application/json");
        when(authFilter.getSecurityContext(request, false)).thenThrow(new AuthenticationException("Invalid token"));

        dashboard.handlePost(request, ADD_BRAIN, response);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(request, never()).getReader();
        verifyNoInteractions(service);
    }

    @Test
    void rejectsAUserWithoutTheAdministratorRole() throws Exception {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(request.getContentType()).thenReturn("application/json");
        when(authFilter.getSecurityContext(request, false)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(() -> "user");

        dashboard.handlePost(request, ADD_BRAIN, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(request, never()).getReader();
        verifyNoInteractions(service);
    }

    @Test
    void letsAnAdministratorAddABrain() throws Exception {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(request.getContentType()).thenReturn("application/json; charset=UTF-8");
        when(authFilter.getSecurityContext(request, false)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(() -> "admin");
        when(securityContext.isUserInRole(Role.ADMIN)).thenReturn(true);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"brainIp\":\"192.0.2.10\"}")));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        dashboard.handlePost(request, ADD_BRAIN, response);

        verify(service).addBrain("192.0.2.10");
        verify(response, never()).sendError(anyInt());
    }
}
