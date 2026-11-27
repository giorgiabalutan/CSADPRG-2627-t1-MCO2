package reports;

import model.TransactionRecord;
import model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Report 4 (REQ-0009): Account Activity and Dormancy. Shows how much each account moves money and
 * how long it has been since its last transaction, so inactive accounts can be followed up.
 */
public final class ActivityDormancyReport {
    /* up to this many days without a transaction is Active, up to the next one is Inactive */
    private static final int ACTIVE_MAX_DAYS = 30;
    private static final int INACTIVE_MAX_DAYS = 90;

    private static final String[] HEADER = {
        "AccountName",
        "TotalTransactions",
        "TotalDeposits",
        "TotalWithdrawals",
        "NetCashFlow",
        "LastTransactionDate",
        "DaysInactive",
        "ActivityStatus"
    };

    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private ActivityDormancyReport() {}

    private static final class Row {
        final String name;
        final long number;
        final int transactions;
        final BigDecimal deposits;
        final BigDecimal withdrawals;
        final LocalDate lastDate;
        final long daysInactive;

        /**
         * Makes one row of the report: the totals of one account.
         *
         * @param name the account name
         * @param number the account number
         * @param transactions how many transactions the account has
         * @param deposits the total of its deposits
         * @param withdrawals the total of its withdrawals
         * @param lastDate the date of its last transaction
         * @param daysInactive the days from the last transaction to the reference date
         * @return None
         */
        Row(
                String name,
                long number,
                int transactions,
                BigDecimal deposits,
                BigDecimal withdrawals,
                LocalDate lastDate,
                long daysInactive) {
            this.name = name;
            this.number = number;
            this.transactions = transactions;
            this.deposits = deposits;
            this.withdrawals = withdrawals;
            this.lastDate = lastDate;
            this.daysInactive = daysInactive;
        }
    }

    /**
     * Builds Report 4. It has one row per account, with the most inactive account first.
     *
     * @param ctx the cleaned dataset
     * @return the finished report
     */
    public static ReportTable build(ReportContext ctx) {
        List<Row> rows = new ArrayList<>();

        for (Map.Entry<Long, List<TransactionRecord>> entry : ctx.getAccounts().entrySet()) {
            List<TransactionRecord> history = entry.getValue();

            BigDecimal deposits = BigDecimal.ZERO;
            BigDecimal withdrawals = BigDecimal.ZERO;
            for (TransactionRecord r : history) {
                if (TransactionType.DEPOSIT.equals(r.getType())) {
                    deposits = deposits.add(r.getAmount());
                } else {
                    withdrawals = withdrawals.add(r.getAmount());
                }
            }

            LocalDate last = history.get(history.size() - 1).getDate();
            rows.add(
                    new Row(
                            ReportContext.nameOf(history),
                            entry.getKey(),
                            history.size(),
                            deposits,
                            withdrawals,
                            last,
                            ChronoUnit.DAYS.between(last, ctx.getReferenceDate())));
        }

        /* most inactive first, then account number to break ties */
        rows.sort(
                Comparator.comparingLong((Row r) -> r.daysInactive)
                        .reversed()
                        .thenComparing(r -> r.name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(r -> r.number));

        List<String[]> out = new ArrayList<>();
        for (Row r : rows) {
            out.add(
                    new String[] {
                        r.name,
                        ReportFormat.count(r.transactions),
                        ReportFormat.money(r.deposits),
                        ReportFormat.money(r.withdrawals),
                        ReportFormat.money(r.deposits.subtract(r.withdrawals)),
                        ReportFormat.date(r.lastDate),
                        ReportFormat.count(r.daysInactive),
                        statusOf(r.daysInactive)
                    });
        }
        return new ReportTable(
                "Account Activity and Dormancy",
                "report4_account_activity_dormancy.csv",
                HEADER,
                out);
    }

    /**
     * Gives the activity status for a number of inactive days: Active, Inactive or Dormant.
     *
     * @param daysInactive the days since the last transaction
     * @return the status
     */
    private static String statusOf(long daysInactive) {
        if (daysInactive <= ACTIVE_MAX_DAYS) {
            return "Active";
        }
        if (daysInactive <= INACTIVE_MAX_DAYS) {
            return "Inactive";
        }
        return "Dormant";
    }
}
