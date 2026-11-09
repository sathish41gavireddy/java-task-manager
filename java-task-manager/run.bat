@echo off
REM Compile and start the Task Manager (Windows). Needs JDK 17 or newer.
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
java -cp out com.sathish.taskmanager.Main
