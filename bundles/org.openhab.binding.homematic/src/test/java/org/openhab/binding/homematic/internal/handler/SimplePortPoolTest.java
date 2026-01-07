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
package org.openhab.binding.homematic.internal.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link org.openhab.binding.homematic.internal.handler.SimplePortPool}.
 *
 * @author Florian Stolte - Initial Contribution
 */
@NonNullByDefault
public class SimplePortPoolTest {

    private @Nullable SimplePortPool simplePortPool;

    @BeforeEach
    public void setup() {
        this.simplePortPool = new SimplePortPool();
    }

    @Test
    public void testPoolOnlyGivesOutPortsNotInUse() {
        SimplePortPool simplePortPool = Objects.requireNonNull(this.simplePortPool);
        final List<Integer> ports = acquireSomePorts();

        final int newPort = simplePortPool.getNextPort();

        for (int port : ports) {
            assertThat(port, not(is(newPort)));
        }
    }

    @Test
    public void testPoolReusesReleasedPort() {
        SimplePortPool simplePortPool = Objects.requireNonNull(this.simplePortPool);
        final List<Integer> ports = acquireSomePorts();
        final int firstPort = ports.get(0);

        simplePortPool.release(firstPort);
        int newPortAfterRelease = simplePortPool.getNextPort();

        assertThat(newPortAfterRelease, is(firstPort));
    }

    @Test
    public void testPoolSkipsPortsThatAreSetInUse() {
        SimplePortPool simplePortPool = Objects.requireNonNull(this.simplePortPool);
        final int firstPort = simplePortPool.getNextPort();

        simplePortPool.setInUse(firstPort + 1);
        int newPortAfterSetInUse = simplePortPool.getNextPort();

        assertThat(newPortAfterSetInUse, is(firstPort + 2));
    }

    private List<Integer> acquireSomePorts() {
        SimplePortPool simplePortPool = Objects.requireNonNull(this.simplePortPool);
        final int numberOfPorts = 5;
        final List<Integer> ports = new ArrayList<>(numberOfPorts);

        for (int i = 0; i < numberOfPorts; i++) {
            ports.add(simplePortPool.getNextPort());
        }

        return ports;
    }
}
