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

/**
 * These are all the Events that can trigger a State change in the Controllable System.
 */
public enum Event {
    INIT_TIMEOUT,
    HEARTBEAT_TIMEOUT,
    ACTIVE_LIMIT_RECEIVED,
    LIMIT_DEACTIVATED,
    FAILSAFE_TIMEOUT
}
