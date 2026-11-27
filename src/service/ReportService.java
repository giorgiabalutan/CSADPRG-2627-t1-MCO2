package service;

import data_processing.DatasetLoader;
import data_processing.ReportWriter;

import reports.ActivityDormancyReport;
import reports.InterestProjectionReport;
import reports.PrivilegeTierReport;
import reports.ReportContext;
import reports.ReportFormat;
import reports.ReportTable;
import reports.SummaryReport;
import reports.SuspiciousBehaviorReport;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Loads the transaction file and generates the reports and summary.json. */
public class ReportService {
    private static final int PREVIEW_ROWS = 2;

    private final String csvPath;
    private final String outputDir;
    private ReportContext context; // nulll until the file is loaded

    /**
     * Makes a service that loads the transaction file and builds the reports.
     *
     * @param csvPath the path of the transaction CSV file
     * @param outputDir the folder where the reports are saved
     * @return None
     */
    public ReportService(String csvPath, String outputDir) {
        this.csvPath = csvPath;
        this.outputDir = outputDir;
    }

    /**
     * Checks if the dataset was loaded and has usable rows.
     *
     * @param None
     * @return true if it is loaded, false if not
     */
    public boolean isLoaded() {
        return context != null;
    }

    /**
     * Reads the file, drops the invalid rows, keeps the PHP rows, and prints the counts.
     *
     * @param None
     * @return true if the dataset has usable rows
     * @throws IOException if the file cannot be read
     */
    public boolean loadDataset() throws IOException {
        DatasetLoader.Result result = DatasetLoader.load(csvPath);

        System.out.println(
                "\nProcessing dataset... ("
                        + ReportFormat.count(result.getRowsLoaded())
                        + " rows loaded, "
                        + ReportFormat.count(result.getPhpRecords().size())
                        + " filtered for PHP currency)");
        if (result.getInvalidRows() > 0) {
            System.out.println(
                    ReportFormat.count(result.getInvalidRows()) + " invalid row(s) were skipped.");
        }
        System.out.println();

        ReportContext loaded = new ReportContext(result.getPhpRecords());
        context = loaded.isEmpty() ? null : loaded;
        return context != null;
    }

    /**
     * Builds every report, saves each to its CSV, saves summary.json, and prints a preview.
     *
     * @param None
     * @return None
     * @throws IOException if a file cannot be written
     * @throws IllegalStateException if the dataset was not loaded
     */
    public void generateReports() throws IOException {
        if (!isLoaded()) {
            throw new IllegalStateException("Dataset is not loaded.");
        }
        new File(outputDir).mkdirs();

        System.out.println("\nGenerating reports...");
        System.out.println("Outputs saved to individual files...\n");

        ReportTable[] tables = {
            PrivilegeTierReport.build(context),
            InterestProjectionReport.build(context),
            SuspiciousBehaviorReport.build(context),
            ActivityDormancyReport.build(context)
        };

        for (int i = 0; i < tables.length; i++) {
            ReportTable table = tables[i];
            String path = new File(outputDir, table.getFileName()).getPath();
            ReportWriter.writeCsv(path, table.getHeader(), table.getRows());

            System.out.println("Report " + (i + 1) + ": " + table.getTitle());
            printPreview(table);
            System.out.println("(Full table exported to " + path + ")\n");
        }

        String json = SummaryReport.toJson(context);
        ReportWriter.writeText(new File(outputDir, "summary.json").getPath(), json);
        System.out.println("Summary State (summary.json):");
        System.out.println(json);
    }

    /**
     * Prints the header and the first few rows of a report.
     *
     * @param table the report to preview
     * @return None
     */
    private static void printPreview(ReportTable table) {
        System.out.println(String.join(" | ", table.getHeader()));
        List<String[]> rows = table.getRows();
        for (int i = 0; i < Math.min(PREVIEW_ROWS, rows.size()); i++) {
            System.out.println(String.join(" | ", rows.get(i)));
        }
        if (rows.isEmpty()) {
            System.out.println("(no rows)");
        }
    }
}
