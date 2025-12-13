@echo off
REM Build script for Tibero Exporter Java version (Windows)

echo ========================================
echo Tibero Exporter - Java Build Script
echo ========================================
echo.

REM Check if Maven is installed
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven is not installed or not in PATH
    echo Please install Maven from: https://maven.apache.org/download.cgi
    exit /b 1
)

echo [INFO] Maven found
mvn -version
echo.

REM Check if Java is installed
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed or not in PATH
    echo Please install Java 11+ from: https://adoptium.net/
    exit /b 1
)

echo [INFO] Java found
java -version
echo.

REM Check if JDBC driver exists
if not exist "jre\tibero7-jdbc.jar" (
    echo [ERROR] Tibero JDBC driver not found at: jre\tibero7-jdbc.jar
    echo Please copy the JDBC driver from your Tibero installation
    exit /b 1
)

echo [INFO] Tibero JDBC driver found
echo.

REM Clean and build
echo [INFO] Building project with Maven...
echo.
call mvn clean package

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo Executable JAR created at:
echo   target\tibero-exporter.jar
echo.
echo To run the exporter:
echo   java -jar target\tibero-exporter.jar --db.host YOUR_HOST --db.user YOUR_USER --db.password YOUR_PASSWORD
echo.
echo For more options:
echo   java -jar target\tibero-exporter.jar --help
echo.

exit /b 0
