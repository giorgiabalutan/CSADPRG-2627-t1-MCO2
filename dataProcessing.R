library(janitor)
library(lubridate)
library(hms)
library(tidyr)
library(jsonlite)

# Writes up the initial values for the newly created .csv files
initCSVs <- function(){
  cat("\nInitializing CSV file.\n")
  if(file.exists("bank_transactions.csv") == FALSE) {
    accDf <- tibble(
      account_name = character(),
      account_number = numeric(),
      date = as_date(character()),
      time = as_hms(character()),
      currency = character(),
      amount = numeric(),
      type = character(),
      current_balance = numeric()
    )
    
    write_csv(accDf, "bank_transactions.csv")
  }
  
  # No initial values for the exchange rates 
  if(file.exists("currencies.csv") == FALSE) {
    currenciesDf <- tibble(
      Currencies = c(
        "Philippine Peso (PHP)",
        "United States Dollar (USD)",
        "Japanese Yen (JPY)",
        "British Pound Sterling (GBP)",
        "Euro (EUR)",
        "Chinese Yuan Renminni (CNY)"),
      Rate = NA_real_
    )
    
    write_csv(currenciesDf, "currencies.csv", na = "")
  }
    
}

# Load the bank transaction csv by reading the columns 
loadCSV <- function() {
  read_csv(
    "bank_transactions.csv",
    col_types = cols (
      account_name = col_character(),
      account_number = col_double(),
      date = col_date(format = "%d/%m/%Y"), # Reads the date in the given format
      time = col_time(format = "%H:%M:%S"), # Reads the time in the given format
      currency = col_character(),
      amount = col_double(),
      type = col_character(),
      current_balance = col_double()
    ),
    show_col_types = FALSE
  )
}

# Processes the csv by filtering out PHP and complete records only
processCSV <- function() {
  currentDf <- loadCSV()
  PHPDf = currentDf %>%
            filter(currency == "PHP") %>%
            drop_na()

  cat("Processing dataset...", paste0("(", nrow(currentDf)), "rows loaded,", nrow(PHPDf), "filtered for PHP currency and NA rows)")
  return (PHPDf)
}

# Registers an account (provided the accountName) to the bank_transactions.csv file
# Appends a new account with deposit as initial type
processRegistration <- function(accountName, initDeposit){
  currentDf <- loadCSV()
  
  newAccountNumber <- 1000000000
  if (nrow(currentDf) > 0)
    newAccountNumber <- max(currentDf$account_number, na.rm = TRUE) + 1
  
    # Included values for the columns
    newAccRow <- tibble(
      account_name = accountName,
      account_number = newAccountNumber,
      date = as_date(now()),
      time = as_hms(floor_date(now(), "second")),
      currency = "PHP",
      amount = initDeposit,
      type = "deposit",
      current_balance = initDeposit
    )
  
  bind_rows(currentDf, newAccRow) %>%
    mutate(date = format(date, "%d/%m/%Y"),
           time = format(time, "%H:%M:%S")) %>%
    write_csv("bank_transactions.csv")
}

#Gets the current balance of the target account 
getCurrentBalance <- function(currentDf, targetAccount) {
  read_currbal <- currentDf %>%
    filter(account_name == targetAccount) %>%
    arrange(desc(date), desc(time)) %>%
    slice(1) %>%
    pull(current_balance)
  
  return (read_currbal)
}

# Updates the balance depending on the given command (either deposit or withdraw) and the given value
updateBalance <- function(currentDf, target_account, command, value, newBal) {
  accountNumber <- currentDf %>%
    filter(account_name == target_account) %>%
    slice(1) %>%
    pull(account_number)
  
  newRow <- tibble(
    account_name = target_account,
    account_number = accountNumber,
    date = as_date(now()),
    time = as_hms(floor_date(now(), "second")),
    currency = "PHP",
    amount = value,
    type = command,
    current_balance = newBal
  )
  
  bind_rows(currentDf, newRow) %>%
    mutate(date = format(date, "%d/%m/%Y"),
           time = format(time, "%H:%M:%S")) %>%
    write_csv("bank_transactions.csv")

  return(loadCSV())
}

