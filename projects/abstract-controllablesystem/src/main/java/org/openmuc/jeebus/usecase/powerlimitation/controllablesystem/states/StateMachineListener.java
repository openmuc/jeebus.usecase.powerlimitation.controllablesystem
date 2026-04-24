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

import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.ActiveLimit;

/**
 * The method {@link #onUpdate} of this Listener will be called on every
 * {@link Event} in the Controllable System.
 */
public interface StateMachineListener {
    /**
     * @param trigger
     *     the {@link Event} that triggered this update
     * @param state
     *     the resulting current {@link State} of the {@link StateMachine}
     * @param limit
     *     the current {@link ActiveLimit} of the Controllable System or null if it
     *     is not limited
     */
    void onUpdate(Event trigger, State state, ActiveLimit limit);
}
