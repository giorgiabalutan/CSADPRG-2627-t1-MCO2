package BankApp

import java.math.BigDecimal
import java.math.RoundingMode

/**
    Helper function to print error messages for feedbacking user input.

    @param[errorMessage] a string describing the error.
    @param[prompt] a string prompting the user for the next input. Usually asking if they wish to retry.
 */
fun printError(errorMessage: String, prompt: String){
    println(errorMessage)
    println()
    print(prompt)
}

/**
    Helper function to redirect the main loop's print function to the proper print function based on the current screen.

    @param[screen] the screen enum the model is currently on.
 */
fun printScreen(screen: Screen){
    when(screen){
        Screen.MENU -> printMenu()
        Screen.REGISTER -> printRegisterAccount()
        Screen.DEPOSIT -> printDeposit()
        Screen.WITHDRAW -> printWithdraw()
        Screen.RECORDRATE -> printRecordRate()
        Screen.CURRENCYEXCHANGE -> printCurrencyExchange()
        Screen.INTEREST -> printInterest()
        Screen.REPORTS -> printReports()
    }
}

/**
    Prints the menu options.
 */
fun printMenu(){
    println()
    println("Select Transaction:")
    println("[1] Register Account")
    println("[2] Deposit Amount")
    println("[3] Withdraw Amount")
    println("[4] Currency Exchange")
    println("[5] Record Exchange Rates")
    println("[6] Show Interest Amount")
    println("[7] Generate Reports")
    println("[8] Quit")
    println()
    print("Choice: ")
}

/**
    Prints the initial prompt for registering an account.
 */
fun printRegisterAccount(){
    println("Register Account Name")
    print("Account Name: ")
}

/**
    Prints the initial prompt for depositing into an account.
 */
fun printDeposit(){
    println("Deposit Amount")
    print("Account Name: ")
}

/**
    Prints the initial prompt for withdrawing from an account.
 */
fun printWithdraw(){
    println("Withdraw Amount")
    print("Account Name: ")
}

/**
    Prints the balance of an account.
    Returns False if the account is not found to allow for the caller to handle it instead.
    NOTE: Different currencies for balance was not implemented so currency is being supplied a default value of Currency.PHP and is not supplied anything else in the implementation.

    @param[model] the basis model.
    @param[name] the name of the account.
    @param[currency] the currency to check.

    @return a Boolean indicating if the account was succesfully found (true) or not (false).
 */
fun printBalance(model: Model, name: String, currency: Currency = Currency.PHP): Boolean {
    val account: Account? = model.accounts[name]
    if (account == null){
        return false
    }
    val balance: BigDecimal? = account.checkWallet(currency)
    if (balance == null){
        return false
    }else{
        println("Current Balance: $balance")
        println("Currency: $currency")
    }
    return true
}

/**
    Prints the Updated Balance of an account.

    NOTE: Different currencies for balance was not implemented so currency is being supplied a default value of Currency.PHP and is not supplied anything else in the implementation.

    @param[model] the basis model.
    @param[name] the name of the account.
    @param[currency] the currency to check.
 */
fun printUpdatedBalance(model: Model, name: String, currency: Currency){
    val account: Account? = model.accounts[name]
    if (account == null){
        return
    }
    val balance: BigDecimal? = account.checkWallet(currency)
    if (balance == null){
        return
    }else{
        println("Updated Balance: $balance")
    }
}

/**
    Prints the initial prompt for recording an exchange rate.
 */
fun printRecordRate(){
    println("Record Exchange Rate")
    println()
    println("[1] ${Currency.PHP.currencyName} (${Currency.PHP.name})")
    println("[2] ${Currency.USD.currencyName} (${Currency.USD.name})")
    println("[3] ${Currency.JPY.currencyName} (${Currency.JPY.name})")
    println("[4] ${Currency.GBP.currencyName} (${Currency.GBP.name})")
    println("[5] ${Currency.EUR.currencyName} (${Currency.EUR.name})")
    println("[6] ${Currency.CNY.currencyName} (${Currency.CNY.name})")
    println()
    print("Select Foreign Currency: ")
}

/**
    Prints the initial prompt for currency exchange.
 */
fun printCurrencyExchange(){
    println("Foreign Currency Exchange")
    println("Source Currency Option:")
    println("[1] Philippine Peso (PHP)")
    println("[2] United States Dollar (USD)")
    println("[3] Japanese Yen (JPY)")
    println("[4] British Pound Sterling (GBP)")
    println("[5] Euro (EUR)")
    println("[6] Chinese Yuan Renminni (CNY)")
    println()
    print("Source Currency: ")
}

/**
    Prints the second currency prompt for currency exchange.
 */
fun printCurrencyExchange2(){
    println()
    println("Exchanged Currency Options:")
    println("[1] Philippine Peso (PHP)")
    println("[2] United States Dollar (USD)")
    println("[3] Japanese Yen (JPY)")
    println("[4] British Pound Sterling (GBP)")
    println("[5] Euro (EUR)")
    println("[6] Chinese Yuan Renminni (CNY)")
    println()
    print("Exchange Currency: ")
}

/**
    Prints the initial prompt for checking interest.
 */
fun printInterest(){
    println("Show Interest Amount")
    print("Account Name: ")
}

//Unused
/**
    Prints the resulting currency exchange value for all currencies.
    NOTE: Was used for Milestone 1 but is no longer needed for MCO1.

    @param[model] the basis model.
    @param[amount] the BigDecimal value to be converted.
 */
fun printCurrencyExchangeResults(model: Model, amount: BigDecimal){
    println()
    println("Exchanged Currency")
    for((i, currency) in Currency.entries.withIndex()){
        val rate = model.exchangeRates.getValue(currency)
        val converted = (amount * rate).setScale(2, RoundingMode.HALF_UP)
        println("[${i+1}] ${currency.currencyName} (${currency.name}) = $converted")
    }
    // println("[1] ${Currency.PHP.currencyName} (${Currency.PHP.name}) = ${amount * model.exchangeRates.getValue(Currency.PHP).setScale(2, RoundingMode.HALF_UP)}")
    // println("[2] ${Currency.USD.currencyName} (${Currency.USD.name}) = ${amount * model.exchangeRates.getValue(Currency.USD).setScale(2, RoundingMode.HALF_UP)}")
    // println("[3] ${Currency.JPY.currencyName} (${Currency.JPY.name}) = ${amount * model.exchangeRates.getValue(Currency.JPY).setScale(2, RoundingMode.HALF_UP)}")
    // println("[4] ${Currency.GBP.currencyName} (${Currency.GBP.name}) = ${amount * model.exchangeRates.getValue(Currency.GBP).setScale(2, RoundingMode.HALF_UP)}")
    // println("[5] ${Currency.EUR.currencyName} (${Currency.EUR.name}) = ${amount * model.exchangeRates.getValue(Currency.EUR).setScale(2, RoundingMode.HALF_UP)}")
    // println("[6] ${Currency.CNY.currencyName} (${Currency.CNY.name}) = ${amount * model.exchangeRates.getValue(Currency.CNY).setScale(2, RoundingMode.HALF_UP)}")
}

fun printReports(){
    println("Generate Reports")
    println()
    println("[1] Load the file")
    println("[2] Generate Reports")
    println("[3] Exit Menu")
    print("Enter choice: ")
}
