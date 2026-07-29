package com.holdhive.analysis.domain.support;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * Herfindahl-Hirschman Index: sum of squared weights (each bucket value
 * divided by the total), scaled to {@link PercentMath#HHI_SCALE}. Shared by
 * {@code ConcentrationCalculator}, {@code LookThroughCalculator} and
 * {@code SectorExposureCalculator}, which otherwise each computed this same
 * summation independently.
 */
public final class Hhi {

    private Hhi() {
    }

    public static BigDecimal of(List<BigDecimal> bucketValues, BigDecimal total) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : bucketValues) {
            BigDecimal weight = value.divide(total, MathContext.DECIMAL64);
            sum = sum.add(weight.multiply(weight));
        }
        return sum.setScale(PercentMath.HHI_SCALE, PercentMath.RM);
    }
}
