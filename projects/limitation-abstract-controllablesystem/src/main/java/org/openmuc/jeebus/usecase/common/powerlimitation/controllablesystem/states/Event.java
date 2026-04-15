package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.states;

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
