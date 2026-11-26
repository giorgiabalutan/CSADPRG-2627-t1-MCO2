package BankApp

import com.jsoizo.kotlincsv.csvReader
import com.jsoizo.kotlincsv.csvWriter
import com.jsoizo.kotlincsv.writer.write
import kotlin.collections.listOf
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CSV {
    companion object {
        val reader = csvReader()
        val writer = csvWriter()

        fun writeTransaction(account: Account, currency: Currency, amount: BigDecimal, type: Transactions) {
            val row = listOf(
                listOf(
                    account.account_name,
                    account.account_number,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    currency.name,
                    amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    type.transactionName,
                    account.checkWallet(currency).toString(),
                )
            )

            FileOutputStream(File(CSV_FILEPATH), true).use { stream ->
                writer.write(row, stream)
            }
        }
    }
}