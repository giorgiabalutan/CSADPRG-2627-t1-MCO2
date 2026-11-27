#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <string.h>
#include <float.h>
#include "custom_types.h"
#include "user.h"

#define RESERVE_RATIO 0.05

/**
 * @name cmpLendingReport();
 * 
 * Comparison function for sorting Lending Reports.
 * Sorts Stability Level (Stable->Moderate->Volatile), then by SafeToLend (Descending), then by AccountName (Alphabetical) or AccountNumber (Ascending).
 */
int cmpLendingReport(const void *x, const void *y){
    const LendingReportRecord *a = x, *b = y;
    
    if (a->Stability != b->Stability){
        return (b->Stability > a->Stability) - (b->Stability < a->Stability);
    }

    if (a->SafeToLend != b->SafeToLend){
        return (b->SafeToLend > a->SafeToLend) - (b->SafeToLend < a->SafeToLend);
    }

    if (tieBreaker == NAME){
        return strcasecmp(a->AccountName,b->AccountName);
    } else {
        return (a->AccountNumber > b->AccountNumber) - (a->AccountNumber < b->AccountNumber);
    }
}

/**
 * @name generateInterestReport();
 * 
 * Generates custom report 4.
 * 
 * Reference:
 * Report 4-Aaron: How much is safe to lend out for Fractional Reserve Banking (5% for BSP)
 *  -AccountName
 *  -AverageMonthlyLow: Get the lowest End of Day balance in each month and average them (for the last year maybe)
 *  -LowestMonthlyLow: The lowest of these balances
 *  -SafeToLend - AverageMonthlyLow * 0.95:   (95% of the average monthly low is probably safe to lend out to others, as the user probably wont withdraw much more than that (5% is the reserve requirement in the BSP apparently?))
 *  -Stability - LowestML / AverageML * 100% (how stable the monthly low is, to check how reliable the SafeToLend calculation would be)
 *  -StabilityRating - >80% Stable, >50% Moderate, <=50% Volatile
 * 
 */
void generateLendingReport(UT_array *reportFourRecords){
    //Gets the date now
    time_t now = time(NULL);
    struct tm *today = localtime(&now);

    //Calculates the ending month and the starting month, (converting years to months by * 12)
    int endIdx = (today->tm_year + 1900) * 12 + today->tm_mon - 1;
    int startIdx = endIdx - (11); //Add 11 previous months
    
    //Iterates through each account
    Account *acc, *temp;
    HASH_ITER(hh, accounts, acc, temp){
        //Initializes array of past 12 months
        double lows[12];
        for (int i = 0; i < 12; i++){
            lows[i] = DBL_MAX;
        }

        double prevBal = 0.0;
        int started = 0, lastMi = 0;

        //Iterates through each of the account's transactions
        for (Transaction *t = NULL; (t = utarray_next(acc->transactions, t));){
            int mi = t->year * 12 + (t->month - 1);
            //If the month surpasses current month, break
            if (mi > endIdx){
                break;
            }

            Transaction *next = utarray_next(acc->transactions, t);
            //If the next transaction exists and is on the same day, skip to it.
            if (next && next->year == t->year && next->month == t->month && next->day == t->day){
                continue;
            }

            //Grabs end of day balance
            double eod = t->balance;
            int idx;

            //Skipped on iteration 1 to just log it.
            //If its a new month, then
            if (started && mi != lastMi){
                //From the last record month, to before the current month
                for (int x = lastMi + 1; x < mi; x++){
                    //Record the currently stored balance as each month's lowest monthly balance
                    idx = x - startIdx;
                    if (idx >= 0 && idx < 12 && prevBal < lows[idx]){
                        lows[idx] = prevBal;
                    }
                }
                //If the day lands on not the first day of the month, the month still has last month's
                //end of day balance at the very least, so we can log that.
                if (t->day > 1){
                    idx = mi - startIdx;
                    if (idx >= 0 && idx < 12 && prevBal < lows[idx]){
                        lows[idx] = prevBal;
                    }
                }
            }

            //If the month of the current transaction falls into the range we are looking for
            //If the eod balance is lower than current recorded, record it.
            idx = mi - startIdx;
            if (idx >= 0 && idx < 12 && eod < lows[idx]){
                lows[idx] = eod;
            }

            //Log the last month seen, and the balance.
            prevBal = eod;
            lastMi = mi;
            started = 1;
        }

        //Checks if any transactions were found at all.
        if (started){
            //Iterates through each month in the range we are looking for from the last seen month onwards.
            for (int x = lastMi + 1; x <= endIdx; x++){
                //Fills up those months, until the present, with the last recorded balance.
                int idx = x - startIdx;
                if (idx >= 0 && idx < 12 && prevBal < lows[idx]){
                    lows[idx] = prevBal;
                }
            }
        }

        //Generates and saves the report for the account.
        LendingReportRecord r4 = {0};
        snprintf(r4.AccountName, sizeof r4.AccountName, "%s", acc->accName);
        r4.AccountNumber = acc->accNumber;

        double sum = 0.0, lowest = DBL_MAX;
        for (int i = 0; i < 12; i++){
            if (lows[i] == DBL_MAX){
                continue;
            }
            sum += lows[i];
            if (lows[i] < lowest){
                lowest = lows[i];
            }
            r4.months++;
        }

        if (r4.months > 0){
            r4.AverageMonthyLow = sum / r4.months;
            r4.LowestMonthlyLow = lowest;
            r4.SafeToLend = r4.AverageMonthyLow * (1.0 - RESERVE_RATIO);
            r4.Stability = r4.AverageMonthyLow > 0 ? r4.LowestMonthlyLow / r4.AverageMonthyLow * 100.0 : 0.0;
            if (r4.Stability >= 80.0){
                r4.rating = STABLE;
            }else if (r4.Stability >= 50.0){
                r4.rating = MODERATE;
            }else{
                r4.rating = VOLATILE;
            }
        }

        utarray_push_back(reportFourRecords, &r4);
    }
}