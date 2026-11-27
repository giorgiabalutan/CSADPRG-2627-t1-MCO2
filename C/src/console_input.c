/**
 * 
 * console_input.c
 * 
 * Custom input functions for safe console input
 * 
 * DISCLAIMER: Code taken and modified from Mikaelo's CCPROG2 MP
 * However, code is originally written by Mikaelo. 
 * Original code comments preserved, modified based on new functionality
 * 
 */

#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <ctype.h>

#include "user.h"

/**
 * @name trimWhitespace();
 * 
 * Trims the string given to it
 * Code adapted from: https://stackoverflow.com/a/122721
 * 
 * @param   string  Pointer to the string to trim
 */
void trimWhitespace(char *str) {
    char *end, *start;

    // Get true start of string
    start = str;
    while(*start && isspace((unsigned char)*start)) start++;

    // Trim trailing space
    end = str + strlen(str) - 1;
    while(end > str && isspace((unsigned char)*end)) end--;

    // Write new null terminator character
    end[1] = '\0';

    // Trim leading space
    memmove(str, start, (end - start) + 2);
}

/*

	@name	flushBuffer();

    @brief	Clears stdin from any extraneous inputs

*/
void flushBuffer() {
    char character;
    // getchar() to receive inputs, starts if and repeats until character is \n or EOF
    while ( ( character = getchar()) != '\n' && character != EOF);
}

/*

	@name	safeStringScanf();

    @brief	Improved Scanf for strings
    @details Improved function to input strings from user. Does not stop at whitespaces, and only stops at newlines. Also flushes buffer

    @param  string          Character pointer (string) to output the data to
    @param  arrayLength     Length of string

*/
int safeStringScanf(char *string, int arrayLength) {
    
    int flag = 0, overflow = 0;

    // EXAMPLE: 31 length == 30 characters consumed, null byte appended to end
    for (int i = 0 ; i < arrayLength && !flag ; i++) {
        char temp;
        scanf("%c", &temp);

        if (temp != '\n' && temp != EOF){
            // terminate string if too long
            if (i == arrayLength - 1){
                overflow = 1;
                string[i] = '\0';
            }
            
            // Stores character to string
            else 
                string[i] = temp;
        }

        // Terminates if it IS \n or EOF
        else {
            string[i] = '\0';
            flag = 1;
        }

    }

    if (!flag){
        flushBuffer();
    }

    return overflow;
}

/*

	@name	safeCharScanf();

    @brief	Improved Scanf for character, flushes buffer after input.

    @param  character   Character pointer to store inputted character to

*/
void safeCharScanf(char *character) {

    // Scans for character
    scanf("%c", character);

    // If \n or EOF, terminate 
    if (*character == '\n' || *character == EOF)
        *character = '\0';

    else
        // Flush stdin
        flushBuffer();

}

/*

	@name	safeIntScanf();

    @brief	Improved Scanf for integer. Checks if valid number and flushes buffer after input.

    @param  number  Integer pointer to store inputted number to

*/
void safeIntScanf(int *number) {
    String tempString = "";
    int temp = 0, length = 0;
    bool valid = false, badText = false;

    // While not valid...
    while (!valid) {
        temp = 0;
        badText = false;

        scanf("%s", tempString);

        // Check for bad text
        length = strlen(tempString);
        bool bracketNotation = (length > 2 && tempString[0] == '[' && tempString[length - 1] == ']');
        for (int i = bracketNotation ; i < length - bracketNotation && badText == 0; i++) {
            char currentChar = tempString[i];

            bool isDigit = (currentChar >= '0' && currentChar <= '9');

            if (!isDigit) {
                badText = true;
            }
        }

        // Retry if invalid
        if (badText) {
            printf("Invalid Input!\n");
            printf("\n");
            printf("Re-enter Input: ");
        }

        else {
            for (int i = 0 ; i < strlen(tempString) ; i++) {
                if (tempString[i] >= '0' && tempString[i] <= '9') {
                    temp *= 10;
                    temp += tempString[i] - '0';
                }
            }

            *number = temp;
            valid = true;
        }

        if (temp != '\n' && temp != EOF)
            // remove any other input (like \n, malformed inputs)
            flushBuffer();
    }
} 

/*

	@name	safeDoubleScanf();

    @brief	Improved Scanf for double. Checks if valid number and flushes buffer after input.

    @param  number  double pointer to store inputted number to

*/
void safeDoubleScanf(double *number) {
    int valid = 0;
    double temp = 0.0;

    // While not valid...
    while (!valid) {
        // If scanf input is valid
        if (scanf("%lf", &temp) == 1) {
            // Set value to pointer
            *number = temp;
            valid = 1;
        }

        // Retry if invalid
        else {
            printf("Invalid Input!\n");
            printf("\n");
            printf("Re-enter Input: ");
        }

        if (temp != '\n' && temp != EOF)
            // remove any other input (like \n, malformed inputs)
            flushBuffer();
    }
} 

/*

	@name	menuInputInt();

    @brief	Menu input system using safeIntScanf()

    @param  min     Integer of the minimum number that the menu has 
    @param  max     Integer of the maximum number that the menu has 
    @param  prompt  String of the menu's prompt 

*/
int menuInputInt(int min, int max, char *prompt) {
    int input = 0, valid = 0;

    // while invalid...
    while (!valid) {

        printf("%s: ", prompt);
        // Get input from user
        safeIntScanf(&input);

        // If less than min / more than min, invalid
        if (input < min || input > max) {
            printf("ERROR: Invalid selection.\n");
        }

        // else, exit loop
        else {
            valid = 1;
        }

    }

    return input;
}