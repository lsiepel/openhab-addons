/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.chromecast.internal.storage;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link AppItem} class manages an AppItem
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class AppItem {

    private String id = "";
    private String displayName = "";

    public AppItem(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Returns true if id and displayName are equal
     *
     * @return true if id and displayName are equal
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof AppItem) {
            AppItem other = (AppItem) obj;
            return Objects.equals(other.id, this.id) || Objects.equals(other.displayName, this.displayName);
        }
        return super.equals(obj);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
