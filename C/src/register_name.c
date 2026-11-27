/**
 * 
 * register_name.c
 * 
 * Contains the name registration submenu
 * 
 */

#include <string.h>
#include <stdio.h>
#include <stdbool.h>

#include "saveload.h"
#include "custom_types.h"
#include "console_input.h"
#include "user.h"

/**
 * @name registerName();
 * 
 * Lets user change the name of the user
 */
void registerName(){
    String temp;
    bool valid = false;

    printf("Register Account Name\n");
    
    while (!valid) {
        printf("Account Name: ");
        safeStringScanf(temp, STRING_LENGTH);
        trimWhitespace(temp);
        for (int i = 0 ; i < strlen(temp) ; i++) {
            if (temp[i] >= '0' && temp[i] <= '9'){
                printf("Invalid input!\n");
                return;
            }
            //Breaks CSV
            if (temp[i] == ','){
                printf("Invalid input!\n");
                return;
            }
        }

        //Checks if acc already exists
        if(accountFind(temp)) {
            printf("Name already registered!\n");
            return;
        }

        valid = true;
    }

    bool valid2 = false;
    double amount = 0.0;

    printf("Initial Deposit: ");
    while (!valid2){
        safeDoubleScanf(&amount);
        if (amount < 5000.0){
            printf("ERROR: Minimum initial deposit is 5,000.00\n");
            return;
        }
        if (amount > MAX_TRANSACTION){
            printf("ERROR: Maximum amont of money per transaction exceeded.\n");
            return;
        }

        valid2 = true;
    }

    long long number = lastAccountNumber + 1;
    accountTransact(temp, number, makeTransaction(DEPOSIT, amount));
    saveUser();
}


// void registerNameOld() {
//     String temp;
//     bool valid = false;
//     printf("Register Account Name\n");

//     while (!valid) {
//         printf("Account Name: ");
//         safeStringScanf(temp, STRING_LENGTH);
//         trimWhitespace(temp);
//         for (int i = 0 ; i < strlen(temp) ; i++) {
//             if (temp[i] >= '0' && temp[i] <= '9'){
//                 printf("Invalid input!\n");
//                 return;
//             }
//         }

//         valid = true;
//     }

//     strcpy(user.accountName, temp);

//     saveUser();
// }