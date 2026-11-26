package BankApp

/********************
 * Last names: Avila
 * Language: Kotlin
 * Paradigm(s): Object-Oriented
 ********************/

import java.math.BigDecimal
import java.math.RoundingMode
import java.math.MathContext
import kotlin.text.toBigDecimalOrNull

/**
    Reads and trims input. Returns an empty string if the input would cause an exception.

    @return the trimmed console input.
 */
fun readTrim(): String {
    return readlnOrNull()?.trim() ?: ""
}

/**
    Checks if the string is an exit code. Returns true if it is.

    @param[input] the string to check.
    @return true if the string is an exit code, false otherwise.
 */
fun isExit(input: String): Boolean {
    when(input.lowercase()){
        "n", "[n]", "cancel" -> {
            return true
        }
        else -> {
            return false
        }
    }
}

/**
    Checks if the string is an accept code. Returns true if it is.

    @param[input] the string to check.
    @return true if the string is an accept code, false otherwise.
 */
fun isAccept(input: String): Boolean {
    when(input.lowercase()){
        "y", "[y]", "yes" -> {
            return true
        }
        else -> {
            return false
        }
    }
}

/**
    Redirects inputs to subfunctions depending on the current Screen the user is on.

    @param[model] the basis model.
 */
fun processInput(model: Model){
    when(model.screen){
        Screen.MENU -> processMenu(model)
        Screen.REGISTER -> processRegisterAccount(model)
        Screen.DEPOSIT -> processDeposit(model)
        Screen.WITHDRAW -> processWithdraw(model)
        Screen.RECORDRATE -> processRecordRate(model)
        Screen.CURRENCYEXCHANGE -> processCurrencyExchange(model)
        Screen.INTEREST -> processInterest(model)
        Screen.REPORTS -> processReports(model)
    }
}

/**
    Redirects the Screen depending on the user's selection. Accepts both Number and [Number].

    @param[model] the basis model.
 */
fun processMenu(model: Model){
    var input: String = readTrim().removeSurrounding("[","]")
    if (isExit(input)){
        model.exit()
    }
    when(input){
        "1" -> {
            model.screen = Screen.REGISTER
        }
        "2" -> {
            model.screen = Screen.DEPOSIT
        }
        "3" -> {
            model.screen = Screen.WITHDRAW
        }
        "4" -> {
            model.screen = Screen.CURRENCYEXCHANGE
        }
        "5" -> {
            model.screen = Screen.RECORDRATE
        }
        "6" -> {
            model.screen = Screen.INTEREST
        }
        "7" -> {
            model.screen = Screen.REPORTS
        }
        "8" -> {
            model.exit()
        }
        else -> {
            printError("ERROR: Invalid input, not one of the given options.","")
        }
    }
    when(input){
        "2","3","6" -> {
            if (!checkAccountExistence(model)){
                model.screen = Screen.MENU
            }
        }
    }
    println()
}

/**
    Register a new account. Takes a string as an input and saves it as the Account Name.
    Registered accounts start with an empty wallet of default currency PHP.
    An account can't be registered with an empty string or an exit code.
    An account can't be registered under an existing name.

    @param[model] the basis model.
 */
fun processRegisterAccount(model: Model){
    var input: String = readTrim()

    if (input == ""){
        printError("ERROR: Account Name must not be empty.", "Register another account (Y/N)?: ")
        var confirm: String = readTrim()
        while(!isAccept(confirm) && !isExit(confirm)){
            print("Register another account (Y/N)?: ")
            confirm = readTrim()
        }
        if (isExit(confirm)){
            model.screen = Screen.MENU
        }
        return
    } else if (isExit(input)){
        model.screen = Screen.MENU
        return
    } else if (input.contains(Regex("\\d"))) {
        printError("ERROR: Account can't have any digits.", "Register another account (Y/N)?: ")
        var confirm: String = readTrim()
        while(!isAccept(confirm) && !isExit(confirm)){
            print("Register another account (Y/N)?: ")
            confirm = readTrim()
        }
        if (isExit(confirm)){
            model.screen = Screen.MENU
        }
        return
    }  else if (model.checkAccount(input) != null){
        printError("ERROR: Account already exists.", "Register another account (Y/N)?: ")
        var confirm: String = readTrim()
        while(!isAccept(confirm) && !isExit(confirm)){
            print("Register another account (Y/N)?: ")
            confirm = readTrim()
        }
        if (isExit(confirm)){
            model.screen = Screen.MENU
        }
        return
    } else {
        while (true) {
            var inputAmount: String = ""
            print("Initial Deposit: ")
            inputAmount = readTrim()
            if (!checkBigDecimalInput(inputAmount)){
                var confirm: String = readTrim()
                while(!isAccept(confirm) && !isExit(confirm)){
                    print("Try another amount (Y/N)?: ")
                    confirm = readTrim()
                }
                if (isExit(confirm)){
                    model.screen = Screen.MENU
                    return
                }
            }else{
                val initialDeposit: BigDecimal = processBigDecimalInput(inputAmount)
                model.registerAccount(name = input, initialDeposit = initialDeposit)
                promptReturnToMenu(model)
                return
            }
        }
    }
}

