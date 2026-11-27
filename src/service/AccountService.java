package service;

import data_processing.CSVReader;
import data_processing.CSVWriter;

import model.Account;
import model.Currency;
import model.TransactionRecord;
import model.TransactionType;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;

public class AccountService {
    public static final BigDecimal MIN_INITIAL_DEPOSIT = new BigDecimal("5000.00");
    private final String csvPath;

    /**
     * Makes a service that reads and writes accounts in the CSV file.
     *
     * @param csvPath the path of the CSV file
     * @return None
     */
    public AccountService(String csvPath) {
        this.csvPath = csvPath;
    }

    /**
     * Finds the account number already used by an exact name.
     *
     * @param name the account name to look for
     * @return the account number, or null if the name is new
     * @throws IOException if the file cannot be read
     */
    public Long findExistingAccountNumber(String name) throws IOException {
        return CSVReader.findAccountNumber(csvPath, name);
    }

    /**
     * Loads the account with this exact name, using its last balance in the file. Names are unique.
     *
     * @param name the account name to look for
     * @return the account, or null if there is none
     * @throws IOException if the file cannot be read
     */
    public Account findByName(String name) throws IOException {
        Long num = CSVReader.findAccountNumber(csvPath, name);
        if (num == null) {
            return null;
        }
        BigDecimal last = CSVReader.getLastBalance(csvPath, num);
        BigDecimal start =
                last == null ? BigDecimal.ZERO : last.setScale(Account.SCALE, RoundingMode.HALF_UP);
        return new Account(name, num, start);
    }

    /**
     * Deposits into the account and saves the transaction to the CSV file.
     *
     * @param account the account to deposit into
     * @param amount the amount to deposit
     * @return None
     * @throws IOException if the file cannot be written
     */
    public void deposit(Account account, BigDecimal amount) throws IOException {
        account.deposit(amount);
        record(account, amount, TransactionType.DEPOSIT);
    }

    /**
     * Withdraws from the account and saves the transaction to the CSV file.
     *
     * @param account the account to withdraw from
     * @param amount the amount to withdraw
     * @return None
     * @throws IOException if the file cannot be written
     */
    public void withdraw(Account account, BigDecimal amount) throws IOException {
        account.withdraw(amount);
        record(account, amount, TransactionType.WITHDRAWAL);
    }

    /**
     * Adds a PHP transaction of the account to the end of the CSV file, using the current date and
     * time.
     *
     * @param account the account the transaction belongs to
     * @param amount the amount of the transaction
     * @param type the transaction type, "deposit" or "withdrawal"
     * @return None
     * @throws IOException if the file cannot be written
     */
    private void record(Account account, BigDecimal amount, String type) throws IOException {
        CSVWriter.append(
                new TransactionRecord(
                        account.getName(),
                        account.getAccountNumber(),
                        LocalDate.now(),
                        LocalTime.now().withNano(0),
                        Currency.PHP,
                        amount,
                        type,
                        account.getBalance()),
                csvPath);
    }

    /**
     * Registers a name with an initial deposit. If the exact name already has an account, its
     * account number and last balance are reused and the deposit is added to it. Otherwise a new
     * account number is made.
     *
     * @param name the account name
     * @param initialDeposit the first deposit; at least MIN_INITIAL_DEPOSIT
     * @return the account after the deposit
     * @throws IOException if the file cannot be read or written
     * @throws IllegalArgumentException if the deposit is too low, or the balance would go over the
     *     maximum
     */
    public Account register(String name, BigDecimal initialDeposit) throws IOException {
        if (initialDeposit.compareTo(MIN_INITIAL_DEPOSIT) < 0) {
            throw new IllegalArgumentException("Initial deposit below minimum.");
        }

        Long existing = CSVReader.findAccountNumber(csvPath, name);
        long newNum;
        BigDecimal start = BigDecimal.ZERO;
        if (existing != null) {
            newNum = existing;
            BigDecimal last = CSVReader.getLastBalance(csvPath, newNum);
            if (last != null) {
                start = last.setScale(Account.SCALE, RoundingMode.HALF_UP);
            }
        } else {
            newNum = CSVReader.getHighestAccountNum(csvPath) + 1;
        }
        Account account = new Account(name, newNum, start);
        account.deposit(initialDeposit);

        CSVWriter.append(
                new TransactionRecord(
                        name,
                        newNum,
                        LocalDate.now(),
                        LocalTime.now().withNano(0),
                        Currency.PHP,
                        initialDeposit,
                        "deposit",
                        account.getBalance()),
                csvPath);

        return account;
    }
}
