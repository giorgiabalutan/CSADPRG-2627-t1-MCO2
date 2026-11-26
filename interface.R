library(readr)
library(dplyr)
library(knitr)
source("dataProcessing.R")

#Checks for cancel in a string input
get_stringInput <- function(prompt)
{ 
  repeat {
    input <- readline(prompt)
    isEmptyString <- nzchar(trimws(input)) # Checks if the string is empty (just spaces)
    noDigitsString <- grepl("[0-9]", input) # Returns TRUE if digit is present
    
    if (tolower(input) %in% 'cancel') {
      return (NULL)
    } 
    
    if (isEmptyString & !noDigitsString) {
      return (input)
    } else {
      cat("ERROR: Invalid input.\n")
    }
  }
  
  
}

#Accepts a numeric input (invalid if its a string, except for cancel)
#Checks if input is inside a provided range
#Checks if input is either int or double since int is mainly used for choices
get_numericInput <- function(prompt, type, range) {
  #The prompt is repeated if an input is not valid
  repeat {
    raw_input <- readline(prompt)
    
    if(tolower(raw_input) %in% 'cancel') {
      return (NULL)
    } 
    
    numeric_input <- suppressWarnings(as.numeric(raw_input))
    if (type == 'int') {
      int_input <- suppressWarnings(as.numeric(numeric_input)) 
      if(!is.na(int_input) & int_input == as.integer(int_input) & between(int_input, range[1], range[2])) {
        return(as.integer(int_input))
      } else {
        cat("ERROR: Invalid input.\n")
    } 
    
    } else if (type == "double"){
      double_input <- suppressWarnings(as.double(numeric_input)) 
      if(!is.na(double_input) & double_input > 0) {
        return(double_input)
      } else {
        cat("ERROR: Invalid input.\n")
      }
    }
  }
}

#Prompts the user for Y/N questions
y_n_prompt <- function(prompt) {
  repeat {
    input <- readline(prompt)
    if (toupper(input) == 'N') {
      return (FALSE)
    } else if (toupper(input) == "Y"){
      return (TRUE)
    } else {
      cat("ERROR: Invalid input.\n")
    }
      
  }
  
}

#Main interface of the program
#Allows the user to enter other interfaces
menuInterface <- function(){
  bQuit <- FALSE
  
  #Allows this interface to continue running even after exiting another interface
  while (!bQuit)
  {
    cat("\n")
    cat("Main Menu\n")
    cat("Select Transaction: \n")
    cat("\n")
    cat("[1] Register Account Name\n")
    cat("[2] Deposit Amount\n")
    cat("[3] Withdraw Amount\n")
    cat("[4] Currency Exchange\n")
    cat("[5] Record Exchange Rates\n")
    cat("[6] Show Interest Amount\n")
    cat("[7] Generate Reports\n")
    cat("[8] Quit\n")
    
    choice_range <- c(1, 8)
    
    choice <- get_numericInput("Choice: ", 'int', choice_range)
    
    if (choice == 1) {
      registrationInterface()
      currentDf <- loadCSV()
    } else if (choice %in% c(2, 3, 6)) {
        #Loads up the csv file into a dataframe
      currentDf <- loadCSV()
        
        # Checks if there is already a registered account (a row is present)
        if(nrow(currentDf) == 0){
          cat("ERROR: Register an account first.\n")
        } else if (choice == 2){
          account <- depositInterface(currentDf) # a newly updated account dataframe is being returned
        } else if (choice == 3){
          account <- withdrawInterface(currentDf)
        }  else if (choice == 6){
          interestInterface(currentDf)
        }
    
    } else if (choice == 4){
        exchangeInterface()
    } else if (choice == 5){
        recordInterface()
    } else if (choice == 7){
        reportsInterface()
    } else if (choice == 8) {
    bQuit <- TRUE
    }
    
    # Always prompted every after interface to determine if the program will continue running
    # Choice N will exit the program 
    if (choice != 8) {
      cat("\n")
      bQuit <- !y_n_prompt("Back to the Main Menu (Y/N): ")
    }
    
    
  }
  cat("Exiting program...\n")
}

# Account registration interface
# Calls processRegistration function to update the account.csv file
registrationInterface <- function(){
  cat("\n")
  
  cat("Register Account Name\n")
  accountName <- get_stringInput("Account Name: ")
  initDeposit <- get_numericInput("Initial Deposit: ", "double", NULL)
  
  while (initDeposit < 5000) {
    cat("\nMinimum initial deposit is 5000.\n")
    initDeposit <- get_numericInput("Initial Deposit: ", "double", NULL)
    if (is.null(accountName) || is.null(initDeposit)) {
      cat("Cancelling...\n")
      return(invisible(NULL))
    }
  }
  
  processRegistration(accountName, initDeposit)
  cat("\nAccount created successfully!\n")
}

