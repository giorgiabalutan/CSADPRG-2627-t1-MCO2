#ifndef USER_H
#define USER_H

#include "custom_types.h"

extern Account *accounts;
extern UT_array *allTransactions;
extern long long lastAccountNumber;
extern ForEx forex;
extern UT_icd transaction_icd;
extern UT_icd log_icd;
extern TieBreaker tieBreaker;

void defaultConfig();
Account *accountFind(const char *name);
Account *accountTransact(const char *name, long long number, Transaction transaction);
Transaction makeTransaction(TransactionType type, double amount);
void dbFreeAll();

#endif
