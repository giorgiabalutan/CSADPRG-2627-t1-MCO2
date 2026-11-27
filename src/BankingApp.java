/***********
 * Last names: Avila, Balutan, Eborde, Ermitanio
 * Language: Java
 * Paradigm(s): OOP
 ********************/

import model.Account;
import model.Currency;
import model.ExchangeRate;
import model.InterestCalculator;

import service.AccountService;
import service.ReportService;

import views.ConsoleIO;
import views.ErrorMessage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Scanner;

/**
 * The main program. It shows the menu and runs each transaction: register a name, deposit,
 * withdraw, exchange currency, record exchange rates, and show interest.
 */
public class BankingApp {

    private static final int RATE_SCALE = 6;
    private static final int MONEY_SCALE = Account.SCALE;

    private static final Currency DEFAULT_CURRENCY = Currency.PHP;

    private static final String REPORT_DIR = "output";
    private final ReportService reportService = new ReportService(CSV_PATH, REPORT_DIR);

    /* the choice in mainMenu is a number from 1 to length */
    private static final String[] MAIN_MENU = {
        "Register Account Name",
        "Deposit Amount",
        "Withdraw Amount",
        "Currency Exchange",
        "Record Exchange Rates",
        "Show Interest Amount",
        "Generate Reports",
        "Exit"
    };

    private Account account;
    private ExchangeRate rates = new ExchangeRate();

    private final Scanner sc = new Scanner(System.in);

    private static final String CSV_PATH = "data/bank_transactions.csv";
    private final AccountService accountService = new AccountService(CSV_PATH);

    /**
     * Shows the main menu and runs the chosen option, again and again, until one of the screens
     * says to quit. Then it prints a goodbye message.
     *
     * @param None
     * @return None
     */
    public void mainMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\nSelect Transaction:");
            ConsoleIO.printOptions(MAIN_MENU);
            System.out.println();

            int choice = ConsoleIO.safeGetInt(sc, "Choice: ", MAIN_MENU.length);

