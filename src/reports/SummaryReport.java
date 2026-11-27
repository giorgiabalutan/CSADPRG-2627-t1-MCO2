package reports;

import model.TransactionRecord;
import model.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** summary.json (REQ-0010): key totals across the dataset and the reports. */
public final class SummaryReport {
    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private SummaryReport() {}

    /**
     * Builds the text of summary.json: the totals, the number of members in each tier, the number
     * of Normal and Suspicious days, and the number of accounts in each activity status. Amounts
     * have exactly 2 decimals and no comma grouping.
     *
     * @param ctx the cleaned dataset
     * @return the JSON text
     */
    public static String toJson(ReportContext ctx) {
        BigDecimal deposits = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        BigDecimal balance = BigDecimal.ZERO;

        for (List<TransactionRecord> history : ctx.getAccounts().values()) {
            balance = balance.add(ReportContext.currentBalanceOf(history));
            for (TransactionRecord r : history) {
                if (TransactionType.DEPOSIT.equals(r.getType())) {
                    deposits = deposits.add(r.getAmount());
                } else {
                    withdrawals = withdrawals.add(r.getAmount());
                }
            }
        }

        /* the last column of each report holds the tier / risk flag / activity status */
        ReportTable tiers = PrivilegeTierReport.build(ctx);
        ReportTable risk = SuspiciousBehaviorReport.build(ctx);
        ReportTable activity = ActivityDormancyReport.build(ctx);

        return "{\n"
                + "  \"total_accounts\": " + ctx.getAccounts().size() + ",\n"
                + "  \"total_deposits\": " + plain(deposits) + ",\n"
                + "  \"total_withdrawals\": " + plain(withdrawals) + ",\n"
                + "  \"total_balance\": " + plain(balance) + ",\n"
                + "  \"total_interest_expense\": "
                + plain(InterestProjectionReport.totalExpectedInterest(ctx)) + ",\n"
                + "  \"total_silver_members\": " + countLast(tiers, "Silver") + ",\n"
                + "  \"total_gold_members\": " + countLast(tiers, "Gold") + ",\n"
                + "  \"total_platinum_members\": " + countLast(tiers, "Platinum") + ",\n"
                + "  \"total_normal_activity\": " + countActiveDays(risk, "Normal") + ",\n"
                + "  \"total_suspicious_activity\": " + countActiveDays(risk, "Suspicious") + ",\n"
                + "  \"total_active_accounts\": " + countLast(activity, "Active") + ",\n"
                + "  \"total_inactive_accounts\": " + countLast(activity, "Inactive") + ",\n"
                + "  \"total_dormant_accounts\": " + countLast(activity, "Dormant") + "\n"
                + "}";
    }

    /**
     * Counts the rows of a report whose last column equals a value.
     *
     * @param table the report to count in
     * @param value the value to look for
     * @return the number of matching rows
     */
    private static int countLast(ReportTable table, String value) {
        int count = 0;
        for (String[] row : table.getRows()) {
            if (row[row.length - 1].equals(value)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts the rows of Report 3 with a risk flag, only for days that had a transaction. Days with
     * no deposit and no withdrawal are left out, so quiet days do not inflate the count.
     *
     * @param table Report 3
     * @param flag the risk flag to count, Normal or Suspicious
     * @return the number of matching rows
     */
    private static int countActiveDays(ReportTable table, String flag) {
        int count = 0;
        for (String[] row : table.getRows()) {
            boolean hadTransaction = !(row[2].equals("0.00") && row[3].equals("0"));
            if (hadTransaction && row[row.length - 1].equals(flag)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Formats an amount with exactly 2 decimals and no comma grouping.
     *
     * @param value the amount to format
     * @return the amount as text
     */
    private static String plain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
