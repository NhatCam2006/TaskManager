@echo off
echo ========================================
echo    CHAT HISTORY DELETION TOOL
echo ========================================
echo.
echo WARNING: This will permanently delete ALL chat messages and files!
echo.
pause

cd /d "%~dp0"

echo Compiling Java classes...
javac -cp "lib/*;src/main/java" src/main/java/com/example/taskmanagerv3/util/ChatDataCleaner.java

if %ERRORLEVEL% NEQ 0 (
    echo Failed to compile. Please check for errors.
    pause
    exit /b 1
)

echo.
echo Running Chat Data Cleaner...
java -cp "lib/*;src/main/java" com.example.taskmanagerv3.util.ChatDataCleaner

echo.
echo Done.
pause
