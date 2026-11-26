package BankApp

import BankApp.CSV.Companion.writer
import com.jsoizo.kotlincsv.writer.write
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.math.BigDecimal
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.TimeZone
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

class Reports {
    fun loadDataset(): MutableMap<String, MutableList<Transaction>> {
        val (validTransactions, invalidTransactions) = Model.readTransactions()
        println("Processing dataset... (${validTransactions.size + invalidTransactions} rows loaded, ${validTransactions.size} filtered for PHP currency)")
        return validTransactions
    }

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
        dataset: MutableMap<String, MutableList<Transaction>>
    ): List<AvgDailyBalanceClass> {
        val now = LocalDateTime.now()
        val averageDailyBalanceList: MutableList<AvgDailyBalanceClass> = mutableListOf()
        dataset.forEach { (_, transactions) ->
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

        FileOutputStream(File("average_daily_balance.csv")).use { stream ->
            writer.write(allRows, stream)
        }

        return sorted
    }

    fun interestExpenseProjection(
        dataset: MutableMap<String, MutableList<Transaction>>
    ): List<InterestExpenseClass> {
        val interestExpenseList: MutableList<InterestExpenseClass> = mutableListOf()
        dataset.forEach { (_, transactions) ->
            val accountName = transactions.last().account_name
            val currentBalance = transactions.last().current_balance

            if (currentBalance < BigDecimal("10,000.00")) return@forEach

            var currentInterest = BigDecimal.ZERO
            for (_ in 1..30) {
                currentInterest *= BigDecimal(0.05).divide(BigDecimal(365.0) , 2, RoundingMode.HALF_UP)
            }
            val actualInterest = currentBalance - currentInterest

            interestExpenseList.add(
                InterestExpenseClass(
                    accountName = accountName,
                    currentBalance = currentBalance,
                    expectedInterest = actualInterest,
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

        FileOutputStream(File("expected_interest.csv")).use { stream ->
            writer.write(allRows, stream)
        }

        return sorted
    }

    fun suspiciousClientBehaviour(
        dataset: MutableMap<String, MutableList<Transaction>>
    ): List<SuspiciousClientClass> {
        var dailySuspiciousClient: MutableList<SuspiciousClientClass> = mutableListOf()
        var earliestDay = LocalDate.now()
        dataset.forEach { (_, transactions) ->
            if (earliestDay.isBefore(transactions.first().dateTime.toLocalDate()))
                earliestDay = transactions.first().dateTime.toLocalDate()
        }

        earliestDay.datesUntil(LocalDate.now()).forEach { date ->
            dataset.forEach { (_, transactions) ->
                val accountName = transactions.first().account_name
                val allTransactions = transactions.filter { it.dateTime.toLocalDate() == date }
                val numberOfDeposits = allTransactions.filter({ it.type == Transactions.Deposit }).size
                val totalDepositAmounts = allTransactions.filter({ it.type == Transactions.Deposit }).sumOf { it.amount }
                val totalWithdrawalAmounts = allTransactions.filter({ it.type == Transactions.Withdraw }).sumOf { it.amount }
                val turnoverVelocity = (totalDepositAmounts / totalWithdrawalAmounts).multiply(BigDecimal(100.0)).setScale(2, RoundingMode.HALF_UP).toDouble();
                val riskFlagLevel = if (
                    totalWithdrawalAmounts >= BigDecimal(500000.00) ||
                    turnoverVelocity >= 90.0
                    ) RiskFlagLevel.Suspicious else RiskFlagLevel.Normal

                dailySuspiciousClient.add(
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

        val sorted = dailySuspiciousClient.sortedWith(
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

        FileOutputStream(File("suspicious_clients.csv")).use { stream ->
            writer.write(allRows, stream)
        }

        return sorted
    }

    fun transactionActivity(
        dataset: MutableMap<String, MutableList<Transaction>>
    ): List<Pair<String, Int>> {
        val sorted = dataset
            .map { (accountNumber, transactions) -> accountNumber to transactions.size }
            .sortedWith(
                compareByDescending<Pair<String, Int>> { it.second }
                    .thenByDescending { it.first }
            )

        val sortedRows = sorted.map { (accountNumber, count) -> listOf(accountNumber, count.toString()) }

        val allRows: List<List<String>> = listOf(listOf("account_number", "total_transactions")) + sortedRows

        FileOutputStream(File("transaction_activity.csv")).use { stream ->
            writer.write(allRows, stream)
        }

        return sorted
    }

    fun summary(
        dataset: MutableMap<String, MutableList<Transaction>>,
        totalInterestExpense: BigDecimal,
        totalSuspicious: Int
    ) {
        var totalAccounts = 0
        var totalDeposits = 0
        var totalWithdrawals = 0
        var totalBalance = BigDecimal("0.0")

        dataset.forEach { (_, transactions) ->
            totalAccounts++
            totalBalance += transactions.last().current_balance
            transactions.forEach { transaction ->
                when (transaction.type) {
                    Transactions.Deposit -> totalDeposits++
                    Transactions.Withdraw -> totalWithdrawals++
                }
            }
        }

        val jsonObject = buildJsonObject {
            put("total_accounts", totalAccounts)
            put("total_deposits", totalDeposits)
            put("total_withdrawals", totalWithdrawals)
            put("total_balance", totalBalance)
            put("total_interest_expense", totalInterestExpense)
            put("total_suspicious", totalSuspicious)
        }

        println(jsonObject.toString())

        val prettyJson = Json { prettyPrint = true }
        val formattedString = prettyJson.encodeToString(JsonObject.serializer(), jsonObject)
        File("summary.json").writeText(formattedString)
    }
}