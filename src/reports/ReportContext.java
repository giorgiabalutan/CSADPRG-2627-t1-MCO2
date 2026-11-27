package reports;

import model.TransactionRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The cleaned dataset, grouped per account and in time order. Every report reads from this, so they
 * all agree on the same data and the same "today".
 */
public final class ReportContext {
    /* account number -> that account's transactions, oldest first */
    private final Map<Long, List<TransactionRecord>> accounts = new TreeMap<>();

    /* the latest transaction date in the file; "today" for ages, inactivity and "last month" */
    private final LocalDate referenceDate;

    /* the earliest transaction date in the file */
    private final LocalDate firstDate;

    /**
     * Groups the cleaned rows per account in time order and finds the first and latest dates.
     *
     * @param records the valid PHP rows, in any order
     * @return None
     */
    public ReportContext(List<TransactionRecord> records) {
        List<TransactionRecord> sorted = new ArrayList<>(records);
        /* List.sort is stable, so rows with the same date and time keep their file order */
        sorted.sort(
                Comparator.comparing(TransactionRecord::getDate)
                        .thenComparing(TransactionRecord::getTime));

        for (TransactionRecord r : sorted) {
            accounts.computeIfAbsent(r.getAccountNumber(), k -> new ArrayList<>()).add(r);
        }
        referenceDate = sorted.isEmpty() ? null : sorted.get(sorted.size() - 1).getDate();
        firstDate = sorted.isEmpty() ? null : sorted.get(0).getDate();
    }

    /**
     * Returns the accounts in ascending account number. Each list of transactions is oldest to
     * newest.
     *
     * @param None
     * @return account number mapped to that account's transactions
     */
    public Map<Long, List<TransactionRecord>> getAccounts() {
        return accounts;
    }

    /**
     * Returns the latest transaction date in the file, used as "today" for the reports.
     *
     * @param None
     * @return the latest date
     */
    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    /**
     * Returns the earliest transaction date in the file.
     *
     * @param None
     * @return the earliest date
     */
    public LocalDate getFirstDate() {
        return firstDate;
    }

    /**
     * Checks if there are no accounts.
     *
     * @param None
     * @return true if there are no accounts, false if not
     */
    public boolean isEmpty() {
        return accounts.isEmpty();
    }

    /**
     * Returns the account's name, taken from its first transaction.
     *
     * @param history the account's transactions, oldest first
     * @return the account name
     */
    public static String nameOf(List<TransactionRecord> history) {
        return history.get(0).getAccountName();
    }

    /**
     * Returns the account's current balance, which is the balance after its newest transaction.
     *
     * @param history the account's transactions, oldest first
     * @return the current balance
     */
    public static java.math.BigDecimal currentBalanceOf(List<TransactionRecord> history) {
        return history.get(history.size() - 1).getBalance();
    }
}
