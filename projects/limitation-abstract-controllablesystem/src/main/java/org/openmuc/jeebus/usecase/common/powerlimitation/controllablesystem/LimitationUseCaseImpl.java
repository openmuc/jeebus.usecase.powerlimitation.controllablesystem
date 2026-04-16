/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem;

import org.openmuc.jeebus.spine.api.*;
import org.openmuc.jeebus.spine.api.Error;
import org.openmuc.jeebus.spine.api.options.WriteListCmdOption;
import org.openmuc.jeebus.spine.spi.*;
import org.openmuc.jeebus.spine.utils.SpineUtilities;
import org.openmuc.jeebus.spine.utils.features.deviceconfiguration.*;
import org.openmuc.jeebus.spine.utils.features.devicediagnosis.HeartbeatDataFunction;
import org.openmuc.jeebus.spine.utils.features.electricalconnection.CharacteristicListDataFunction;
import org.openmuc.jeebus.spine.utils.features.electricalconnection.ElectricalConnectionFeature;
import org.openmuc.jeebus.spine.utils.features.loadcontrol.LimitDescriptionFunction;
import org.openmuc.jeebus.spine.utils.features.loadcontrol.LimitListDataFunction;
import org.openmuc.jeebus.spine.utils.features.loadcontrol.LoadControlFeature;
import org.openmuc.jeebus.spine.xsd.v1.*;
import org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.states.State;
import org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.states.StateMachine;
import org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.states.StateMachineListener;
import org.slf4j.Logger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.*;

import static org.openmuc.jeebus.spine.api.PresenceIndication.MANDATORY;
import static org.openmuc.jeebus.spine.api.options.WriteListCmdOption.*;
import static org.openmuc.jeebus.spine.utils.SpineUtilities.toDuration;
import static org.openmuc.jeebus.spine.xsd.v1.DeviceConfigurationKeyNameEnumType.FAILSAFE_DURATION_MINIMUM;
import static org.openmuc.jeebus.spine.xsd.v1.EntityTypeEnumType.*;
import static org.openmuc.jeebus.spine.xsd.v1.FeatureTypeEnumType.*;
import static org.openmuc.jeebus.spine.xsd.v1.FeatureTypeEnumType.GENERIC;
import static org.openmuc.jeebus.spine.xsd.v1.FunctionEnumType.DEVICE_DIAGNOSIS_HEARTBEAT_DATA;
import static org.openmuc.jeebus.spine.xsd.v1.RoleType.CLIENT;
import static org.openmuc.jeebus.spine.xsd.v1.RoleType.SERVER;

@AllowedEntityTypes({
    CEM,
    COMPRESSOR,
    EVSE,
    HEAT_PUMP_APPLIANCE,
    INVERTER,
    SMART_ENERGY_APPLIANCE,
    SUB_METER_ELECTRICITY
})
public abstract class LimitationUseCaseImpl implements LimitationUseCase {
    public static final int DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 60;
    private final Logger logger;

    private static final String ACTOR = "ControllableSystem";
    private final String useCaseName;
    private final String useCaseVersion;
    private final List<Long> scenarioSupport;
    protected final LimitationConfig limitationConfig;
    // inject fields go here
    @Inject
    private Entity entity;
    @Inject(role = CLIENT)
    private Feature clientFeature;
    @Inject
    private LoadControlFeature loadControlFeature;
    @Inject
    private DeviceConfigurationFeature deviceConfigurationFeature;
    @Inject
    protected ElectricalConnectionFeature electricalConnectionFeature;
    @Inject
    private HeartbeatDataFunction heartbeatDataFunction;

    // TODO check spec - in principle this could vary depending on the entity type
    private static final Set<FeatureRequirement> FEATURE_REQUIREMENTS = Set.of(
        new FeatureRequirement(
            GENERIC,
            CLIENT
        ),
        new FeatureRequirement(
            DEVICE_CONFIGURATION,
            SERVER,
            KeyValueListDataFunction.class,
            KeyValueDescriptionListDataFunction.class
        ),
        new FeatureRequirement(
            ELECTRICAL_CONNECTION,
            SERVER,
            CharacteristicListDataFunction.class
        ),
        new FeatureRequirement(
            DEVICE_DIAGNOSIS,
            SERVER,
            HeartbeatDataFunction.class
        ),
        new FeatureRequirement(
            LOAD_CONTROL,
            SERVER,
            LimitDescriptionFunction.class,
            LimitListDataFunction.class
        )
    );

