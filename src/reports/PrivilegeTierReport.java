package reports;

import model.TransactionRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Report 1 (REQ-0006): Banking Privilege Tiers. */
public final class PrivilegeTierReport {
    private static final BigDecimal MIN_AVERAGE = new BigDecimal("100000.00");
    private static final BigDecimal GOLD_FROM = new BigDecimal("500000.00");
    private static final BigDecimal PLATINUM_FROM = new BigDecimal("1000000.00");

    private static final String[] HEADER = {
        "AccountName", "AccountAge", "CurrentBalance", "AverageDailyBalance", "PrivilegeTier"
    };

    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private PrivilegeTierReport() {}

    private static final class Row {
        final String name;
        final long number;
        final long age;
        final BigDecimal balance;
        final BigDecimal average;

        /**
         * Makes one row of the report: one account and its tier figures.
         *
         * @param name the account name
         * @param number the account number
         * @param age the days since the account's first transaction
         * @param balance the current balance
         * @param average the average daily balance of last month
         * @return None
         */
        Row(String name, long number, long age, BigDecimal balance, BigDecimal average) {
            this.name = name;
            this.number = number;
            this.age = age;
            this.balance = balance;
            this.average = average;
        }
    }

    /**
     * Builds Report 1. It has one row for each account whose average daily balance last month
     * reaches the minimum, with the highest average first.
     *
     * @param ctx the cleaned dataset
     * @return the finished report
     */
    public static ReportTable build(ReportContext ctx) {
        LocalDate today = ctx.getReferenceDate();

        /* "last month" = the full calendar month before the month of the latest transaction */
        YearMonth month = YearMonth.from(today).minusMonths(1);

        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Long, List<TransactionRecord>> entry : ctx.getAccounts().entrySet()) {
            List<TransactionRecord> history = entry.getValue();
            BigDecimal average = averageDailyBalance(history, month);

            if (average.compareTo(MIN_AVERAGE) < 0) {
                continue;
            }
            long age = ChronoUnit.DAYS.between(history.get(0).getDate(), today);
            rows.add(
                    new Row(
                            ReportContext.nameOf(history),
                            entry.getKey(),
                            age,
                            ReportContext.currentBalanceOf(history),
                            average));
        }

        rows.sort(
                Comparator.comparing((Row r) -> r.average)
                        .reversed()
                        .thenComparing(r -> r.name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(r -> r.number));

        List<String[]> out = new ArrayList<>();
        for (Row r : rows) {
            out.add(
                    new String[] {
                        r.name,
                        ReportFormat.count(r.age),
                        ReportFormat.money(r.balance),
                        ReportFormat.money(r.average),
                        tierOf(r.average)
                    });
        }
        return new ReportTable(
                "Banking Privilege Tiers", "report1_banking_privilege_tiers.csv", HEADER, out);
    }

    /**
     * Works out the average of the end-of-day balances for every day of a month. A day with no
     * transaction keeps the previous day's balance, and days before the account existed count as 0.
     * The result is rounded to 2 decimal places (half up).
     *
     * @param history the account's transactions, oldest first
     * @param month the month to average
     * @return the average daily balance
     */
    static BigDecimal averageDailyBalance(List<TransactionRecord> history, YearMonth month) {
        /* date -> balance after that day's last transaction (history is in time order) */
        TreeMap<LocalDate, BigDecimal> endOfDay = new TreeMap<>();
        for (TransactionRecord r : history) {
            endOfDay.put(r.getDate(), r.getBalance());
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (LocalDate day = month.atDay(1); !day.isAfter(month.atEndOfMonth()); day = day.plusDays(1)) {
            Map.Entry<LocalDate, BigDecimal> latest = endOfDay.floorEntry(day);
            if (latest != null) {
                sum = sum.add(latest.getValue());
            }
        }
        return sum.divide(BigDecimal.valueOf(month.lengthOfMonth()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Gives the privilege tier for an average daily balance: Silver, Gold or Platinum.
     *
     * @param average the average daily balance
     * @return the tier
     */
    private static String tierOf(BigDecimal average) {
        if (average.compareTo(PLATINUM_FROM) >= 0) {
            return "Platinum";
        }
        if (average.compareTo(GOLD_FROM) >= 0) {
            return "Gold";
        }
        return "Silver";
    }
}
