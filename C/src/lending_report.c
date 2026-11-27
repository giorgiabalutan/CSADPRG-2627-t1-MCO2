#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <string.h>
#include <float.h>
#include "custom_types.h"
#include "user.h"

#define RESERVE_RATIO 0.05

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

void generateLendingReport(UT_array *reportFourRecords){
    time_t now = time(NULL);
    struct tm *today = localtime(&now);
    int endIdx = (today->tm_year + 1900) * 12 + today->tm_mon - 1;
    int startIdx = endIdx - (11); //Add 11 previous months
    
    Account *acc, *temp;
    HASH_ITER(hh, accounts, acc, temp){
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

            double eod = t->balance;
            int idx;

            if (started && mi != lastMi){
                for (int x = lastMi + 1; x < mi; x++){
                    idx = x - startIdx;
                    if (idx >= 0 && idx < 12 && prevBal < lows[idx]){
                        lows[idx] = prevBal;
                    }
                }

                if (t->day > 1){
                    idx = mi - startIdx;
                    if (idx >= 0 && idx < 12 && prevBal < lows[idx]){
                        lows[idx] = prevBal;
                    }
                }
            }

            idx = mi - startIdx;
            if (idx >= 0 && idx < 12 && eod < lows[idx]){
                lows[idx] = eod;
            }

            prevBal = eod;
            lastMi = mi;
            started = 1;
        }

        if (started){
            for (int x = lastMi + 1; x <= endIdx; x++){
                int idx = x - startIdx;
                if (idx >= 0 && idx < 12 && prevBal < lows[idx]){
                    lows[idx] = prevBal;
                }
            }
        }

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