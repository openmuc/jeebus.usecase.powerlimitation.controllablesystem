/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.demo;


import org.openmuc.jeebus.ship.api.ShipNodeConfiguration;
import org.openmuc.jeebus.shipspine.ShipCommunication;
import org.openmuc.jeebus.spine.api.Device;
import org.openmuc.jeebus.spine.utils.datatypes.ScaledNumberWrapper;
import org.openmuc.jeebus.spine.xsd.v1.DeviceTypeEnumType;
import org.openmuc.jeebus.spine.xsd.v1.EntityTypeEnumType;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.ActiveLimit;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.SimpleLimitationConfig;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.lpc.LpcCs;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.lpp.LppCs;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.states.Event;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.states.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.openmuc.jeebus.shipspine.ShipCommunication.ConnectClientsTo.TRUSTED;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );

    public static void main(String... args) {
        LOG.info(
            "Starting controllable system demo with args {}",
            Arrays.toString(args)
        );

        List<String> argList = Arrays.asList(args);

        ShipNodeConfiguration shipConfig = new ShipNodeConfiguration(
            argList.isEmpty() ? "0.0.0.0" : argList.get(0),
            argList.size() >= 2 ? Integer.parseInt(argList.get(1)) : 8080,
            "/ship/",
            true,
            "EXAMPLEBRAND-EEB01M3EU-001122334455",
            "local.",
            "Dishwasher ExampleCompany EEB01M4EU",
            "exampleAlias",
            "keystore.jks",
            "CHANGEME".toCharArray(),
            "CHANGEME".toCharArray(),
            "CN=example name",
            3650
        );

        ShipCommunication shipCommunication = new ShipCommunication(
            shipConfig
        ).withConnectClientsTo(
            TRUSTED // Configure which SHIP devices to connect to (ALL, TRUSTED, NONE)
        );

        if (argList.size() >= 3) {
            shipCommunication = shipCommunication.withTrustedSkis(
                // Here you can pre-trust remote SHIP devices identified by their SKI
                new HashSet<>(argList.subList(2, argList.size() - 1))
            );
        }

        ScaledNumberWrapper bigScaledNumber = new ScaledNumberWrapper(12, 6);

        String failsafeDuration = "PT2H";
        LpcCs lpcCs = new LpcCs(
            // TODO: here, you can define initial default values. The javadoc
            //  should explain the different parameters.
            new SimpleLimitationConfig(
                failsafeDuration,
                bigScaledNumber,
                bigScaledNumber,
                bigScaledNumber
        ));
        LppCs lppCs = new LppCs(
            new SimpleLimitationConfig(
                failsafeDuration,
                bigScaledNumber,
                /* For LPP, the LoadControl Limit is negative, but NominalMax
                 * and Failsafe are positive. */
                bigScaledNumber.negate(),
                bigScaledNumber
        ));

        lpcCs.addListener((trigger, state, limit) -> log(
            trigger,
            state,
            limit,
            lpcCs.getCharacteristicType()
        ));
        lppCs.addListener((trigger, state, limit) -> log(
            trigger,
            state,
            limit,
            lppCs.getCharacteristicType()
        ));

        lpcCs.addListener(((event, state, activeLimit) -> {
            // TODO: your listener goes here
        }));

        LOG.info("Initial Limit: {}", lpcCs.getActiveLimit());

        Device device = Device
            .getBuilder()
            // Set the SPINE device type
            .withDeviceType(DeviceTypeEnumType.GENERIC)
            // Set SHIP as the communication protocol
            .withCommunication(shipCommunication)
            // Set the SPINE device ID
            .withId("d:_n:MinimalExample_123")
            /* Enable the automatic SPINE DetailedDiscovery + UseCaseDiscovery of
             * remote devices */
            .withDiscoverDevices(true)
            .addEntity()
            .setType(EntityTypeEnumType.CEM)
            .withUseCases(
                /* Here you can add supported EEBus Use Cases to the device.
                 * These must implement the UseCase interface. */
                lpcCs,
                lppCs
            )
            .applyToDevice()
            .build();
    }

    private static void log(
        Event trigger,
        State state,
        ActiveLimit limit,
        String direction
    ) {
        LOG.info(
            "Event {} was fired resulting in State: {}; Active {} limit: {}",
            trigger.name(),
            state.name(),
            direction,
            limit
        );
    }
}
