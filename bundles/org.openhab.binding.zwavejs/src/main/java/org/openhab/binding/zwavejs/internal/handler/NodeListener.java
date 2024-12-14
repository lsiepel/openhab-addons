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
import org.openhab.binding.zwavejs.internal.api.dto.Event;
import org.openhab.binding.zwavejs.internal.api.dto.Node;

/**
 * The {@link NodeListener} is notified when a node has been removed or added.
 *
 * @author Leo Siepel - Initial contribution
 * 
 */
@NonNullByDefault
public interface NodeListener {

    /**
     * This method returns the node id of the listener
     * 
     * @return
     */
    Integer getId();

    /**
     * This method is called whenever the state of the node has changed.
     *
     * @param node The node which received the state update.
     */
    boolean onNodeStateChanged(Node node);

    /**
     * This method is called whenever the state of the node has changed.
     *
     * @param event The event update.
     */
    boolean onNodeStateChanged(Event event);

    /**
     * This method is called whenever a node is removed.
     */
    void onNodeRemoved();

    /**
     * This method is called whenever a node is added.
     *
     * @param Node The node which is added.
     */
    void onNodeAdded(Node node);
}
