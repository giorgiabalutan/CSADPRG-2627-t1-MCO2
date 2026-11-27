/**
 * 
 * custom_types.h
 * 
 * Header file for defining constants, types
 * 
 */

#include "uthash.h"
#include "utarray.h"

#ifndef CUSTOM_TYPES_H
#define CUSTOM_TYPES_H

// STRINGS
#define STRING_LENGTH 64 // 63 + null byte
typedef char String[STRING_LENGTH];

// NUMBER LIMITS
#define MAX_ACCOUNT_BALANCE 1000000000.00 // 1 billion in base currency
#define MAX_TRANSACTION 10000000 // 10 million in base currency
#define MAX_CONVERSION_RATES 10 // Only 10 currencies will be stored
#define MAX_FUNCTIONS 10 // Only 10 functions will be stored

// CONSTS
#define ANNUAL_INTEREST 0.05
#define BASE_CURRENCY "PHP"

// ENUMS
typedef enum {DEPOSIT, WITHDRAW} TransactionType;
typedef enum {NAME, ID} TieBreaker;
typedef enum {SILVER, GOLD, PLATINUM} PrivilegeTier;
typedef enum {SUSPICIOUS, NORMAL} RiskFlagLevel;
typedef enum {STABLE, MODERATE, VOLATILE} StabilityRating;

extern UT_icd transaction_icd;

// USER STRUCTS
typedef struct {
    String currencyName;
    String currencyCode;
    double rateFromPHP;
} ConversionRate;

typedef struct{
    int day;
    int month;
    int year;
    int hour;
    int minute;
    int second;
    String currencyName;
    double amount;
    TransactionType type;
    double balance;
} Transaction;

typedef struct{
    String accName; /* Hash Key */
    long long accNumber;
    double balance;
    UT_array *transactions;
    UT_hash_handle hh;
} Account;

typedef struct{
    ConversionRate rates[MAX_CONVERSION_RATES];
    int length;
} ForEx;

typedef struct{
    Account *acc;
    Transaction t;
} LogEntry;

// MENU
typedef struct {
    String functionName;
    void (*ptr)();
} VoidFunction;

typedef struct {
    VoidFunction functions[MAX_FUNCTIONS];
    int length;
} VoidMenu;

typedef struct {
    int found;
    int loaded;
} LoadData;

#endif








// typedef struct {
//     ConversionRate conversionRates[MAX_CONVERSION_RATES];
//     String accountName;
//     double balance;
//     int conversionRatesLength;
//     int currentCurrency;
// } User;
