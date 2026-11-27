#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <string.h>
#include <float.h>
#include "console_input.h"
#include "custom_types.h"
#include "saveload.h"
#include "user.h"

#define REPORT1 "report1_banking_privilege_tiers.csv"
#define REPORT2 "report2_interest_expense_projection.csv"
#define REPORT3 "report3_suspicious_client_behavior.csv"
#define REPORT4 "report4_lending_safety.csv"
#define WITHDRAWAL_LIMIT 500000.0
#define VELOCITY_LIMIT 90.0
#define RESERVE_RATIO 0.05



typedef struct{
    String AccountName;
    int AccountAge;
    double CurrentBalance;
    double AverageDailyBalance;
    PrivilegeTier tier;
    long long AccountNumber;
} ReportOneRecord;

typedef struct{
    String AccountName;
    double CurrentBalance;
    double ExpectedInterest;
    long long AccountNumber;
} ReportTwoRecord;

typedef struct{
    String AccountName;
    long long AccountNumber;
    int day;
    int month;
    int year;
    double totalWithdrawal;
    int nDeposits;
    double totalDeposit;
    double turnoverVelocity;
    RiskFlagLevel risk;
} ReportThreeRecord;

typedef struct{
    String AccountName;
    long long AccountNumber;
    double AverageMonthyLow;
    double LowestMonthlyLow;
    double SafeToLend;
    double Stability;
    int months;
    StabilityRating rating;
} ReportFourRecord;

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

void prevMonth(int year, int month, int *pyear, int *pmonth){
    if (month == 0){
        *pyear = year - 1;
        *pmonth = 11;
    }else{
        *pyear = year;
        *pmonth = month - 1;
    }
}

static int cmpR1(const void *x, const void *y){
    const ReportOneRecord *a = x, *b = y;
    if (a->AverageDailyBalance != b->AverageDailyBalance){
        return (a->AverageDailyBalance < b->AverageDailyBalance) - (a->AverageDailyBalance > b->AverageDailyBalance);
    }
    if (tieBreaker == NAME){
        return strcasecmp(a->AccountName,b->AccountName);
    } else {
        return (a->AccountNumber > b->AccountNumber) - (a->AccountNumber < b->AccountNumber);
    }
}

static int cmpR2(const void *x, const void *y){
    const ReportTwoRecord *a = x, *b = y;
    if (a->ExpectedInterest != b->ExpectedInterest){
        return (a->ExpectedInterest < b->ExpectedInterest) - (a->ExpectedInterest > b->ExpectedInterest);
    }
    if (tieBreaker == NAME){
        return strcasecmp(a->AccountName,b->AccountName);
    } else {
        return (a->AccountNumber > b->AccountNumber) - (a->AccountNumber < b->AccountNumber);
    }
}

static int cmpR3(const void *x, const void *y){
    const ReportThreeRecord *a = x, *b = y;
    
    if (a->risk != b->risk){
        if (a->risk == SUSPICIOUS){
            return -1;
        }
        return 1;
    }

    if (a->year != b->year){
        return (a->year > b->year) - (a->year < b->year);
    }
    if (a->month != b->month){
        return (a->month > b->month) - (a->month < b->month);
    }
    if (a->day != b->day){
        return (a->day > b->day) - (a->day < b->day);
    }

    if (tieBreaker == NAME){
        return strcasecmp(a->AccountName,b->AccountName);
    } else {
        return (a->AccountNumber > b->AccountNumber) - (a->AccountNumber < b->AccountNumber);
    }
}

