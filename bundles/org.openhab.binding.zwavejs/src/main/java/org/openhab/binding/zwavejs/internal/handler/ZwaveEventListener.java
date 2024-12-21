/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

package org.openhab.binding.zwavejs.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.zwavejs.internal.api.dto.messages.BaseMessage;
import org.openhab.binding.zwavejs.internal.discovery.NodeDiscoveryService;

/**
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public interface ZwaveEventListener {

    /**
     * Register {@link NodeDiscoveryService} to bridge handler
     *
     * @param listener the discovery service
     * @return {@code true} if the new discovery service is accepted
     */
    boolean registerDiscoveryListener(NodeDiscoveryService listener);

    /**
     * Unregister {@link NodeDiscoveryService} from bridge handler
     *
     * @return {@code true} if the discovery service was removed
     */
    boolean unregisterDiscoveryListener();

    /**
     * Register a node listener.
     *
     * @param nodeListener the node listener
     * @return {@code true} if the collection of listeners has changed as a result of this call
     */
    boolean registerNodeListener(NodeListener nodeListener);

    /**
     * Unregister a node listener.
     *
     * @param nodeListener the node listener
     * @return {@code true} if the collection of listeners has changed as a result of this call
     */
    boolean unregisterNodeListener(NodeListener nodeListener);

    /**
     * Inform listener about new event
     *
     * @param message the event message
     */
    void onEvent(BaseMessage message);
}
