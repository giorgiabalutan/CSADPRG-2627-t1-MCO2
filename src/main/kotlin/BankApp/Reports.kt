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
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.collections.forEach

enum class PrivilegeTier {
    Silver,
    Gold,
    Platinum,
}

enum class RiskFlagLevel {
    Suspicious,
    Normal
}

data class AvgDailyBalanceClass (
    val accountName: String,
    val accountAge: Int,
    val currentBalance: BigDecimal,
    val averageDailyBalance: BigDecimal,
    val privilegeTier: PrivilegeTier,
)

data class InterestExpenseClass (
    val accountName: String,
    val currentBalance: BigDecimal,
    val expectedInterest: BigDecimal,
)

data class SuspiciousClientClass (
    val accountName: String,
    val date: LocalDate,
    val totalWithdrawalAmounts: BigDecimal,
    val numberOfDeposits: Int,
    val totalDepositAmounts: BigDecimal,
    val turnoverVelocity: Double,
    val riskFlagLevel: RiskFlagLevel,
)

@OptIn(ExperimentalSerializationApi::class)
private fun BigDecimal.toJsonNumber(): JsonPrimitive =
    JsonUnquotedLiteral(this.setScale(2, RoundingMode.HALF_UP).toPlainString())

class Reports {
    companion object {
        var dataset: MutableMap<String, MutableList<Transaction>>? = null

        fun averageDailyBalanceLastMonth(
            transactions: List<Transaction>,
            now: LocalDateTime = LocalDateTime.now()
        ): BigDecimal {
            val endDate = now.toLocalDate()
            val startDate = endDate.minusMonths(1)

            val endOfDay = transactions
                .filter { !it.dateTime.isAfter(now) }
                .sortedBy { it.dateTime }
                .associate { it.dateTime.toLocalDate() to it.current_balance }

            var balance = endOfDay.filterKeys { it < startDate }
                .maxByOrNull { it.key }?.value ?: BigDecimal.ZERO

            val days = startDate.datesUntil(
                endDate.plusDays(1)
            ).toList()

            val total = days.fold(BigDecimal.ZERO) { sum, day ->
                balance = endOfDay[day] ?: balance
                sum + balance
            }

            return total.divide(
                BigDecimal(days.size),
                2,
                RoundingMode.HALF_UP
            )
        }

        fun averageDailyBalance(
        ): List<AvgDailyBalanceClass> {
            val now = LocalDateTime.now()
            val averageDailyBalanceList: MutableList<AvgDailyBalanceClass> = mutableListOf()
            dataset?.forEach { (_, transactions) ->
                val accountName = transactions.first().account_name
                val accountAge = ChronoUnit.DAYS.between(
                    transactions.first().dateTime,
                    transactions.last().dateTime
                ).toInt()
                val currentBalance = transactions.last().current_balance
                val averageDailyBalance = averageDailyBalanceLastMonth(transactions, now)
                val privilegeTier = if (averageDailyBalance < BigDecimal(500000.00))
                    PrivilegeTier.Silver else if (averageDailyBalance < BigDecimal(1000000.00))
                    PrivilegeTier.Gold else PrivilegeTier.Platinum

                averageDailyBalanceList.add(AvgDailyBalanceClass(
                    accountName = accountName,
                    accountAge = accountAge,
                    currentBalance = currentBalance,
                    averageDailyBalance = averageDailyBalance,
                    privilegeTier = privilegeTier,
                ))
            }

            val sorted = averageDailyBalanceList.sortedWith(
                compareByDescending<AvgDailyBalanceClass> { it.averageDailyBalance }
            )

            val sortedRows = sorted.map { entry -> listOf(
                entry.accountName,
                entry.accountAge.toString(),
                entry.currentBalance.toString(),
                entry.averageDailyBalance.toString(),
                entry.privilegeTier.toString(),
            ) }

            val allRows: List<List<String>> = listOf(
                listOf(
                    "AccountName",
                    "AccountAge",
                    "CurrentBalance",
                    "AverageDailyBalance",
                    "PrivilegeTier",
                )
            ) + sortedRows

            FileOutputStream(File("report1_banking_privilege_tiers.csv")).use { stream ->
                writer.write(allRows, stream)
            }

            return sorted
        }

        fun interestExpenseProjection(
        ): List<InterestExpenseClass> {
            val interestExpenseList: MutableList<InterestExpenseClass> = mutableListOf()
            dataset?.forEach { (_, transactions) ->
                val accountName = transactions.last().account_name
                val currentBalance = transactions.last().current_balance

                if (currentBalance < BigDecimal(10000.00)) return@forEach

                val annualRate = BigDecimal("0.05")
                val dailyRate = annualRate.divide(BigDecimal(365), 10, RoundingMode.HALF_UP)

                var balance = currentBalance
                repeat(30) {
                    balance += balance * dailyRate
                }

                val actualInterest = (balance - currentBalance).setScale(2, RoundingMode.HALF_UP)

                interestExpenseList.add(
                    InterestExpenseClass(
                        accountName = accountName,
                        currentBalance = currentBalance,
                        expectedInterest = actualInterest.setScale(2, RoundingMode.HALF_UP),
                    )
                )
            }

            val sorted = interestExpenseList.sortedWith(
                compareByDescending<InterestExpenseClass> { it.expectedInterest }
            )

            val sortedRows = sorted.map { entry -> listOf(
                entry.accountName,
                entry.currentBalance.toString(),
                entry.expectedInterest.toString(),
            ) }

            val allRows: List<List<String>> = listOf(
                listOf(
                    "AccountName",
                    "CurrentBalance",
                    "ExpectedInterest",
                )
            ) + sortedRows

            FileOutputStream(File("report2_interest_expense_projection.csv")).use { stream ->
                writer.write(allRows, stream)
            }

            return sorted
        }

        fun suspiciousClientBehaviour(
        ): List<SuspiciousClientClass> {
            val results = mutableListOf<SuspiciousClientClass>()

            dataset?.forEach { (_, transactions) ->
                if (transactions.isEmpty()) return@forEach
                val accountName = transactions.first().account_name

                transactions.groupBy { it.dateTime.toLocalDate() }.forEach { (date, dayTx) ->
                    val deposits = dayTx.filter { it.type == Transactions.Deposit }
                    val totalDepositAmounts = deposits.sumOf { it.amount }
                    val totalWithdrawalAmounts = dayTx
                        .filter { it.type == Transactions.Withdrawal }
                        .sumOf { it.amount }

                    val turnoverVelocity = if (totalWithdrawalAmounts.signum() == 0) {
                        0.0
                    } else {
                        totalDepositAmounts
                            .divide(totalWithdrawalAmounts, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal(100))
                            .setScale(2, RoundingMode.HALF_UP)
                            .toDouble()
                    }

                    val riskFlagLevel =
                        if (totalWithdrawalAmounts >= BigDecimal("500000.00") || turnoverVelocity >= 90.0)
                            RiskFlagLevel.Suspicious
                        else
                            RiskFlagLevel.Normal

                    results.add(
                        SuspiciousClientClass(
                            accountName = accountName,
                            date = date,
                            totalWithdrawalAmounts = totalWithdrawalAmounts,
                            numberOfDeposits = deposits.size,
                            totalDepositAmounts = totalDepositAmounts,
                            turnoverVelocity = turnoverVelocity,
                            riskFlagLevel = riskFlagLevel,
                        )
                    )
                }
            }

            val sorted = results.sortedWith(
                compareBy<SuspiciousClientClass> { it.date }
                    .thenByDescending { it.totalWithdrawalAmounts }
            )

            val sortedRows = sorted.map { entry -> listOf(
                entry.accountName,
                entry.date.toString(),
                entry.totalWithdrawalAmounts.toString(),
                entry.numberOfDeposits.toString(),
                entry.totalDepositAmounts.toString(),
                entry.turnoverVelocity.toString(),
                entry.riskFlagLevel.toString(),
            ) }

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

            FileOutputStream(File("report3_suspicious_client_behavior.csv")).use { stream ->
                writer.write(allRows, stream)
            }

            return sorted
        }

        fun transactionActivity(): List<Pair<String, Int>> {
            val sorted = dataset
                ?.map { (accountNumber, transactions) -> accountNumber to transactions.size }
                ?.sortedWith(
                    compareByDescending<Pair<String, Int>> { it.second }
                        .thenByDescending { it.first }
                )
                ?: emptyList()

            val allRows: List<List<String>> =
                listOf(listOf("AccountNumber", "TotalTransactions")) +
                        sorted.map { (accountNumber, count) -> listOf(accountNumber, count.toString()) }

            FileOutputStream(File("report4_total_transaction_activity.csv")).use { stream ->
                writer.write(allRows, stream)
            }

            return sorted
        }

        fun summary(
            totalInterestExpense: BigDecimal,
            totalSuspicious: Int
        ): String {
            var totalAccounts = 0
            var totalDeposits = 0
            var totalWithdrawals = 0
            var totalBalance = BigDecimal("0.0")

            dataset?.forEach { (_, transactions) ->
                totalAccounts++
                totalBalance += transactions.last().current_balance
                transactions.forEach { transaction ->
                    when (transaction.type) {
                        Transactions.Deposit -> totalDeposits++
                        Transactions.Withdrawal -> totalWithdrawals++
                    }
                }
            }

            val jsonObject = buildJsonObject {
                put("total_accounts", totalAccounts)
                put("total_deposits", totalDeposits)
                put("total_withdrawals", totalWithdrawals)
                put("total_balance", totalBalance.toJsonNumber())
                put("total_interest_expense", totalInterestExpense.toJsonNumber())
                put("total_suspicious", totalSuspicious)
            }

            val prettyJson = Json { prettyPrint = true }
            val formattedString = prettyJson.encodeToString(JsonObject.serializer(), jsonObject)
            File("summary.json").writeText(formattedString)

            return jsonObject.toString()
        }

        fun loadDataset() {
            val (validTransactions, invalidTransactions) = Model.readTransactions()
            println("Processing dataset... (${validTransactions.entries.sumOf { it.value.size } + invalidTransactions} rows loaded, ${validTransactions.entries.sumOf { it.value.size }} filtered for PHP currency)")
            dataset = validTransactions
        }

        fun generateReports() {
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
                totalSuspicious = suspicious.filter{ it.date == LocalDate.now() }.count { it.riskFlagLevel == RiskFlagLevel.Suspicious }
            )
            println(summary)
        }
    }
}