static int cmpR4(const void *x, const void *y){
    const ReportFourRecord *a = x, *b = y;
    
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

void generateReport1(UT_array *reportOneRecords){
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
        ReportOneRecord r1;
        double prevMonthBal = 0.0; // Tracks the balance from last month
        double currentBal = 0.0; // Tracks the current balance
        double eod[31] = {0}; //balance seen at the end of day of each day of the month
        bool hasTransaction[31] = {0}; //whether a day has had a transaction
        bool firstTransaction = true;
        int openDay, openMon, openYear;

        //Iterates through each of the account's transactions
        for (Transaction *t = NULL; (t = utarray_next(acc->transactions, t));){
            //Getting Report 1 Data
            currentBal = t->balance;

            if (firstTransaction){
                firstTransaction = false;
                openDay = t->day;
                openMon = t->month;
                openYear = t->year;
            }
            if(t->year < fyear || (t->year == fyear && t->month < fmon)){
                prevMonthBal = t->balance;
            } else if (t->year == fyear && t->month == fmon){
                eod[t->day - 1] = t->balance; //Latest transaction that updated it is retained
                hasTransaction[t->day = 1] = 1;
            }
        }

        //Calculating Report 1 Data
        double bal = prevMonthBal, sum = 0.0;
        for (int d = 0; d < days; d++){
            if (hasTransaction[d]){
                bal = eod[d];
            }
            sum += bal;
        }
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

void generateReport2(UT_array *reportTwoRecords, UT_array *reportOneRecords){
    for (ReportOneRecord *r1 = NULL; (r1 = utarray_next(reportOneRecords,r1));){
        ReportTwoRecord r2;
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

void generateReport3(UT_array *reportThreeRecords, double *totalDepositAmount, double *totalWithdrawAmount){
    // Iterates through each account's transactions
    Account *acc, *temp;
    HASH_ITER(hh, accounts, acc, temp){
        int curY = 0, curM = 0, curD = 0;
        double deposit = 0.0, withdrawal = 0.0;
        int nDeposits = 0;
        //Iterates through each of the account's transactions
        for (Transaction *t = NULL; (t = utarray_next(acc->transactions, t));){
            if (curY == 0 && curM == 0 && curD == 0){
                curY = t->year;
                curM = t->month;
                curD = t->day;
                if (t->type == DEPOSIT){
                    nDeposits += 1;
                    deposit += t->amount;
                    *totalDepositAmount += t->amount;
                }else{
                    withdrawal += t->amount;
                    *totalWithdrawAmount += t->amount;
                }
                continue;
            }
            if (curD != t->day || curM != t->month || curY != t->year){
                ReportThreeRecord r3 = {0};
                snprintf(r3.AccountName, sizeof r3.AccountName, "%s", acc->accName);
                r3.AccountNumber = acc->accNumber;
                r3.day = curD;
                r3.month = curM;
                r3.year = curY;
                r3.totalWithdrawal = withdrawal;
                r3.nDeposits = nDeposits;
                r3.totalDeposit = deposit;
                if (deposit > 0.0){
                    double velocity = withdrawal/deposit * 100.0;
                    r3.turnoverVelocity = velocity;
                    if (velocity >= VELOCITY_LIMIT || withdrawal >= WITHDRAWAL_LIMIT){
                        r3.risk = SUSPICIOUS;
                    }else{
                        r3.risk = NORMAL;
                    }
                }else{
                    r3.turnoverVelocity = 0.0;
                    if (withdrawal >= WITHDRAWAL_LIMIT){
                        r3.risk = SUSPICIOUS;
                    }else{
                        r3.risk = NORMAL;
                    }
                }
                utarray_push_back(reportThreeRecords, &r3);

                //Resets after pushing backlog
                deposit = 0.0;
                withdrawal = 0.0;
                nDeposits = 0;

                curY = t->year;
                curM = t->month;
                curD = t->day;
                if (t->type == DEPOSIT){
                    nDeposits += 1;
                    deposit += t->amount;
                    *totalDepositAmount += t->amount;
                }else{
                    withdrawal += t->amount;
                    *totalWithdrawAmount += t->amount;
                }
                continue;
            }else{
                if (t->type == DEPOSIT){
                    nDeposits += 1;
                    deposit += t->amount;
                    *totalDepositAmount += t->amount;
                }else{
                    withdrawal += t->amount;
                    *totalWithdrawAmount += t->amount;
                }
                continue;
            }
        }
        //Pushes the final record.
        if (curY != 0){
            ReportThreeRecord r3;
            snprintf(r3.AccountName, sizeof r3.AccountName, "%s", acc->accName);
            r3.day = curD;
            r3.month = curM;
            r3.year = curY;
            r3.totalWithdrawal = withdrawal;
            r3.nDeposits = nDeposits;
            r3.totalDeposit = deposit;
            if (deposit > 0.0){
                double velocity = withdrawal/deposit * 100.0;
                r3.turnoverVelocity = velocity;
                if (velocity >= VELOCITY_LIMIT || withdrawal >= WITHDRAWAL_LIMIT){
                    r3.risk = SUSPICIOUS;
                }else{
                    r3.risk = NORMAL;
                }
            }else{
                r3.turnoverVelocity = 0.0;
                if (withdrawal >= WITHDRAWAL_LIMIT){
                    r3.risk = SUSPICIOUS;
                }else{
                    r3.risk = NORMAL;
                }
            }
            utarray_push_back(reportThreeRecords, &r3);
        }
    }
}

void generateReport4(UT_array *reportFourRecords){
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

        ReportFourRecord r4 = {0};
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
        UT_icd r1r_icd = {sizeof(ReportOneRecord), NULL, NULL, NULL};
        UT_array *reportOneRecords;
        utarray_new(reportOneRecords, &r1r_icd);

        generateReport1(reportOneRecords);

        utarray_sort(reportOneRecords, cmpR1);
        FILE *f1 = fopen(REPORT1, "w");
        if (!f1){
            printf("ERROR: Can't Create Report 1 File");
            return;
        }
        fprintf(f1, "AccountName,AccountAge,CurrentBalance,AverageDailyBalance,PrivelegeTier\n");
        for (ReportOneRecord *r1 = NULL; (r1 = utarray_next(reportOneRecords,r1));){
            nAccounts++;
            totalBalance += r1->CurrentBalance;
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
        UT_icd r2r_icd = {sizeof(ReportTwoRecord), NULL, NULL, NULL};
        UT_array *reportTwoRecords;
        utarray_new(reportTwoRecords, &r2r_icd);

        generateReport2(reportTwoRecords, reportOneRecords);

        utarray_sort(reportTwoRecords, cmpR2);
        FILE *f2 = fopen(REPORT2, "w");
        if (!f2){
            printf("ERROR: Can't Create Report 2 File");
            return;
        }
        fprintf(f2, "AccountName,CurrentBalance,ExpectedInterest\n");
        for (ReportTwoRecord *r2 = NULL; (r2 = utarray_next(reportTwoRecords,r2));){
            totalInterestExpense += r2->ExpectedInterest;
            fprintf(f2, "%s,%.2f,%.2f\n",
            r2->AccountName, r2->CurrentBalance, r2->ExpectedInterest
            );
        }
        fclose(f2);

        //Report 3
        UT_icd r3r_icd = {sizeof(ReportThreeRecord), NULL, NULL, NULL};
        UT_array *reportThreeRecords;
        utarray_new(reportThreeRecords, &r3r_icd);

        generateReport3(reportThreeRecords, &totalDepositAmount, &totalWithdrawAmount);

        utarray_sort(reportThreeRecords, cmpR3);;
        FILE *f3 = fopen(REPORT3, "w");
        if (!f3){
            printf("ERROR: Can't Create Report 3 File");
            return;
        }
        fprintf(f3, "AccountName,Date,TotalWithdrawalAmounts,NumberOfDeposits,TotalDepositAmounts,TurnoverVelocity,RiskFlagLevel\n");
        for (ReportThreeRecord *r3 = NULL; (r3 = utarray_next(reportThreeRecords,r3));){
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
        UT_icd r4r_icd = {sizeof(ReportFourRecord), NULL, NULL, NULL};
        UT_array *reportFourRecords;
        utarray_new(reportFourRecords, &r4r_icd);

        generateReport4(reportFourRecords);

        utarray_sort(reportFourRecords, cmpR4);
        FILE *f4 = fopen(REPORT4, "w");
        if (!f4){
            printf("ERROR: Can't Create Report 4 File");
            return;
        }
        fprintf(f4, "AccountName,AverageMonthlyLow,LowestMonthlyLow,SafeToLend,Stability,StabilityRating\n");
        for (ReportFourRecord *r4 = NULL; (r4 = utarray_next(reportFourRecords,r4));){
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
        for (ReportOneRecord *r1 = NULL; (r1 = utarray_next(reportOneRecords,r1));){
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
        for (ReportTwoRecord *r2 = NULL; (r2 = utarray_next(reportTwoRecords,r2));){
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
        for (ReportThreeRecord *r3 = NULL; (r3 = utarray_next(reportThreeRecords,r3));){
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
        for (ReportFourRecord *r4 = NULL; (r4 = utarray_next(reportFourRecords,r4));){
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