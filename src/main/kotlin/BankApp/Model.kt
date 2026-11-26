package BankApp

import java.math.BigDecimal
import java.math.RoundingMode
import java.io.*
import kotlin.collections.iterator
import com.jsoizo.kotlincsv.csvReader
import com.jsoizo.kotlincsv.csvWriter
import com.jsoizo.kotlincsv.reader.readFromFile
import com.jsoizo.kotlincsv.reader.withHeader
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Date
import kotlin.random.Random

/**
    The main sections of the program.

    Used to divide input processing and outputs into different functions.
 */
enum class Screen{
    /**
        The user can start Transactions here.
    */
    MENU,
    /**
        The user can register an Account here.
     */
    REGISTER,
    /**
        The user can deposit to an Account here.
     */
    DEPOSIT,
    /**
        The user can withdraw from an Account here.
     */
    WITHDRAW,
    /**
        The user can record exchange rates here.
     */
    RECORDRATE,
    /**
        The user can exchange currency here.
     */
    CURRENCYEXCHANGE,
    /**
        The user can check interest here.
     */
    INTEREST
}

const val ACCOUNT_FILEPATH = "accounts.dat"
const val CSV_FILEPATH = "bank_transactions.csv"
const val RATES_FILEPATH = "rates.dat"

/**
    Represents one of the supported currencies to the banking app.

    @property currencyName the full display name of the currency.
 */
enum class Currency(val currencyName: String){
    PHP("Philippine Peso"),
    USD("United States Dollar"),
    JPY("Japanese Yen"),
    GBP("British Pound Sterling"),
    EUR("Euro"),
    CNY("Chinese Yuan Renminni")
}

enum class Transactions(val transactionName: String) {
    Deposit("deposit"),
    Withdraw("withdraw"),
}

/**
    Represents a Wallet containing an account's balance for a certain currency.
    NOTE: Accounts having multiple currencies was not implemented, so accounts only use a wallet for the default currency PHP.

    @constructor sets the currency and balance to the given currency and Initial Balance. Asserts that Initial Balance must be a Positive value.
    @property currency the type of currency the wallet is storing.
    @param initialBalance the amount of initial balance the wallet should begin with. Has a default value of 0 with a BigDecimal scale of 2.
    @property balance the amount of the currency in the wallet.
 */
class Wallet(val currency: Currency, initialBalance: BigDecimal = BigDecimal("0.00")){
    var balance: BigDecimal
        private set

    /**
        Deposits a value into the account.
        Fails if the amount is non-positive.

        @param[amount] the amount to deposit.
     */
    fun deposit(amount: BigDecimal){
        if (amount >= BigDecimal.ZERO){
            balance += amount
        }
    }

    /**
        Withdraws a value from the account.
        Fails if the amount is non-positive.
        Fails if the amount is greater than the current balance.

        @param[amount] the amount to deposit.
     */
    fun withdraw(amount: BigDecimal){
        if (amount >= BigDecimal.ZERO && amount <= balance){
            balance -= amount
        }
    }

    init{
        require(initialBalance >= BigDecimal.ZERO) {"Initial Balance must be Positive"}
        balance = initialBalance
    }
}

/**
    Represents an account in the banking app.
    NOTE: Accounts having multiple currencies was not implemented, so accounts only use a wallet for the default currency PHP.

    @property name the name of the account.
    @property wallets the [Wallet]s in this account.
 */
class Account(val account_name: String, val account_number: String){
    private var wallets: MutableMap<Currency, Wallet> = mutableMapOf<Currency, Wallet>()

    /**
        Creates a new Wallet for the account.
        NOTE: Accounts having multiple currencies was not implemented, so accounts only use a wallet for the default currency PHP.

        @param[currency] the type of currency the wallet should hold.
        @param[initialBalance] the initial balance of the wallet.
     */
    fun createWallet(currency: Currency, initialBalance: BigDecimal = BigDecimal("0.00")){
        if (wallets.containsKey(currency)){
            //Not really being used if only php wallet
            printError("ERROR: Wallet already exists.","")
        }else{
            wallets.put(currency, Wallet(currency, initialBalance))
        }
    }

    /**
        Checks the amount of money in the account's wallet.
        NOTE: Accounts having multiple currencies was not implemented, so accounts only use a wallet for the default currency PHP.

        @param[currency] the currency type of the target wallet.
        @return the balance of the wallet, or null if the wallet does not exist.
     */
    fun checkWallet(currency: Currency): BigDecimal?{
        return wallets[currency]?.balance
    }

