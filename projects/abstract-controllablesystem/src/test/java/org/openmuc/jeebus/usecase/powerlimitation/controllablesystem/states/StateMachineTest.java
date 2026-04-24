/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.states;

import org.jmock.lib.concurrent.DeterministicScheduler;
import org.junit.jupiter.api.Test;
import org.openmuc.jeebus.spine.api.DataValidationException;
import org.openmuc.jeebus.spine.api.Feature;
import org.openmuc.jeebus.spine.api.FeatureWrapper;
import org.openmuc.jeebus.spine.api.RequestResult;
import org.openmuc.jeebus.spine.impl.EntityBuilder;
import org.openmuc.jeebus.spine.utils.features.deviceconfiguration.*;
import org.openmuc.jeebus.spine.utils.features.loadcontrol.LimitDescriptionFunction;
import org.openmuc.jeebus.spine.utils.features.loadcontrol.LimitListDataFunction;
import org.openmuc.jeebus.spine.utils.features.loadcontrol.LoadControlFeature;
import org.openmuc.jeebus.spine.xsd.v1.*;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.FailsafeKeyValueDescriptions;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.LoadControlLimit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.states.State.*;

class StateMachineTest {

    static final LoadControlFeature loadControlFeature;
    static final LimitDescriptionFunction limitDescriptionFunction;
    static final LimitListDataFunction limitListDataFunction;

    static {
        loadControlFeature = FeatureWrapper.newFeature(LoadControlFeature.class);
        limitListDataFunction = loadControlFeature.addLimitListDataFunction();
        limitDescriptionFunction = loadControlFeature.addLimitDescriptionFunction();
    }

    @Test
    public void testInitLimit() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 0
        StateMachine machine = getStateMachine();
        assertThat(machine.getState(), is(INIT));
        assertThat(machine.getActiveLimit().toString(), startsWith("22E+3 W"));

