/********************
 * Last names: Avila, Balutan, Eborde, Ermitanio
 * Language: Java
 * Paradigm(s): OOP
 ********************/

package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A bank account. It keeps the owner's name and a balance. The balance has 2 decimal places and
 * stays between 0 and MAX_BALANCE.
 */
public class Account {
    /* money has 2 decimal places */
    public static final int SCALE = 2;

    /* the balance can never go above this, no matter how many deposits are made */
    public static final BigDecimal MAX_BALANCE = new BigDecimal("1000000000000");

    private final String name;
    private final long accountNumber;
    private BigDecimal balance;

    /**
     * Makes an account with a name, an account number and a starting balance.
     *
     * @param name the owner's name
     * @param accountNum the account number
     * @param startingBalance the balance to start with; at most 2 decimal places
     * @return None
     */
    public Account(String name, long accountNum, BigDecimal startingBalance) {
        this.name = name;
        this.accountNumber = accountNum;
        this.balance = startingBalance.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * Returns the account's name.
     *
     * @param None
     * @return the name of the account
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the current balance.
     *
     * @param None
     * @return the balance
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Returns how much more can be deposited before the balance reaches MAX_BALANCE.
     *
     * @param None
     * @return the amount that can still be deposited
     */
    public BigDecimal getRemainingCapacity() {
        return MAX_BALANCE.subtract(balance);
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
     * Stops with an error if the amount is not positive or has more than 2 decimal places.
     *
     * @param amount the amount to check
     * @param action the word used in the error message, like "Deposit"
     * @return None
     * @throws IllegalArgumentException if the amount is not valid
     */
    private static void requireValidAmount(BigDecimal amount, String action) {
        if (amount.signum() <= 0) { // zero or negative
            throw new IllegalArgumentException(action + " must be positive.");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new IllegalArgumentException(
                    action + " can have at most " + SCALE + " decimal places.");
        }
    }

    /**
     * Adds an amount to the balance.
     *
     * @param amount the amount to add; positive, at most 2 decimal places
     * @return None
     * @throws IllegalArgumentException if the amount is not valid, or the new balance would be more
     *     than MAX_BALANCE
     */
    public void deposit(BigDecimal amount) {
        requireValidAmount(amount, "Deposit");
        if (amount.compareTo(getRemainingCapacity()) > 0) {
            throw new IllegalArgumentException("Deposit would exceed the maximum balance.");
        }
        balance = balance.add(amount).setScale(SCALE); // exact: amount has at most SCALE decimals
    }

    /**
     * Takes an amount out of the balance.
     *
     * @param amount the amount to take out; positive, at most 2 decimal places
     * @return None
     * @throws IllegalArgumentException if the amount is not valid, or is more than the balance
     */
    public void withdraw(BigDecimal amount) {
        requireValidAmount(amount, "Withdraw");
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
        balance = balance.subtract(amount).setScale(SCALE);
    }
}