    //Unused
    /**
        Checks how many wallets the account has.
     */
    fun checkWalletNum(): Int{
        return wallets.size
    }

    /**
        Deposits an amount to a wallet.
        Lets the corresponding wallet function handle it.

        @param[amount] the amount to deposit.
        @param[currency] the currency type of the target wallet.
     */
    fun depositToWallet(amount: BigDecimal, currency: Currency){
        wallets[currency]?.deposit(amount)
        CSV.writeTransaction(this, currency,amount, Transactions.Deposit)
    }

    /**
        Withdraws an amount from a wallet.
        Lets the corresponding wallet function handle it.

        @param[amount] the amount to withdraw.
        @param[currency] the currency type of the target wallet.
     */
    fun withdrawFromWallet(amount: BigDecimal, currency: Currency){
        wallets[currency]?.withdraw(amount)
        CSV.writeTransaction(this, currency,amount, Transactions.Withdraw)
    }

}

class Transaction(
    val account_name: String,
    val account_number: String,
    val dateTime: LocalDateTime,
    val currency: Currency,
    val amount: BigDecimal,
    val type: Transactions,
    val current_balance: BigDecimal
) {
}

/**
    A model of the banking app's data.

    @property running indicates if the program should currently be on.
    @property screen indicates the current screen the user is on.
    @property accounts a map of the accounts in the system, with the key being the account's name.
    @property exchangeRates a map of the supported Currencies and their exchange rates based on PHP.
 */
class Model(){
    var running: Boolean = true
        private set
    var screen: Screen = Screen.MENU
    var accounts: MutableMap<String, Account> = mutableMapOf<String, Account>()
    var account_set: MutableSet<String> = mutableSetOf()
    //If initial rates are empty, change it to ZERO
    var exchangeRates: MutableMap<Currency, BigDecimal> = mutableMapOf<Currency, BigDecimal>(
        Currency.PHP to BigDecimal("1.00"),
        Currency.USD to BigDecimal("0.00"),
        Currency.JPY to BigDecimal("0.00"),
        Currency.GBP to BigDecimal("0.00"),
        Currency.EUR to BigDecimal("0.00"),
        Currency.CNY to BigDecimal("0.00")
    )

    /**
        Registers an account and adds it to the MutableMap. Also creates an empty wallet for it in the default PHP currency.

        @param[name] the name of the new account.
     */
    fun registerAccount(name: String, initialDeposit: BigDecimal){
        var account_number: String = ""
        for (i in 1..10) {
            account_number = "1092${Random.nextInt(1, 999999).toString().padStart(6, '0')}"
            if (!account_set.contains(account_number)) break
        }

        val account: Account = Account(account_name = name, account_number = account_number)
        account.createWallet(Currency.PHP, initialBalance = initialDeposit)
        accounts[name] = account
        account_set.add(account_number)

        CSV.writeTransaction(account, Currency.PHP, initialDeposit, Transactions.Deposit)
    }

    /**
        Checks if there exists an account in the model.

        @return true if an account exists, false otherwise.
     */
    fun accExists(): Boolean{
        return !accounts.isEmpty();
    }

    /**
        Deposits money into an account.

        @param[name] the name of the new account.
        @param[amount] the amount of money to deposit.
        @param[currency] the wallet to deposit it to.
     */
    fun deposit(name: String, amount: BigDecimal, currency: Currency){
        accounts[name]?.depositToWallet(amount, currency)
    }

    /**
        Withdraws money from an account.

        @param[name] the name of the new account.
        @param[amount] the amount of money to withdraw.
        @param[currency] the wallet to withdraw it from.
     */
    fun withdraw(name: String, amount: BigDecimal, currency: Currency){
        accounts[name]?.withdrawFromWallet(amount, currency)
    }

    /**
        Checks if an account exists in the system.
        
        return the account's name if it exists, null otherwise.
     */
    fun checkAccount(name: String): Account?{
        return accounts[name]
    }

    /**
        Changes the exchange rate of one of the supported currencies.
        Fails if the value is non-positibe.

        @param[rate] the new rate to set it to.
        @param[currency] the currency to change the rate of.
     */
    fun changeRate(rate: BigDecimal, currency: Currency){
        if (rate > BigDecimal.ZERO){
            exchangeRates[currency] = rate
        }
    }