            switch (choice) {
                case 1:
                    running = registerAccountMenu();
                    break;
                case 2:
                    running = depositMenu();
                    break;
                case 3:
                    running = withdrawMenu();
                    break;
                case 4:
                    boolean stayInConvertMenu = true;

                    while (stayInConvertMenu) {
                        stayInConvertMenu = foreignCurrencyExchangeMenu();
                    }
                    break;
                case 5:
                    running = recordExchangeRateMenu();

                    break;
                case 6:
                    running = showInterestAmountMenu();

                    break;
                case 7:
                    reportMenu();

                    break;
                case 8:
                    running = false;
                    break;
            }
        }

        System.out.println("\nThank you for using the Banking App. Goodbye!");
    }

    /**
     * Runs the Register Account Name screen. It asks for a name and makes a new account with it. An
     * account that was made before is replaced.
     *
     * @param None
     * @return true to go back to the main menu, false to quit
     */
    public boolean registerAccountMenu() {
        System.out.println("Register Account Name");

        String name = ConsoleIO.safeGetName(sc, "Account Name: ");

        BigDecimal deposit;

        while (true) {
            deposit = ConsoleIO.safeGetAmount(sc, "Initial Deposit: ", MONEY_SCALE);

            if (deposit.compareTo(AccountService.MIN_INITIAL_DEPOSIT) >= 0) break;

            ConsoleIO.printErrorMessage(
                    ErrorMessage.INITIAL_DEPOSIT_TOO_LOW,
                    ConsoleIO.formatAmount(AccountService.MIN_INITIAL_DEPOSIT));
        }

        try {
            Long existing = accountService.findExistingAccountNumber(name);
            if (existing != null) {
                System.out.println(
                        "\nAn account named \""
                                + name
                                + "\" already exists (Account No. "
                                + existing
                                + "). Your deposit will be added to it.");
            }

            account = accountService.register(name, deposit);

            if (existing != null) {
                System.out.println(
                        "\nDeposit added to the existing account! Balance: "
                                + ConsoleIO.formatAmount(account.getBalance()));
            } else {
                System.out.println("\nAccount created successfully!");
            }
        } catch (IOException e) {
            ConsoleIO.printErrorMessage(ErrorMessage.ACCOUNT_NOT_SAVED);
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        return ConsoleIO.safeGetYesNo(sc, "Back to the Main Menu? (Y/N): ");
    }

    /**
     * Checks that an account was registered. If not, prints an error.
     *
     * @return true if there is an account, false if not
     */
    private boolean hasAccount() {
        if (account == null) {
            ConsoleIO.printErrorMessage(ErrorMessage.NAME_NOT_RECORDED);
            return false;
        }
        return true;
    }

    /**
     * Prints the account name, the current balance, and the currency.
     *
     * @param None
     * @return None
     */
    private void printAccountInfo() {
        System.out.println("Account Name: " + account.getName());
        System.out.println("Current Balance: " + ConsoleIO.formatAmount(account.getBalance()));
        System.out.println("Currency: " + DEFAULT_CURRENCY.getUnit());
    }

    /**
     * Asks for the account name and loads that account from the file. Names are unique.
     *
     * @return true if the account was found, false if not
     */
    private boolean selectAccount() {
        String name = ConsoleIO.safeGetName(sc, "Account Name: ");
        try {
            Account found = accountService.findByName(name);
            if (found == null) {
                ConsoleIO.printErrorMessage(ErrorMessage.ACCOUNT_NOT_FOUND);
                return false;
            }
            account = found;
            return true;
        } catch (IOException e) {
            ConsoleIO.printErrorMessage(ErrorMessage.ACCOUNT_NOT_LOADED);
            return false;
        }
    }

    /** Applies a deposit or withdrawal to the account and saves it to the file. */
    private boolean saveTransaction(boolean isDeposit, BigDecimal amount) {
        try {
            if (isDeposit) {
                accountService.deposit(account, amount);
            } else {
                accountService.withdraw(account, amount);
            }
            return true;
        } catch (IOException e) {
            ConsoleIO.printErrorMessage(ErrorMessage.ACCOUNT_NOT_SAVED);
            return false;
        }
    }

    /**
     * Runs the Deposit Amount screen. It asks for an amount and adds it to the balance.
     *
     * @param None
     * @return true to go back to the main menu, false to quit
     */
    public boolean depositMenu() {
        System.out.println("Deposit Amount");

        if (selectAccount()) {

            printAccountInfo();

            /* If the balance is already at the maximum */
            if (account.getRemainingCapacity().signum() == 0) {
                ConsoleIO.printErrorMessage(ErrorMessage.ACCOUNT_BALANCE_FULL);
            } else {
                BigDecimal depositAmt;

                while (true) {
                    depositAmt = ConsoleIO.safeGetAmount(sc, "Deposit Amount: ", MONEY_SCALE);
                    if (depositAmt.compareTo(account.getRemainingCapacity()) <= 0) {
                        break;
                    }

                    ConsoleIO.printErrorMessage(
                            ErrorMessage.DEPOSIT_EXCEED_LIMIT,
                            ConsoleIO.formatAmount(account.getRemainingCapacity()));
                }
                if (saveTransaction(true, depositAmt)) {
                    System.out.println(
                            "Updated Balance: " + ConsoleIO.formatAmount(account.getBalance()));
                }
            }
        }

        return ConsoleIO.safeGetYesNo(sc, "Back to the Main Menu? (Y/N): ");
    }

    /**
     * Runs the Withdraw Amount screen. It asks for an amount and takes it out of the balance.
     *
     * @param None
     * @return true to go back to the main menu, false to quit
     */
    public boolean withdrawMenu() {
        System.out.println("Withdraw Amount");

        if (selectAccount()) {
            printAccountInfo();

            /* If account balance is zero */
            if (account.getBalance().signum() == 0) {
                ConsoleIO.printErrorMessage(ErrorMessage.ACCOUNT_BALANCE_ZERO);
            } else {
                BigDecimal withdrawAmt;

                while (true) {
                    withdrawAmt = ConsoleIO.safeGetAmount(sc, "Withdraw Amount: ", MONEY_SCALE);
                    if (withdrawAmt.compareTo(account.getBalance()) <= 0) {
                        break;
                    }

                    ConsoleIO.printErrorMessage(ErrorMessage.WITHDRAW_EXCEED_BALANCE);
                }
                if (saveTransaction(false, withdrawAmt)) {
                    System.out.println(
                            "Updated Balance: " + ConsoleIO.formatAmount(account.getBalance()));
                }
            }
        }
        return ConsoleIO.safeGetYesNo(sc, "Back to the Main Menu? (Y/N): ");
    }

    /**
     * Prints all the currencies as a numbered list.
     *
     * @param None
     * @return None
     */
    private static void printCurrencies() {
        Currency[] currencies = Currency.values();
        String[] names = new String[currencies.length];

        for (int i = 0; i < currencies.length; i++) {
            names[i] = currencies[i].getFullName();
        }

        ConsoleIO.printOptions(names);
        System.out.println();
    }

    /**
     * Runs the Record Exchange Rate screen. It lists the currencies, then asks which one to set and
     * what its rate is (PHP per 1 unit).
     *
     * @param None
     * @return true to go back to the main menu, false to quit
     */
    public boolean recordExchangeRateMenu() {
        System.out.println("Record Exchange Rate\n");

        printCurrencies();

        int choice =
                ConsoleIO.safeGetInt(sc, "Select Foreign Currency: ", Currency.values().length);
        BigDecimal exchangeRate = ConsoleIO.safeGetAmount(sc, "Exchange Rate: ", RATE_SCALE);

        rates.setRate(Currency.values()[choice - 1], exchangeRate);

        return ConsoleIO.safeGetYesNo(sc, "Back to the Main Menu? (Y/N): ");
    }

    /**
     * Checks that a rate was recorded for the currency. If not, prints an error.
     *
     * @param currency the currency to check
     * @return true if it has a rate, false if not
     */
    private boolean hasRate(Currency currency) {
        if (!rates.hasRate(currency)) {
            ConsoleIO.printErrorMessage(ErrorMessage.RATE_NOT_RECORDED);
            return false;
        }
        return true;
    }

    /**
     * Runs one round of Currency Exchange. It asks for the source currency, the amount, and the
     * currency to change to, then prints the converted amount.
     *
     * @param None
     * @return true to convert another amount, false to go back to the main menu
     */
    public boolean foreignCurrencyExchangeMenu() {
        System.out.println("Foreign Currency Exchange");
        printCurrencies();

        int sourceChoice =
                ConsoleIO.safeGetInt(sc, "Select Currency Option: ", Currency.values().length);
        Currency source = Currency.values()[sourceChoice - 1];

        if (hasRate(source)) {
            BigDecimal sourceAmt = ConsoleIO.safeGetAmount(sc, "Source Amount: ", MONEY_SCALE);

            System.out.println("Exchanged Currency Options:");
            printCurrencies();

            int exchangeChoice =
                    ConsoleIO.safeGetInt(sc, "Exchange Currency: ", Currency.values().length);
            Currency target = Currency.values()[exchangeChoice - 1];

            if (hasRate(target)) {
                BigDecimal convertedAmt = rates.convert(source, target, sourceAmt);
                System.out.println("Exchange Amount: " + ConsoleIO.formatAmount(convertedAmt));
            }
        }

        return ConsoleIO.safeGetYesNo(sc, "Convert another currency (Y/N)?... ");
    }

    /**
     * Runs the Show Interest Amount screen. It asks for a number of days and prints the interest
     * and the balance for each day.
     *
     * @return true to go back to the main menu, false to quit
     */
    public boolean showInterestAmountMenu() {
        System.out.println("Show Interest Amount");

        if (hasAccount()) {
            printAccountInfo();
            System.out.println(
                    "Interest Rate: "
                            + InterestCalculator.getAnnualRate()
                                    .movePointRight(2)
                                    .stripTrailingZeros()
                                    .toPlainString()
                            + "%");
            System.out.println();

            int days = ConsoleIO.safeGetInt(sc, "Total Number of Days: ", Integer.MAX_VALUE);
            BigDecimal balance = account.getBalance();

            System.out.println();
            System.out.println("Day | Interest | Balance |");

            for (int day = 1; day <= days; day++) {
                BigDecimal interest = InterestCalculator.dailyInterest(balance);
                balance = balance.add(interest);

                System.out.printf(
                        "%-3d | %-8s | %-7s |%n",
                        day, ConsoleIO.formatAmount(interest), ConsoleIO.formatAmount(balance));
            }
            System.out.println();
        }

        return ConsoleIO.safeGetYesNo(sc, "Back to the Main Menu? (Y/N): ");
    }

    public boolean reportMenu() {
        System.out.println("Generate Reports");

        while (true) {
            ConsoleIO.printOptions("Load the file", "Generate Reports");
            int choice = ConsoleIO.safeGetInt(sc, "Enter choice:", 2);

            try {
                if (choice == 1) {
                    if (!reportService.loadDataset()) {
                        ConsoleIO.printErrorMessage(ErrorMessage.DATASET_EMPTY);
                    }

                    continue;
                }

                if (!reportService.isLoaded()) {
                    ConsoleIO.printErrorMessage(ErrorMessage.DATASET_NOT_LOADED);

                    continue;
                }

                reportService.generateReports();
            } catch (IOException e) {
                ConsoleIO.printErrorMessage(ErrorMessage.REPORT_FAILED);
            }

            break;
        }

        return ConsoleIO.safeGetYesNo(sc, "Back to the Main Menu? (Y/N): ");
    }

    /**
     * Starts the program.
     *
     * @param args
     * @return None
     */
    public static void main(String[] args) {
        BankingApp app = new BankingApp();

        app.mainMenu();
    }
}
