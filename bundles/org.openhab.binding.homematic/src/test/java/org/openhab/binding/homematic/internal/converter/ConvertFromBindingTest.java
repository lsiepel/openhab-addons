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
package org.openhab.binding.homematic.internal.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.homematic.internal.model.HmDatapoint;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.State;

/**
 * Tests for
 * {@link org.openhab.binding.homematic.internal.converter.type.AbstractTypeConverter#convertFromBinding(HmDatapoint)}.
 *
 * @author Michael Reitler - Initial Contribution
 *
 */
@NonNullByDefault
public class ConvertFromBindingTest extends BaseConverterTest {

    @Test
    public void testDecimalTypeConverter() throws ConverterException {
        State convertedState;
        TypeConverter<?> decimalConverter = ConverterFactory.createConverter("Number");

        // the binding is backwards compatible, so clients may still use DecimalType, even if a unit is used
        floatDp.setUnit("%");

        floatDp.setValue(99.9);
        convertedState = decimalConverter.convertFromBinding(floatDp);
        assertThat(convertedState, instanceOf(DecimalType.class));
        DecimalType decimalState = (DecimalType) Objects.requireNonNull(convertedState);
        assertThat(decimalState.doubleValue(), is(99.9));

        floatDp.setValue(77.77777778);
        convertedState = decimalConverter.convertFromBinding(floatDp);
        assertThat(convertedState, instanceOf(DecimalType.class));
        decimalState = (DecimalType) Objects.requireNonNull(convertedState);
        assertThat(decimalState.doubleValue(), is(77.777778));

        integerDp.setValue(99);
        convertedState = decimalConverter.convertFromBinding(integerDp);
        assertThat(convertedState, instanceOf(DecimalType.class));
        decimalState = (DecimalType) Objects.requireNonNull(convertedState);
        assertThat(decimalState.doubleValue(), is(99.0));
    }

    @Test
    public void testQuantityTypeConverter() throws ConverterException {
        State convertedState;
        TypeConverter<?> temperatureConverter = ConverterFactory.createConverter("Number:Temperature");
        TypeConverter<?> frequencyConverter = ConverterFactory.createConverter("Number:Frequency");
        TypeConverter<?> timeConverter = ConverterFactory.createConverter("Number:Time");

        floatQuantityDp.setValue(10.5);
        floatQuantityDp.setUnit("°C");
        convertedState = temperatureConverter.convertFromBinding(floatQuantityDp);
        assertThat(convertedState, instanceOf(QuantityType.class));
        QuantityType<?> quantityState = (QuantityType<?>) Objects.requireNonNull(convertedState);
        assertThat(quantityState.getDimension(), is(equalTo(SIUnits.CELSIUS.getDimension())));
        assertThat(quantityState.doubleValue(), is(10.5));

        floatQuantityDp.setUnit("Â°C");
        convertedState = temperatureConverter.convertFromBinding(floatQuantityDp);
        assertThat(convertedState, instanceOf(QuantityType.class));
        quantityState = (QuantityType<?>) Objects.requireNonNull(convertedState);
        assertThat(quantityState.getDimension(), is(equalTo(SIUnits.CELSIUS.getDimension())));
        assertThat(quantityState.doubleValue(), is(10.5));

        integerQuantityDp.setValue(50000);
        integerQuantityDp.setUnit("mHz");
        convertedState = frequencyConverter.convertFromBinding(integerQuantityDp);
        assertThat(convertedState, instanceOf(QuantityType.class));
        quantityState = (QuantityType<?>) Objects.requireNonNull(convertedState);
        assertThat(quantityState.getDimension(), is(equalTo(Units.HERTZ.getDimension())));
        assertThat(quantityState.getUnit(), is(equalTo(MetricPrefix.MILLI(Units.HERTZ))));
        assertThat(quantityState.intValue(), is(50000));

        floatQuantityDp.setValue(0.7);
        floatQuantityDp.setUnit("100%");
        convertedState = timeConverter.convertFromBinding(floatQuantityDp);
        assertThat(convertedState, instanceOf(QuantityType.class));
        quantityState = (QuantityType<?>) Objects.requireNonNull(convertedState);
        assertThat(quantityState.getDimension(), is(equalTo(Units.ONE.getDimension())));
        assertThat(quantityState.doubleValue(), is(70.0));
        assertThat(quantityState.getUnit(), is(Units.PERCENT));
    }

    @Test
    public void testPercentTypeConverter() throws ConverterException {
        State convertedState;
        TypeConverter<?> percentTypeConverter = ConverterFactory.createConverter("Dimmer");

        // the binding is backwards compatible, so clients may still use DecimalType, even if a unit is used
        integerDp.setUnit("%");

        integerDp.setValue(99);
        integerDp.setMaxValue(100);
        convertedState = percentTypeConverter.convertFromBinding(integerDp);
        assertThat(convertedState, instanceOf(PercentType.class));
        PercentType percentState = (PercentType) Objects.requireNonNull(convertedState);
        assertThat(percentState.doubleValue(), is(99.0));
    }
}
