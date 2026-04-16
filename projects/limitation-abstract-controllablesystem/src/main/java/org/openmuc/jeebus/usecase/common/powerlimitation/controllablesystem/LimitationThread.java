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
import org.openmuc.jeebus.spine.xsd.v1.*;
import org.openmuc.jeebus.spine.spi.SpineSubscription;
import org.openmuc.jeebus.spine.utils.SpineUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.openmuc.jeebus.spine.xsd.v1.FeatureTypeEnumType.DEVICE_DIAGNOSIS;

public class LimitationThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(LimitationThread.class);
    private final LimitationUseCaseImpl useCase;
    private final Device device;
    private final SpineSubscription listener;
    private final FeatureAddressType deviceDiagnosisAddress;
    private final Feature clientFeature;

    public LimitationThread(
        LimitationUseCaseImpl useCase,
        Device device,
        RuntimeListener runtimeListener,
        UseCasePartner partner,
        Feature clientFeature
    ) {
        super(SpineUtilities.getThreadName(useCase.getName(), useCase.getActor()));
        this.useCase = useCase;
        this.device = device;
        this.listener = runtimeListener;
        this.clientFeature = clientFeature;

        deviceDiagnosisAddress = partner.getCompleteFeatureAddress(DEVICE_DIAGNOSIS);
    }

    @Override
    public void run(){
        startPreScenarioCommunication();
        startInitialScenarioCommunication();
        startRuntimeScenarioCommunication();
    }

    private void startPreScenarioCommunication() {
        if (device == null) {
            throw new IllegalStateException("Cannot subscribe when device not built yet");
        }
        try {
            clientFeature.requestSubscription(
                deviceDiagnosisAddress,
                DEVICE_DIAGNOSIS,
                listener
            ).get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(
                "There was an error subscribing to their DeviceDiagnosis."
                    + "Aborting CS use case execution.",
                e
            );
        }
        LOGGER.debug("Pre-Scenario communication completed");
    }

    private void startInitialScenarioCommunication() {
        CompletableFuture<RequestResult> deviceDiagnosisHeartbeatDataRequest
            = clientFeature.requestRead(
            deviceDiagnosisAddress,
            getDeviceDiagnosisHeartbeatDataReadCmd()
        );
        try {
            /* Let's interpret a successful heartbeat read request as a valid
             * received heartbeat. We may want to change this to test whether EGs
             * actually send heartbeats by themselves... */
            listener.messageReceived(deviceDiagnosisHeartbeatDataRequest.get());
        }
        catch (InterruptedException | ExecutionException e) {
            LOGGER.error("There was an error in the initial scenario communication:", e);
        }
    }

    private CmdType getDeviceDiagnosisHeartbeatDataReadCmd() {
        CmdType cmd = new CmdType();
        cmd.setFunction(FunctionEnumType.DEVICE_DIAGNOSIS_HEARTBEAT_DATA.value());
        cmd.setDeviceDiagnosisHeartbeatData(new DeviceDiagnosisHeartbeatDataType());
        return cmd;
    }

    private void startRuntimeScenarioCommunication() {
        /* Heartbeats start at runtime.
         * If a heart beats and nobody is there to listen
         * does it make a sound? */
        useCase.startHeartbeat();
    }
}