/**
    Prompts the user if they wish to return to the Menu or do another transaction.

    @param[model] the basis model.
 */
fun promptReturnToMenu(model: Model){
    println()
    print("Back to the Main Menu (Y/N): ")
    var input: String = readTrim()
    while(!isAccept(input) && !isExit(input)){
        print("Back to the Main Menu (Y/N)?: ")
        input = readTrim()
    }
    if (!isExit(input)){
        model.screen = Screen.MENU
    }
}

/**
    Deposits a positive value with a maximum scale of 2 decimal points to an existing account.

    @param[model] the basis model.
 */
fun processDeposit(model: Model){
    var inputName: String = promptAccount(model)
    if (inputName == ""){
        return
    }

    var validDeposit = false
    var inputAmount: String = ""
    do{
        print("\nDeposit Amount: ")
        inputAmount = readTrim()
        if (!checkBigDecimalInput(inputAmount)){
            var confirm: String = readTrim()
            while(!isAccept(confirm) && !isExit(confirm)){
                print("Try another amount (Y/N)?: ")
                confirm = readTrim()
            }
            if (isExit(confirm)){
                model.screen = Screen.MENU
                return
            }
        }else{
            validDeposit = true
        }
    } while (!validDeposit)

    if (validDeposit){
        var amount: BigDecimal = processBigDecimalInput(inputAmount)
        //TO DO?: Default Currency PHP
        model.deposit(inputName, amount, Currency.PHP)
        printUpdatedBalance(model, inputName, Currency.PHP)
        promptReturnToMenu(model)
    }
}

/**
    Withdraws a positive value with a maximum scale of 2 decimal points to an existing account.
    It does not accept a withdrawal value that exceeds the account's balance.

    @param[model] the basis model.
 */
fun processWithdraw(model: Model){
    var inputName: String = promptAccount(model)
    if (inputName == ""){
        return
    }

    var validWithdraw = false
    //TO DO?: Default Currency PHP
    var withdrawLimit: BigDecimal = model.checkAccount(inputName)?.checkWallet(Currency.PHP) ?: BigDecimal.ZERO
    var inputAmount: String = ""
    do{
        print("\nWithdraw Amount: ")
        inputAmount = readTrim()
        if (!checkBigDecimalInput(inputAmount)){
            var confirm: String = readTrim()
            while(!isAccept(confirm) && !isExit(confirm)){
                print("Try another amount (Y/N)?: ")
                confirm = readTrim()
            }
            if (isExit(confirm)){
                model.screen = Screen.MENU
                return
            }
        }else{
            if (processBigDecimalInput(inputAmount) <= withdrawLimit){
                validWithdraw = true
            } else {
                printError("ERROR: Amount to withdraw has exceeded the current balance.", "Try another amount (Y/N)?: ")
                var confirm: String = readTrim()
                while(!isAccept(confirm) && !isExit(confirm)){
                    print("Try another amount (Y/N)?: ")
                    confirm = readTrim()
                }
                if (isExit(confirm)){
                    model.screen = Screen.MENU
                    return
                }
            }
        }
    } while (!validWithdraw)

    if (validWithdraw){
        var amount: BigDecimal = processBigDecimalInput(inputAmount)
        //TO DO?: Default Currency PHP
        model.withdraw(inputName, amount, Currency.PHP)
        printUpdatedBalance(model, inputName, Currency.PHP)
        promptReturnToMenu(model)
    }
}

/**
    Records a currency exchange rate for one of the six currencies.
    The value is based on the default currency, PHP.
    It accepts positive values with a maximum scale of 6 decimal places.

    @param[model] the basis model.
 */