    /**
        Calculates the value if you exchange from one currency to another.
        It does so by first converting to PHP, then converting to the target currency.

        @param[currency1] the source currency.
        @param[currency2] the target currency.
        @param[amount] the amount to convert.

        @return a BigDecimal holding the converted value rounded to two decimal places.
     */
    fun calculateExchange(currency1: Currency, currency2: Currency, amount: BigDecimal): BigDecimal{
        val rate1 = exchangeRates.getValue(currency1)
        val rate2 = exchangeRates.getValue(currency2)
        return (amount*rate1).divide(rate2, 2, RoundingMode.HALF_UP)
    }

    /**
        Checks if a rate has been set yet or not.
        Returns false if it is either null, or BigDecimal.Zero or less.

        @param[currency] the currency to check the rate of.

        @return true if it has been set, false otherwise.
     */
    fun checkRateExists(currency: Currency): Boolean{
        val rate = exchangeRates[currency]
        if (rate == null){
            return false
        }
        if (rate <= BigDecimal.ZERO){
            return false
        }
        return true
    }

    /**
        Saves and Exits the programs.
     */
    fun exit(){
        DataOutputStream(BufferedOutputStream(FileOutputStream(ACCOUNT_FILEPATH))).use { out ->
            out.writeInt(accounts.size)
            for(account in accounts.values){
                out.writeUTF(account.account_name)
                out.writeUTF(account.account_number)
                out.writeInt(account.checkWalletNum())
                for (currency in Currency.entries) {
                    var balance: BigDecimal? = account.checkWallet(currency)
                    if (balance != null){
                        out.writeUTF(currency.name)
                        out.writeUTF(balance.toPlainString())
                    }
                }
            }
        }
        DataOutputStream(BufferedOutputStream(FileOutputStream(RATES_FILEPATH))).use { out ->
            out.writeInt(exchangeRates.size)
            for((currency, rate) in exchangeRates){
                out.writeUTF(currency.name)
                out.writeUTF(rate.toPlainString())
            }
        }
        running = false
    }

