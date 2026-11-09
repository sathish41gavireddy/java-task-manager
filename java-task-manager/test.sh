#!/bin/sh
# Compile everything and run the tests (Mac/Linux).
mkdir -p out
javac -d out $(find src test -name "*.java")
java -cp out com.sathish.taskmanager.TestRunner
