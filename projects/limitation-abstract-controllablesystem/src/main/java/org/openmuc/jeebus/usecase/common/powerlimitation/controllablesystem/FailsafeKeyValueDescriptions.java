package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem;

import org.openmuc.jeebus.spine.xsd.v1.*;

public class FailsafeKeyValueDescriptions {

    public static DeviceConfigurationKeyValueDescriptionDataType createFailsafeDurationDescription() {
        DeviceConfigurationKeyValueDescriptionDataType description =
            new DeviceConfigurationKeyValueDescriptionDataType();
        description.setKeyName(DeviceConfigurationKeyNameEnumType.FAILSAFE_DURATION_MINIMUM.value());
        description.setValueType(DeviceConfigurationKeyValueTypeType.DURATION);
        return description;
    }

    public static DeviceConfigurationKeyValueDescriptionDataType createFailsafeLimitDescription(EnergyDirectionEnumType direction) {
        DeviceConfigurationKeyNameEnumType keyName = direction == EnergyDirectionEnumType.CONSUME
                ? DeviceConfigurationKeyNameEnumType.FAILSAFE_CONSUMPTION_ACTIVE_POWER_LIMIT
                : DeviceConfigurationKeyNameEnumType.FAILSAFE_PRODUCTION_ACTIVE_POWER_LIMIT;
        return new DeviceConfigurationKeyValueDescriptionDataType()
                .withKeyName(keyName.value())
                .withUnit("W")
                .withValueType(DeviceConfigurationKeyValueTypeType.SCALED_NUMBER);
    }
}