    /**
        Loads the data from the files into the model.
     */
    fun load(){
        if (File(ACCOUNT_FILEPATH).exists()) {
            try{
                DataInputStream(BufferedInputStream(FileInputStream(ACCOUNT_FILEPATH))).use { input ->
                    for (i in 1..input.readInt()) {
                        val account_name: String = input.readUTF()
                        val account_number: String = input.readUTF()
                        val account: Account = Account(account_name, account_number)
                        accounts[account_name] = account
                        account_set.add(account_number)
                        for(j in 1..input.readInt()){
                            val currency: Currency = Currency.valueOf(input.readUTF())
                            val balance: BigDecimal = BigDecimal(input.readUTF())
                            account.createWallet(currency, balance)
                        }
                    }
                }
            } catch (e: IOException) {
                println("IOException: File is damaged.")
            } catch (e: IllegalArgumentException) {
                println("IllegalArgumentException: File is corrupted.")
            }
        }

        if (File(CSV_FILEPATH).exists()) {
            try {
                CSV.reader.readFromFile(File(CSV_FILEPATH)) { rows ->
                    val records = rows.withHeader().toList()
                    records.take(10000).forEach { row ->
                        if (
                            (row["account_name"] ?: "").isNotEmpty() &&
                            (row["account_number"] ?: "").isNotEmpty() &&
                            (row["currency"] ?: "").isNotEmpty() &&
                            (row["current_balance"] ?: "").isNotEmpty()
                        ) {
                            val account: Account = accounts[(row["account_name"] ?: "")] ?: Account((row["account_name"] ?: ""), (row["account_number"] ?: ""))
                            if (accounts[(row["account_name"] ?: "")] == null) {
                                accounts[(row["account_name"] ?: "")] = account
                                account_set.add((row["account_number"] ?: ""))
                            }

                            val rowCurrency: Currency = Currency.valueOf((row["currency"] ?: "PHP"))
                            if (account.checkWallet(rowCurrency) == null) {
                                val currency: Currency = Currency.valueOf((row["currency"] ?: "PHP"))
                                val balance: BigDecimal = BigDecimal(row["current_balance"])
                                account.createWallet(currency, balance)
                            } else {
                                val difference: BigDecimal = BigDecimal(row["current_balance"]) - (account.checkWallet(rowCurrency) ?: BigDecimal.ZERO)
                                if (difference > BigDecimal.ZERO) account.depositToWallet(difference, rowCurrency)
                                else account.withdrawFromWallet(difference, rowCurrency)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                println("IOException: File is damaged.")
            } catch (e: IllegalArgumentException) {
                println("IllegalArgumentException: File is corrupted.")
            }
        }

        if (File(RATES_FILEPATH).exists()) {
            try{
                DataInputStream(BufferedInputStream(FileInputStream(RATES_FILEPATH))).use { input ->
                    for (i in 1..input.readInt()) {
                        val currency = Currency.valueOf(input.readUTF())
                        exchangeRates[currency] = BigDecimal(input.readUTF())
                    }
                }
            } catch (e: IOException) {
                println("IOException: File is damaged.")
            } catch (e: IllegalArgumentException) {
                println("IllegalArgumentException: File is corrupted.")
            }
        }
    }

    companion object {

        fun isValidDate(input: String, pattern: String = "dd/MM/yyyy"): Boolean {
            return try {
                val formatter = DateTimeFormatter.ofPattern(pattern)
                    .withResolverStyle(ResolverStyle.STRICT)
                LocalDate.parse(input, formatter)
                true
            } catch (e: DateTimeParseException) {
                false
            }
        }

        fun readTransactions(): Pair<MutableMap<String, MutableList<Transaction>>, Int> {
            val account_number_regex: Regex = Regex("""1092[0-9]{6}""")
            val time_regex: Regex = Regex("""^(?:[0-9]|1[0-9]|2[0-3]):(?:[0-5][0-9]):(?:[0-5][0-9])$""")
            val amount_regex: Regex = Regex("""[0-9]+.[0-9]{2}""")

            val allTransactions: MutableMap<String, MutableList<Transaction>> = mutableMapOf()
            var invalidTransactions: Int = 0

            if (File(CSV_FILEPATH).exists()) {
                try {
                    CSV.reader.readFromFile(File(CSV_FILEPATH)) { rows ->
                        val records = rows.withHeader().toList()
                        records.take(10000).forEach { row ->
                            if (
                                (row["account_name"] ?: "").isNotEmpty() &&
                                (row["account_number"] ?: "").isNotEmpty() &&
                                (row["date"] ?: "").isNotEmpty() &&
                                (row["time"] ?: "").isNotEmpty() &&
                                (row["currency"] ?: "").isNotEmpty() &&
                                (row["amount"] ?: "").isNotEmpty() &&
                                (row["type"] ?: "").isNotEmpty() &&
                                (row["current_balance"] ?: "").isNotEmpty()
                            ) {
                                if (
                                // Valid account number
                                    account_number_regex.matches(row["account_number"] ?: "") &&

                                    // Valid date
                                    isValidDate((row["date"] ?: "")) &&

                                    // Valid time
                                    time_regex.matches(row["time"] ?: "") &&

                                    // Valid currency
                                    Currency.entries.any({ it.name == (row["currency"] ?: "") }) &&

                                    // Valid transaction type
                                    Transactions.entries.any({ it.transactionName == (row["transaction"] ?: "") }) &&

                                    // Valid amounts
                                    amount_regex.matches(row["amount"] ?: "") &&
                                    amount_regex.matches(row["current_balance"] ?: "") &&
                                    BigDecimal(row["amount"] ?: "") > BigDecimal.ZERO &&
                                    BigDecimal(row["current_balance"] ?: "") > BigDecimal.ZERO
                                ) {
                                    if (allTransactions[row["account_number"]] == null)
                                        allTransactions[row["account_number"] ?: ""] = mutableListOf()

                                    allTransactions[row["account_number"]]?.add(Transaction(
                                        account_name = row["account_name"] ?: "",
                                        account_number = row["account_number"] ?: "",
                                        dateTime = LocalDateTime.parse("${row["date"] ?: ""} ${row["time"] ?: ""}"),
                                        currency = Currency.valueOf(row["currency"] ?: "PHP"),
                                        amount = BigDecimal(row["amount"] ?: ""),
                                        type = Transactions.valueOf(row["type"] ?: ""),
                                        current_balance = BigDecimal(row["current_balance"] ?: "0.0"),
                                    ))

                                    allTransactions[(row["account_number"] ?: "")] = allTransactions[row["account_number"]]?.sortedBy { it.dateTime } as MutableList<Transaction>
                                }

                                else invalidTransactions += 1
                            }

                            else invalidTransactions += 1
                        }
                    }
                } catch (e: IOException) {
                    println("IOException: File is damaged.")
                } catch (e: IllegalArgumentException) {
                    println("IllegalArgumentException: File is corrupted.")
                }
            }

            return Pair(allTransactions, invalidTransactions)
        }
    }

}