fun processRecordRate(model: Model){
    var input: String = readTrim().removeSurrounding("[","]")
    var currency: Currency? = null
    when(input){
        "1" -> {
            currency = Currency.PHP
        }
        "2" -> {
            currency = Currency.USD
        }
        "3" -> {
            currency = Currency.JPY
        }
        "4" -> {
            currency = Currency.GBP
        }
        "5" -> {
            currency = Currency.EUR
        }
        "6" -> {
            currency = Currency.CNY
        }
        else -> {
            currency = null
        }
    }
    if (currency == null){
        printError("ERROR: Invalid input, not one of the given options.","Select another currency (Y/N)?: ")
        var confirm: String = readTrim()
        while(!isAccept(confirm) && !isExit(confirm)){
            print("Select another currency (Y/N)?: ")
            confirm = readTrim()
        }
        if (isExit(confirm)){
            model.screen = Screen.MENU
            return
        }else{
            return
        }
    }


    var validRate = false
    var inputAmount: String = ""
    do{
        print("Exchange Rate: ")
        inputAmount = readTrim()
        if (!checkBigDecimalInputRateScale(inputAmount)){
            var confirm: String = readTrim()
            while(!isAccept(confirm) && !isExit(confirm)){
                print("Try another amount (Y/N)?: ")
                confirm = readTrim()
            }
            if (isExit(confirm)){
                model.screen = Screen.MENU
                return
            }
        }else{
            if (processBigDecimalInputRateScale(inputAmount) != BigDecimal.ZERO){
                validRate = true
            } else {
                printError("ERROR: Can't set rate to 0.", "Try another amount (Y/N)?: ")
                var confirm: String = readTrim()
                while(!isAccept(confirm) && !isExit(confirm)){
                    print("Try another amount (Y/N)?: ")
                    confirm = readTrim()
                }
                if (isExit(confirm)){
                    model.screen = Screen.MENU
                    return
                }
            }
        }
    } while (!validRate)

    if (validRate){
        var amount: BigDecimal = processBigDecimalInputRateScale(inputAmount)
        //TO DO?: Default Currency PHP
        model.changeRate(amount, currency)
        promptReturnToMenu(model)
    }
}

/**
    Prompts for a Source Currency, Source Amount, and Target Currency.
    It will then print the Target Amount which is how much currency you would get from the currency exchange.

    @param[model] the basis model.
 */
fun processCurrencyExchange(model: Model){
    var input: String = readTrim().removeSurrounding("[","]")
    var currency: Currency? = null
    when(input){
        "1" -> {
            currency = Currency.PHP
        }
        "2" -> {
            currency = Currency.USD
        }
        "3" -> {
            currency = Currency.JPY
        }
        "4" -> {
            currency = Currency.GBP
        }
        "5" -> {
            currency = Currency.EUR
        }
        "6" -> {
            currency = Currency.CNY
        }
        else -> {
            currency = null
        }
    }
    if (currency == null){
        printError("ERROR: Invalid input, not one of the given options.","Convert another currency (Y/N)?: ")
        var confirm: String = readTrim()
        while(!isAccept(confirm) && !isExit(confirm)){
            print("Convert another currency (Y/N)?: ")
            confirm = readTrim()
        }
        if (isExit(confirm)){
            model.screen = Screen.MENU
            return
        }else{
            return
        }
    } else if (!model.checkRateExists(currency)) {
        printError("ERROR: You must record the exchange rate first before converting.","Convert another currency (Y/N)?: ")
        var confirm: String = readTrim()
        while(!isAccept(confirm) && !isExit(confirm)){
            print("Convert another currency (Y/N)?: ")
            confirm = readTrim()
        }
        if (isExit(confirm)){
            model.screen = Screen.MENU
            return
        }else{
            return
        }
    }

    var validAmount = false
    var inputAmount: String = ""
    do{
        print("Source Amount: ")
        inputAmount = readTrim()
        if (!checkBigDecimalInput(inputAmount)){
            var confirm: String = readTrim()
            while(!isAccept(confirm) && !isExit(confirm)){
                print("Try another amount (Y/N)?: ")
                confirm = readTrim()
            }
            if (isExit(confirm)){
                model.screen = Screen.MENU
                return
            }
        }else{
            validAmount = true
        }
    } while (!validAmount)

    var validCurr = false
    var input2: String = ""
    var currency2: Currency? = null

    do {
        printCurrencyExchange2()
        input2 = readTrim().removeSurrounding("[","]")
        when(input2){
            "1" -> {
                currency2 = Currency.PHP
            }
            "2" -> {
                currency2 = Currency.USD
            }
            "3" -> {
                currency2 = Currency.JPY
            }
            "4" -> {
                currency2 = Currency.GBP
            }
            "5" -> {
                currency2 = Currency.EUR
            }
            "6" -> {
                currency2 = Currency.CNY
            }
            else -> {
                currency2 = null
            }
        }
        if (currency2 == null){
            printError("ERROR: Invalid input, not one of the given options.","Select another currency (Y/N)?: ")
            var confirm: String = readTrim()
            while(!isAccept(confirm) && !isExit(confirm)){
                print("Select another currency (Y/N)?: ")
                confirm = readTrim()
            }
            if (isExit(confirm)){
                model.screen = Screen.MENU
                return
            }
        } else if (!model.checkRateExists(currency)) {
            printError("ERROR: You must record the exchange rate first before converting.","Convert another currency (Y/N)?: ")
            var confirm: String = readTrim()
            while(!isAccept(confirm) && !isExit(confirm)){
                print("Select another currency (Y/N)?: ")
                confirm = readTrim()
            }
            if (isExit(confirm)){
                model.screen = Screen.MENU
                return
            }else{
                currency2 = null
            }
        } else{
            validCurr = true
        }
    } while(currency2 == null)

    if (validAmount && validCurr){
        var amount: BigDecimal = processBigDecimalInput(inputAmount)
        println("Exchange Amount: ${model.calculateExchange(currency, currency2, amount)}")
        promptReturnToMenu(model)
    }
}

