/**
 * 
 * saveload.c
 * 
 * Contains the functions for saving and loading the user
 * 
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include "user.h"
#include "saveload.h"

#define CSV_FILE "bank_transactions.csv"

/**
 * @name saveUser();
 * 
 * Auto-saves the user to csv file
 */
void saveUser() {
    FILE *f = fopen(CSV_FILE, "w");
    if (!f){
        return;
    }

    fprintf(f, "account_name,account_number,date,time,currency,amount,type,current_balance\n");

    for (LogEntry *e = NULL; (e = utarray_next(allTransactions, e));){
        Transaction *t = &e -> t;
        Account *acc = e -> acc;
        fprintf(f, "%s,%lld,%02d/%02d/%04d,%d:%02d:%02d,%s,%.2f,%s,%.2f\n",
            acc->accName, acc->accNumber,
            t->day, t->month, t->year, t->hour, t->minute, t->second,
            BASE_CURRENCY, t->amount,
            t->type == DEPOSIT? "deposit" : "withdrawal",
            t->balance);
    }
    fclose(f);

    // Account *acc, *temp;
    // HASH_ITER(hh, accounts, acc, temp){
    //     for (Transaction *t = NULL; (t = utarray_next(acc->transactions, t));){
    //         fprintf(f, "%s,%lld,%02d/%02d/%04d,%d:%02d:%02d,%s,%.2f,%s,%.2f\n",
    //             acc->accName, acc->accNumber,
    //             t->day, t->month, t->year, t->hour, t->minute, t->second,
    //             BASE_CURRENCY, t->amount,
    //             t->type == DEPOSIT? "deposit" : "withdraw",
    //             t->balance);
    //     }
    // }
    // fclose(f);
}

typedef struct{
    String name;
    long long number;
    Transaction t;
    size_t order;
} Row;

/**
 * TO DO
 * returns the first chronologically row
 * order maintains scan order
 */
static int cmpRow(const void *x, const void *y){
    const Row *a = x, *b = y;
    if (a->t.year != b->t.year){
        return a->t.year - b->t.year;
    }
    if (a->t.month != b->t.month){
        return a->t.month - b->t.month;
    }
    if (a->t.day != b->t.day){
        return a->t.day - b->t.day;
    }
    if (a->t.hour != b->t.hour){
        return a->t.hour - b->t.hour;
    }
    if (a->t.minute != b->t.minute){
        return a->t.minute - b->t.minute;
    }
    if (a->t.second != b->t.second){
        return a->t.second - b->t.second;
    }
    return (a->order > b-> order) - (a->order < b->order);
}

/**
 * @name loadUser();
 * 
 * Auto-load user to global var from csv file
 */
LoadData loadUser() {
    utarray_new(allTransactions, &log_icd);

    bool printCullReason = true;

    FILE *f = fopen(CSV_FILE, "r");
    if (!f){
        LoadData result;
        result.found = 0;
        result.loaded = 0;
        return result;
    }

    UT_icd row_icd = {sizeof(Row), NULL, NULL, NULL};
    UT_array *rows;
    utarray_new(rows, &row_icd);

    char line[256];
    char orig[256];
    size_t order = 0;
    fgets(line, sizeof line, f); /* skip header */

    int linesFound = 0;
    int linesLoaded = 0;
    while (fgets(line, sizeof line, f)){
        linesFound++;
        line[strcspn(line, "\r\n")] = '\0';
        snprintf(orig, sizeof orig, "%s", line);  /* copy BEFORE splitting */

        char *fields[8];
        int count = 0;
        char *p = line;
        while (count < 8) {
            fields[count++] = p;
            p = strchr(p, ',');
            if (!p){
                break;
            }
            *p++ = '\0';
        }
        if (count != 8){
            if (printCullReason){
                printf("Column Count (%d): %s\n", count, orig);
            }
            continue;
        }

        Row r = {0};

        snprintf(r.name, sizeof r.name, "%s", fields[0]);
        if (strlen(r.name) == 0){
            if (printCullReason){
                printf("Empty Name: %s\n", orig);
            }
            continue;
        }

        r.number = strtoll(fields[1], NULL, 10);

        if (sscanf(fields[2], "%d/%d/%d", &r.t.day, &r.t.month, &r.t.year) != 3){
            if (printCullReason){
                printf("Date: %s\n", orig);
            }
            continue;
        }
        if (sscanf(fields[3], "%d:%d:%d", &r.t.hour, &r.t.minute, &r.t.second) != 3){
            if (printCullReason){
                printf("Time: %s\n", orig);
            }
            continue;
        }

        //Field 4 doesn't matter its default currency

        r.t.amount = strtod(fields[5], NULL);

        if (strcmp(fields[6], "deposit") == 0){
            r.t.type = DEPOSIT;
        }else if (strcmp(fields[6], "withdrawal") == 0){
            r.t.type = WITHDRAW;
        }else{
            if (printCullReason){
                printf("Type: %s\n", orig);
            }
            continue;
        }

        r.order = order++;
        utarray_push_back(rows, &r);
        linesLoaded++;
    }
    fclose(f);

    utarray_sort(rows, cmpRow);

    for (Row *r = NULL; (r = utarray_next(rows,r));){
        accountTransact(r->name, r->number, r->t);
    }

    utarray_free(rows);
    LoadData result;
    result.found = linesFound;
    result.loaded = linesLoaded;
    return result;
}


/**
 * @name saveUserOld();
 * 
 * Auto-saves the user to bin file
 */
// void saveUserOld() {
//     FILE* ptr;
//     ptr = fopen("./save.bin", "wb");

//     if (ptr != NULL) {
//         fwrite(&user, sizeof(User), 1, ptr);
//         fclose(ptr);
//     }

// }

/**
 * @name loadUserOld();
 * 
 * Auto-load user to global var from bin file
 */
// void loadUserOld() {
//     FILE* ptr;
//     ptr = fopen("./save.bin", "rb");

//     if (ptr != NULL) {
//         fread(&user, sizeof(User), 1, ptr);
//         fclose(ptr);
//     } 
    
//     // Initialize defaults when file does not exist
//     else {
//         defaultConfig();
//         saveUser();
//         loadUser();
//     }
// }