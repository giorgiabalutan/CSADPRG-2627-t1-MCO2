package data_processing;

import model.Currency;
import model.TransactionRecord;
import model.TransactionType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the CSV for report generation: parses every row, drops invalid ones, and keeps only the
 * PHP rows (REQ-0003 to REQ-0005).
 */
public final class DatasetLoader {

    /** What the loader found: the usable rows plus the counts to log. */
    public static final class Result {
        private final List<TransactionRecord> phpRecords;
        private final int rowsLoaded;
        private final int invalidRows;

        /**
         * Keeps what the loader found.
         *
         * @param phpRecords the valid PHP rows
         * @param rowsLoaded how many valid rows there were in any currency
         * @param invalidRows how many rows were skipped
         * @return None
         */
        Result(List<TransactionRecord> phpRecords, int rowsLoaded, int invalidRows) {
            this.phpRecords = phpRecords;
            this.rowsLoaded = rowsLoaded;
            this.invalidRows = invalidRows;
        }

        /**
         * Returns the valid rows that are in PHP.
         *
         * @param None
         * @return the PHP rows
         */
        public List<TransactionRecord> getPhpRecords() {
            return phpRecords;
        }

        /**
         * Returns how many rows parsed and passed validation, in any currency.
         *
         * @param None
         * @return the number of valid rows
         */
        public int getRowsLoaded() {
            return rowsLoaded;
        }

        /**
         * Returns how many rows were skipped because of a bad date, number, currency or a missing
         * value.
         *
         * @param None
         * @return the number of invalid rows
         */
        public int getInvalidRows() {
            return invalidRows;
        }
    }

    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private DatasetLoader() {}

    /**
     * Reads the CSV file, drops the invalid rows, and keeps the PHP rows for the reports.
     *
     * @param filePath the path of the CSV file
     * @return the PHP rows and the counts of valid and invalid rows
     * @throws IOException if the file cannot be read
     */
    public static Result load(String filePath) throws IOException {
        List<TransactionRecord> php = new ArrayList<>();
        int loaded = 0;
        int invalid = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    TransactionRecord r = CSVReader.parseLine(line);
                    if (!isValid(r)) {
                        invalid++;
                        continue;
                    }
                    loaded++;
                    if (r.getCurrency() == Currency.PHP) {
                        php.add(r);
                    }
                } catch (Exception e) { // bad date, number, currency code or missing column
                    invalid++;
                }
            }
        }
        return new Result(php, loaded, invalid);
    }

    /**
     * Checks that a row has a name, a known transaction type, and a positive amount.
     *
     * @param r the row to check
     * @return true if the row is usable, false if not
     */
    private static boolean isValid(TransactionRecord r) {
        boolean knownType =
                TransactionType.DEPOSIT.equals(r.getType())
                        || TransactionType.WITHDRAWAL.equals(r.getType());
        return !r.getAccountName().isEmpty() && knownType && r.getAmount().signum() > 0;
    }
}