/**
    Calculates the Interest of an account over a user inputted positive number of days.
    Prints the Interest and New Values for each day.
    Rounds off the stored value to 2 decimal places for parity with the other implementations of the banking app.

    @param[model] the basis model.
 */
fun processInterest(model: Model){
    //Magic numbers? whats that?
    var use2ScaleEverywhere = true

    var inputName: String = promptAccount(model)
    if (inputName == ""){
        return
    }
    println("Interest Rate: 5%")

    var validDaycount = false
    var inputDays: String = ""
    var days = -1
    do{
        print("\nTotal Number of Days: ")
        var inputAmount: String = readTrim().lowercase().removeSuffix("days").removeSuffix("day").trim()
        days = inputAmount.toIntOrNull() ?: -1
        if (days <= 0){
            printError("ERROR: Must be a positive number of days", "Try another amount of days (Y/N)?: ")
            var confirm: String = readTrim()
            while(!isAccept(confirm) && !isExit(confirm)){
                print("Try another amount of days (Y/N)?: ")
                confirm = readTrim()
            }
            if (isExit(confirm)){
                model.screen = Screen.MENU
                return
            }
        }else{
            validDaycount = true
        }
    } while (!validDaycount)

    if (validDaycount){
        //TO DO?: Default PHP
        var amount = model.checkAccount(inputName)?.checkWallet(Currency.PHP) ?: BigDecimal.ZERO
        val dailyInterest = BigDecimal.ONE.add(BigDecimal("0.05").divide(BigDecimal("365"), 20, RoundingMode.HALF_UP))
        //Cap of 30 sig fig
        val finalAmount = amount.multiply(dailyInterest.pow(days, MathContext(40)))
        val maxWidth = maxOf(10,finalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString().length)
        val maxWidthDay = maxOf(5,days.toString().length)

        println("${"Day".padEnd(maxWidthDay)} | ${"Interest".padEnd(maxWidth)} | ${"Balance".padEnd(maxWidth)}")
        for (day in 1..days){
            var newAmount = amount.multiply(dailyInterest).setScale(20, RoundingMode.HALF_UP)
            if (use2ScaleEverywhere){
                newAmount = amount.multiply(dailyInterest).setScale(2, RoundingMode.HALF_UP)
            }
            var interest = newAmount - amount
            println("${day.toString().padEnd(maxWidthDay)} | ${interest.setScale(2, RoundingMode.HALF_UP).toPlainString().padEnd(maxWidth)} | ${newAmount.setScale(2, RoundingMode.HALF_UP).toPlainString().padEnd(maxWidth)}")
            amount = newAmount
        }

        promptReturnToMenu(model)
    }
}

fun processReports(model: Model){
    while (true) {
        val input: String = readTrim().removeSurrounding("[","]")
        if (isExit(input)){
            model.exit()
        }
        when(input){
            "1" -> {
                Reports.loadDataset()
            }
            "2" -> {
                Reports.generateReports()
            }
            "3" -> {
                model.screen = Screen.MENU
                break
            }
            else -> {
                printError("ERROR: Invalid input, not one of the given options.","")
            }
        }
        println()
        printReports()
    }
}