        assertThat(machine.canUpdateFailsafeValues(), is(false));
        assertThat(machine.wasHeartbeatReceived(), is(false));
    }

    @Test
    public void testInitToControlled() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 1
        StateMachine machine = getStateMachine();

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(false));
        assertThat(machine.getState(), is(CONTROLLED));
        assertThat(machine.getActiveLimit(), is(nullValue()));
    }

    @Test
    public void testInitToLimited() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 2
        StateMachine machine = getStateMachine();

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(true));
        assertThat(machine.getState(), is(LIMITED));
        assertThat(machine.getActiveLimit().toString(), startsWith("10 W"));
    }

    @Test
    public void testInitToAutonomous() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 3
        DeterministicScheduler scheduler = new DeterministicScheduler();
        StateMachine machine = getStateMachine(scheduler);

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(INIT));

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(AUTONOMOUS));
        assertThat(machine.getActiveLimit(), is(nullValue()));
    }

    @Test
    public void testControlledToLimited() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 4
        StateMachine machine = getStateMachine();

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(false));
        assertThat(machine.getState(), is(CONTROLLED));

        machine.updateLimit(getLimit(true));
        assertThat(machine.getState(), is(LIMITED));
        assertThat(machine.getActiveLimit().toString(), startsWith("10 W"));
    }

    @Test
    public void testControlledToFailsafe() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 5
        DeterministicScheduler scheduler = new DeterministicScheduler();

        StateMachine machine = getStateMachine(scheduler);

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(false));
        assertThat(machine.getState(), is(CONTROLLED));

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(CONTROLLED));

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(FAILSAFE));
        assertThat(machine.getActiveLimit().toString(), startsWith("22E+3 W"));
    }

    @Test
    public void testLimitedToControlled() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 6
        StateMachine machine = getStateMachine();

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(true));
        assertThat(machine.getState(), is(LIMITED));

        machine.updateLimit(getLimit(false));
        assertThat(machine.getState(), is(CONTROLLED));
        assertThat(machine.getActiveLimit(), is(nullValue()));
    }

    @Test
    public void testLimitedToFailsafe() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 7
        DeterministicScheduler scheduler = new DeterministicScheduler();
        StateMachine machine = getStateMachine(scheduler);

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(true));
        assertThat(machine.getState(), is(LIMITED));

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(LIMITED));

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(FAILSAFE));
        assertThat(machine.getActiveLimit().toString(), startsWith("22E+3 W"));
    }

    @Test
    public void testFailsafeToControlled() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 8
        DeterministicScheduler scheduler = new DeterministicScheduler();
        StateMachine machine = getStateMachine(scheduler);

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(true));
        assertThat(machine.getState(), is(LIMITED));

        scheduler.tick(2, MINUTES);
        assertThat(machine.getState(), is(FAILSAFE));

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(FAILSAFE));

        machine.updateLimit(getLimit(false));
        assertThat(machine.getState(), is(CONTROLLED));
        assertThat(machine.getActiveLimit(), is(nullValue()));
    }

    @Test
    public void testFailsafeToLimited() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 9
        DeterministicScheduler scheduler = new DeterministicScheduler();
        StateMachine machine = getStateMachine(scheduler);

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(true));
        assertThat(machine.getState(), is(LIMITED));

        scheduler.tick(2, MINUTES);
        assertThat(machine.getState(), is(FAILSAFE));

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(FAILSAFE));

        machine.updateLimit(getLimit(true));

        assertThat(machine.getState(), is(LIMITED));
        assertThat(machine.getActiveLimit().toString(), startsWith("10 W"));
    }

    @Test
    public void testFailsafeToAutonomousThroughExpiration()
        throws DatatypeConfigurationException, DataValidationException {
        // Transition 10
        DeterministicScheduler scheduler = new DeterministicScheduler();
        StateMachine machine = getStateMachine(scheduler);

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(true));
        assertThat(machine.getState(), is(LIMITED));

        scheduler.tick(2, MINUTES);
        assertThat(machine.getState(), is(FAILSAFE));

        scheduler.tick(2, MINUTES);
        assertThat(machine.getState(), is(FAILSAFE));

        scheduler.tick(2, HOURS);
        assertThat(machine.getState(), is(AUTONOMOUS));
        assertThat(machine.getActiveLimit(), is(nullValue()));
    }

    @Test
    public void testFailsafeToAutonomousThroughHeartbeat()
        throws DatatypeConfigurationException, DataValidationException {
        // Transition 10
        DeterministicScheduler scheduler = new DeterministicScheduler();
        StateMachine machine = getStateMachine(scheduler);

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(INIT));

        machine.updateLimit(getLimit(true));
        assertThat(machine.getState(), is(LIMITED));

        scheduler.tick(2, MINUTES);
        assertThat(machine.getState(), is(FAILSAFE));

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(FAILSAFE));

        scheduler.tick(2, MINUTES);
        assertThat(machine.getState(), is(AUTONOMOUS));
    }

    @Test
    public void testAutonomousToControlled() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 11
        DeterministicScheduler scheduler = new DeterministicScheduler();
        StateMachine machine = getStateMachine(scheduler);

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(INIT));

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(AUTONOMOUS));

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(AUTONOMOUS));

        machine.updateLimit(getLimit(false));
        assertThat(machine.getState(), is(CONTROLLED));
        assertThat(machine.getActiveLimit(), is(nullValue()));
    }

    @Test
    public void testAutonomousToLimited() throws DatatypeConfigurationException,
        DataValidationException {
        // Transition 12
        DeterministicScheduler scheduler = new DeterministicScheduler();
        StateMachine machine = getStateMachine(scheduler);

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(INIT));

        scheduler.tick(1, MINUTES);
        assertThat(machine.getState(), is(AUTONOMOUS));

        machine.receiveHeartbeat(NOTIFICATION);
        assertThat(machine.getState(), is(AUTONOMOUS));

        machine.updateLimit(getLimit(true));
        assertThat(machine.getState(), is(LIMITED));
        assertThat(machine.getActiveLimit().toString(), startsWith("10 W"));
    }

    @Test
    public void testSetLimitsWithinControlled() throws
        DatatypeConfigurationException, DataValidationException {
        StateMachine machine = getStateMachine();

        machine.receiveHeartbeat(NOTIFICATION);
        machine.updateLimit(getLimit(false));

        assertThat(machine.getActiveLimit(), is(nullValue()));
        machine.updateLimit(getLimit(false));

        assertThat(machine.getActiveLimit(), is(nullValue()));
    }

    private static final RequestResult NOTIFICATION = new RequestResult() {
        @Override
        public DatagramType getDatagram() {
            return null;
        }

        @Override
        public CmdType getCmd() {
            return null;
        }

        @Override
        public FeatureAddressType getSenderAddress() {
            FeatureAddressType featureAddressType = new FeatureAddressType();
            featureAddressType.setDevice("test_device");
            return featureAddressType;
        }

        @Override
        public ResultDataType getResultData() {
            return null;
        }
    };

    private static LoadControlLimit getLimit(boolean isActive) throws
        DataValidationException {
        LoadControlLimit loadControlLimit = new LoadControlLimit(
            loadControlFeature,
            new ScaledNumberType(10L, (short) 0),
            EnergyDirectionEnumType.CONSUME
        );
        LoadControlLimitDataType updateData = loadControlLimit.getDataCopy();
        updateData.setIsLimitActive(isActive);
        limitListDataFunction.updateData(0, updateData);
        return loadControlLimit;
    }

    private static StateMachine getStateMachine(
        ScheduledExecutorService scheduler,
        String failsafeDurationString
    ) throws DatatypeConfigurationException, DataValidationException {
        KeyValue failStateLimit = getFailsafeLimit();

        Duration failsafeDuration = DatatypeFactory
            .newInstance()
            .newDuration(failsafeDurationString);

        return new StateMachine(scheduler, failsafeDuration, failStateLimit);
    }

    private static KeyValue getFailsafeLimit() throws DataValidationException {
        DeviceConfigurationKeyValueValueType limitValue
            = new DeviceConfigurationKeyValueValueType();
        limitValue.setScaledNumber(new ScaledNumberType());
        limitValue.getScaledNumber().setNumber(22L);
        limitValue.getScaledNumber().setScale((short) 3);

        KeyValueListDataFunction keyValueListDataFunction
            = new KeyValueListDataFunction();
        KeyValueDescriptionListDataFunction keyValueDescriptionListDataFunction
            = new KeyValueDescriptionListDataFunction();
        Feature deviceConfiguration = Feature
            .getBuilder()
            .setType(FeatureTypeEnumType.DEVICE_CONFIGURATION)
            .setRole(RoleType.SERVER)
            .addFunction(keyValueListDataFunction)
            .addFunction(keyValueDescriptionListDataFunction)
            .build();
        EntityBuilder entityBuilder = new EntityBuilder();
        entityBuilder.addFeature(deviceConfiguration);

        // TODO: Test production also
        return new KeyValueInitialData(
            new DeviceConfigurationKeyValueDataType()
                .withValue(limitValue)
                .withIsValueChangeable(true),
            FailsafeKeyValueDescriptions.createFailsafeLimitDescription(
                EnergyDirectionEnumType.CONSUME
            ),
            null
        ).addToFeature(deviceConfiguration.getFeatureWrapper(
            DeviceConfigurationFeature.class));
    }

    private static StateMachine getStateMachine(
        ScheduledExecutorService scheduler
    ) throws DatatypeConfigurationException, DataValidationException {
        return getStateMachine(scheduler, "PT2H");
    }

    private static StateMachine getStateMachine() throws
        DatatypeConfigurationException, DataValidationException {
        return getStateMachine(Executors.newSingleThreadScheduledExecutor(), "PT2H");
    }
}