/********************
 * Last names: Avila, Balutan, Eborde, Ermitanio
 * Language: Java
 * Paradigm(s): OOP
 ********************/

package model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Calculates the daily interest on a balance.
 */

public final class InterestCalculator {
    private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.05");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final int SCALE = 2;
    public static final int MAX_DAYS = 365;

     
    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private InterestCalculator() {
    }

    /**
     * Returns the yearly interest rate as a decimal (0.05 means 5%).
     *
     * @param None
     * @return the annual rate
     */
    public static BigDecimal getAnnualRate() {
        return ANNUAL_RATE;
    }

    /**
     * Works out one day of interest on a balance, rounded to 2 decimal places (half up).
     *
     * @param balance the balance at the end of the day
     * @return the interest for that day
     */
    public static BigDecimal dailyInterest(BigDecimal balance) {
        BigDecimal dailyRate = ANNUAL_RATE.divide(DAYS_IN_YEAR, MathContext.DECIMAL128); // 34 significant digits
        return balance.multiply(dailyRate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Works out the interest over several days, compounded daily at full precision and rounded only
     * once at the end (2 decimal places, half up).
     *
     * @param balance the starting balance
     * @param days the number of days to compound
     * @return the total interest earned
     */
    public static BigDecimal compoundedInterest(BigDecimal balance, int days) {
        return compoundedInterestExact(balance, days).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Works out the same interest as compoundedInterest but does not round it, so a total can be
     * rounded only once.
     *
     * @param balance the starting balance
     * @param days the number of days to compound
     * @return the total interest earned, not rounded
     */
    public static BigDecimal compoundedInterestExact(BigDecimal balance, int days) {
        BigDecimal dailyRate = ANNUAL_RATE.divide(DAYS_IN_YEAR, MathContext.DECIMAL128);
        BigDecimal factor = BigDecimal.ONE.add(dailyRate).pow(days, MathContext.DECIMAL128);
        return balance.multiply(factor, MathContext.DECIMAL128).subtract(balance);
    }
}
