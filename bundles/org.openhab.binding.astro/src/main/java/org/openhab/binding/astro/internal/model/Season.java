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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import javax.measure.quantity.Time;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.astro.internal.util.DateTimeUtils;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;

/**
 * Holds the season dates of the year and the current name.
 *
 * @author Gerhard Riegler - Initial contribution
 */
@NonNullByDefault
public class Season {
    private Calendar spring = Calendar.getInstance();
    private Calendar summer = Calendar.getInstance();
    private Calendar autumn = Calendar.getInstance();
    private Calendar winter = Calendar.getInstance();

    private @Nullable SeasonName name;

    /**
     * Returns the date of the beginning of spring.
     */
    public Calendar getSpring() {
        return spring;
    }

    /**
     * Sets the date of the beginning of spring.
     */
    public void setSpring(Calendar spring) {
        this.spring = spring;
    }

    /**
     * Returns the date of the beginning of summer.
     */
    public Calendar getSummer() {
        return summer;
    }

    /**
     * Sets the date of the beginning of summer.
     */
    public void setSummer(Calendar summer) {
        this.summer = summer;
    }

    /**
     * Returns the date of the beginning of autumn.
     */
    public Calendar getAutumn() {
        return autumn;
    }

    /**
     * Sets the date of the beginning of autumn.
     */
    public void setAutumn(Calendar autumn) {
        this.autumn = autumn;
    }

    /**
     * Returns the date of the beginning of winter.
     */
    public Calendar getWinter() {
        return winter;
    }

    /**
     * Returns the date of the beginning of winter.
     */
    public void setWinter(Calendar winter) {
        this.winter = winter;
    }

    /**
     * Returns the current season name.
     */
    public @Nullable SeasonName getName() {
        return name;
    }

    /**
     * Sets the current season name.
     */
    public void setName(@Nullable SeasonName name) {
        this.name = name;
    }

    /**
     * Returns the next season.
     */
    public Calendar getNextSeason() {
        return DateTimeUtils.getNextFromToday(spring, summer, autumn, winter);
    }

    /**
     * Returns the next season name.
     */
    public SeasonName getNextName() {
        SeasonName nameLocal = name;
        if (nameLocal == null) {
            return SeasonName.values()[0];
        }
        int ordinal = nameLocal.ordinal() + 1;
        if (ordinal > 3) {
            ordinal = 0;
        }
        return SeasonName.values()[ordinal];
    }

    /**
     * Returns the time left for current season
     */
    public QuantityType<Time> getTimeLeft() {
        final Calendar now = Calendar.getInstance();
        final Calendar next = getNextSeason();
        final Duration timeLeft = Duration.of(next.getTimeInMillis() - now.getTimeInMillis(), ChronoUnit.MILLIS);

        return new QuantityType<>(timeLeft.toDays(), Units.DAY);
    }
}
