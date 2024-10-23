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
package org.openhab.binding.jeelink.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Computes a rolling average of readings.
 *
 * @author Volker Bier - Initial contribution
 */
@NonNullByDefault
public abstract class RollingReadingAverage<R extends Reading> {
    private int size = 0;
    private int maxSize;
    private @Nullable R total = null;
    private int index = 0;
    private R[] samples;

    public RollingReadingAverage(R[] array) {
        maxSize = array.length;
        samples = array;
    }

    public void add(R reading) {
        if (size < maxSize) {
            size++;
        }
        @Nullable
        R totalLocal = total;
        if (totalLocal == null) {
            total = reading;
        } else {
            totalLocal = add(totalLocal, reading);
            total = substract(totalLocal, samples[index]);
        }

        samples[index] = reading;
        if (++index == maxSize) {
            index = 0;
        }
    }

    public @Nullable R getAverage() {
        @Nullable
        R total = this.total;
        if (total == null) {
            return null;
        }
        return divide(total, size);
    }

    protected abstract R add(R value1, @Nullable R value2);

    protected abstract R substract(R from, @Nullable R value);

    protected abstract R divide(R value, int count);
}
