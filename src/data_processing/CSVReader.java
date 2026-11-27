package data_processing;

import model.Currency;
import model.TransactionRecord;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {
    private static final DateTimeFormatter TIME_IN = DateTimeFormatter.ofPattern("H:mm:ss");

    /**
     * Finds the latest transaction date in the CSV file.
     *
     * @param filePath the path of the CSV file
     * @return the latest date, or null if the file has no valid rows
     * @throws IOException if the file cannot be read
     */
    public static LocalDate findLatestDate(String filePath) throws IOException {
        LocalDate latest = null;

        for (TransactionRecord r : readAll(filePath)) {
            if (latest == null || r.getDate().isAfter(latest)) {
                latest = r.getDate();
            }
        }

        return latest;
    }

    /**
     * Finds the balance after the newest transaction of an account.
     *
     * @param filePath the path of the CSV file
     * @param accountNumber the account to look up
     * @return the last balance, or null if the account has no rows
     * @throws IOException if the file cannot be read
     */
    public static BigDecimal getLastBalance(String filePath, long accountNumber)
            throws IOException {
        BigDecimal last = null;

        for (TransactionRecord r : readAll(filePath)) {
            if (r.getAccountNumber() == accountNumber) {
                last = r.getBalance();
            }
        }

        return last;
    }

    /**
     * Finds the account number used by an exact account name.
     *
     * @param filePath the path of the CSV file
     * @param name the exact account name to look for
     * @return the account number, or null if the name is new
     * @throws IOException if the file cannot be read
     */
    public static Long findAccountNumber(String filePath, String name) throws IOException {
        for (TransactionRecord r : readAll(filePath)) {
            if (r.getAccountName().equals(name)) {
                return r.getAccountNumber();
            }
        }
        return null;
    }

    /**
     * Finds the highest account number in the CSV file, used to make the next new account number.
     *
     * @param filePath the path of the CSV file
     * @return the highest account number, or 0 if the file has no rows
     * @throws IOException if the file cannot be read
     */
    public static long getHighestAccountNum(String filePath) throws IOException {
        long highest = 0;

        for (TransactionRecord r : readAll(filePath)) {
            highest = Math.max(highest, r.getAccountNumber());
        }

        return highest;
    }

    /**
     * Reads every row of the CSV file. The header is skipped, and a row that cannot be parsed is
     * skipped with a message.
     *
     * @param filePath the path of the CSV file
     * @return the parsed rows, in file order
     * @throws IOException if the file cannot be read
     */
    public static List<TransactionRecord> readAll(String filePath) throws IOException {
        List<TransactionRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine(); // skip headers
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    records.add(parseLine(line));
                } catch (Exception e) {
                    System.err.println("Skipping bad row: " + line);
                }
            }
        }

        return records;
    }

    /**
     * Turns one line of the CSV file into a TransactionRecord. The line is split on commas, which
     * is why account names cannot have commas.
     *
     * @param line one line of the CSV file
     * @return the parsed record
     * @throws RuntimeException if a value is missing or is not a valid date, number or currency
     */
    public static TransactionRecord parseLine(String line) {
        String[] v = line.split(",");

        return new TransactionRecord(
                v[0].trim(),
                Long.parseLong(v[1].trim()),
                LocalDate.parse(v[2].trim(), CSVWriter.DATE_FMT),
                LocalTime.parse(v[3].trim(), TIME_IN),
                Currency.valueOf(v[4].trim()),
                new BigDecimal(v[5].trim()),
                v[6].trim(),
                new BigDecimal(v[7].trim()));
    }
}
