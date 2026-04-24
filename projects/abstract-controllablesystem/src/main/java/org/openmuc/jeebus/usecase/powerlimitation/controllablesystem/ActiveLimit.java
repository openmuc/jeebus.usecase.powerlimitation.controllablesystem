/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.usecase.powerlimitation.controllablesystem;

import org.openmuc.jeebus.spine.utils.datatypes.ScaledNumberWrapper;
import org.openmuc.jeebus.spine.xsd.v1.*;
import org.openmuc.jeebus.spine.utils.features.deviceconfiguration.KeyValue;

import javax.xml.datatype.Duration;
import java.util.Objects;
import java.util.Optional;

import static org.openmuc.jeebus.spine.xsd.v1.DeviceConfigurationKeyNameEnumType.FAILSAFE_CONSUMPTION_ACTIVE_POWER_LIMIT;
import static org.openmuc.jeebus.spine.xsd.v1.DeviceConfigurationKeyNameEnumType.FAILSAFE_PRODUCTION_ACTIVE_POWER_LIMIT;

/**
 * This is an immutable representation of an active limit.
 * It consists of a unit, an optional duration and a scaled number to be interpreted
 * with the following formular: number * 10^scale
 */
public class ActiveLimit {

    private final String unit;

    private final ScaledNumberWrapper scaledNumber;

    private final Optional<String> duration;

    public ActiveLimit(KeyValue failsafeLimit, Duration failsafeDuration) {
        DeviceConfigurationKeyValueDescriptionDataType description
            = failsafeLimit.getDescriptionCopy();

        if (
            !Objects.equals(
                FAILSAFE_CONSUMPTION_ACTIVE_POWER_LIMIT.value(),
                description.getKeyName())
            && !Objects.equals(
                FAILSAFE_PRODUCTION_ACTIVE_POWER_LIMIT.value(),
                description.getKeyName()
        )) {
            throw new IllegalArgumentException(
                "The given Key must be a FAILSAFE_CONSUMPTION_ACTIVE_POWER_LIMIT or "
                    + "FAILSAFE_PRODUCTION_ACTIVE_POWER_LIMIT but was "
                    + description.getKeyName());
        }
        unit = description.getUnit();

        this.scaledNumber = new ScaledNumberWrapper(
            failsafeLimit
                .getDataCopy()
                .getValue()
                .getScaledNumber()
        );

        this.duration = Optional.of(failsafeDuration.toString());
    }

    public ActiveLimit(LoadControlLimit limit) {
        this.unit = limit.getDescriptionCopy().getUnit();

        LoadControlLimitDataType dataCopy = limit.getDataCopy();

        this.scaledNumber = new ScaledNumberWrapper(dataCopy.getValue());

        this.duration = Optional.ofNullable(dataCopy.getTimePeriod())
            .map(TimePeriodType::getEndTime);
    }

    public String getUnit() {
        return unit;
    }

    public ScaledNumberWrapper getScaledNumber() {
        return scaledNumber;
    }

    public Optional<String> getDuration() {
        return duration;
    }

    /**
     * @return {@link ScaledNumberWrapper#toDouble()}
     */
    public Double getResultingValue() {
        return scaledNumber.toDouble();
    }

    @Override
    public String toString() {
        String result = scaledNumber.toString() + " " + getUnit() + ", active ";

        result += duration.map(s -> "for " + s).orElse("until EG disconnects");

        return result;
    }
}