    private static final Map<Long, PresenceIndication> SCENARIO_REQUIREMENTS
        = Map.of(1L, MANDATORY, 2L, MANDATORY, 3L, MANDATORY, 4L, MANDATORY);

    private static final Set<CommunicationPartnerFeatureRequirement>
        FEATURE_FUNCTION_REQUIREMENTS = Set.of(
            new CommunicationPartnerFeatureRequirement(
                DEVICE_DIAGNOSIS,
                Map.of(DEVICE_DIAGNOSIS_HEARTBEAT_DATA, Map.of(3L, MANDATORY))
    ));

    private final KeoCompatabilityBindingListener bindingListener;
    private final Duration initialFailsafeDuration;
    private DeviceConfigurationKeyValueDataType
        initalFailsafeLimit;

    public EnergyDirectionEnumType getLimitDirection() {
        return limitDirection;
    }

    private final EnergyDirectionEnumType limitDirection;

    private EntityAddressType address;
    private Device device = null;

    protected RuntimeListener runtimeListener;
    private StateMachine stateMachine;
    protected LoadControlLimit loadControlLimit;

    protected RunningKeyValue failsafeDuration;
    protected RunningKeyValue failsafeLimit;

    private LimitationThread limitationThread;

    private static final Long LOWER_DURATION_BOUND = (long) (1000 * 60 * 60 * 2);
    private static final Long UPPER_DURATION_BOUND = (long) (1000 * 60 * 60 * 24);

    private static Long nominalMaxIdCount = 0L;
    protected int electricalConnectionNominalMaxIndex;

    public LimitationUseCaseImpl(
        String name,
        String version,
        List<Long> scenarioSupport,
        LimitationConfig limitationConfig,
        EnergyDirectionEnumType limitDirection,
        Logger logger
    ) {
        useCaseName = name;
        useCaseVersion = version;
        this.scenarioSupport = scenarioSupport;
        this.limitDirection = limitDirection;
        this.limitationConfig = limitationConfig;
        this.logger = logger;

        initialFailsafeDuration = toDuration(limitationConfig
            .getFailsafeDurationMin());

        initalFailsafeLimit = new DeviceConfigurationKeyValueDataType().withValue(
            new DeviceConfigurationKeyValueValueType().withScaledNumber(
                limitationConfig.getFailsafeLimit()
            ));

        stateMachine = new StateMachine(
            initialFailsafeDuration,
            new KeyValue() {
                @Override
                public DeviceConfigurationKeyValueDataType getDataCopy() {
                    return initalFailsafeLimit;
                }

                @Override
                public DeviceConfigurationKeyValueDescriptionDataType getDescriptionCopy() {
                    return FailsafeKeyValueDescriptions.createFailsafeLimitDescription(
                        limitDirection);
                }

                @Override
                public DeviceConfigurationKeyValueConstraintsDataType getConstraintsCopy() {
                    return null;
                }
            }
        );

        bindingListener = new KeoCompatabilityBindingListener(this);
    }

    private void initFeatures() {
        deviceConfigurationFeature
            .getKeyValueListDataFunction()
            .addUseCaseMatchingDataWriteValidation(this::validateDeviceConfigurationUpdate);
        // No delete write
        deviceConfigurationFeature
            .getKeyValueListDataFunction()
            .setAllowedWriteCmdOptions(
                FULL_WRITE,
                PARTIAL_WRITE_BY_FUNCTION_ID,
                PARTIAL_WRITE_BY_SELECTOR
            );

        deviceConfigurationFeature
            .getKeyValueDescriptionListDataFunction()
            .setWritable(false, false);

        loadControlFeature.getLimitDescriptionFunction().setWritable(false, false);
        loadControlFeature.getLimitListDataFunction().setAllowedWriteCmdOptions(
            FULL_WRITE,
            PARTIAL_WRITE_BY_FUNCTION_ID,
            PARTIAL_WRITE_BY_SELECTOR,
            DELETE_ELEMENTS,
            DELETE_ELEMENTS_BY_SELECTOR
        );
        loadControlFeature
            .getLimitListDataFunction()
            .addUseCaseDataValidation(this::validateLoadControlData);
        loadControlFeature
            .getLimitListDataFunction()
            .addUseCaseWriteValidationFull(this::validateLoadControlWrite);
    }