# Updates the rate in the currencies.csv file depending on the selected currency 
# and its corresponding rate
updateCurrencies <- function(selectedCurrency, rate){
  currenciesDf <- read.csv("currencies.csv")
  currenciesDf[selectedCurrency, 2] <- rate
  write_csv(currenciesDf, "currencies.csv")
}

# Calculates the daily interest and the corresponding balance
# given the number of days provided
processInterest <- function(currentBalance, num_days){
  # Daily interest = (current balance * 5%) / 365
  dailyInterest <- currentBalance * (5 / 100) / 365
  
  # Initialize an empty vector to be filled up in a for loop
  balanceVector <- c()
  
  # Day 1 interest balance
  balanceVector[1] <- currentBalance + dailyInterest
  
  # Day 2 until the given days
  for (i in seq_len(num_days)[-1]) {
    # Dependent on the previous day's balance
    dailyInterest <- balanceVector[i - 1] * (5 / 100) / 365
    balanceVector[i] <- balanceVector[i - 1] + dailyInterest
  }
  
  # Creates the data frame to be displayed as a table
  # Columns = Days, Interest, Balance
  interestDf <- data.frame(
    Days = 1:num_days,
    Interest = round_half_up(dailyInterest, 2),
    Balance = round_half_up(balanceVector, 2)
  )
  
  return (interestDf)
}

# Processes the currency exchange from one currency to another
processExchange <- function(currenciesDf, remainingDf, sourceCurrency, exchangeCurrency, sourceAmount) {
  sourceRate <- currenciesDf[sourceCurrency, 2]
  exchangeRate <- remainingDf[exchangeCurrency, 2]
  
  # Checks if a rate is already recorded in the currencies.csv file
  # If not, exchange is failed and returns null
  if(is.na(sourceRate) || is.na(exchangeRate)) {
    cat("ERROR: You must record the exchange rate first before converting.\n")
    return (NULL)
  } else {
      # Ex: source amount = 500
      #     source rate = 1 (PHP)
      #     exchange rate = 62 (USD)
      #     exchange amount = 8.06 (USD)
      exchangeAmount = (sourceAmount * sourceRate) / exchangeRate
  }
  
  return(round_half_up(exchangeAmount, 2))
}

# Generates report 1 which is about the average daily balances and privilege tiers
generateReport1 <- function(currentDf) {
  month_start <- floor_date(today(), "month") %m-% months(1) #"month" round down the today() date and %m-% subtracts 1 month from it
  #while ensuring that it is exactly the first day of that month
  
  month_end <- floor_date(today(), "month") - days(1) #1 day is subtracted from the rounded down today() date
  
  #gets all balances that are within the month
  month_balances <- currentDf %>%
    group_by(account_number, date) %>%
    arrange(date, time) %>%
    filter(date >= month_start & date <= month_end) %>%
    slice_tail(n = 1) %>%
    ungroup() %>%
    select(account_name, account_number, date, current_balance)

  #gets all the latest previous balances for balances that can be carried over
  previous_balances <- currentDf %>%
    group_by(account_number) %>%
    arrange(date, time) %>%
    filter(date < month_start) %>%
    slice_tail(n = 1) %>%
    ungroup() %>%
    select(account_name, account_number, date, current_balance) %>%
    rename(prev_balance = current_balance, prev_date = date)
    
  #contains the carried over balances
  carried_balances <- bind_rows(
    previous_balances %>%
      anti_join (
        month_balances %>%
          filter(date == month_start),
        by = "account_name"
      ) %>%
      mutate(date = month_start) %>%
      select(account_name, account_number, date, current_balance = prev_balance),
    
    month_balances
  ) %>% arrange(account_name, date)
  
  #multiply a balance of a date to how many days until it was changed
  final_balances <- carried_balances %>%
    group_by(account_number) %>%
    arrange(date, .by_group = TRUE) %>%
    mutate(
      next_date = lead(date, default = month_end + days(1)),
      days_active = as.numeric(difftime(next_date, date, units = "days")),
      balance_per_days = days_active * current_balance
    ) %>%
    ungroup()
  
  total_days <- as.numeric(difftime(month_end + days(1), month_start, units = "days"))
  
  privilege_tiers <- final_balances %>%
    group_by(account_number) %>%
    summarise(
      AverageDailyBalance = round_half_up((sum(balance_per_days, na.rm = TRUE) / total_days), 2)
    ) %>%
    ungroup() %>%
    mutate(
      PrivilegeTier = case_when(
        AverageDailyBalance < 500000 ~ "Silver",
        AverageDailyBalance < 1000000 ~ "Gold",
        AverageDailyBalance >= 500000 ~ "Platinum"
      ),
    )
  
  #Count the number of days since it has opened (from today)
  account_ages <- currentDf %>%
    group_by(account_number, account_name) %>%
    arrange(date, time) %>%
    summarise(
      AccountAge = as.numeric(today() - min(date)),
      CurrentBalance = round_half_up(last(current_balance), 2)
    ) %>%
    ungroup() 
    
  #The final report by summarizing everything
  report1Df <- account_ages %>%
    left_join(privilege_tiers, by = "account_number") %>%
    filter(AverageDailyBalance > 100000) %>%
    arrange(desc(AverageDailyBalance), account_name) %>%
    mutate(CurrentBalance = sprintf("%.2f", CurrentBalance), 
           AverageDailyBalance = sprintf("%.2f", AverageDailyBalance)) %>%
    select(AccountName = account_name, AccountAge, CurrentBalance, 
           AverageDailyBalance, PrivilegeTier)
  
  
  print(kable(head(report1Df, 2), digits = 2))
  
  write_csv(report1Df, "report1_banking_privilege_tiers.csv")
  
  return(report1Df)
}

