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
package org.openhab.binding.insteon.internal.transport.stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.insteon.internal.transport.message.Msg;

/**
 * Interface for classes that want to listen to notifications from the port io stream
 *
 * @author Jeremy Setton - Initial contribution
 */
@NonNullByDefault
public interface IOStreamListener {
    /**
     * Notifies that the io stream has disconnected
     */
    public void disconnected();

    /**
     * Notifies that the io stream has received a message
     *
     * @param msg the message received
     */
    public void messageReceived(Msg msg);

    /**
     * Notifies that the io stream has received bad data
     */
    public void invalidMessageReceived();

    /**
     * Notifies that the io stream has sent a message
     *
     * @param msg the message sent
     */
    public void messageSent(Msg msg);
}
