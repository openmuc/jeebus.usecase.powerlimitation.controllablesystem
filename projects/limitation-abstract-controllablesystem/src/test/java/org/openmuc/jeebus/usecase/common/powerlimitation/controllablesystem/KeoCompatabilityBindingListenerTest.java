package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.jupiter.api.*;
import org.openmuc.jeebus.spine.api.UseCasePartner;
import org.openmuc.jeebus.spine.xsd.v1.*;

import java.util.List;

class KeoCompatabilityBindingListenerTest {

    public static final String TEST_DEVICE = "testDevice";

    public static final NodeManagementBindingRequestCallType.BindingRequest
        BINDING_REQUEST = new NodeManagementBindingRequestCallType.BindingRequest()
        .withClientAddress(new FeatureAddressType()
            .withDevice(TEST_DEVICE)
            .withEntity(7L));

    public static final NodeManagementDetailedDiscoveryDeviceInformationType
        DEVICE_INFO = new NodeManagementDetailedDiscoveryDeviceInformationType(
        new NodeManagementDetailedDiscoveryDeviceInformationType.Description()
            .withDeviceAddress(new DeviceAddressType()
                .withDevice(TEST_DEVICE)));

    private final UseCasePartner partner = new UseCasePartner(
        "testComm",
        DEVICE_INFO,
        null,
        null,
        new NodeManagementUseCaseDataType.UseCaseInformation()
            .withAddress(new FeatureAddressType()
                .withDevice(TEST_DEVICE)
                .withEntity(7L))
    );

    private final List<UseCasePartner> partners = List.of(
        partner,
        new UseCasePartner(
            "testComm",
            DEVICE_INFO,
            null,
            null,
            new NodeManagementUseCaseDataType.UseCaseInformation()
                .withAddress(new FeatureAddressType()
                    .withDevice(TEST_DEVICE)
                    .withEntity(3L)))
    );

    @Test
    public void testOnlyBindings() {
        Mockery context = new Mockery();

        LimitationUseCase mock = context.mock(LimitationUseCase.class);

        KeoCompatabilityBindingListener listener
            = new KeoCompatabilityBindingListener(mock);

        context.checking(new Expectations() {{
            never(mock).startUseCase(partner);
        }});

        listener.onBind(BINDING_REQUEST);

        context.assertIsSatisfied();
    }

    @Test
    public void testOnlyPartners() {
        Mockery context = new Mockery();

        LimitationUseCase mock = context.mock(LimitationUseCase.class);

        KeoCompatabilityBindingListener listener
            = new KeoCompatabilityBindingListener(mock);

        context.checking(new Expectations() {{
            never(mock).startUseCase(partner);
        }});

        listener.setPartners(partners);

        context.assertIsSatisfied();
    }

    @Test
    public void testBindingFirst() {
        Mockery context = new Mockery();

        LimitationUseCase mock = context.mock(LimitationUseCase.class);

        KeoCompatabilityBindingListener listener
            = new KeoCompatabilityBindingListener(mock);

        listener.onBind(BINDING_REQUEST);

        context.checking(new Expectations() {{
            oneOf(mock).startUseCase(partner);
        }});

        listener.setPartners(partners);

        context.assertIsSatisfied();
    }

    @Test
    public void testPartnersFirst() {
        Mockery context = new Mockery();

        LimitationUseCase mock = context.mock(LimitationUseCase.class);

        KeoCompatabilityBindingListener listener
            = new KeoCompatabilityBindingListener(mock);

        listener.setPartners(partners);

        context.checking(new Expectations() {{
            oneOf(mock).startUseCase(partner);
        }});

        listener.onBind(BINDING_REQUEST);

        context.assertIsSatisfied();
    }
}
