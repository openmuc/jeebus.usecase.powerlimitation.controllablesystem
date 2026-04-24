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

import org.openmuc.jeebus.spine.xsd.v1.DeviceConfigurationKeyNameEnumType;
import org.openmuc.jeebus.spine.xsd.v1.DeviceConfigurationKeyValueDescriptionDataType;
import org.openmuc.jeebus.spine.xsd.v1.DeviceConfigurationKeyValueTypeType;
import org.openmuc.jeebus.spine.xsd.v1.EnergyDirectionEnumType;

import static org.openmuc.jeebus.spine.xsd.v1.DeviceConfigurationKeyNameEnumType.*;

public class FailsafeKeyValueDescriptions {

    public static DeviceConfigurationKeyValueDescriptionDataType createFailsafeDurationDescription() {
        DeviceConfigurationKeyValueDescriptionDataType description =
            new DeviceConfigurationKeyValueDescriptionDataType();
        description.setKeyName(FAILSAFE_DURATION_MINIMUM.value());
        description.setValueType(DeviceConfigurationKeyValueTypeType.DURATION);
        return description;
    }

    public static DeviceConfigurationKeyValueDescriptionDataType createFailsafeLimitDescription(
        EnergyDirectionEnumType direction
    ) {
        DeviceConfigurationKeyNameEnumType keyName = direction
            == EnergyDirectionEnumType.CONSUME ?
            FAILSAFE_CONSUMPTION_ACTIVE_POWER_LIMIT :
            FAILSAFE_PRODUCTION_ACTIVE_POWER_LIMIT;

        return new DeviceConfigurationKeyValueDescriptionDataType()
            .withKeyName(keyName.value())
            .withUnit("W")
            .withValueType(DeviceConfigurationKeyValueTypeType.SCALED_NUMBER);
    }
}
