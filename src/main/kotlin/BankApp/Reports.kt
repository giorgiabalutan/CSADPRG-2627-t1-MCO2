package BankApp

import BankApp.CSV.Companion.writer
import com.jsoizo.kotlincsv.writer.write
import de.m3y.kformat.Table
import de.m3y.kformat.table
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.collections.forEach

/**
 * Enum class to hold privilege tiers
 */
enum class PrivilegeTier {
    Silver,
    Gold,
    Platinum,
}

/**
 * Enum class to hold the risk level of accounts
 */
enum class RiskFlagLevel(val level: Int) {
    Suspicious(1),
    Normal(2),
}

/**
 * Data class to organize in AvgDailyBalance report
 */
data class AvgDailyBalanceClass (
    val accountName: String,
    val accountAge: Int,
    val currentBalance: BigDecimal,
    val averageDailyBalance: BigDecimal,
    val privilegeTier: PrivilegeTier,
)

/**
 * Data class to organize in InterestExpense report
 */
data class InterestExpenseClass (
    val accountName: String,
    val currentBalance: BigDecimal,
    val expectedInterest: BigDecimal,
)

/**
 * Data class to organize in SuspiciousClients report
 */
data class SuspiciousClientClass (
    val accountName: String,
    val date: LocalDate,
    val totalWithdrawalAmounts: BigDecimal,
    val numberOfDeposits: Int,
    val totalDepositAmounts: BigDecimal,
    val turnoverVelocity: Double,
    val riskFlagLevel: RiskFlagLevel,
)

/**
 * Extend BigDecimal to add JSON output
 */
@OptIn(ExperimentalSerializationApi::class)
private fun BigDecimal.toJsonNumber(): JsonPrimitive =
    JsonUnquotedLiteral(this.setScale(2, RoundingMode.HALF_UP).toPlainString())

