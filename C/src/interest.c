/**
 * 
 * interest.c
 * 
 * Contains the interest calculator submenu
 * 
 */

#include <stdio.h>
#include <string.h>
#include <stdbool.h>

#include "console_input.h"
#include "custom_types.h"
#include "user.h"

/**
 * @name interest();
 * 
 * Let user see interest given inputted number of dats
 */
void interest() {
    bool breakLoop = false;
    int days = 0;
    String interestBuffer, balanceBuffer;
    char temp = 'n';
    String name;

    //TO DO: Find a way to check if hash table is empty
    if (false) {}
    // if (strlen(user.accountName) <= 0) printf("ERROR: Please register your account first by registering your name!\n");
    else {
        printf("Show Interest Amount\n");

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
        double currentBalance = acc->balance;

        printf("Current Balance: %.2lf\n", currentBalance);
        printf("Currency: %s\n", BASE_CURRENCY);
        printf("Interest Rate: %d\n", (int) (ANNUAL_INTEREST * 100));

        printf("\n");

        // While temp is N...
        while (!breakLoop) {
            printf("Total Number of Days: ");
            safeIntScanf(&days);

            if (days <= 0) printf("ERROR: Invalid input!");
            else {
                printf("Day | Interest | Balance |\n");
                // For every day...
                for (int i = 1 ; i <= days ; i++) {
                    // Calculate interest
                    double interest = currentBalance * (ANNUAL_INTEREST / 365);
                    currentBalance += interest;

                    // Generate format
                    sprintf(interestBuffer, "%.2lf", interest);
                    sprintf(balanceBuffer, "%.2lf", currentBalance);

                    // Print
                    printf("%-4d| %-9s| %-8s|\n", i, interestBuffer, balanceBuffer);
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