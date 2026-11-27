package reports;

import model.InterestCalculator;
import model.TransactionRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Report 2 (REQ-0007): Interest Expense Projection for the next 30 days. */
public final class InterestProjectionReport {
    private static final BigDecimal MIN_BALANCE = new BigDecimal("10000.00");
    private static final int DAYS = 30;

    private static final String[] HEADER = {"AccountName", "CurrentBalance", "ExpectedInterest"};

    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private InterestProjectionReport() {}

    private static final class Row {
        final String name;
        final long number;
        final BigDecimal balance;
        final BigDecimal interest;
        final BigDecimal exact; // unrounded, for the total

        /**
         * Makes one row of the report: the projected interest of one account.
         *
         * @param name the account name
         * @param number the account number
         * @param balance the current balance
         * @param interest the expected interest, rounded to 2 decimal places
         * @param exact the expected interest, not rounded, used for the total
         * @return None
         */
        Row(String name, long number, BigDecimal balance, BigDecimal interest, BigDecimal exact) {
            this.name = name;
            this.number = number;
            this.balance = balance;
            this.interest = interest;
            this.exact = exact;
        }
    }

    /**
     * Builds Report 2. It has one row for each account with a balance of at least the minimum, with
     * the highest interest first.
     *
     * @param ctx the cleaned dataset
     * @return the finished report
     */
    public static ReportTable build(ReportContext ctx) {
        List<String[]> out = new ArrayList<>();
        for (Row r : compute(ctx)) {
            out.add(
                    new String[] {
                        r.name, ReportFormat.money(r.balance), ReportFormat.money(r.interest)
                    });
        }
        return new ReportTable(
                "Interest Expense Projection",
                "report2_interest_expense_projection.csv",
                HEADER,
                out);
    }

    /**
     * Adds up the expected interest of every account in the report, rounding only once at the end.
     * It is used for summary.json.
     *
     * @param ctx the cleaned dataset
     * @return the total expected interest
     */
    public static BigDecimal totalExpectedInterest(ReportContext ctx) {
        BigDecimal total = BigDecimal.ZERO;
        for (Row r : compute(ctx)) {
            total = total.add(r.exact);
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Works out the expected interest of every account that qualifies, sorted with the highest
     * interest first.
     *
     * @param ctx the cleaned dataset
     * @return one row per account
     */
    private static List<Row> compute(ReportContext ctx) {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Long, List<TransactionRecord>> entry : ctx.getAccounts().entrySet()) {
            List<TransactionRecord> history = entry.getValue();
            BigDecimal balance = ReportContext.currentBalanceOf(history);

            if (balance.compareTo(MIN_BALANCE) < 0) {
                continue;
            }
            rows.add(
                    new Row(
                            ReportContext.nameOf(history),
                            entry.getKey(),
                            balance,
                            expectedInterest(balance),
                            InterestCalculator.compoundedInterestExact(balance, DAYS)));
        }
        rows.sort(
                Comparator.comparing((Row r) -> r.interest)
                        .reversed()
                        .thenComparing(r -> r.name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(r -> r.number));
        return rows;
    }

    /**
     * Works out the interest over 30 days, compounded daily at full precision and rounded once at
     * the end.
     *
     * @param startingBalance the current balance
     * @return the interest for 30 days
     */
    static BigDecimal expectedInterest(BigDecimal startingBalance) {
        return InterestCalculator.compoundedInterest(startingBalance, DAYS);
    }
}