class Reports {
    companion object {
        // Holds the dataset for loading and accessing
        var dataset: MutableMap<String, MutableList<Transaction>>? = null

        /**
         * Helper function to get the average daily balance for one user
         *
         * @param transactions The account's transactions
         * @param now The current date (already initialized)
         */
        fun averageDailyBalanceLastMonth(
            transactions: List<Transaction>,
            now: LocalDateTime = LocalDateTime.now()
        ): BigDecimal {
            // Get current date, and last date
            val endDate = now.toLocalDate()
            val startDate = endDate.minusMonths(1)

            // Get all end of day transactions
            val endOfDay = transactions
                .filter { !it.dateTime.isAfter(now) }
                .sortedBy { it.dateTime }
                .associate { it.dateTime.toLocalDate() to it.current_balance }

            // Get end of day balance
            var balance = endOfDay.filterKeys { it < startDate }
                .maxByOrNull { it.key }?.value ?: BigDecimal.ZERO

            // Get all days
            val days = startDate.datesUntil(
                endDate.plusDays(1)
            ).toList()

            // Get total balance over all days
            val total = days.fold(BigDecimal.ZERO) { sum, day ->
                balance = endOfDay[day] ?: balance
                sum + balance
            }

            // Get total / days (average)
            return total.divide(
                BigDecimal(days.size),
                2,
                RoundingMode.HALF_UP
            )
        }

        /**
         * Generates, saves, and returns the average daily balance report
         */
        fun averageDailyBalance(): List<AvgDailyBalanceClass> {
            // Get current date
            val now = LocalDateTime.now()
            val averageDailyBalanceList: MutableList<AvgDailyBalanceClass> = mutableListOf()

            // For every account...
            dataset?.forEach { (_, transactions) ->
                if (transactions.last().current_balance < BigDecimal(100000.00)) return@forEach

                val accountName = transactions.first().account_name

                // Get account age by subtracting dates from first ever, and last transaction
                val accountAge = ChronoUnit.DAYS.between(
                    transactions.first().dateTime,
                    transactions.last().dateTime
                ).toInt()

                // Call helper function to get balance
                val currentBalance = transactions.last().current_balance
                val averageDailyBalance = averageDailyBalanceLastMonth(transactions, now)
                val privilegeTier = if (averageDailyBalance < BigDecimal(500000.00))
                    PrivilegeTier.Silver else if (averageDailyBalance < BigDecimal(1000000.00))
                    PrivilegeTier.Gold else PrivilegeTier.Platinum

                // Create new object and add to list
                averageDailyBalanceList.add(AvgDailyBalanceClass(
                    accountName = accountName,
                    accountAge = accountAge,
                    currentBalance = currentBalance,
                    averageDailyBalance = averageDailyBalance,
                    privilegeTier = privilegeTier,
                ))
            }

            // Sort by average daily balance
            val sorted = averageDailyBalanceList.sortedWith(
                compareByDescending<AvgDailyBalanceClass> { it.averageDailyBalance }
            )

            // Create CSV rows
            val sortedRows = sorted.map { entry -> listOf(
                entry.accountName,
                entry.accountAge.toString(),
                entry.currentBalance.toString(),
                entry.averageDailyBalance.toString(),
                entry.privilegeTier.toString(),
            ) }

            // Add headers
            val allRows: List<List<String>> = listOf(
                listOf(
                    "AccountName",
                    "AccountAge",
                    "CurrentBalance",
                    "AverageDailyBalance",
                    "PrivilegeTier",
                )
            ) + sortedRows

            // Output to CSV
            FileOutputStream(File("report1_banking_privilege_tiers.csv")).use { stream ->
                writer.write(allRows, stream)
            }

            return sorted
        }

        /**
         * Generates, saves, and returns the interest expense report
         */
        fun interestExpenseProjection(): List<InterestExpenseClass> {
            val interestExpenseList: MutableList<InterestExpenseClass> = mutableListOf()

            // For each account...
            dataset?.forEach { (_, transactions) ->
                val accountName = transactions.last().account_name
                val currentBalance = transactions.last().current_balance

                // Filter out 10,000.00 and below
                if (currentBalance < BigDecimal(10000.00)) return@forEach

                // Get daily rate
                val annualRate = BigDecimal("0.05")
                val dailyRate = annualRate.divide(BigDecimal(365), 10, RoundingMode.HALF_UP)

                // Calculate final balance from compounding interest
                var balance = currentBalance
                repeat(30) {
                    balance += balance * dailyRate
                }

                // Subtract initial balance and final balance to get interest gained
                val actualInterest = (balance - currentBalance).setScale(2, RoundingMode.HALF_UP)

                // Create new object and add to list
                interestExpenseList.add(
                    InterestExpenseClass(
                        accountName = accountName,
                        currentBalance = currentBalance,
                        expectedInterest = actualInterest.setScale(2, RoundingMode.HALF_UP),
                    )
                )
            }

            // Sort by interest
            val sorted = interestExpenseList.sortedWith(
                compareByDescending<InterestExpenseClass> { it.expectedInterest }
            )

            // Create CSV rows
            val sortedRows = sorted.map { entry -> listOf(
                entry.accountName,
                entry.currentBalance.toString(),
                entry.expectedInterest.toString(),
            ) }

            // Add CSV headers
            val allRows: List<List<String>> = listOf(
                listOf(
                    "AccountName",
                    "CurrentBalance",
                    "ExpectedInterest",
                )
            ) + sortedRows

            // Output to file
            FileOutputStream(File("report2_interest_expense_projection.csv")).use { stream ->
                writer.write(allRows, stream)
            }

            return sorted
        }

        /**
         * Generates, saves, and returns the suspicious clients report
         */
        fun suspiciousClientBehaviour(): List<SuspiciousClientClass> {
            val results = mutableListOf<SuspiciousClientClass>()

            // For every account...
            dataset?.forEach { (_, transactions) ->
                // If no transactions, return
                if (transactions.isEmpty()) return@forEach
                val accountName = transactions.first().account_name

                // Group by date
                transactions.groupBy { it.dateTime.toLocalDate() }.forEach { (date, dayTx) ->
                    // Get deposits/withdrawals
                    val numberOfDeposits = dayTx
                        .count{ it.type == Transactions.Deposit }
                    val totalDepositAmounts = dayTx
                        .filter { it.type == Transactions.Deposit }
                        .sumOf { it.amount }
                    val totalWithdrawalAmounts = dayTx
                        .filter { it.type == Transactions.Withdrawal }
                        .sumOf { it.amount }

                    // Calculate turnover velocity
                    val turnoverVelocity = if (totalWithdrawalAmounts.signum() == 0) {
                        0.0
                    } else {
                        totalDepositAmounts
                            .divide(totalWithdrawalAmounts, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal(100))
                            .setScale(2, RoundingMode.HALF_UP)
                            .toDouble()
                    }

                    // Calculate risk level
                    val riskFlagLevel =
                        if (totalWithdrawalAmounts >= BigDecimal(500000.00) || turnoverVelocity >= 90.0)
                            RiskFlagLevel.Suspicious
                        else
                            RiskFlagLevel.Normal

                    // Create new object and add to list
                    results.add(
                        SuspiciousClientClass(
                            accountName = accountName,
                            date = date,
                            totalWithdrawalAmounts = totalWithdrawalAmounts,
                            numberOfDeposits = numberOfDeposits,
                            totalDepositAmounts = totalDepositAmounts,
                            turnoverVelocity = turnoverVelocity,
                            riskFlagLevel = riskFlagLevel,
                        )
                    )
                }
            }

            // Sort by date, then total withdrawal amounts
            val sorted = results.sortedWith(
                compareBy<SuspiciousClientClass> { it.date }
                    .thenBy { it.riskFlagLevel.level }
            )

            // Create CSV rows
            val sortedRows = sorted.map { entry -> listOf(
                entry.accountName,
                entry.date.toString(),
                entry.totalWithdrawalAmounts.toString(),
                entry.numberOfDeposits.toString(),
                entry.totalDepositAmounts.toString(),
                entry.turnoverVelocity.toString(),
                entry.riskFlagLevel.toString(),
            ) }

            // Add CSV header
            val allRows: List<List<String>> = listOf(
                listOf(
                    "AccountName",
                    "Date",
                    "TotalWithdrawalAmounts",
                    "NumberOfDeposits",
                    "TotalDepositAmounts",
                    "TurnoverVelocity",
                    "RiskFlagLevel",
                )
            ) + sortedRows

            // Write to file
            FileOutputStream(File("report3_suspicious_client_behavior.csv")).use { stream ->
                writer.write(allRows, stream)
            }

            return sorted
        }

        /**
         * Generates, saves, and returns the transaction activity report
         *
         * JUSTIFICATION: Banks may use this to check for unused accounts
         */
        fun transactionActivity(): List<Pair<String, Int>> {
            // For every account, pair account number with number of transactions
            // Sort by number of transactions, then account number
            val sorted = dataset
                ?.map { (accountNumber, transactions) -> accountNumber to transactions.size }
                ?.sortedWith(
                    compareByDescending<Pair<String, Int>> { it.second }
                        .thenByDescending { it.first }
                )
                ?: emptyList()

            // Write CSV rows, Add CSV header
            val allRows: List<List<String>> =
                listOf(listOf("AccountNumber", "TotalTransactions")) +
                        sorted.map { (accountNumber, count) -> listOf(accountNumber, count.toString()) }

            // Write to File
            FileOutputStream(File("report4_total_transaction_activity.csv")).use { stream ->
                writer.write(allRows, stream)
            }

            return sorted
        }

        /**
         * Generates, saves, and returns the summary
         *
         * @param totalInterestExpense Pre-calculated total for interest expense
         * @param totalSuspicious Pre-calculated total for suspicious transaction days
         * @param totalNormal Pre-calculated total for normal transaction days
         */
        fun summary(
            totalInterestExpense: BigDecimal,
            totalSuspicious: Int,
            totalNormal: Int,
            averageDailyBalance: List<AvgDailyBalanceClass>
        ): String {
            var totalAccounts = 0
            var totalDeposits = BigDecimal.ZERO
            var totalWithdrawals = BigDecimal.ZERO
            var totalBalance = BigDecimal("0.0")

            // For every account...
            dataset?.forEach { (_, transactions) ->
                totalAccounts++
                totalBalance += transactions.last().current_balance
                transactions.forEach { transaction ->
                    when (transaction.type) {
                        Transactions.Deposit -> totalDeposits += transaction.amount
                        Transactions.Withdrawal -> totalWithdrawals += transaction.amount
                    }
                }
            }

            // Create JSON object
            val jsonObject = buildJsonObject {
                put("total_accounts", totalAccounts)
                put("total_deposits", totalDeposits.toJsonNumber())
                put("total_withdrawals", totalWithdrawals.toJsonNumber())
                put("total_balance", totalBalance.toJsonNumber())
                put("total_interest_expense", totalInterestExpense.toJsonNumber())
                putJsonObject("suspicious_transactions") {
                    put("suspicious", totalSuspicious)
                    put("normal", totalNormal)
                }
                putJsonObject("privilege_tier") {
                    put("total_privilege_platinum", averageDailyBalance.count { it.privilegeTier == PrivilegeTier.Platinum })
                    put("total_privilege_gold", averageDailyBalance.count { it.privilegeTier == PrivilegeTier.Gold })
                    put("total_privilege_silver", averageDailyBalance.count { it.privilegeTier == PrivilegeTier.Silver })
                }
            }

            // Enable JSON formatting
            val prettyJson = Json { prettyPrint = true }
            val formattedString = prettyJson.encodeToString(JsonObject.serializer(), jsonObject)

            // Write to file
            File("summary.json").writeText(formattedString)

            return jsonObject.toString()
        }

        /**
         * Loads the CSV into dataset variable
         */
        fun loadDataset() {
            val (validTransactions, invalidTransactions) = Model.readTransactions()
            println("Processing dataset... (${validTransactions.entries.sumOf { it.value.size } + invalidTransactions} rows loaded, ${validTransactions.entries.sumOf { it.value.size }} filtered for PHP currency)")
            dataset = validTransactions
        }

        /**
         * Generates and displays the reports
         */
        fun generateReports() {
            // Check if dataset is not loaded yet
            if (dataset == null) {
                println("ERROR: Dataset is not loaded.")
                return
            }

            println("Generating reports...")
            println("Output saved to individual files...")
            println()

            println("Report 1: Banking Privilege Tiers")
            val avgTiers = averageDailyBalance()
            print(table {
                header("AccountName", "AccountAge", "CurrentBalance", "AverageDailyBalance", "PrivilegeTier")

                row(
                    avgTiers.first().accountName,
                    avgTiers.first().accountAge,
                    avgTiers.first().currentBalance,
                    avgTiers.first().averageDailyBalance,
                    avgTiers.first().privilegeTier,
                )

                row(
                    avgTiers[1].accountName,
                    avgTiers[1].accountAge,
                    avgTiers[1].currentBalance,
                    avgTiers[1].averageDailyBalance,
                    avgTiers[1].privilegeTier
                )

                hints {
                    precision("CurrentBalance", 2)
                    precision("AverageDailyBalance", 2)
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
            }.render(StringBuilder()))
            println()
            println("(Full table exported to report1_banking_privilege_tiers.csv)")

            println("Report 2: Interest Expense Projection")
            val interest = interestExpenseProjection()
            print(table {
                header("AccountName", "CurrentBalance", "ExpectedInterest",)

                row(
                    interest.first().accountName,
                    interest.first().currentBalance,
                    interest.first().expectedInterest,
                )

                row(
                    interest[1].accountName,
                    interest[1].currentBalance,
                    interest[1].expectedInterest,
                )

                hints {
                    precision("CurrentBalance", 2)
                    precision("ExpectedInterest", 2)
                    postfix("ExpectedInterest", "%")
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
            }.render(StringBuilder()))
            println()
            println("(Full table exported to report2_interest_expense_projection.csv)")

            println("Report 3: Suspicious Client Behavior")
            val suspicious = suspiciousClientBehaviour()
            print(table {
                header(
                    "AccountName",
                    "Date",
                    "TotalWithdrawalAmounts",
                    "NumberOfDeposits",
                    "TotalDepositAmounts",
                    "TurnoverVelocity",
                    "RiskFlagLevel"
                )

                row(
                    suspicious.first().accountName,
                    suspicious.first().date,
                    suspicious.first().totalWithdrawalAmounts,
                    suspicious.first().numberOfDeposits,
                    suspicious.first().totalDepositAmounts,
                    suspicious.first().turnoverVelocity,
                    suspicious.first().riskFlagLevel,
                )

                row(
                    suspicious[1].accountName,
                    suspicious[1].date,
                    suspicious[1].totalWithdrawalAmounts,
                    suspicious[1].numberOfDeposits,
                    suspicious[1].totalDepositAmounts,
                    suspicious[1].turnoverVelocity,
                    suspicious[1].riskFlagLevel,
                )

                hints {
                    precision("TotalWithdrawalAmounts", 2)
                    precision("TotalDepositAmounts", 2)
                    precision("TurnoverVelocity", 2)
                    postfix("TurnoverVelocity", "%")
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
            }.render(StringBuilder()))
            println()
            println("(Full table exported to report3_suspicious_client_behavior.csv)")

            println("Report 4: Total Transaction Activity per account, last month")
            val total = transactionActivity()
            print(table {
                header("AccountNumber", "TotalTransactions",)

                row(
                    total.first().first,
                    total.first().second,
                )

                row(
                    total[1].first,
                    total[1].second,
                )

                hints {
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
            }.render(StringBuilder()))
            println()
            println("(Full table exported to report2_interest_expense_projection.csv)")

            println()
            println("Summary State (summary.json):")
            val summary = summary(
                totalInterestExpense = interest.sumOf { it.expectedInterest },
                totalSuspicious = suspicious.count { it.riskFlagLevel == RiskFlagLevel.Suspicious },
                totalNormal = suspicious.count { it.riskFlagLevel == RiskFlagLevel.Suspicious },
                averageDailyBalance = avgTiers
            )
            println(summary)
        }
    }
}