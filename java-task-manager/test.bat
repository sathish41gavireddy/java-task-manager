@echo off
REM Compile everything and run the tests (Windows).
if not exist out mkdir out
dir /s /b src\*.java test\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
java -cp out com.sathish.taskmanager.TestRunner
