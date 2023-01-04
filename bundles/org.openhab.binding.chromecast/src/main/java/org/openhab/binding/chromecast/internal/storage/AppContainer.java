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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.storage.DeletableStorage;
import org.openhab.core.storage.Storage;

/**
 * The {@link PresetContainer} class manages an AppContainer which contains all discovered applications from the
 * ChromeCast
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class AppContainer {

    private final Map<String, AppItem> mapOfApps = new HashMap<>();
    private Storage<AppItem> storage;

    /**
     * Creates a new instance of this class
     */
    public AppContainer(Storage<AppItem> storage) {
        this.storage = storage;
        readFromStorage();
    }

    /**
     * Returns a Collection of all applications
     *
     * @param operationModeType
     */
    public Collection<AppItem> getAllApps() {
        return mapOfApps.values();
    }

    /**
     * Adds an AppItem as available application if it does not allready exist
     *
     * @param appId
     * @param appItem
     *
     */
    public void put(String appId, AppItem appItem) {
        if (!mapOfApps.containsKey(appId)) {
            mapOfApps.put(appId, appItem);
            writeToStorage();
        }
    }

    /**
     * Remove the application stored under the specified appId
     * 
     * @param appId
     */
    public void remove(String appId) {
        mapOfApps.remove(appId);
        writeToStorage();
    }

    /**
     * Returns the application with appId
     *
     * @param presetID
     *
     * @throws AppNotFoundException if Preset could not be found
     */
    public AppItem get(String appId) throws NoAppFoundException {
        AppItem appFound = mapOfApps.get(appId);
        if (appFound != null) {
            return appFound;
        } else {
            throw new NoAppFoundException();
        }
    }

    /**
     * Deletes all applications from the storage.
     */
    public void clear() {
        if (storage instanceof DeletableStorage) {
            ((DeletableStorage<AppItem>) storage).delete();
        } else {
            Collection<@NonNull String> keys = storage.getKeys();
            keys.forEach(key -> storage.remove(key));
        }
    }

    private void writeToStorage() {
        getAllApps().stream().forEach(item -> storage.put(item.getId(), item));
    }

    private void readFromStorage() {
        storage.stream().forEach(item -> {
            AppItem appItem = item.getValue();
            if (appItem != null) {
                put(item.getKey(), appItem);
            }
        });
    }
}
