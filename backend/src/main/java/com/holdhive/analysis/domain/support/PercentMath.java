package com.holdhive.analysis.domain.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared scale/rounding constants and percentage math used across the L0-L4
 * calculators. Extracted to avoid repeating an identical {@code percentOf}
 * private method in every calculator.
 */
public final class PercentMath {

    public static final int MONEY_SCALE = 2;
    public static final int PERCENT_SCALE = 2;
    public static final int HHI_SCALE = 4;
    public static final RoundingMode RM = RoundingMode.HALF_UP;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private PercentMath() {
    }

    /** {@code part / total * 100}, scaled to {@link #PERCENT_SCALE}; zero if total is zero/null. */
    public static BigDecimal percentOf(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE, RM);
        }
        return part.multiply(ONE_HUNDRED).divide(total, PERCENT_SCALE, RM);
    }

    /** Shorthand for {@code value.setScale(MONEY_SCALE, RM)}. */
    public static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RM);
    }
}
