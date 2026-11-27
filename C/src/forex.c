/**
 * 
 * forex.c
 * 
 * Contains the foreign exchange (currency exchange, rate update) submenus
 * 
 */

#include <string.h>
#include <stdio.h>
#include <stdbool.h>

#include "saveload.h"
#include "console_input.h"
#include "user.h"

/**
 * @name currencyExchange();
 * 
 * Lets the user convert between currencies
 */
void currencyExchange() {
    bool breakLoop = false;
    char choice = 0;
    while (!breakLoop) {
        {
            double sourceAmount = 0, sourceInPHP;
            int sourceCurrency, destCurrency;
            printf("Foreign Currency Exchange\n");

            // === [ SOURCE CURRENCY SELECTION ] ===
            printf("Source Currency Option:\n");
            
            // For every currency...
            for (int i = 0 ; i < forex.length ; i++) {
                // Print the conversion (src * rate)
                printf("[%d] %s (%s)\n", 
                    i + 1, 
                    forex.rates[i].currencyName, 
                    forex.rates[i].currencyCode
                );
            };

            printf("\n");

            // Select source currency
            sourceCurrency = menuInputInt(1, forex.length, "Source Currency: ") - 1;
            printf("Source Amount: ");
            safeDoubleScanf(&sourceAmount);

            printf("\n");

            // === [ EXCHANGED CURRENCY SELECTION ] ===
            printf("Exchanged Currency Options:\n");
            // For every currency...
            for (int i = 0 ; i < forex.length ; i++) {
                // Print the conversion (src * rate)
                printf("[%d] %s (%s)\n", 
                    i + 1, 
                    forex.rates[i].currencyName, 
                    forex.rates[i].currencyCode
                );
            };

            printf("\n");

            // Select source currency
            destCurrency = menuInputInt(1, forex.length, "Exchange Currency: ") - 1;

            printf("\n");

            // Print source currency, amount
            if (forex.rates[sourceCurrency].rateFromPHP == 0 || forex.rates[destCurrency].rateFromPHP == 0) {
                printf("ERROR: Rate not set.\n");
            }

            else {
                sourceInPHP = sourceAmount / forex.rates[sourceCurrency].rateFromPHP;
                printf("Exchange Amount: %.2lf\n", sourceInPHP * forex.rates[destCurrency].rateFromPHP);
            }
        }

        while (!breakLoop) {
            printf("Convert another ([Y]es, [N]o)? ");
            safeCharScanf(&choice);
            if (choice >= 'A' && choice <= 'Z') choice += 0x20;
            
            if (choice == 'n') breakLoop = true;
            else if (choice == 'y') break;
            else printf("ERROR: Invalid selection!\n");
        }
    }
}

/**
 * @name recordExchange();
 * 
 * Lets user update the rates
 */
void recordExchange() {
    bool valid = false;
    int select = 0;
    double temp = 0.0;
    printf("Record Exchange\n");

    // For every currency...
    for (int i = 0; i < forex.length ; i++) {
        printf("[%d] %s (%s)\n", 
            i + 1, 
            forex.rates[i].currencyName, 
            forex.rates[i].currencyCode
        );
    }

    printf("\n");

    // Get selected currency
    select = menuInputInt(1, forex.length, "Select Foreign Currency") - 1;
    
    // Let user change selected rate for currency
    while (!valid) {
        printf("Exchange Rate: ");
        safeDoubleScanf(&temp);

        if (temp <= 0.0) printf("ERROR: Invalid input!\n");
        else { forex.rates[select].rateFromPHP = temp; valid = true; }
    }

    saveUser();
}