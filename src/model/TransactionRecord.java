package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class TransactionRecord {

    private final String accountName;
    private final long accountNumber;
    private final LocalDate date;
    private final LocalTime time;
    private final Currency currency;
    private final BigDecimal amount;
    private final String type;
    private final BigDecimal balance;

    /**
     * Makes one row of the transaction file.
     *
     * @param accountName the name of the account
     * @param accountNumber the account number
     * @param date the date of the transaction
     * @param time the time of the transaction
     * @param currency the currency of the amount
     * @param amount the amount that was deposited or withdrawn
     * @param type the transaction type, "deposit" or "withdrawal"
     * @param balance the account balance after the transaction
     * @return None
     */
    public TransactionRecord(
            String accountName,
            long accountNumber,
            LocalDate date,
            LocalTime time,
            Currency currency,
            BigDecimal amount,
            String type,
            BigDecimal balance) {

        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.date = date;
        this.time = time;
        this.currency = currency;
        this.amount = amount;
        this.type = type;
        this.balance = balance;
    }

    /**
     * Returns the name of the account.
     *
     * @param None
     * @return the account name
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Returns the account number.
     *
     * @param None
     * @return the account number
     */
    public long getAccountNumber() {
        return accountNumber;
    }

    /**
     * Returns the date of the transaction.
     *
     * @param None
     * @return the date
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Returns the time of the transaction.
     *
     * @param None
     * @return the time
     */
    public LocalTime getTime() {
        return time;
    }

    /**
     * Returns the currency of the amount.
     *
     * @param None
     * @return the currency
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Returns the amount that was deposited or withdrawn.
     *
     * @param None
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Returns the transaction type.
     *
     * @param None
     * @return "deposit" or "withdrawal"
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the account balance after the transaction.
     *
     * @param None
     * @return the balance
     */
    public BigDecimal getBalance() {
        return balance;
    }
}
