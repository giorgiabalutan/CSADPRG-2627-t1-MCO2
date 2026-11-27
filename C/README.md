# CSADPRG - MCO1: Banking and Currency App (C)

*Author: EBORDE, Mikaelo*

This is the C component of CSADPRG MCO1, a Banking and Currency App.

## How to Compile

### Pre-requisites

- Any C compiler, however, this was developed in, and compile script `./compile.sh` uses, GCC.
- (optional) Any Bash shell to run the compilation script.

### Bash Script

1. Open / Change Directory to this folder. (`.../C`)
2. Run compile.sh. (`./compile.sh`)

### Manual

1. Open your terminal, or change directory, to this folder
2. Run:

```sh
gcc -Wall -std=c99 -I ./include ./src/*.c -o csadprg-mco1-c && csadprg-mco1-c.exe
```