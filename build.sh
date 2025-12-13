#!/bin/bash
# Build script for Tibero Exporter Java version (Linux/macOS)

set -e

echo "========================================"
echo "Tibero Exporter - Java Build Script"
echo "========================================"
echo

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven is not installed or not in PATH"
    echo "Please install Maven from: https://maven.apache.org/download.cgi"
    exit 1
fi

echo "[INFO] Maven found"
mvn -version
echo

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not in PATH"
    echo "Please install Java 11+ from: https://adoptium.net/"
    exit 1
fi

echo "[INFO] Java found"
java -version
echo

# Check if JDBC driver exists
if [ ! -f "jre/tibero7-jdbc.jar" ]; then
    echo "[ERROR] Tibero JDBC driver not found at: jre/tibero7-jdbc.jar"
    echo "Please copy the JDBC driver from your Tibero installation"
    exit 1
fi

echo "[INFO] Tibero JDBC driver found"
echo

# Clean and build
echo "[INFO] Building project with Maven..."
echo
mvn clean package

echo
echo "========================================"
echo "Build completed successfully!"
echo "========================================"
echo
echo "Executable JAR created at:"
echo "  target/tibero-exporter.jar"
echo
echo "To run the exporter:"
echo "  java -jar target/tibero-exporter.jar --db.host YOUR_HOST --db.user YOUR_USER --db.password YOUR_PASSWORD"
echo
echo "For more options:"
echo "  java -jar target/tibero-exporter.jar --help"
echo
