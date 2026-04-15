package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem;

import org.openmuc.jeebus.spine.utils.features.deviceconfiguration.KeyValue;

public interface FailsafeListener {

    void updateFailsafeDuration(KeyValue duration);
    void updateFailsafeLimit(KeyValue failsafeLimit);

    default void updateKeyValue(KeyValue keyValue) {
        switch (keyValue.getDescriptionCopy().getValueType()) {
            case DURATION:
                updateFailsafeDuration(keyValue);
            case SCALED_NUMBER:
                updateFailsafeLimit(keyValue);
        }
    }
}
