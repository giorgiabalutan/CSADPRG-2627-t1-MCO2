#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <string.h>
#include <float.h>
#include "custom_types.h"
#include "user.h"

/**
 * @name daysInMonth();
 * 
 * Returns how many days there are in that month and year.
 */
int daysInMonth(int year, int month){
    //January is month 0
    int actualYear = year+1900;
    if (month == 1){
        if (actualYear % 400 == 0 || (actualYear % 4 == 0 && !(actualYear%100 == 0))){
            return 29;
        }
        return 28;
    }
    static const int days[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    return days[month];
}

/**
 * @name prevMonth();
 * 
 * Returns the month and year of one month back.
 */
void prevMonth(int year, int month, int *pyear, int *pmonth){
    if (month == 0){
        *pyear = year - 1;
        *pmonth = 11;
    }else{
        *pyear = year;
        *pmonth = month - 1;
    }
}

/**
 * @name cmpPrivelegeReport();
 * 
 * Comparison function for sorting Privelege Reports.
 * Sorts By AverageDailyBalance (Descending), then by AccountName (Alphabetical) or AccountNumber (Ascending).
 */
int cmpPrivelegeReport(const void *x, const void *y){
    const PrivelegeReportRecord *a = x, *b = y;
    if (a->AverageDailyBalance != b->AverageDailyBalance){
        return (a->AverageDailyBalance < b->AverageDailyBalance) - (a->AverageDailyBalance > b->AverageDailyBalance);
    }
    if (tieBreaker == NAME){
        return strcasecmp(a->AccountName,b->AccountName);
    } else {
        return (a->AccountNumber > b->AccountNumber) - (a->AccountNumber < b->AccountNumber);
    }
}

/**
 * @name generatePrivelegeReport();
 * 
 * Generates report 1.
 */
void generatePrivelegeReport(UT_array *reportOneRecords){
    //Finds target month for Report 1
    time_t now = time(NULL);
    struct tm *timeNow = localtime(&now);
    int tyear, tmon;
    prevMonth(timeNow->tm_year, timeNow->tm_mon, &tyear, &tmon);
    int days = daysInMonth(tyear, tmon);
    int fyear = tyear + 1900;
    int fmon = tmon + 1;

        
    // Iterates through each account's transactions
    Account *acc, *temp;
    HASH_ITER(hh, accounts, acc, temp){
        //Report 1 Initializations
        PrivelegeReportRecord r1;
        double prevMonthBal = 0.0; // Tracks the balance from last month
        double currentBal = 0.0; // Tracks the current balance
        double eod[31] = {0}; //balance seen at the end of day of each day of the month
        int hasTransaction[31] = {0}; //whether a day has had a transaction
        bool firstTransaction = true;
        int openDay, openMon, openYear;

        //Iterates through each of the account's transactions
        for (Transaction *t = NULL; (t = utarray_next(acc->transactions, t));){
            //Getting Report 1 Data
            currentBal = t->balance;

            //Just saves the date for calculating age
            if (firstTransaction){
                firstTransaction = false;
                openDay = t->day;
                openMon = t->month;
                openYear = t->year;
            }
            //Still at before target month, keep track of current balance going into it.
            if(t->year < fyear || (t->year == fyear && t->month < fmon)){
                prevMonthBal = t->balance;
            }
            //Reached target month, track when the balance changes.
            else if (t->year == fyear && t->month == fmon){
                eod[t->day - 1] = t->balance; //Latest transaction that updated it is retained
                hasTransaction[t->day - 1] = 1;
            }
        }

        //Calculating Report 1 Data
        double bal = prevMonthBal, sum = 0.0;

        //This loop just walks through each day of the month.
        for (int d = 0; d < days; d++){
            //If the balance changed here, update it.
            if (hasTransaction[d]){
                bal = eod[d];
            }
            //Just adds the balance every day.
            sum += bal;
        }
        //Gets the average.
        double adb = sum / (double) days;

        //Saving Report 1 Data
        snprintf(r1.AccountName, sizeof r1.AccountName, "%s", acc->accName);
        r1.AccountNumber = acc->accNumber;
        r1.CurrentBalance = currentBal; //or acc->balance
        r1.AverageDailyBalance = adb;

        //Calculating Age
        struct tm open = {0};
        open.tm_year = openYear - 1900;
        open.tm_mon = openMon - 1;
        open.tm_mday = openDay;
        open.tm_isdst = -1;
        time_t opened = mktime(&open);
        int age = (int)(difftime(time(NULL),opened)/86400);
        r1.AccountAge = age;

        //Calculating Privelege Tier
        if (adb >= 1000000.00){
            r1.tier = PLATINUM;
        }else if (adb >= 500000.00){
            r1.tier = GOLD;
        }else{
            r1.tier = SILVER;
        }
        utarray_push_back(reportOneRecords, &r1);
    }
}