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
package org.openhab.binding.amazonechocontrol.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Test;
import org.openhab.binding.amazonechocontrol.internal.connection.AmazonLoginClient;
import org.openhab.binding.amazonechocontrol.internal.connection.AmazonLoginClient.LoginResult;

/**
 * The {@link ServletUriTest} contains tests for the {@link ServletUri} record
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class LoginTest {

    public static HttpClient createClient() throws Exception {
        // SSL context factory (required for HTTPS)
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setEndpointIdentificationAlgorithm("HTTPS"); // verify hostnames

        // Create HttpClient with SSL
        HttpClient client = new HttpClient(sslContextFactory);

        // Optional: set timeouts
        client.setConnectTimeout(10_000);
        client.setIdleTimeout(30_000);
        client.setFollowRedirects(true);
        // Start the client
        client.start();

        return client;
    }

    @Test
    public void testLogin() throws Exception {
        HttpClient httpClient = createClient();

        AmazonLoginClient loginClient = new AmazonLoginClient("x@x.com", "**********", "com",
                httpClient);

        LoginResult result = loginClient.login("123456");

        assertNotNull(result);
    }
}
