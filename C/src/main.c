/********************
 * Last names: Eborde
 * Language: C
 * Paradigm(s): Procedural Programming, Modular Programming (Imperative)
 ********************/

/**
 * 
 * main.c
 * 
 * Main entrypoint of program
 * 
 */

#include <stdio.h>

#include "user.h"
#include "saveload.h"
#include "main_menu.h"

/**
 * @name main();
 * 
 * Main function of program
 */
int main() {
    // Load user into global var
    defaultConfig();
    loadUser();

    // Load main menu
    mainMenu();

    // Saves data
    saveUser();

    //Clears memory
    dbFreeAll();

    return 0;
}