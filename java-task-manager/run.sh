#!/bin/sh
# Compile and start the Task Manager (Mac/Linux). Needs JDK 17 or newer.
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out com.sathish.taskmanager.Main
