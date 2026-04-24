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

import org.openmuc.jeebus.spine.api.DataValidationException;
import org.openmuc.jeebus.spine.spi.function.DataUpdateTransaction;
import org.openmuc.jeebus.spine.utils.features.loadcontrol.LoadControlFeature;
import org.openmuc.jeebus.spine.xsd.v1.*;

import java.util.Objects;

public class LoadControlLimit {
    private static Long limitIdCount = 0L;
    private final LoadControlFeature loadControlFeature;
    private Integer dataIdx;
    private Integer descriptionIdx;
    private final Long limitId = limitIdCount++;

    public Long getLimitId() {
        return limitId;
    }

    public LoadControlLimitDescriptionDataType getDescriptionCopy() {
        return loadControlFeature
            .getLimitDescriptionFunction()
            .getDataCopyForFirst(description -> Objects.equals(
                description.getLimitId(),
                limitId
            ));
    }

    public LoadControlLimitDataType getDataCopy() {
        return loadControlFeature
            .getLimitListDataFunction()
            .getDataCopyForFirst(data -> Objects.equals(data.getLimitId(), limitId));
    }

    public boolean isActive() {
        return getDataCopy().getIsLimitActive();
    }

    public LoadControlLimit(
        LoadControlFeature loadControlFeature,
        ScaledNumberType scaledNumber,
        EnergyDirectionEnumType direction
    ) throws DataValidationException {

        this.loadControlFeature = loadControlFeature;

        DataUpdateTransaction dataUpdateTransaction = new DataUpdateTransaction();
        dataUpdateTransaction.addSingleData(
            loadControlFeature.getLimitListDataFunction(),
            getData(scaledNumber),
            idx -> dataIdx = idx
        );
        dataUpdateTransaction.addSingleData(
            loadControlFeature.getLimitDescriptionFunction(),
            getDescription(direction),
            idx -> descriptionIdx = idx
        );

        dataUpdateTransaction.runUpdate();
    }

    private LoadControlLimitDataType getData(ScaledNumberType scaledNumber) {
        LoadControlLimitDataType data = new LoadControlLimitDataType();
        data.setLimitId(limitId);
        data.setValue(scaledNumber);
        data.setIsLimitChangeable(true);
        data.setIsLimitActive(false);
        return data;
    }

    private LoadControlLimitDescriptionDataType getDescription(
        EnergyDirectionEnumType direction
    ) {
        LoadControlLimitDescriptionDataType description
            = new LoadControlLimitDescriptionDataType();
        description.setLimitType(LoadControlLimitTypeEnumType.SIGN_DEPENDENT_ABS_VALUE_LIMIT.value());
        description.setLimitCategory(LoadControlCategoryEnumType.OBLIGATION.value());
        description.setLimitId(limitId);
        if (direction != null) {
            description.setLimitDirection(direction.value());
        }
        description.setUnit("W");
        description.setScopeType(ScopeTypeEnumType.ACTIVE_POWER_LIMIT.value());
        /* FIXME: Hardcoding a bogus measurementId because some implementations need
         *  this field to be set. We should hook it up to the actual ID eventually */
        description.setMeasurementId(0L);
        return description;
    }

    public void setData(LoadControlLimitDataType data) throws
        DataValidationException {
        loadControlFeature.getLimitListDataFunction().updateData(dataIdx, data);
    }
}
