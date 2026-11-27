package model;

/** The two transaction types as they are spelled in bank_transactions.csv. */
public final class TransactionType {
    public static final String DEPOSIT = "deposit";
    public static final String WITHDRAWAL = "withdrawal";

    /**
     * Stops anyone from making an object of this class, since it only holds constants.
     *
     * @param None
     * @return None
     */
    private TransactionType() {}
}
