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

import org.openmuc.jeebus.spine.api.DataUpdateType;
import org.openmuc.jeebus.spine.api.RequestResult;
import org.openmuc.jeebus.spine.spi.SpineSubscription;
import org.openmuc.jeebus.spine.utils.features.deviceconfiguration.KeyValue;
import org.openmuc.jeebus.spine.utils.features.deviceconfiguration.RunningKeyValue;
import org.openmuc.jeebus.spine.xsd.v1.DeviceDiagnosisHeartbeatDataType;
import org.openmuc.jeebus.spine.xsd.v1.LoadControlLimitDataType;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.states.StateMachine;

import java.util.ArrayList;
import java.util.List;

public class RuntimeListener implements FailsafeListener, SpineSubscription {
    private final StateMachine stateMachine;
    private final LoadControlLimit loadControlLimit;
    private List<DeviceDiagnosisHeartbeatDataType> receivedHeartbeats;

    public RuntimeListener(
        StateMachine stateMachine,
        LoadControlLimit loadControlLimit
    ) {
        this.stateMachine = stateMachine;
        this.loadControlLimit = loadControlLimit;
    }

    @Override
    public void messageReceived(RequestResult notification) {
        stateMachine.receiveHeartbeat(notification);
        if (receivedHeartbeats != null) {
            receivedHeartbeats.add(notification
                .getCmd()
                .getDeviceDiagnosisHeartbeatData());
        }
    }

    @Override
    public void updateFailsafeLimit(
        KeyValue failsafeLimit
    ) {
        stateMachine.updateFailsafeLimit(failsafeLimit);
    }

    @Override
    public void updateFailsafeDuration(KeyValue durationData) {
        stateMachine.updateFailsafeDuration(
            durationData
                .getDataCopy()
                .getValue()
                .getDuration()
        );
    }

    public void updateLimit(
        LoadControlLimitDataType update,
        DataUpdateType updateType,
        Integer idx
    ) {
        if (loadControlLimit.getLimitId().equals(update.getLimitId())) {
            stateMachine.updateLimit(loadControlLimit);
        }
    }

    public void enableHeartbeatTracking() {
        if (receivedHeartbeats == null) {
            receivedHeartbeats = new ArrayList<>();
        }
    }

    public List<DeviceDiagnosisHeartbeatDataType> getHeartbeats() {
        return receivedHeartbeats;
    }

    RunningKeyValue.WriteDataListener getFailsafeLimitListener(RunningKeyValue keyValue) {
        return (_data, _updateType) -> updateFailsafeLimit(keyValue);
    }

    RunningKeyValue.WriteDataListener getFailsafeDurationListener(RunningKeyValue keyValue) {
        return (_data, _updateType) -> updateFailsafeDuration(keyValue);
    }
}
