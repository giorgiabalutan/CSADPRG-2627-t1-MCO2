/********************
 * Last names: Avila, Balutan, Eborde, Ermitanio
 * Language: Java
 * Paradigm(s): OOP
 ********************/

package views;

/** Every error message the user can see. */
public enum ErrorMessage {
    NON_POSITIVE_AMOUNT("Amount must be greater than zero."),
    AMOUNT_OUT_OF_SCALE("Amount can have at most %d decimal places."),
    AMOUNT_TOO_LARGE("Amount is too large. Enter an amount up to %s."),
    /* the two %d are the lowest and highest choice, e.g. "from 1 to 6" */
    INPUT_OUT_OF_RANGE("Choice is out of range. Enter a number from %d to %d."),
    /* "valid number" or "valid amount" */
    INVALID_INPUT_TYPE("Invalid input. Enter a valid %s."),
    INVALID_YES_NO("Invalid input. Enter Y or N."),
    EMPTY_INPUT("Input cannot be empty."),
    NAME_INVALID(
            "Invalid name. Start with a letter, then use only letters, spaces, and . ' - (no"
                    + " numbers)."),
    NAME_TOO_LONG("Account name is too long. Enter at most %d characters."),
    RATE_NOT_RECORDED("Exchange rate is not recorded yet. Record it first using option 5."),
    NAME_NOT_RECORDED("Account name is not recorded yet. Register it first using option 1."),
    ACCOUNT_BALANCE_ZERO("Account balance is zero. Deposit an amount first using option 2."),
    WITHDRAW_EXCEED_BALANCE("Insufficient balance. Enter an amount up to your current balance."),
    /* %s is the most that can still be deposited, e.g. "400000000000.00" */
    DEPOSIT_EXCEED_LIMIT(
            "Deposit would exceed the maximum account balance. Enter an amount up to %s."),
    ACCOUNT_BALANCE_FULL("Account is already at the maximum balance. Withdraw an amount first."),
    INITIAL_DEPOSIT_TOO_LOW("The initial deposit is too low. Please deposit at least PHP 5,000."),
    ACCOUNT_NOT_SAVED("Could not save the account."),
    ACCOUNT_NOT_FOUND("Account not found. Check the name or register it first using option 1."),
    ACCOUNT_NOT_LOADED("Could not read the account file."),
    DATASET_EMPTY("No valid rows were found in the data file."),
    DATASET_NOT_LOADED("The file is not loaded yet. Load it first using option 1."),
    REPORT_FAILED("Could not read or write the report files.");

    private final String message;

    /**
     * Sets the text of a message.
     *
     * @param message the text, which may have %d or %s spots
     * @return None
     */
    ErrorMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the message with the %d and %s spots filled in.
     *
     * @param args the values to put in, in order
     * @return the final text
     */
    public String getMessage(Object... args) {
        return String.format(message, args);
    }
}