# Generates the second report which is about the expected interest using the compound formula after 30 days
generateReport2 <- function(currentDf) {
  report2Df <- currentDf %>%
    group_by(account_number) %>%
    arrange(desc(date), desc(time)) %>%
    slice(1) %>%
    filter(current_balance >= 10000) %>%
    ungroup() %>%
    mutate(ExpectedInterest = round_half_up(current_balance * ((1 + 0.05 / 365)^30 - 1), 2),
           CurrentBalance = round_half_up(current_balance, 2),
           CurrentBalance = sprintf("%.2f", CurrentBalance),
           ExpectedInterest = sprintf("%.2f", ExpectedInterest)) %>%
    arrange(desc(ExpectedInterest), account_name) %>%
    select(AccountName = account_name, CurrentBalance, ExpectedInterest)
  
  print(kable(head(report2Df, 2), digits = 2))
  
  write_csv(report2Df, "report2_interest_expense_projection.csv")
  return(report2Df)
}

# Generates the third report which is about suspicious activity
generateReport3 <- function(currentDf) {
  #Counts the total amount of withdrawal per day
  withdrawDf <- currentDf %>%
    group_by(account_number, account_name, date) %>%
    filter(type == "withdrawal") %>%
    summarize(TotalWithdrawalAmounts = round_half_up(sum(amount, na.rm = TRUE), 2)) %>%
    ungroup()
  
  #Counts the total amount of deposit per day
  depositDf <- currentDf %>%
    group_by(account_number, account_name, date) %>%
    filter(type == "deposit") %>%
    summarize(NumberOfDeposits = n(),
              TotalDepositAmounts = round_half_up(sum(amount, na.rm = TRUE), 2)) %>%
    ungroup()
  
  #Summarizes the total withdrawal and deposiit and uses that data for turnover velocity
  #and risk flag level
  report3Df <- withdrawDf %>%
    full_join(depositDf, by = c("account_number", "account_name", "date")) %>%
    replace_na(list(TotalWithdrawalAmounts = 0,
                    NumberOfDeposits = 0,
                    TotalDepositAmounts = 0)) %>%
    mutate(date = format(date, "%d/%m/%Y"),
           TurnoverVelocity = round_half_up(if_else(
                                TotalDepositAmounts > 0,
                                TotalWithdrawalAmounts / TotalDepositAmounts * 100,
                                NA_real_), 2),
           RiskFlagLevel = case_when( 
                               TotalWithdrawalAmounts >= 500000 ~ "Suspicious",
                               TotalDepositAmounts == 0 | TotalWithdrawalAmounts == 0 ~ "Normal",
                               TurnoverVelocity >= 90 ~ "Suspicious",
                               TRUE ~ "Normal" #Default value
           ),
           TotalWithdrawalAmounts = sprintf("%.2f", TotalWithdrawalAmounts),
           TotalDepositAmounts = sprintf("%.2f", TotalDepositAmounts),
           TurnoverVelocity = sprintf("%.2f", TurnoverVelocity)) %>%
    select(AccountName = account_name, Date = date, 
           TotalWithdrawalAmounts, NumberOfDeposits, TotalDepositAmounts,
           TurnoverVelocity, RiskFlagLevel) %>%
    arrange(Date, desc(RiskFlagLevel), AccountName)

  print(kable(head(report3Df, 2), digits = 2))
  
  write_csv(report3Df, "report3_suspicious_client_behavior.csv")
  return(report3Df)
}

