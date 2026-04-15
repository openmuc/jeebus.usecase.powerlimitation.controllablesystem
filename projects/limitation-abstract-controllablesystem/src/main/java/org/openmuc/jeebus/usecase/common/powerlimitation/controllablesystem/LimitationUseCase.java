package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem;

import org.openmuc.jeebus.spine.api.UseCasePartner;
import org.openmuc.jeebus.spine.spi.UseCase;

public interface LimitationUseCase extends UseCase {
    void startUseCase(UseCasePartner partner);
}
