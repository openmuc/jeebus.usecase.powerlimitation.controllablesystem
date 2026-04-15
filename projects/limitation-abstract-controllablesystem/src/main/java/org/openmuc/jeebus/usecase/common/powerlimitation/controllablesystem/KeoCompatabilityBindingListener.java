package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem;

import org.openmuc.jeebus.spine.api.UseCasePartner;
import org.openmuc.jeebus.spine.spi.BindingListener;
import org.openmuc.jeebus.spine.xsd.v1.NodeManagementBindingRequestCallType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KeoCompatabilityBindingListener implements BindingListener {

    private NodeManagementBindingRequestCallType.BindingRequest lastBinding;

    private List<UseCasePartner> partners = Collections.emptyList();
    private final LimitationUseCase useCase;

    public KeoCompatabilityBindingListener(LimitationUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void onBind(NodeManagementBindingRequestCallType.BindingRequest request) {
        this.lastBinding = request;
        evaluatePartners();
    }

    public void setPartners(List<UseCasePartner> partners) {
        this.partners = partners;
        evaluatePartners();
    }

    private void evaluatePartners() {
        if (
            ! partners.isEmpty()
            && Objects.nonNull(lastBinding)
            && Objects.equals(
                partners
                    .get(0)
                    .getDeviceInfo()
                    .getDescription()
                    .getDeviceAddress()
                    .getDevice(),
                lastBinding.getClientAddress().getDevice()
        )) {
            partners
                .stream()
                .filter(partner -> Objects.equals(
                    partner
                        .getUseCaseInfo()
                        .getAddress()
                        .getEntity(),
                    lastBinding.getClientAddress().getEntity()
                ))
                .findAny()
                .ifPresent(useCase::startUseCase);
        }
    }

}
