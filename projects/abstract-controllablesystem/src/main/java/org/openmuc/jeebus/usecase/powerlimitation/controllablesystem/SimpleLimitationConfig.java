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

import org.openmuc.jeebus.spine.utils.datatypes.ScaledNumberWrapper;

public class SimpleLimitationConfig implements LimitationConfig {
    private final String failsafeDurationMin;
    private final ScaledNumberWrapper failsafeLimit;
    private final ScaledNumberWrapper loadControlLimit;
    private final ScaledNumberWrapper nominalMax;

    public SimpleLimitationConfig(LimitationConfig config) {
        this(
            config.getFailsafeDurationMin(),
            config.getFailsafeLimit(),
            config.getLoadControlLimit(),
            config.getNominalMax()
        );
    }

    public SimpleLimitationConfig(
        String failsafeDurationMin,
        ScaledNumberWrapper failsafeLimit,
        ScaledNumberWrapper loadControlLimit,
        ScaledNumberWrapper nominalMax
    ) {
        this.failsafeDurationMin = failsafeDurationMin;
        this.failsafeLimit = failsafeLimit;
        this.loadControlLimit = loadControlLimit;
        this.nominalMax = nominalMax;
    }

    @Override
    public String getFailsafeDurationMin() {
        return failsafeDurationMin;
    }

    public SimpleLimitationConfig withFailsafeDurationMin(String failsafeDurationMin) {
        return new SimpleLimitationConfig(failsafeDurationMin, failsafeLimit, loadControlLimit, nominalMax);
    }

    @Override
    public ScaledNumberWrapper getFailsafeLimit() {
        return failsafeLimit;
    }

    public SimpleLimitationConfig withFailsafeLimit(ScaledNumberWrapper failsafeLimit) {
        return new SimpleLimitationConfig(failsafeDurationMin, failsafeLimit, loadControlLimit, nominalMax);
    }

    @Override
    public ScaledNumberWrapper getLoadControlLimit() {
        return loadControlLimit;
    }

    public  SimpleLimitationConfig withLoadControlLimit(ScaledNumberWrapper loadControlLimit) {
        return new SimpleLimitationConfig(failsafeDurationMin, failsafeLimit, loadControlLimit, nominalMax);
    }

    @Override
    public ScaledNumberWrapper getNominalMax() {
        return nominalMax;
    }

    public SimpleLimitationConfig withNominalMax(ScaledNumberWrapper nominalMax) {
        return new SimpleLimitationConfig(failsafeDurationMin, failsafeLimit, loadControlLimit, nominalMax);
    }
}