# Allows the user to add/deposit money to their account balance
# Captures the desired amount and displays the updated balance after
depositInterface <- function(currentDf){
  cat("\n")

  cat("Deposit Amount\n")
  targetAccount <- get_stringInput("Account Name: ")
  
  #currentBalance comes from pull which returns a numeric.
  #If it returns 0, it means there is no row corresponding to it.
  #Signifying an account that does not exist
  currentBalance <- getCurrentBalance(currentDf, targetAccount)
  while (length(currentBalance) == 0) {
    cat("Account not found.\n")
    currentBalance <- getCurrentBalance(currentDf, targetAccount)
  }
  
  cat("Current Balance:", currentBalance, "\n")
  cat("Currency: PHP\n")
  
  
  cat("\n")
  depositAmount <- get_numericInput("Deposit Amount: ", 'double', NULL)
  if (is.null(depositAmount)) {
    cat("Cancelling...\n")
    return(invisible(NULL))
  }
  
  #Check if decimal is only up to two decimal places
  while(!isTRUE(depositAmount == round(depositAmount, 2))){
    cat ("ERROR: Only 2 decimal places are allowed.\n")
    depositAmount <- get_numericInput("Deposit Amount: ", 'double', NULL)
  }
  
  # Processes the update to the account balance in the account.csv file
  currentDf <- updateBalance(currentDf, targetAccount, "deposit", depositAmount, currentBalance + depositAmount)
  cat(sprintf("Updated Balance: %.2f\n", currentBalance + depositAmount))
  
  
  return(currentDf)
}

# Allows the user to withdraw money from the account 
withdrawInterface <- function(currentDf){
  cat("\n")
  
  cat("Withdraw Amount\n")
  targetAccount <- get_stringInput("Account Name: ")
  
  currentBalance <- getCurrentBalance(currentDf, targetAccount)
  while (length(currentBalance) == 0) {
    cat("Account not found.\n")
    currentBalance <- getCurrentBalance(currentDf, targetAccount)
  }
  
  if (currentBalance == 0) {
    cat("ERROR: Zero balance. Unable to withdraw.\n")
  } else {
    cat("Current Balance:", currentBalance, "\n")
    cat("Currency: PHP\n")
  }
  
  cat("\n")
  while (currentBalance > 0) {
    withdrawAmount <- get_numericInput("Withdraw Amount: ", 'double', NULL)
    if (is.null(withdrawAmount)) {
      cat("Cancelling...\n")
      return(invisible(NULL))
    }
    
    #Check if decimal is only up to two decimal places
    while(!isTRUE(withdrawAmount == round(withdrawAmount, 2))){
      cat ("ERROR: Only 2 decimal places are allowed.\n")
      withdrawAmount <- get_numericInput("Withdraw Amount: ", 'double', NULL)
    }
    
    # Checks first if the amount is greater than the current balance before
    # processing the change
    if (withdrawAmount > currentBalance) {
      cat("ERROR: Amount to withdraw has exceeded the current balance.\n")
    } else {
      currentDf <- updateBalance(currentDf, targetAccount, "withdrawal", withdrawAmount, currentBalance - withdrawAmount) 
      cat(sprintf("Updated Balance: %.2f\n", currentBalance - withdrawAmount))
      break
    }
  }
  
  
  return(currentDf)
  
  
}

# Allows the user to select a currency and input the conversion rate to that currency
recordInterface <- function(){
  cat("\n")
  
  cat("Record Exchange Rate\n")
  cat("\n")
  
  cat("[1] Philippine Peso (PHP)\n")
  cat("[2] United States Dollar (USD)\n")
  cat("[3] Japanese Yen (JPY)\n")
  cat("[4] British Pound Sterling (GBP)\n")
  cat("[5] Euro (EUR)\n")
  cat("[6] Chinese Yuan Renminni (CNY)\n")
  cat("\n")
  
  choice_range <- c(1, 6)
  
  selectedCurrency <- get_numericInput("Select Foreign Currency: ", 'int', choice_range)
  exchangeRate <-  get_numericInput("Exchange Rate: ", 'double', NULL)
  
  if (is.null(selectedCurrency) || is.null(exchangeRate)) {
    cat("Cancelling...\n")
    return(invisible(NULL))
  }
  
  # Processes the currency change 
  updateCurrencies(selectedCurrency, exchangeRate)

}

