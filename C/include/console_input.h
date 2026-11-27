/**
 * 
 * console_input.h
 * 
 * Header file for console_input.c
 * ( taken from Mikaelo's CCPROG2 MP, code originally written by Mikaelo. )
 * 
 */

#ifndef CONSOLE_INPUT_H
#define CONSOLE_INPUT_H

void trimWhitespace(char *str);
void flushBuffer();
int safeStringScanf(char *string, int arrayLength); 
void safeCharScanf(char *character);
void safeIntScanf(int *number);
void safeDoubleScanf(double *number); 
int menuInputInt(int min, int max, char *prompt);

#endif