/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.astro.internal.model;

import static org.openhab.core.library.unit.MetricPrefix.MILLI;

import java.util.Calendar;
import java.util.Comparator;

import javax.measure.quantity.Time;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.astro.internal.util.DateTimeUtils;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;

/**
 * Range class which holds a start and an end calendar object.
 *
 * @author Gerhard Riegler - Initial contribution
 * @author Christoph Weitkamp - Introduced UoM
 */
@NonNullByDefault
public class Range {

    private @Nullable Calendar start;
    private @Nullable Calendar end;

    public Range() {
    }

    public Range(@Nullable Calendar start, @Nullable Calendar end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the start of the range.
     */
    public @Nullable Calendar getStart() {
        return start;
    }

    /**
     * Returns the end of the range.
     */
    public @Nullable Calendar getEnd() {
        return end;
    }

    /**
     * Returns the duration in minutes.
     */
    public @Nullable QuantityType<Time> getDuration() {
        Calendar startLocal = start;
        Calendar endLocal = end;
        if (startLocal == null || endLocal == null) {
            return null;
        }
        if (startLocal.after(endLocal)) {
            return new QuantityType<>(0, Units.MINUTE);
        }
        return new QuantityType<>(endLocal.getTimeInMillis() - startLocal.getTimeInMillis(), MILLI(Units.SECOND))
                .toUnit(Units.MINUTE);
    }

    /**
     * Returns true, if the given calendar matches into the range.
     */
    public boolean matches(Calendar cal) {
        Calendar startLocal = start;
        Calendar endLocal = end;
        if (startLocal == null && endLocal == null) {
            return false;
        }
        long matchStart = startLocal != null ? startLocal.getTimeInMillis()
                : DateTimeUtils.truncateToMidnight(cal).getTimeInMillis();
        long matchEnd = endLocal != null ? endLocal.getTimeInMillis()
                : DateTimeUtils.endOfDayDate(cal).getTimeInMillis();
        return cal.getTimeInMillis() >= matchStart && cal.getTimeInMillis() < matchEnd;
    }

    private static Comparator<Calendar> nullSafeCalendarComparator = Comparator.nullsFirst(Calendar::compareTo);

    private static Comparator<Range> rangeComparator = Comparator.comparing(Range::getStart, nullSafeCalendarComparator)
            .thenComparing(Range::getEnd, nullSafeCalendarComparator);

    public int compareTo(Range that) {
        return rangeComparator.compare(this, that);
    }
}
