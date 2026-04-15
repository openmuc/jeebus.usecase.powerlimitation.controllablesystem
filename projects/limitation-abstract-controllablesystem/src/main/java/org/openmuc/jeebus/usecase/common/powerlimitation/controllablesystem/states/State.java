package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem.states;

/**
 * L’État, c’est moi!
 * This immutable enum only holds static logic.
 */
public enum State {
    /**
     * Controllable System starts in "init" state after completion of its (re)start;
     * CS limited by the Failsafe Consumption Active Power Limit according to
     * [LPC-901/1] and [LPC-901/2]. The Active Power Consumption Limit SHALL be
     * deactivated ([LPC-009/2]).
     */
    INIT {
        @Override
        public State handleEvent(Event trigger, boolean heartbeatReceived) {
            switch (trigger) {
                // Transition 1
                case LIMIT_DEACTIVATED:
                    return heartbeatReceived ? CONTROLLED : this;
                // Transition 2
                case ACTIVE_LIMIT_RECEIVED:
                    return heartbeatReceived ? LIMITED : this;
                // Transition 3
                case INIT_TIMEOUT:
                    return AUTONOMOUS;
                default:
                    return this;
            }
        }
    },
    /**
     * Controllable System is not limited, but still controlled by Actor Energy Guard
     * (unlike state "unlimited/autonomous").
     * The Active Power Consumption Limit SHALL be deactivated ([LPC-009/2]).
     */
    CONTROLLED {
        @Override
        public State handleEvent(Event trigger, boolean heartbeatReceived) {
            switch (trigger) {
                // Transition 4
                case ACTIVE_LIMIT_RECEIVED:
                    return heartbeatReceived ? LIMITED : this;
                // Transition 5
                case HEARTBEAT_TIMEOUT:
                    return FAILSAFE;
                default:
                    return this;
            }
        }

        @Override
        public boolean canUpdateFailsafeValues() {
            return true;
        }
    },
    /**
     * Controllable System is in a limited state (controlled by the Actor Energy Guard) where a
     * limited amount of power is consumed. The Active Power Consumption Limit SHALL be activated
     * ([LPC-009/1]).
     */
    LIMITED {
        @Override
        public State handleEvent(Event trigger, boolean heartbeatReceived) {
            switch (trigger) {
                // Transition 6
                case LIMIT_DEACTIVATED:
                    return heartbeatReceived ? CONTROLLED : this;
                // Transition 7
                case HEARTBEAT_TIMEOUT:
                    return FAILSAFE;
                default:
                    return this;
            }
        }

        @Override
        public boolean canUpdateFailsafeValues() {
            return true;
        }
    },
    /**
     * Controllable System is in "failsafe state" (not controlled by the Energy Guard) where it
     * is limited by the failsafe limit. The Active Power Consumption Limit SHALL be deactivated ([LPC-
     * 009/2]).
     */
    FAILSAFE {
        @Override
        public State handleEvent(Event trigger, boolean heartbeatReceived) {
            switch (trigger) {
                // Transition 8
                case LIMIT_DEACTIVATED:
                    return heartbeatReceived ? CONTROLLED : this;
                // Transition 9
                case ACTIVE_LIMIT_RECEIVED:
                    return heartbeatReceived ? LIMITED : this;
                // Transition 10
                case FAILSAFE_TIMEOUT:
                case HEARTBEAT_TIMEOUT:
                    return AUTONOMOUS;
                default:
                    return this;
            }
        }
    },
    /**
     * Controllable System is not limited and consumes power as if there would be no
     * external power limitation available.
     * The Active Power Consumption Limit SHALL be deactivated ([LPC-009/2])
     */
    AUTONOMOUS {
        @Override
        public State handleEvent(Event trigger, boolean heartbeatReceived) {
            switch (trigger) {
                // Transition 11
                case LIMIT_DEACTIVATED:
                    return heartbeatReceived ? CONTROLLED : this;
                // Transition 12
                case ACTIVE_LIMIT_RECEIVED:
                    return heartbeatReceived ? LIMITED : this;
                default:
                    return this;
            }
        }
    };

    abstract State handleEvent(Event trigger, boolean heartbeatReceived);

    public boolean canUpdateFailsafeValues() {
        return false;
    }
}
