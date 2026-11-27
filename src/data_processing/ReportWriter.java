package data_processing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/** Writes finished report tables to CSV files. */
public final class ReportWriter {
    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private ReportWriter() {}

    /**
     * Writes the header and the rows to a CSV file. A field with a comma or a quote is wrapped in
     * quotes.
     *
     * @param filePath the path of the file to write
     * @param header the column names
     * @param rows the rows, already formatted as text
     * @return None
     * @throws IOException if the file cannot be written
     */
    public static void writeCsv(String filePath, String[] header, List<String[]> rows)
            throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath))) {
            w.write(toLine(header));
            w.newLine();
            for (String[] row : rows) {
                w.write(toLine(row));
                w.newLine();
            }
        }
    }

    /**
     * Writes plain text to a file. It is used for summary.json.
     *
     * @param filePath the path of the file to write
     * @param text the text to write
     * @return None
     * @throws IOException if the file cannot be written
     */
    public static void writeText(String filePath, String text) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath))) {
            w.write(text);
            w.newLine();
        }
    }

    /**
     * Joins the fields of one row into a CSV line.
     *
     * @param fields the fields of the row
     * @return the CSV line
     */
    private static String toLine(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    /**
     * Wraps a field in quotes if it has a comma, a quote or a new line. A quote inside is doubled.
     *
     * @param field the field to check
     * @return the safe field
     */
    private static String escape(String field) {
        if (field.indexOf(',') >= 0 || field.indexOf('"') >= 0 || field.indexOf('\n') >= 0) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
