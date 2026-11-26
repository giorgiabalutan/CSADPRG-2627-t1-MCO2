
# Report 1 (AverageDailyBalance)

- Sort by AverageDailyBalance (desc)

AccountName - account_name

AccountAge - current date - first transaction date

CurrentBalance - latest transaction current_balance

AverageDailyBalance -

PrivilegeTier - `PrivilegeTier.Silver if avgDailyBalance < 500000.00 else PrivilegeTier.Gold if avgDailyBalance < 1000000.00 else PrivilegeTier.Platinum`

# Report 2 (Interest Expense Projection)

- Filter by `current_balance >= 10000.00`
- Sort by ExpectedInterest (desc)

AccountName - account_name

CurrentBalance - latest transaction current_balance

ExpectedInterest - for-loop EoD * (0.05 / 365)

# Report 3 (Suspicious Client Behavior)

- Sort by Date (asc), RiskFlagLevel (desc)

AccountName - account_name

Date - Date of entry

TotalWithdrawalAmounts - `Transaction.all({ it.name == account_name && it.type == Transactions.Withdraw && it.date == date }).sumOf({ it.amount })`

TotalDepositAmounts - `Transaction.all({ it.name == account_name && it.type == Transactions.Deposit && it.date == date }).sumOf({ it.amount })`

TurnoverVelocity - `total_withdrawal / total_deposit * 100`

RiskFlagLevel - `RiskFlagLevel.Suspicious if (total_withdrawal >= 500000.00 || TurnoverVelocity >= 90.0) else RiskFlagLevel.Normal`


# Report 4 (Transaction Activity)

- Sort by transaction activity

AccountName
TotalTransactionActivity

# Summary

total_accounts -

total_deposits -

total_withdrawals -

total_balance -

total_interest_expense -

total_suspicious - 
