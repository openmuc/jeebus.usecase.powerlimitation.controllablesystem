package org.openmuc.jeebus.usecase.common.powerlimitation.controllablesystem;

import org.openmuc.jeebus.spine.utils.datatypes.ScaledNumberWrapper;

/**
 * Data source for the configuration values that need to be known at device startup.
 * <p>
 * Users are encouraged to create their own implementations in order to e.g. work
 * with a configuration framework. There is also a simple data class implementation
 * available in {@link SimpleLimitationConfig}.
 * <p>
 * The class linking the limitation use cases to our own config system lives in
 * another module.
 */
public interface LimitationConfig {
    String getFailsafeDurationMin();

    ScaledNumberWrapper getFailsafeLimit();

    ScaledNumberWrapper getLoadControlLimit();

    ScaledNumberWrapper getNominalMax();

    /**
     * Our default value for failsafe duration: two hours (minimum allowed by the spec).
     */
    String DEFAULT_FAILSAFE_DURATION_MIN = "PT2H";
    /**
     * Our default value for power limits: 12 MW
     */
    ScaledNumberWrapper DEFAULT_BIG_POWER = new ScaledNumberWrapper(12,6);
}
