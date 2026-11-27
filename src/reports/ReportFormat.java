package reports;

import data_processing.CSVWriter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.math.RoundingMode;

/** Number and date formatting shared by every report, so all CSVs look the same (REQ-0012). */
public final class ReportFormat {
    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private ReportFormat() {}

    /**
     * Formats an amount with 2 decimal places, rounded half up, and no comma grouping. For example,
     * 1234567.5 becomes "1234567.50".
     *
     * @param value the amount to format
     * @return the amount as text
     */
    public static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Formats a whole number with no comma grouping.
     *
     * @param value the number to format
     * @return the number as text
     */
    public static String count(long value) {
        return String.valueOf(value);
    }

    /**
     * Formats a date as dd/MM/yyyy, the same as the transaction file.
     *
     * @param date the date to format
     * @return the date as text
     */
    public static String date(LocalDate date) {
        return date.format(CSVWriter.DATE_FMT);
    }
}