    private void validateLoadControlWrite(
        List<LoadControlLimitDataType> dataTypeList,
        List<LoadControlLimitListDataSelectorsType> selctorList,
        LoadControlLimitDataElementsType elements,
        WriteListCmdOption writeListCmdOption
    ) throws DataValidationException {
        if (!stateMachine.wasHeartbeatReceived()) {
            throw new DataValidationException(
                "LoadControlLimit Write-Command but no Heartbeat was received in"
                    + " time."
            );
        }
        switch (writeListCmdOption) {
            case FULL_WRITE:
            case PARTIAL_WRITE_BY_FUNCTION_ID:
            case PARTIAL_WRITE_BY_SELECTOR:
                validateLimitList(dataTypeList);
                break;
            case DELETE_ELEMENTS:
            case DELETE_ELEMENTS_BY_SELECTOR:
                if (!isValidLoadControlDelete(elements)) {
                    throw new DataValidationException(
                        "May not delete limitId, isLimitActive, isLimitChangeable"
                            + " or value."
                    );
                }
                break;
            case DELETE_BY_SELECTOR:
                throw new DataValidationException(
                    "Deleting whole LoadControlLimits is not allowed."
                );
        }
    }

    public abstract String getCharacteristicType();

    protected abstract void validateLimitList(
        List<LoadControlLimitDataType> dataTypeList
    ) throws DataValidationException;

    private static boolean isValidLoadControlDelete(
        LoadControlLimitDataElementsType elements
    ) {
        return elements.getLimitId() == null
            || elements.getIsLimitActive() == null
            || elements.getIsLimitChangeable() == null
            || elements.getValue() == null;
    }

    private void validateLoadControlData(LoadControlLimitDataType data)
        throws DataValidationException {

        if (data.getTimePeriod() != null
            && data.getTimePeriod().getEndTime() != null) {
            try {
                DatatypeFactory
                    .newDefaultInstance()
                    .newDuration(data.getTimePeriod().getEndTime());
            }
            catch (IllegalArgumentException e) {
                throw new DataValidationException(
                    "Duration must be relative time",
                    e
                );
            }
        }
    }

    private void initLoadControlLimit() {
        try {
            loadControlLimit = new LoadControlLimit(
                loadControlFeature,
                limitationConfig.getLoadControlLimit(),
                limitDirection
            );
        }
        catch (DataValidationException e) {
            throw new RuntimeException(
                "Failed to initialize Load Control Limit: ", e
            );
        }
    }

    private void initNominalMax() {
        ElectricalConnectionCharacteristicDataType data
            = new ElectricalConnectionCharacteristicDataType();

        data.setElectricalConnectionId(nominalMaxIdCount++);
        // FIXME: figure out what these sub-identifiers are for
        data.setCharacteristicId(0L);
        data.setParameterId(0L);

        data.setValue(limitationConfig.getNominalMax());

        data.setUnit("W");

        data.setCharacteristicContext(
            ElectricalConnectionCharacteristicContextEnumType.ENTITY.value()
        );

        String characteristicType = getCharacteristicType();

        data.setCharacteristicType(characteristicType);

        try {
            electricalConnectionNominalMaxIndex = electricalConnectionFeature
                .getCharacteristicListDataFunction()
                .addData(data);
        }
        catch (DataValidationException e) {
            throw new RuntimeException(
                "Failed to initialize Electrical Connection Nominal Max: " + e
            );
        }
    }

