package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.states;

import org.openmuc.jeebus.spine.api.RequestResult;
import org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.ActiveLimit;
import org.openmuc.jeebus.spine.utils.SpineUtilities;
import org.openmuc.jeebus.spine.utils.features.deviceconfiguration.KeyValue;
import org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.LoadControlLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.*;
import static org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.states.State.*;
import static org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.states.Event.*;

public class StateMachine {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> heartbeatExpiration = null;
    private final ScheduledFuture<?> initExpiration;

    private LoadControlLimit loadControlLimit;
    private KeyValue failsafeLimit;
    private Duration failsafeDuration;

    private State currentState;

    private final List<StateMachineListener> listeners = new ArrayList<>();

    public StateMachine(
        Duration failsafeDuration,
        KeyValue failsafeLimit
    ) {
        this(Executors.newSingleThreadScheduledExecutor(), failsafeDuration, failsafeLimit);
    }

    StateMachine(
        ScheduledExecutorService executor,
        Duration failsafeDuration,
        KeyValue failsafeLimit
    ) {
        this.executor = executor;
        this.failsafeDuration = failsafeDuration;
        this.failsafeLimit = failsafeLimit;

        this.currentState = INIT;
        this.initExpiration = scheduleInitTimeout();
    }

    private void transition(Event trigger, State from, State to) {
        if (
            to != null
            && from != to
            && currentState == from
        ) {
            if (Objects.equals(from, INIT)) {
                initExpiration.cancel(false);
            }

            currentState = to;

            if (Objects.equals(currentState, FAILSAFE)) {
                scheduleFailsafeExpiration();
            }
        }
        notifyListeners(trigger);
    }

    private void transitionToNextState(Event trigger) {
        transition(
            trigger,
            currentState,
            currentState.handleEvent(trigger, wasHeartbeatReceived())
        );
    }

    private void notifyListeners(Event trigger) {
        for (StateMachineListener listener : listeners) {
            listener.onUpdate(trigger, currentState, getActiveLimit());
        }
    }

    private ScheduledFuture<?> scheduleInitTimeout() {
        return executor.schedule(
            () -> transitionToNextState(INIT_TIMEOUT),
            2,
            MINUTES
        );
    }

    private ScheduledFuture<?> scheduleHeartbeatTimeout() {
        return executor.schedule(
            () -> transitionToNextState(HEARTBEAT_TIMEOUT),
            2,
            MINUTES
        );
    }

    private void scheduleFailsafeExpiration(){
        executor.schedule(
            () -> {
                transitionToNextState(FAILSAFE_TIMEOUT);
                LOGGER.info("failsafe duration expired");
            },
            SpineUtilities.durationToRelativeMilliseconds(failsafeDuration.toString()),
            MILLISECONDS
        );
    }

    public boolean wasHeartbeatReceived() {
        // If a heartbeat was received, there is an active expiration schedule for it.
        return Objects.nonNull(this.heartbeatExpiration) && !heartbeatExpiration.isDone();
    }

    public void receiveHeartbeat(RequestResult notification) {
        if (heartbeatExpiration != null) {
            heartbeatExpiration.cancel(false);
        }
        heartbeatExpiration = scheduleHeartbeatTimeout();

        LOGGER.debug("received heartbeat from {}", notification.getSenderAddress().getDevice());
    }

    public void updateLimit(LoadControlLimit limit) {
        if (wasHeartbeatReceived()) {
            this.loadControlLimit = limit;
            Event event = limit.isActive() ?
                ACTIVE_LIMIT_RECEIVED :
                LIMIT_DEACTIVATED;

            transitionToNextState(event);
        }
    }

    public void updateFailsafeLimit(KeyValue failsafeLimit) {
        if (Objects.nonNull(failsafeLimit)
            && canUpdateFailsafeValues()
        ) {
            this.failsafeLimit = failsafeLimit;
        }
    }

    public State getState() {
        return currentState;
    }

    public void updateFailsafeDuration(Duration duration) {
        if (Objects.nonNull(duration)
            && canUpdateFailsafeValues()
        ) {
            this.failsafeDuration = duration;
        }
    }

    public boolean canUpdateFailsafeValues() {
        return currentState.canUpdateFailsafeValues();
    }

    public ActiveLimit getActiveLimit() {
        switch (currentState) {
            case LIMITED:
                return new ActiveLimit(this.loadControlLimit);
            case INIT:
            case FAILSAFE:
                return new ActiveLimit(this.failsafeLimit, this.failsafeDuration);
            case CONTROLLED:
            case AUTONOMOUS:
                return null;
        }
        throw new IllegalStateException("StateMachine is in an illegal state.");
    }

    public void addListener(StateMachineListener what) {
        listeners.add(what);
    }

}
