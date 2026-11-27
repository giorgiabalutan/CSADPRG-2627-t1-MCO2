package reports;

import model.TransactionRecord;
import model.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Report 3 (REQ-0008): Suspicious Client Behavior, one row per account per day. */
public final class SuspiciousBehaviorReport {
    private static final BigDecimal WITHDRAWAL_LIMIT = new BigDecimal("500000.00");
    private static final BigDecimal VELOCITY_LIMIT = new BigDecimal("90.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final String SUSPICIOUS = "Suspicious";
    private static final String NORMAL = "Normal";

    private static final String[] HEADER = {
        "AccountName",
        "Date",
        "TotalWithdrawalAmounts",
        "NumberOfDeposits",
        "TotalDepositAmounts",
        "TurnoverVelocity",
        "RiskFlagLevel"
    };

    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private SuspiciousBehaviorReport() {}

    /* one account on one day */
    private static final class Row {
        final String name;
        final long number;
        final LocalDate date;
        BigDecimal withdrawals = BigDecimal.ZERO;
        BigDecimal deposits = BigDecimal.ZERO;
        int depositCount = 0;
        BigDecimal velocity = BigDecimal.ZERO;
        boolean suspicious = false;

        /**
         * Makes one row of the report: one account on one day.
         *
         * @param name the account name
         * @param number the account number
         * @param date the day of the row
         * @return None
         */
        Row(String name, long number, LocalDate date) {
            this.name = name;
            this.number = number;
            this.date = date;
        }
    }

    /**
     * Builds Report 3. It has one row for every account on every day of the dataset, sorted by
     * date, with Suspicious days first.
     *
     * @param ctx the cleaned dataset
     * @return the finished report
     */
    public static ReportTable build(ReportContext ctx) {
        List<Row> rows = new ArrayList<>();

        for (Map.Entry<Long, List<TransactionRecord>> entry : ctx.getAccounts().entrySet()) {
            List<TransactionRecord> history = entry.getValue();
            String name = ReportContext.nameOf(history);

            Map<LocalDate, Row> perDay = new LinkedHashMap<>();
            for (TransactionRecord r : history) {
                Row row =
                        perDay.computeIfAbsent(
                                r.getDate(), d -> new Row(name, entry.getKey(), d));
                if (TransactionType.DEPOSIT.equals(r.getType())) {
                    row.deposits = row.deposits.add(r.getAmount());
                    row.depositCount++;
                } else {
                    row.withdrawals = row.withdrawals.add(r.getAmount());
                }
            }

            /* every day of the dataset for every account; days with no transaction are Normal */
            for (LocalDate day = ctx.getFirstDate();
                    !day.isAfter(ctx.getReferenceDate());
                    day = day.plusDays(1)) {
                Row row = perDay.getOrDefault(day, new Row(name, entry.getKey(), day));
                flag(row);
                rows.add(row);
            }
        }

        /* date ascending, Suspicious before Normal, then name, then account number */
        rows.sort(
                Comparator.comparing((Row r) -> r.date)
                        .thenComparing(r -> r.suspicious ? 0 : 1)
                        .thenComparing(r -> r.name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(r -> r.number));

        List<String[]> out = new ArrayList<>();
        for (Row r : rows) {
            out.add(
                    new String[] {
                        r.name,
                        ReportFormat.date(r.date),
                        ReportFormat.money(r.withdrawals),
                        ReportFormat.count(r.depositCount),
                        ReportFormat.money(r.deposits),
                        r.deposits.signum() > 0 ? ReportFormat.money(r.velocity) : "N/A",
                        r.suspicious ? SUSPICIOUS : NORMAL
                    });
        }
        return new ReportTable(
                "Suspicious Client Behavior",
                "report3_suspicious_client_behavior.csv",
                HEADER,
                out);
    }

    /**
     * Works out the turnover velocity (total withdrawal / total deposit x 100, rounded to 2
     * decimals) and the risk flag of a row. With no deposit that day, the velocity cannot be
     * computed, so only the withdrawal limit applies.
     *
     * @param row the row to fill in
     * @return None
     */
    private static void flag(Row row) {
        if (row.deposits.signum() > 0) {
            row.velocity =
                    row.withdrawals
                            .multiply(HUNDRED)
                            .divide(row.deposits, 2, RoundingMode.HALF_UP);
        }
        row.suspicious =
                row.withdrawals.compareTo(WITHDRAWAL_LIMIT) >= 0
                        || row.velocity.compareTo(VELOCITY_LIMIT) >= 0;
    }
}
