#!/usr/bin/env bash
cd "$(dirname "$0")"
rm -rf out
javac -d out $(find src -name "*.java")
cd src
java -cp ../out BankingApp
