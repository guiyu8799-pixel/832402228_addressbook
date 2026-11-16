@echo off
REM ============================================================================
REM  One-click launcher for the address book.
REM
REM  Starts the Spring Boot application, waits until /api/health answers, and
REM  then opens the browser at http://localhost:8080/.
REM
REM  Usage: double-click this file, or run start.bat from a terminal.
REM ============================================================================

setlocal
set "ROOT=%~dp0"
set "JAR=%ROOT%target\addressbook-1.0.0.jar"
set "PORT=8080"

echo ============================================================
echo  Address book - local demonstration
echo ============================================================
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] "java" was not found on PATH.
    echo         Install a JDK 8 or newer and make sure java.exe is on PATH.
    goto :fail
)

if not exist "%JAR%" (
    echo [ERROR] The application jar is missing:
    echo         %JAR%
    echo.
    echo         Build it first with:
    echo             mvn clean package
    goto :fail
)

echo [1/2] Starting the application on port %PORT% ...
pushd "%ROOT%"
start "addressbook" cmd /c "java -Dfile.encoding=UTF-8 -jar "%JAR%""
popd

echo       Waiting for the API to become ready ...
set "READY="
for /l %%i in (1,1,60) do (
    if not defined READY (
        curl -s -o nul "http://localhost:%PORT%/api/health" && set "READY=1"
        if not defined READY timeout /t 1 >nul
    )
)

if not defined READY (
    echo [ERROR] The application did not answer on port %PORT% within 60 seconds.
    goto :fail
)

echo [2/2] Opening http://localhost:%PORT%/ ...
start "" "http://localhost:%PORT%/"
echo.
echo Done. Close the "addressbook" window to stop the server.
exit /b 0

:fail
echo.
echo Startup failed.
pause
exit /b 1
