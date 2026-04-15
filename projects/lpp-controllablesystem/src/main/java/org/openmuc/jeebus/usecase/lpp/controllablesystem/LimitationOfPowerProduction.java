package org.openmuc.jeebus.usecase.lpp.controllablesystem;

import org.openmuc.jeebus.spine.api.DataValidationException;
import org.openmuc.jeebus.spine.utils.datatypes.ScaledNumberWrapper;
import org.openmuc.jeebus.spine.xsd.v1.EnergyDirectionEnumType;
import org.openmuc.jeebus.spine.xsd.v1.LoadControlLimitDataType;
import org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.LimitationConfig;
import org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.LimitationUseCaseImpl;
import org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.SimpleLimitationConfig;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

import static org.openmuc.jeebus.spine.xsd.v1.ElectricalConnectionCharacteristicTypeEnumType.*;

public class LimitationOfPowerProduction extends LimitationUseCaseImpl {
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
        // we don't have a negate() method, so we have to do this instead...
        new ScaledNumberWrapper(
            (Long) (-LimitationConfig.DEFAULT_BIG_POWER.getNumber()),
            LimitationConfig.DEFAULT_BIG_POWER.getScale()
        ),
        LimitationConfig.DEFAULT_BIG_POWER
    );

    public LimitationOfPowerProduction(
        LimitationConfig limitationConfig
    ) {
        super(
            "limitationOfPowerProduction",
            "1.0.0",
            SCENARIO_SUPPORT_LIST,
            limitationConfig,
            EnergyDirectionEnumType.PRODUCE,
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
                scaledNumber.toDouble() <= 0
        )) {
            throw new DataValidationException(
                "LPP LoadControlLimits SHALL be <= 0."
            );
        }
    }

    @Override
    public String getCharacteristicType() {
        return isEms()
            ? CONTRACTUAL_PRODUCTION_NOMINAL_MAX.value()
            : POWER_PRODUCTION_NOMINAL_MAX.value();
    }

    /**
     * @return A {@link LimitationConfig} with all default values: 2 hours for
     * Failsafe Duration Minimum and 12 MW for all power limits.
     */
    public static SimpleLimitationConfig defaultLimitationConfig() {
        return DEFAULT_CONFIG;
    }
}
