#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <string.h>
#include <float.h>
#include "custom_types.h"
#include "user.h"

#define WITHDRAWAL_LIMIT 500000.0
#define VELOCITY_LIMIT 90.0

int cmpRiskReport(const void *x, const void *y){
    const RiskReportRecord *a = x, *b = y;
    
    if (a->year != b->year){
        return (a->year > b->year) - (a->year < b->year);
    }
    if (a->month != b->month){
        return (a->month > b->month) - (a->month < b->month);
    }
    if (a->day != b->day){
        return (a->day > b->day) - (a->day < b->day);
    }

    if (a->risk != b->risk){
        if (a->risk == SUSPICIOUS){
            return -1;
        }
        return 1;
    }



    if (tieBreaker == NAME){
        return strcasecmp(a->AccountName,b->AccountName);
    } else {
        return (a->AccountNumber > b->AccountNumber) - (a->AccountNumber < b->AccountNumber);
    }
}

void generateRiskReport(UT_array *reportThreeRecords, double *totalDepositAmount, double *totalWithdrawAmount){
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
                RiskReportRecord r3 = {0};
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
            RiskReportRecord r3;
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