    protected boolean isEms() {
        return Objects.equals(
            this.entity.getType(),
            EntityTypeEnumType.CEM
        );
    }

    private void initKeyValues() {
        try {
            Optional<RunningKeyValue> existingFailsafeDuration
                = deviceConfigurationFeature.getKeyValueForKeyName(
                    FAILSAFE_DURATION_MINIMUM
            );
            if (existingFailsafeDuration.isPresent()) {
                failsafeDuration = existingFailsafeDuration.get();
            }
            else {
                failsafeDuration = new KeyValueInitialData(
                    new DeviceConfigurationKeyValueDataType().withValue(
                        new DeviceConfigurationKeyValueValueType()
                            .withDuration(initialFailsafeDuration)
                    ).withIsValueChangeable(true),
                    FailsafeKeyValueDescriptions.createFailsafeDurationDescription(),
                    null
                ).addToFeature(deviceConfigurationFeature);
                failsafeDuration.setWriteDataAllowed(true);
            }
            failsafeLimit = new KeyValueInitialData(
                initalFailsafeLimit.withIsValueChangeable(true),
                FailsafeKeyValueDescriptions.createFailsafeLimitDescription(
                    limitDirection),
                null
            ).addToFeature(deviceConfigurationFeature);
            failsafeLimit.setWriteDataAllowed(true);
        }
        catch (DataValidationException e) {
            throw new RuntimeException(
                "Failed to initialize KeyValues because of faulty initial data: "
                ,
                e
            );
        }
    }

    private void startUseCase(List<UseCasePartner> partners) {
        UseCasePartner firstPartner = partners.get(0);

        if (partners.size() > 1) {
            logger.warn(
                "SPINE Discovery process yielded multiple Use Case instances on the "
                    + "same device. Enabling KEO Compatibility Mode.");
            bindingListener.setPartners(partners);
        }
        else {
            startUseCase(firstPartner);
        }
    }

    public void startUseCase(UseCasePartner partner) {
        logger.info(
            "EnergyGuard found at {} Entity {}, beginning Use Case execution",
            partner.getDeviceInfo().getDescription().getDeviceAddress().getDevice(),
            partner.getEntityInfo().getDescription().getEntityAddress().getEntity()
        );
        if (Objects.nonNull(limitationThread)) {
            logger.warn("There already was a Thread for the Use Case execution. "
                + "Interrupting it and starting a new one.");
            limitationThread.interrupt();
        }
        limitationThread = new LimitationThread(
            this,
            device,
            runtimeListener,
            partner,
            clientFeature
        );

        limitationThread.start();
    }

    @Override
    public final String getActor() {
        return ACTOR;
    }

    @Override
    public final String getName() {
        return useCaseName;
    }

    @Override
    public final String getVersion() {
        return useCaseVersion;
    }

    @Override
    public final List<Long> getScenarioSupport() {
        return scenarioSupport;
    }

    @Override
    public FeatureAddressType getAddress() {
        FeatureAddressType featureAddress = new FeatureAddressType();
        featureAddress.setDevice(address.getDevice());
        featureAddress.getEntity().addAll(address.getEntity());
        return featureAddress;
    }