/**
    Prompts the user for which account to use.
    Empty account names and Exit code account names can not be created so this returns an empty string for error handling.
    Also returns an empty string if the account is not found.

    @param[model] the basis model.
    @return String containing the account name if valid, an empty String otherwise.
 */
fun promptAccount(model: Model): String{
    var inputName: String = readTrim()
    if (inputName == ""){
        printError("ERROR: Account Name must not be empty.", "Try another account (Y/N)?: ")
        var confirm: String = readTrim()
        while(!isAccept(confirm) && !isExit(confirm)){
            print("Try another account (Y/N)?: ")
            confirm = readTrim()
        }
        if (isExit(confirm)){
            model.screen = Screen.MENU
        }
        return ""
    } else if (isExit(inputName)){
        model.screen = Screen.MENU
        return ""
    } else {
        if (!printBalance(model, inputName, Currency.PHP)){
            printError("ERROR: Account not found.", "Try another account (Y/N)?: ")
            var confirm: String = readTrim()
            while(!isAccept(confirm) && !isExit(confirm)){
                print("Try another account (Y/N)?: ")
                confirm = readTrim()
            }
            if (isExit(confirm)){
                model.screen = Screen.MENU
            }
            return ""
        } else {
            return inputName
        }
    }
}

/**
    Returns the input in BigDecimal form.

    @param[inputAmount] the string to convert to BigDecimal form.
    @return a BigDecimal value matching the string. Returns BigDecimal.ZERO in case it is called without checking validity first and it fails.
 */
fun processBigDecimalInput(inputAmount: String): BigDecimal{
    if (checkBigDecimalInput(inputAmount)){
        return inputAmount.trim().toBigDecimal().setScale(2)
    }else{
        return BigDecimal.ZERO
    }
}

/**
    Checks if the input amount is a permissible BigDecimal value.
    
    @param[input] the string to check the BigDecimal form of.
    @return false if the format is invalid, it is not greater than 0, or has too many decimal numbers. true otherwise.
 */
fun checkBigDecimalInput(input: String): Boolean{
    val amount: BigDecimal? = input.trim().toBigDecimalOrNull()
    if (amount == null){
        printError("ERROR: Invalid number format.", "Try another amount (Y/N)?: ")
        return false
    }
    if (amount <= BigDecimal.ZERO){
        printError("ERROR: Amount must be greater than 0.", "Try another amount (Y/N)?: ")
        return false
    }
    if (amount.stripTrailingZeros().scale() > 2){
        printError("ERROR: Too many decimal numbers.", "Try another amount (Y/N)?: ")
        return false
    }
    return true
}

/**
    Returns the input in BigDecimal form.
    Version that checks a different scale for Recording Exchange Rates.

    @param[inputAmount] the string to convert to BigDecimal form.
    @return a BigDecimal value matching the string. Returns BigDecimal.ZERO in case it is called without checking validity first and it fails.
 */
fun processBigDecimalInputRateScale(inputAmount: String): BigDecimal{
    if (checkBigDecimalInputRateScale(inputAmount)){
        return inputAmount.trim().toBigDecimal().setScale(6)
    }else{
        return BigDecimal.ZERO
    }
}

/**
    Checks if the input amount is a permissible BigDecimal value.
    Version that checks a different scale for Recording Exchange Rates.
    
    @param[input] the string to check the BigDecimal form of.
    @return false if the format is invalid, it is not greater than 0, or has too many decimal numbers. true otherwise.
 */
fun checkBigDecimalInputRateScale(input: String): Boolean{
    val amount: BigDecimal? = input.trim().toBigDecimalOrNull()
    if (amount == null){
        printError("ERROR: Invalid number format.", "Try another amount (Y/N)?: ")
        return false
    }
    if (amount <= BigDecimal.ZERO){
        printError("ERROR: Amount must be greater than 0.", "Try another amount (Y/N)?: ")
        return false
    }
    if (amount.stripTrailingZeros().scale() > 6){
        printError("ERROR: Too many decimal numbers.", "Try another amount (Y/N)?: ")
        return false
    }
    return true
}

/**
    Checks if any account exists in the model.
    Prints an error message if there is none.

    @param[model] the basis model.
    @return true if an account exists, false otherwise.
 */
fun checkAccountExistence(model: Model): Boolean{
    if (model.accExists()){
        return true
    }else{
        println("\n\nERROR: This transaction is unavailable until atleast one account has been registered.")
        return false
    }
}