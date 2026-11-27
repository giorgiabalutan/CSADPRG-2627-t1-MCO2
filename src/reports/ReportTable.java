package reports;

import java.util.List;

/** A finished report: its title, output file name, column names and rows (already formatted). */
public final class ReportTable {
    private final String title;
    private final String fileName;
    private final String[] header;
    private final List<String[]> rows;

    /**
     * Makes a finished report with its title, file name, column names and rows.
     *
     * @param title the title of the report
     * @param fileName the name of the CSV file to save it to
     * @param header the column names
     * @param rows the rows, already formatted as text
     * @return None
     */
    public ReportTable(String title, String fileName, String[] header, List<String[]> rows) {
        this.title = title;
        this.fileName = fileName;
        this.header = header;
        this.rows = rows;
    }

    /**
     * Returns the title of the report.
     *
     * @param None
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the name of the CSV file the report is saved to.
     *
     * @param None
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the column names.
     *
     * @param None
     * @return the column names
     */
    public String[] getHeader() {
        return header;
    }

    /**
     * Returns the rows of the report.
     *
     * @param None
     * @return the rows, already formatted as text
     */
    public List<String[]> getRows() {
        return rows;
    }
}
