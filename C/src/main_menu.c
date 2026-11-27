/**
 * 
 * main_menu.c
 * 
 * Contains the main menu loop
 * 
 */

#include <stdio.h>
#include <stdbool.h>

#include "console_input.h"
#include "custom_types.h"
#include "forex.h"
#include "deposit_withdraw.h"
#include "register_name.h"
#include "interest.h"
#include "generate_report.h"

/**
 * @name mainMenu();
 * 
 * Main menu of the program
 */
void mainMenu() {
    // Set menu options
    VoidMenu menu = {
        {
            { "Register Account Name", registerName },
            { "Deposit Amount", deposit },
            { "Withdraw Amount", withdraw },
            { "Currency Exchange", currencyExchange },
            { "Record Exchange Rates", recordExchange },
            { "Show Interest Amount", interest },
            { "Generate Reports", generateReport},
        },
        7
    };
    int select = 0;
    bool exit = false;
    
    // While exit bool is not true...
    while (!exit) {
        printf("Select Transaction:\n");
        // Print all options
        for (int i = 0 ; i < menu.length ; i++) {
            printf("[%d] %s\n", i + 1, menu.functions[i].functionName);
        }

        // Print exit function
        printf("[%d] Exit\n", menu.length + 1);

        printf("\n");

        // Get user input
        select = menuInputInt(1, menu.length + 1, "Choice");

        // Run function if not exit
        if (select != menu.length + 1)
            menu.functions[select - 1].ptr();

        else
            exit = true;

        printf("\n");
    }
}