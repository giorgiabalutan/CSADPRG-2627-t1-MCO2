/********************
 * Last names: Avila, Balutan, Eborde, Ermitanio
 * Language: Java
 * Paradigm(s): OOP
 ********************/

package model;

/** The currencies the app supports. PHP is the base currency. */
public enum Currency {
    PHP("PHP", "Philippine Peso (PHP)"),
    USD("USD", "United States Dollar (USD)"),
    JPY("JPY", "Japanese Yen (JPY)"),
    GBP("GBP", "British Pound Sterling (GBP)"),
    EUR("EUR", "Euro (EUR)"),
    CNY("CNY", "Chinese Yuan Renminni (CNY)");

    /* 3-letter code, e.g. "PHP" */
    private final String unit;

    /* name shown in the menus, e.g. "Philippine Peso (PHP)" */
    private final String fullName;

    /**
     * Sets the code and the display name of a currency.
     *
     * @param unit the 3-letter code, like "PHP"
     * @param fullName the name shown in menus, like "Philippine Peso (PHP)"
     * @return None
     */
    Currency(String unit, String fullName) {
        this.unit = unit;
        this.fullName = fullName;
    }

    /**
     * Returns the 3-letter code, like "USD".
     *
     * @param None
     * @return the code of the currency
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns the name shown in menus, like "United States Dollar (USD)".
     *
     * @param None
     * @return the display name of the currency
     */
    public String getFullName() {
        return fullName;
    }
}