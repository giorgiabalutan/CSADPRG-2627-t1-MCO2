package data_processing;

import model.TransactionRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class CSVWriter {
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Adds one transaction to the end of the CSV file.
     *
     * @param r the transaction to save
     * @param filePath the path of the CSV file
     * @return None
     * @throws IOException if the file cannot be written
     */
    public static void append(TransactionRecord r, String filePath) throws IOException {
        String line =
                String.join(
                        ",",
                        r.getAccountName(),
                        String.valueOf(r.getAccountNumber()),
                        r.getDate().format(DATE_FMT),
                        r.getTime().format(TIME_FMT),
                        r.getCurrency().getUnit(),
                        r.getAmount().toPlainString(),
                        r.getType(),
                        r.getBalance().toPlainString());

        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
            w.write(line);
            w.newLine();
        }
    }
}
