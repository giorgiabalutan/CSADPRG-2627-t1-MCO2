/**
 * 
 * user.c
 * 
 * Contains the user global variable, and the default settings for the user
 * 
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "user.h"


// Global var for Users
Account *accounts = NULL;
UT_array *allTransactions = NULL;
long long lastAccountNumber = 0;
ForEx forex;
UT_icd transaction_icd = {sizeof(Transaction), NULL, NULL, NULL};
UT_icd log_icd = {sizeof(LogEntry), NULL, NULL, NULL};
TieBreaker tieBreaker = NAME;


/**
 * @name defaultConfig();
 * 
 * Default config of user
 */
void defaultConfig() {
    forex.length = 6;
    ConversionRate rates[MAX_CONVERSION_RATES] = {
        // Oct 2, 9:00 PM UTC (Morningstar)

        // 1 PHP = 1 PHP
        { "Philippine Peso", "PHP", 1 },

        // 1 PHP = 0.016 USD
        { "United States Dollar", "USD", 0 },

        // 1 PHP = 2.52 JPY
        { "Japanese Yen", "JPY", 0 },

        // 1 PHP = 0.012 GBP
        { "British Pound Sterling", "GBP", 0 },

        // 1 PHP = 0.014 Euro 
        { "Euro", "EUR", 0 },

        // 1 PHP = 0.11 CNY
        { "Chinese Yuan Renminni", "CNY", 0 },
    };

    memcpy(forex.rates, rates, sizeof(rates));
}

/**
 * @name accountFind();
 * 
 * TO DO
 */
Account *accountFind(const char *name) {
    Account *acc;
    HASH_FIND_STR(accounts, name, acc);
    return acc;
}

/**
 * TO DO
 */
Transaction makeTransaction(TransactionType type, double amount) {
    time_t now = time(NULL);
    struct tm *t = localtime(&now);
    Transaction row = {0};

    row.day = t->tm_mday;
    row.month = t->tm_mon + 1;
    row.year = t->tm_year + 1900;
    row.hour = t->tm_hour;
    row.minute = t->tm_min;
    row.second = t->tm_sec;

    row.type = type;
    row.amount = amount;
    
    return row;
}

/**
 * TO DO
 */
Account *accountTransact(const char *name, long long number, Transaction transaction){
    Account *acc = accountFind(name);
    if (!acc){
        if (transaction.type != DEPOSIT){
            return NULL;
        }
        acc = malloc(sizeof *acc);
        if (!acc){
            return NULL;
        }

        snprintf(acc->accName, sizeof acc->accName, "%s", name);
        acc->accNumber = number;
        acc->balance = 0.0;
        utarray_new(acc->transactions, &transaction_icd);
        HASH_ADD_STR(accounts, accName, acc);

        if (number > lastAccountNumber){
            lastAccountNumber = number;
        }
    }

    if(transaction.type == DEPOSIT){
        acc->balance += transaction.amount;
    } else {
        acc->balance -= transaction.amount;
    }

    transaction.balance = acc->balance;
    utarray_push_back(acc->transactions, &transaction);
    LogEntry e = {acc, transaction};
    utarray_push_back(allTransactions, &e);
    return acc;
}

/**
 * TO DO
 */
void dbFreeAll(){
    Account *acc, *temp;
    HASH_ITER(hh, accounts, acc, temp){
        utarray_free(acc->transactions);
        HASH_DEL(accounts, acc);
        free(acc);
    }
    utarray_free(allTransactions);
    allTransactions = NULL;
    lastAccountNumber = 0;
}