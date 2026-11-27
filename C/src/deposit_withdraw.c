/**
 * 
 * deposit_withdraw.c
 * 
 * Contains the deposit and withdraw submenus
 * 
 */

#include <stdio.h>
#include <string.h>
#include <stdbool.h>

#include "saveload.h"
#include "console_input.h"
#include "user.h"

/**
 * @name deposit();
 * 
 * Lets the user make a deposit
 */
void deposit() {
    bool breakLoop = false;
    char temp = 'N';
    double depositAmount = 0.0;
    String name;

    //TO DO: Find a way to check if hash table is empty
    if (false) {}
    // if (strlen(user.accountName) <= 0) printf("ERROR: Please register your account first by registering your name!\n");
    else {
        printf("Deposit Amount\n");

        bool valid = false;
        while (!valid) {
            printf("Account Name: ");
            safeStringScanf(name, STRING_LENGTH);
            trimWhitespace(name);
            for (int i = 0 ; i < strlen(name) ; i++) {
                if (name[i] >= '0' && name[i] <= '9'){
                    printf("Invalid input!\n");
                    return;
                }
            }

            //Checks if acc exists
            if(!accountFind(name)) {
                printf("ERROR: Account not found.\n");
                return;
            }

            valid = true;
        }

        Account *acc = accountFind(name);

        printf("Current Balance: %.2lf\n", acc->balance);
        printf("Currency: %s\n", BASE_CURRENCY);
        printf("\n");

        // While exit char is N...
        while (!breakLoop) {
            {
                printf("Deposit Amount: ");
                safeDoubleScanf(&depositAmount);

                // Get checks to ensure rule enforcement
                bool rangeCheck = depositAmount > 0.0,
                    transactionCheck = depositAmount < MAX_TRANSACTION,
                    balanceCheck = acc->balance + depositAmount < MAX_ACCOUNT_BALANCE;

                // If input is less than 0
                if (!rangeCheck) {
                    printf("ERROR: Input must be more than zero.\n");
                }

                // If input is more than transaction limit
                else if (!transactionCheck) {
                    printf("ERROR: Maximum amont of money per transaction exceeded.\n");
                }

                // If final balance is more than account limit
                else if (!balanceCheck) {
                    printf("ERROR: Balance will exceed account limit\n");
                }

                else {
                    accountTransact(acc->accName, acc->accNumber, makeTransaction(DEPOSIT, depositAmount));
                    saveUser();
                    printf("Updated Balance: %.2lf\n", acc->balance);
                }
            }

            while (!breakLoop) {
                printf("Back to Main Menu? ([Y]es, [N]o)? ");
                safeCharScanf(&temp);
                if (temp >= 'A' && temp <= 'Z') temp += 0x20;
                
                if (temp == 'y') breakLoop = true;
                else if (temp == 'n') break;
                else printf("ERROR: Invalid selection!\n");
            }
        }
    }
}

/**
 * @name withdraw();
 * 
 * Lets user withdraw from the account balance
 */
void withdraw() {
    bool breakLoop = false;
    char temp = 'N';
    double withdrawAmount = 0.0;
    String name;

    //TO DO: Find a way to check if hash table is empty
    if (false) {}
    // if (strlen(user.accountName) <= 0) printf("ERROR: Please register your account first by registering your name!\n");
    else {
        printf("Withdraw Amount\n");

        bool valid = false;
        while (!valid) {
            printf("Account Name: ");
            safeStringScanf(name, STRING_LENGTH);
            trimWhitespace(name);
            for (int i = 0 ; i < strlen(name) ; i++) {
                if (name[i] >= '0' && name[i] <= '9'){
                    printf("Invalid input!\n");
                    return;
                }
            }

            //Checks if acc exists
            if(!accountFind(name)) {
                printf("ERROR: Account not found.\n");
                return;
            }

            valid = true;
        }

        Account *acc = accountFind(name);

        printf("Current Balance: %.2lf\n", acc->balance);
        printf("Currency: %s\n", BASE_CURRENCY);
        printf("\n");

        while (!breakLoop) {
            {
                printf("Withdraw Amount: ");
                safeDoubleScanf(&withdrawAmount);

                // Get checks to ensure rule enforcement
                bool rangeCheck = withdrawAmount > 0.0,
                    transactionCheck = withdrawAmount < MAX_TRANSACTION,
                    balanceCheck = acc->balance - withdrawAmount >= -0.0000000001;

                // If input is less than 0
                if (!rangeCheck) {
                    printf("ERROR: Input should be more than zero.\n");
                }

                // If input is more than transaction limit
                else if (!transactionCheck) {
                    printf("ERROR: Maximum amont of money per transaction exceeded.\n");
                }

                // If final balance is negative
                else if (!balanceCheck) {
                    printf("ERROR: Withdrawn amount should be less than or equal to current balance.\n");
                }

                else {
                    accountTransact(acc->accName, acc->accNumber, makeTransaction(WITHDRAW, withdrawAmount));
                    saveUser();

                    if (acc->balance < 0) acc->balance = 0;

                    printf("Updated Balance: %.2lf\n", acc->balance);
                }
            }

            while (!breakLoop) {
                printf("Back to Main Menu? ([Y]es, [N]o)? ");
                safeCharScanf(&temp);
                if (temp >= 'A' && temp <= 'Z') temp += 0x20;
                
                if (temp == 'y') breakLoop = true;
                else if (temp == 'n') break;
                else printf("ERROR: Invalid selection!\n");
            }
        }
    }
}