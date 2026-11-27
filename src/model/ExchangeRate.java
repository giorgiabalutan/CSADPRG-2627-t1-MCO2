/********************
 * Last names: Avila, Balutan, Eborde, Ermitanio
 * Language: Java
 * Paradigm(s): OOP
 ********************/

package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Keeps the exchange rate of each currencies and converts them if needed.
 */
public class ExchangeRate {
    private static final int RESULT_SCALE = 2;

    private final Map<Currency, BigDecimal> rates = new EnumMap<>(Currency.class);

    /**
     * Makes a rate table where only PHP is set, with a rate of 1.
     *
     * @param None
     * @return None
     */
    public ExchangeRate() {
        rates.put(Currency.PHP, BigDecimal.ONE); // default; can be overwritten
    }

    /**
     * Saves the rate of a currency. If it already had a rate, the new one replaces it.
     *
     * @param currency the currency to set
     * @param rate PHP per 1 unit of the currency
     * @return None
     */
    public void setRate(Currency currency, BigDecimal rate) {
        rates.put(currency, rate);
    }

    /**
     * Checks if a rate was recorded for the currency.
     *
     * @param currency the currency to check
     * @return true if it has a rate, false if not
     */
    public boolean hasRate(Currency currency) {
        return rates.containsKey(currency);
    }

    /**
     * Returns the rate of a currency.
     *
     * @param currency the currency to look up
     * @return the rate, or null if none was recorded
     */
    public BigDecimal getRate(Currency currency) {
        return rates.get(currency);
    }

    /**
     * Converts an amount from one currency to another. The result is rounded to 2 decimal places
     * (half up).
     *
     * @param from the currency the amount is in
     * @param to the currency to change it to
     * @param amount the amount to convert
     * @return the converted amount
     * @throws IllegalArgumentException if either currency has no rate
     */
    public BigDecimal convert(Currency from, Currency to, BigDecimal amount) {
        if (!hasRate(from) || !hasRate(to)) {
            throw new IllegalArgumentException("Exchange rate not set for one of the currencies.");
        }
        return amount.multiply(getRate(from))
                .divide(getRate(to), RESULT_SCALE, RoundingMode.HALF_UP);
    }
}