# Generatees report 4 which is quarterly percentage change of balances
generateReport4 <- function(currentDf) {
  quarterly_balance <- currentDf %>%
    arrange(date, time) %>%
    mutate(quarter = paste0("Q", quarter(date))) %>% #Quarters the dates
    group_by(account_number, quarter) %>%
    slice_tail(n = 1) %>%
    ungroup() %>%
    select(account_name, account_number, quarter, current_balance) %>%
    pivot_wider(
      names_from = quarter,
      values_from = current_balance
    )
  
  report4Df <- quarterly_balance %>%
    mutate(
      Q1_to_Q2PctChange = sprintf("%.2f", round_half_up((Q2 - Q1) / Q1 * 100, 2)), #The quarter changes are computed manually
      Q2_to_Q3PctChange = sprintf("%.2f", round_half_up((Q3 - Q2) / Q2 * 100, 2)),
      Q3_to_Q4PctChange = sprintf("%.2f", round_half_up((Q4 - Q3) / Q3 * 100, 2)),
      YearTotalBalanceGrowth = sprintf("%.2f", round_half_up((Q4 - Q1) / Q1 * 100, 2)),
      Description = case_when( #The description is based on the yearly growth balance
        YearTotalBalanceGrowth > 0 ~ "More Deposits",
        YearTotalBalanceGrowth < 0 ~ "More Withdrawals",
        .default = "No Growth"
      )
    ) %>%
    select(AccountName = account_name, Q1_to_Q2PctChange, 
           Q2_to_Q3PctChange,
           Q3_to_Q4PctChange,
           YearTotalBalanceGrowth,
           Description) %>%
    arrange(AccountName)
  
  print(kable(head(report4Df, 2), digits = 2))
  write_csv(report4Df, "report4_quarterly_percentage_change.csv")
  return(report4Df)
  
}

#Generates the json summary
generateJSON <- function(currentDf, report1Df, report2Df, report3Df, report4Df) {
  currentBalances <- currentDf %>%
    group_by(account_number) %>%
    arrange(desc(date), desc(time)) %>%
    slice(1) %>%
    ungroup() %>%
    select(current_balance)
  
  JSONDf <- tibble(
    TotalAccounts = n_distinct(currentDf$account_number, na.rm = TRUE),
    TotalDepositAmount = round_half_up(sum(as.numeric(currentDf$amount[currentDf$type == "deposit"]), na.rm = TRUE), 2),
    TotalWithdrawAmount = round_half_up(sum(as.numeric(currentDf$amount[currentDf$type == "withdrawal"]), na.rm = TRUE), 2),
    TotalBalance = round_half_up(sum(as.numeric(currentBalances$current_balance), na.rm = TRUE), 2),
    TotalInterestExpense = round_half_up(sum(as.numeric(report2Df$ExpectedInterest), na.rm = TRUE), 2),
    TotalSilverMembers = sum(report1Df$PrivilegeTier == "Silver", na.rm = TRUE),
    TotalGoldMembers = sum(report1Df$PrivilegeTier == "Gold", na.rm = TRUE),
    TotalPlatinumMembers = sum(report1Df$PrivilegeTier == "Platinum", na.rm = TRUE),
    TotalNormalActivity = sum(report3Df$RiskFlagLevel == "Normal", na.rm = TRUE),
    TotalSuspiciousActivity = sum(report3Df$RiskFlagLevel == "Suspicious", na.rm = TRUE),
    AvgYearBalanceGrowth = round_half_up(mean(as.numeric(report4Df$YearTotalBalanceGrowth), na.rm = TRUE), 2)
  )
  
  write_json(JSONDf, "summary.json", pretty = TRUE, auto_unbox = TRUE, na = NULL)
  load_json <- fromJSON("summary.json")
  
  return(load_json)
}
