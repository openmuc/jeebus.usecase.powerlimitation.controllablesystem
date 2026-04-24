/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.lpc;

import org.openmuc.jeebus.spine.api.DataValidationException;
import org.openmuc.jeebus.spine.utils.datatypes.ScaledNumberWrapper;
import org.openmuc.jeebus.spine.xsd.v1.EnergyDirectionEnumType;
import org.openmuc.jeebus.spine.xsd.v1.LoadControlLimitDataType;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.LimitationConfig;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.LimitationUseCaseImpl;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.SimpleLimitationConfig;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

import static org.openmuc.jeebus.spine.xsd.v1.ElectricalConnectionCharacteristicTypeEnumType.CONTRACTUAL_CONSUMPTION_NOMINAL_MAX;
import static org.openmuc.jeebus.spine.xsd.v1.ElectricalConnectionCharacteristicTypeEnumType.POWER_CONSUMPTION_NOMINAL_MAX;

public class LimitationOfPowerConsumption extends LimitationUseCaseImpl {
    private static final List<Long> SCENARIO_SUPPORT_LIST = List.of(
        // Scenario 1 - Control active power consumption limit
        1L,
        // Scenario 2 - Failsafe values
        2L,
        // Scenario 3 - Heartbeat
        3L,
        // Scenario 4 - Constraints
        4L
    );

    private static final SimpleLimitationConfig DEFAULT_CONFIG
        = new SimpleLimitationConfig(
        LimitationConfig.DEFAULT_FAILSAFE_DURATION_MIN,
        LimitationConfig.DEFAULT_BIG_POWER,
        LimitationConfig.DEFAULT_BIG_POWER,
        LimitationConfig.DEFAULT_BIG_POWER
    );

    public LimitationOfPowerConsumption(
        LimitationConfig limitationConfig
    ) {
        super(
            "limitationOfPowerConsumption",
            "1.0.0",
            SCENARIO_SUPPORT_LIST,
            limitationConfig,
            EnergyDirectionEnumType.CONSUME,
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        );
    }

    @Override
    protected void validateLimitList(
        List<LoadControlLimitDataType> dataTypeList
    ) throws DataValidationException {
        if (!dataTypeList
            .stream()
            .filter(limit -> Objects.equals(
                limit.getLimitId(),
                loadControlLimit.getLimitId()
            ))
            .map(LoadControlLimitDataType::getValue)
            .filter(Objects::nonNull)
            .map(ScaledNumberWrapper::new)
            .allMatch(scaledNumber ->
                scaledNumber.toDouble() >= 0
            )) {
            throw new DataValidationException(
                "LPC LoadControlLimits SHALL be >= 0."
            );
        }
    }

    @Override
    public String getCharacteristicType() {
        return isEms()
            ? CONTRACTUAL_CONSUMPTION_NOMINAL_MAX.value()
            : POWER_CONSUMPTION_NOMINAL_MAX.value();
    }

    /**
     * @return A {@link LimitationConfig} with all default values: 2 hours for
     * Failsafe Duration Minimum and 12 MW for all power limits.
     */
    public static SimpleLimitationConfig defaultLimitationConfig() {
        return DEFAULT_CONFIG;
    }
}