    private void validateDeviceConfigurationUpdate(
        DeviceConfigurationKeyValueDataType update,
        List<DeviceConfigurationKeyValueDataType> dataToUpdateList,
        WriteListCmdOption writeCmdOption
    ) throws SpineException {
        if (Objects.equals(update.getKeyId(), getFailsafeDuration().getKeyId())
            && update.getValue().getDuration() != null
        ) {
            Optional<UseCase> other = device
                .getUseCases()
                .keySet()
                .stream()
                .filter(useCase -> Objects.equals(
                    this.entity,
                    device.getUseCases().get(useCase)
                ))
                .filter(useCase -> LimitationUseCase.class.isAssignableFrom(useCase.getClass()))
                .filter(useCase -> useCase != this)
                .findAny();

            if (other.isPresent()
                && !this.canUpdateFailsafeValues()
                && !((LimitationUseCaseImpl) other.get()).canUpdateFailsafeValues()
            ) {
                throw getStateMachineException();
            }
            else if (!isValidDuration(update.getValue().getDuration())) {
                throw new SpineException(
                    Error.COMMAND_REJECTED,
                    "Received write request for Key Value with an invalid duration."
                );
            }
        } else if (Objects.equals(update.getKeyId(), getFailsafeLimit().getKeyId())
            && update.getValue().getScaledNumber() != null
        ) {

            if (!stateMachine.canUpdateFailsafeValues()) {
                throw getStateMachineException();
            } else if (update.getValue().getScaledNumber().getNumber() == null
                || update.getValue().getScaledNumber().getScale() == null
                && update.getValue().getScaledNumber().getNumber() < 0) {

                throw new SpineException(
                    Error.COMMAND_REJECTED,
                    "Received write request for Key Value with an invalid "
                        + "scaled number."
                );
            }
        }
    }

    private SpineException getStateMachineException() {
        return new SpineException(
            Error.COMMAND_REJECTED,
            "May not update "
                + getLimitDirection()
                + " failsafe value, is in State: "
                + stateMachine.getState()
        );
    }

    private static boolean isValidDuration(Duration duration) {
        Long milliseconds
            = SpineUtilities.durationToRelativeMilliseconds(duration.toString());

        return milliseconds >= LOWER_DURATION_BOUND
            && milliseconds <= UPPER_DURATION_BOUND;
    }

    @Override
    public void setup() {
        this.device = entity.getDevice();
        address = entity.getStaticAddress();
        loadControlFeature.getFeature().addBindingListener(bindingListener);

        initFeatures();
        initKeyValues();
        initLoadControlLimit();

        runtimeListener = new RuntimeListener(stateMachine, loadControlLimit);

        failsafeLimit.addWriteDataListener(
            runtimeListener.getFailsafeLimitListener(failsafeLimit));
        failsafeDuration.addWriteDataListener(
            runtimeListener.getFailsafeDurationListener(failsafeDuration));

        loadControlFeature
            .getLimitListDataFunction()
            .addUseCaseWriteDataListener(runtimeListener::updateLimit);

        stateMachine.updateLimit(loadControlLimit);

        initNominalMax();

        UseCaseListener useCaseListener = this::startUseCase;

        this.device.getNodeManagement().addUseCaseListener(
            useCaseListener,
            getName(),
            "EnergyGuard",
            SCENARIO_REQUIREMENTS,
            FEATURE_FUNCTION_REQUIREMENTS
        );
    }

    @Override
    public Set<FeatureRequirement> getFeatureRequirements(EntityTypeEnumType entityType) {
        return FEATURE_REQUIREMENTS;
    }

    /**
     * Add a listener that will be notified on all state changing Events in the
     * internal state machine of the Controllable System.
     *
     * @param what
     *     the listener to add
     */
    public void addListener(StateMachineListener what) {
        stateMachine.addListener(what);
    }

    /**
     * @return an immutable representation of the currently {@link ActiveLimit} of
     * the Controllable System or null if it is not limited
     */
    public ActiveLimit getActiveLimit() {
        return stateMachine.getActiveLimit();
    }

    public State getState() {
        return stateMachine.getState();
    }

    public void enableHeartbeatTracking() {
        runtimeListener.enableHeartbeatTracking();
    }

    public List<DeviceDiagnosisHeartbeatDataType> getHeartbeats() {
        return runtimeListener.getHeartbeats();
    }

    public void startHeartbeat() {
        heartbeatDataFunction.startHeartbeat();
    }

    @Override
    public void close() {
        // TODO: figure out what needs cleaning up
    }

    public HeartbeatDataFunction getHeartbeatDataFunction() {
        return heartbeatDataFunction;
    }

    public RunningKeyValue getFailsafeDuration() {
        return failsafeDuration;
    }

    public RunningKeyValue getFailsafeLimit() {
        return failsafeLimit;
    }

    private boolean canUpdateFailsafeValues() {
        return stateMachine.canUpdateFailsafeValues();
    }

}