# Allows the user to exchange money from a source currency to an exchange currency
exchangeInterface <- function(){
  bQuit <- FALSE
  while (!bQuit) {
    currenciesDf <- read.csv("currencies.csv")
    cat("\n")
    cat("Foreign Currency Exchange\n")
    cat("Source Currency Options:\n")
    # Prints out all the currency names
    for (i in seq_len(nrow(currenciesDf))) {
      cat(sprintf("[%d] %s\n", i, currenciesDf[i, 1]))
    }
    cat("\n")
    choice_range <- c(1, 6)
    
    sourceCurrency <- get_numericInput("Source Currency: ", 'int', choice_range)
    sourceAmount <- get_numericInput("Source Amount: ", 'double', NULL)
    if (is.null(sourceCurrency) || is.null(sourceAmount)) {
      cat("Cancelling...\n")
      return(invisible(NULL))
    }
    
    #Selected source currency is removed from the exchange currency options
    remainingDf <- currenciesDf[-sourceCurrency, ]
    
    cat("\n")
    cat("Exchanged Currency Options:\n")
    
    for (i in seq_len(nrow(remainingDf))) {
      cat(sprintf("[%d] %s\n", i, remainingDf[i, 1]))
    }
    cat("\n")
    choice_range <- c(1, 5)
    
    exchangeCurrency <- get_numericInput("Exchange Currency: ", 'int', choice_range)
    if (is.null(exchangeCurrency)) {
      cat("Cancelling...\n")
      return(invisible(NULL))
    }
    # Proccesses the exchange
    exchangeAmount <- processExchange(currenciesDf, remainingDf, sourceCurrency, exchangeCurrency, sourceAmount)
    
    # If the exchange is successful, the source amount is converted to PHP and is subtracted to the 
    # account balance of the user
    if(!is.null(exchangeAmount)) {
      cat(sprintf("Exchange Amount: %.2f\n", exchangeAmount))
      bQuit <- TRUE
    } else { 
      bQuit <- !y_n_prompt("Convert another currency (Y/N): ")
    }
  }
  
}

# Allows the user to view a formatted table of daily interest and corresponding balance given n days 
interestInterface <- function(currentDf){
  cat("\n")
  cat("Show Interest Amount\n")
  # Accesses the account details from the account(dataframe) parameter 
  targetAccount <- get_stringInput("Account Name: ")
  
  currentBalance <- getCurrentBalance(currentDf, targetAccount)
  while (length(currentBalance) == 0) {
    cat("Account not found.\n")
    currentBalance <- getCurrentBalance(currentDf, targetAccount)
  }
  cat("Current Balance:", currentBalance, "\n")
  cat("Currency: PHP\n")
  cat("Interest Rate: 5% \n")
  cat("\n")
  
  num_days <- get_numericInput("Total Number of Days: ", 'int', c(1, Inf))
  if (is.null(num_days)) {
    cat("Cancelling...\n")
    return(invisible(NULL))
  }
  
  interestDf <- processInterest(currentBalance, num_days)
  # Library knitr formatted table
  print(kable(interestDf))
}

reportsInterface <- function() {
  bQuit <- FALSE
  while (!bQuit) {
    cat("\nGenerate Reports\n\n")
    
    cat("[1] Load the File\n")
    cat("[2] Generate Reports\n")
    choice_range <- c(1, 2)
    
    choice <- get_numericInput("Enter choice: ", 'int', choice_range)
    filteredDf <- loadCSV()
    
    cat("\n")
    if (choice == 1) {
      filteredDf <- processCSV()
    } else if (choice == 2) {
      cat("Generating reports...\n")
      cat("Outputs saved to individual files...\n\n")
      
      cat("Report 1: Banking Privilege Tiers\n")
      report1Df <- generateReport1(filteredDf)
      
      cat("\n(Full table exported to report1_banking_privilege_tiers.csv)\n\n")
      
      cat("Report 2: Interest Expense Projection")
      report2Df <- generateReport2(filteredDf)
      
      cat("\n(Full table exported to report2_interest_expense_projection.csv)\n\n")
      
      cat("Report 3: Suspicious Client Behavior")
      report3Df <- generateReport3(filteredDf)
      
      cat("\n(Full table exported to report3_suspicious_client_behavior.csv)\n\n")
      
      cat("Report 4: Quarterly Balance Percentage Change\n")
      report4Df <- generateReport4(filteredDf)
      
      cat("\n(Full table exported to report4_quarterly_percentage_change.csv)\n\n")
      
      cat("Summary State (summary.json):\n\n")
      load_json <- generateJSON(filteredDf, report1Df, report2Df, report3Df, report4Df)
      print(load_json)
      
      bQuit <- TRUE
    }
  }
  
}
