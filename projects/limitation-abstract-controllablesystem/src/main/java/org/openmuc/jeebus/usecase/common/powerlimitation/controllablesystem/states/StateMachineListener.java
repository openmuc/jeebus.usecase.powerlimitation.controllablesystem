package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.states;

import org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.ActiveLimit;

/**
 * The method {@link #onUpdate} of this Listener will be called on every {@link Event} in the Controllable System.
 */
public interface StateMachineListener {
    /**
     * @param trigger the {@link Event} that triggered this update
     * @param state the resulting current {@link State} of the {@link StateMachine}
     * @param limit the current {@link ActiveLimit} of the Controllable System or null if it is not limited
     */
    void onUpdate(Event trigger, State state, ActiveLimit limit);
}
