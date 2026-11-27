#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <string.h>
#include <float.h>
#include "console_input.h"
#include "custom_types.h"
#include "saveload.h"
#include "user.h"

#include "privilege_report.h"
#include "interest_report.h"
#include "risk_report.h"
#include "lending_report.h"
#define REPORT1 "report1_banking_privilege_tiers.csv"
#define REPORT2 "report2_interest_expense_projection.csv"
#define REPORT3 "report3_suspicious_client_behavior.csv"
#define REPORT4 "report4_lending_safety.csv"
#define PRIVILEGE_MIN_BAL 100000.00
#define INTEREST_MIN_BAL 10000.00

void generateReport(){
    int select = 0;
    bool exitChoice = false;
    
    // While exit bool is not true...
    while (!exitChoice) {
        printf("Generate Reports\n\n");
        printf("[1] Load the file\n");
        printf("[2] Generate Reports\n");

        // Get user input
        select = menuInputInt(1, 2, "Enter Choice");

        exitChoice = true;

        printf("\n");
    }

    if (select == 1){
        printf("Processing dataset... ");
        dbFreeAll();
        LoadData result = loadUser();
        printf("(%d rows loaded, %d filtered for PHP currency)", result.loaded, result.found-result.loaded);
    }else{
        printf("Generating reports...\n");
        //Tracking Data for the summary JSON
        int nAccounts = 0;
        double totalDepositAmount = 0.0;
        double totalWithdrawAmount = 0.0;
        double totalInterestExpense = 0.0;
        double totalBalance = 0.0;

        int nSilver = 0;
        int nGold = 0;
        int nPlatinum = 0;
        int nNormal = 0;
        int nSus = 0;

        double safeToLend = 0.0;
        int nStable = 0;
        int nModerate = 0;
        int nVolatile = 0;

        //Report 1
        UT_icd r1r_icd = {sizeof(PrivelegeReportRecord), NULL, NULL, NULL};
        UT_array *reportOneRecords;
        utarray_new(reportOneRecords, &r1r_icd);

        generatePrivelegeReport(reportOneRecords);

        utarray_sort(reportOneRecords, cmpPrivelegeReport);
        FILE *f1 = fopen(REPORT1, "w");
        if (!f1){
            printf("ERROR: Can't Create Report 1 File");
            return;
        }
        fprintf(f1, "AccountName,AccountAge,CurrentBalance,AverageDailyBalance,PrivelegeTier\n");
        for (PrivelegeReportRecord *r1 = NULL; (r1 = utarray_next(reportOneRecords,r1));){
            nAccounts++;
            totalBalance += r1->CurrentBalance;
            // Filters out under minimum balance.
            if (r1->AverageDailyBalance < PRIVILEGE_MIN_BAL){
                continue;
            }
            if (r1->tier == PLATINUM){
                nPlatinum++;
            }else if (r1->tier == GOLD){
                nGold++;
            }else{
                nSilver++;
            }
            fprintf(f1, "%s,%d,%.2f,%.2f,%s\n",
            r1->AccountName, r1->AccountAge, r1->CurrentBalance, r1->AverageDailyBalance,
            r1->tier == PLATINUM? "Platinum" :
            r1->tier == GOLD? "Gold" : "Silver"
            );
        }
        fclose(f1);

        //Report 2
        //Since we already have each account from the previous report along with their current balance, we shall commandeer that data.
        UT_icd r2r_icd = {sizeof(InterestReportRecord), NULL, NULL, NULL};
        UT_array *reportTwoRecords;
        utarray_new(reportTwoRecords, &r2r_icd);

        generateInterestReport(reportTwoRecords, reportOneRecords);

        utarray_sort(reportTwoRecords, cmpInterestReport);
        FILE *f2 = fopen(REPORT2, "w");
        if (!f2){
            printf("ERROR: Can't Create Report 2 File");
            return;
        }
        fprintf(f2, "AccountName,CurrentBalance,ExpectedInterest\n");
        for (InterestReportRecord *r2 = NULL; (r2 = utarray_next(reportTwoRecords,r2));){
            // Filters under minimum balance
            if (r2->CurrentBalance < INTEREST_MIN_BAL){
                continue;
            }
            totalInterestExpense += r2->ExpectedInterest;
            fprintf(f2, "%s,%.2f,%.2f\n",
            r2->AccountName, r2->CurrentBalance, r2->ExpectedInterest
            );
        }
        fclose(f2);

        //Report 3
        UT_icd r3r_icd = {sizeof(RiskReportRecord), NULL, NULL, NULL};
        UT_array *reportThreeRecords;
        utarray_new(reportThreeRecords, &r3r_icd);

        generateRiskReport(reportThreeRecords, &totalDepositAmount, &totalWithdrawAmount);

        utarray_sort(reportThreeRecords, cmpRiskReport);;
        FILE *f3 = fopen(REPORT3, "w");
        if (!f3){
            printf("ERROR: Can't Create Report 3 File");
            return;
        }
        fprintf(f3, "AccountName,Date,TotalWithdrawalAmounts,NumberOfDeposits,TotalDepositAmounts,TurnoverVelocity,RiskFlagLevel\n");
        for (RiskReportRecord *r3 = NULL; (r3 = utarray_next(reportThreeRecords,r3));){
            if (r3->risk == SUSPICIOUS){
                nSus++;
            }else{
                nNormal++;
            }
            fprintf(f3, "%s,%02d/%02d/%04d,%.2f,%d,%.2f,%.2f%%,%s\n",
            r3->AccountName, r3->day, r3->month, r3->year,
            r3->totalWithdrawal, r3->nDeposits, r3->totalDeposit,
            r3->turnoverVelocity,
            r3->risk == SUSPICIOUS? "Suspicious" : "Normal"
            );
        }
        fclose(f3);

        //Report 4
        UT_icd r4r_icd = {sizeof(LendingReportRecord), NULL, NULL, NULL};
        UT_array *reportFourRecords;
        utarray_new(reportFourRecords, &r4r_icd);

        generateLendingReport(reportFourRecords);

        utarray_sort(reportFourRecords, cmpLendingReport);
        FILE *f4 = fopen(REPORT4, "w");
        if (!f4){
            printf("ERROR: Can't Create Report 4 File");
            return;
        }
        fprintf(f4, "AccountName,AverageMonthlyLow,LowestMonthlyLow,SafeToLend,Stability,StabilityRating\n");
        for (LendingReportRecord *r4 = NULL; (r4 = utarray_next(reportFourRecords,r4));){
            safeToLend += r4->SafeToLend;
            if (r4->rating == STABLE){
                nStable++;
            }else if (r4->rating == MODERATE){
                nModerate++;
            }else{
                nVolatile++;
            }
            fprintf(f4, "%s,%.2f,%.2f,%.2f,%.2f%%,%s\n",
            r4->AccountName, r4->AverageMonthyLow, r4->LowestMonthlyLow, r4->SafeToLend, r4->Stability,
            r4->rating == STABLE? "Stable" :
            r4->rating == MODERATE? "Moderate" : "Volatile"
            );
        }
        fclose(f4);

        

        // DISPLAYING
        printf("Outputs saved to individual files...\n\n");
        printf("Report 1: Banking Privilege Tiers\n");

        //Display 2
        int displayCount = 0;
        printf("AccountName,AccountAge,CurrentBalance,AverageDailyBalance,PrivelegeTier\n");
        for (PrivelegeReportRecord *r1 = NULL; (r1 = utarray_next(reportOneRecords,r1));){
            // Filters out under minimum balance.
            if (r1->AverageDailyBalance < PRIVILEGE_MIN_BAL){
                continue;
            }
            printf("%s,%d,%.2f,%.2f,%s\n",
            r1->AccountName, r1->AccountAge, r1->CurrentBalance, r1->AverageDailyBalance,
            r1->tier == PLATINUM? "Platinum" :
            r1->tier == GOLD? "Gold" : "Silver"
            );
            displayCount++;
            if (displayCount > 1){
                break;
            }
        }

        printf("(Full table exported to report1_banking_privilege_tiers.csv)\n\n");

        printf("Report 2: Interest Expense Projection\n");

        //Display 2
        displayCount = 0;
        printf("AccountName,CurrentBalance,ExpectedInterest\n");
        for (InterestReportRecord *r2 = NULL; (r2 = utarray_next(reportTwoRecords,r2));){
            // Filters under minimum balance
            if (r2->CurrentBalance < INTEREST_MIN_BAL){
                continue;
            }
            printf("%s,%.2f,%.2f\n",
            r2->AccountName, r2->CurrentBalance, r2->ExpectedInterest
            );
            displayCount++;
            if (displayCount > 1){
                break;
            }
        }

        printf("(Full table exported to report2_interest_expense_projection.csv)\n\n");

        printf("Report 3: Suspicious Client Behavior\n");

        //Display 2
        displayCount = 0;
        printf("AccountName,Date,TotalWithdrawalAmounts,NumberOfDeposits,TotalDepositAmounts,TurnoverVelocity,RiskFlagLevel\n");
        for (RiskReportRecord *r3 = NULL; (r3 = utarray_next(reportThreeRecords,r3));){
            printf("%s,%02d/%02d/%04d,%.2f,%d,%.2f,%.2f%%,%s\n",
            r3->AccountName, r3->day, r3->month, r3->year,
            r3->totalWithdrawal, r3->nDeposits, r3->totalDeposit,
            r3->turnoverVelocity,
            r3->risk == SUSPICIOUS? "Suspicious" : "Normal"
            );
            displayCount++;
            if (displayCount > 1){
                break;
            }
        }

        printf("(Full table exported to report3_suspicious_client_behavior.csv)\n\n");

        printf("Report 4: Lending Safety under Fractional Reserve Banking\n");

        //Display first 2 rows
        displayCount = 0;
        printf("AccountName,AverageMonthlyLow,LowestMonthlyLow,SafeToLend,Stability,StabilityRating\n");
        for (LendingReportRecord *r4 = NULL; (r4 = utarray_next(reportFourRecords,r4));){
            printf("%s,%.2f,%.2f,%.2f,%.2f%%,%s\n",
            r4->AccountName, r4->AverageMonthyLow, r4->LowestMonthlyLow, r4->SafeToLend, r4->Stability,
            r4->rating == STABLE? "Stable" :
            r4->rating == MODERATE? "Moderate" : "Volatile"
            );
            displayCount++;
            if (displayCount > 1){
                break;
            }
        }

        printf("(Full table exported to report4_lending_safety.csv)\n\n");

        printf("Summary State (summary.json):\n");
        FILE *summary = fopen("summary.json", "w");
        if (!summary){
            printf("ERROR: Can't Create Summary File");
            return;
        }

        fprintf(summary,
            "{\n"
            " \"total_accounts\": %d,\n"
            " \"total_deposits\": %.2f,\n"
            " \"total_withdrawals\": %.2f,\n"
            " \"total_balance\": %.2f,\n"
            " \"total_interest_expense\": %.2f,\n"
            " \"privelege_tiers\": {\n"
            "   \"silver\": %d,\n"
            "   \"gold\": %d,\n"
            "   \"platinum\": %d\n"
            " },\n"
            " \"risk_flags\": {\n"
            "   \"normal\": %d,\n"
            "   \"suspicious\": %d\n"
            " },\n"
            " \"safe_to_lend\": %.2f,\n"
            " \"stability_rating\": {\n"
            "   \"stable\": %d,\n"
            "   \"moderate\": %d,\n"
            "   \"volatile\": %d\n"
            " }\n"
            "}\n",
            nAccounts, totalDepositAmount, totalWithdrawAmount,
            totalBalance, totalInterestExpense,
            nSilver, nGold, nPlatinum,
            nNormal, nSus,
            safeToLend,
            nStable, nModerate, nVolatile);
        fclose(summary);

        printf("{\n"
            " \"total_accounts\": %d,\n"
            " \"total_deposits\": %.2f,\n"
            " \"total_withdrawals\": %.2f,\n"
            " \"total_balance\": %.2f,\n"
            " \"total_interest_expense\": %.2f,\n"
            " \"privelege_tiers\": {\n"
            "   \"silver\": %d,\n"
            "   \"gold\": %d,\n"
            "   \"platinum\": %d\n"
            " },\n"
            " \"risk_flags\": {\n"
            "   \"normal\": %d,\n"
            "   \"suspicious\": %d\n"
            " },\n"
            " \"safe_to_lend\": %.2f,\n"
            " \"stability_rating\": {\n"
            "   \"stable\": %d,\n"
            "   \"moderate\": %d,\n"
            "   \"volatile\": %d\n"
            " }\n"
            "}\n",
            nAccounts, totalDepositAmount, totalWithdrawAmount,
            totalBalance, totalInterestExpense,
            nSilver, nGold, nPlatinum,
            nNormal, nSus,
            safeToLend,
            nStable, nModerate, nVolatile);
        

        utarray_free(reportOneRecords);
        utarray_free(reportTwoRecords);
        utarray_free(reportThreeRecords);
        utarray_free(reportFourRecords);
        reportOneRecords = NULL;
        reportTwoRecords = NULL;
        reportThreeRecords = NULL;
        reportFourRecords = NULL;
    }
}