#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <string.h>
#include <float.h>
#include "custom_types.h"
#include "user.h"

int cmpInterestReport(const void *x, const void *y){
    const InterestReportRecord *a = x, *b = y;
    if (a->ExpectedInterest != b->ExpectedInterest){
        return (a->ExpectedInterest < b->ExpectedInterest) - (a->ExpectedInterest > b->ExpectedInterest);
    }
    if (tieBreaker == NAME){
        return strcasecmp(a->AccountName,b->AccountName);
    } else {
        return (a->AccountNumber > b->AccountNumber) - (a->AccountNumber < b->AccountNumber);
    }
}

void generateInterestReport(UT_array *reportTwoRecords, UT_array *reportOneRecords){
    for (PrivelegeReportRecord *r1 = NULL; (r1 = utarray_next(reportOneRecords,r1));){
        InterestReportRecord r2;
        snprintf(r2.AccountName, sizeof r2.AccountName, "%s", r1->AccountName);
        r2.CurrentBalance = r1->CurrentBalance;
        r2.AccountNumber = r1->AccountNumber;

        double balance = r1->CurrentBalance;
        double total = 0.0;
        for (int i = 1 ; i <= 30 ; i++) {
            // Calculate interest
            double interest = balance * (ANNUAL_INTEREST / 365);
            balance += interest;
            total += interest;
        }
        r2.ExpectedInterest = total;
        utarray_push_back(reportTwoRecords, &r2);
    }
}