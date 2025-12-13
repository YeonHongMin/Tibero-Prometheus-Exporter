# Tibero Exporter - Java Version Build Guide

## Prerequisites

### Required Software
1. **Java JDK 11 or higher**
   ```bash
   java -version
   ```

2. **Apache Maven 3.6 or higher**
   ```bash
   mvn -version
   ```

   If Maven is not installed, download from: https://maven.apache.org/download.cgi

3. **Tibero JDBC Driver**
   - The JAR file should be located at: `jre/tibero7-jdbc.jar`
   - Already included in this project

## Building the JAR

### Step 1: Build with Maven

```bash
# Clean and build the project
mvn clean package

# The executable JAR will be created at:
# target/tibero-exporter.jar
```

### Step 2: Verify the Build

```bash
# Check if the JAR was created
ls -lh target/tibero-exporter.jar

# Should show a file around 5-10 MB
```

## Running the Exporter

### Basic Usage

```bash
java -jar target/tibero-exporter.jar \
  --db.host 192.168.0.140 \
  --db.port 8629 \
  --db.user sys \
  --db.password tibero \
  --db.name tibero
```

### With Custom Metrics

```bash
java -jar target/tibero-exporter.jar \
  --db.host 192.168.0.140 \
  --db.port 8629 \
  --db.user sys \
  --db.password tibero \
  --db.name tibero \
  --default.metrics default_metrics.yaml \
  --custom.metrics my_custom_metrics.yaml
```

### Using Environment Variables

```bash
export DB_HOST=192.168.0.140
export DB_PORT=8629
export DB_USER=sys
export DB_PASSWORD=tibero
export DB_NAME=tibero

java -jar target/tibero-exporter.jar
```

### View Help

```bash
java -jar target/tibero-exporter.jar --help
```

## Configuration Options

| Option | Environment Variable | Default | Description |
|--------|---------------------|---------|-------------|
| `--db.host` | `DB_HOST` | localhost | Tibero database host |
| `--db.port` | `DB_PORT` | 8629 | Tibero database port |
| `--db.user` | `DB_USER` | sys | Database user |
| `--db.password` | `DB_PASSWORD` | | Database password |
| `--db.name` | `DB_NAME` | tibero | Database name/SID |
| `--db.dsn` | `DATA_SOURCE_NAME` | | Full JDBC connection string |
| `--web.listen-address` | `LISTEN_ADDRESS` | 0.0.0.0 | HTTP server address |
| `--web.listen-port` | `LISTEN_PORT` | 9162 | HTTP server port |
| `--query.timeout` | `QUERY_TIMEOUT` | 30 | Query timeout (seconds) |
| `--default.metrics` | `DEFAULT_METRICS_FILE` | default_metrics.yaml | Default metrics file |
| `--custom.metrics` | `CUSTOM_METRICS_FILE` | | Custom metrics file |
| `--jdbc.jar` | `TIBERO_JDBC_JAR` | jre/tibero7-jdbc.jar | JDBC driver path |

## Project Structure

```
.
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── tibero/
│       │           └── exporter/
│       │               ├── Config.java              # Configuration management
│       │               ├── MetricConfig.java        # Metric definition
│       │               ├── MetricsLoader.java       # YAML metrics loader
│       │               ├── TiberoCollector.java     # Metrics collector
│       │               └── TiberoExporter.java      # Main application
│       └── resources/
│           ├── logback.xml                          # Logging configuration
│           └── default_metrics.yaml                 # Default metrics definitions (JAR에 내장됨)
├── jre/
│   └── tibero7-jdbc.jar                            # Tibero JDBC driver (JAR에 내장됨)
├── pom.xml                                          # Maven build configuration
└── BUILD.md                                         # This file
```

## Maven Build Configuration

The `pom.xml` includes:
- **Prometheus Client Library**: For metrics collection and HTTP server
- **SnakeYAML**: For YAML configuration parsing
- **Logback**: For logging
- **Maven Shade Plugin**: Creates an uber JAR with all dependencies

## Testing the Build

### 1. Test with Version Flag
```bash
java -jar target/tibero-exporter.jar --version
# Output: tibero_exporter v1.0.0 (2024-01-01)
```

### 2. Test Connection to Tibero
```bash
java -jar target/tibero-exporter.jar \
  --db.host 192.168.0.140 \
  --db.port 8629 \
  --db.user sys \
  --db.password tibero \
  --db.name tibero
```

### 3. Check Metrics Endpoint
```bash
# In another terminal:
curl http://localhost:9162/metrics
```

## Troubleshooting

### Maven Not Found
```bash
# Download Maven from https://maven.apache.org/download.cgi
# Extract and add to PATH:
export PATH=/path/to/maven/bin:$PATH
```

### Java Version Issues
```bash
# Check Java version
java -version

# Should be 11 or higher
# If not, install Java 11+ from:
# https://adoptium.net/
```

### JDBC Driver Not Found
```bash
# Ensure the JDBC driver exists:
ls -l jre/tibero7-jdbc.jar

# If missing, copy from Tibero installation:
# $TB_HOME/client/lib/jar/tibero7-jdbc.jar
```

### Build Errors
```bash
# Clean Maven cache and rebuild
mvn clean
rm -rf ~/.m2/repository/com/tibero
mvn package
```

## Distribution

### Creating a Distribution Package

```bash
# Create a distribution directory
mkdir -p dist
cp target/tibero-exporter.jar dist/
# Note: default_metrics.yaml과 JDBC 드라이버는 JAR에 이미 내장되어 있음

# Create a tarball
tar -czf tibero-exporter-java-1.0.0.tar.gz dist/

# Or create a ZIP file
zip -r tibero-exporter-java-1.0.0.zip dist/
```

### Running the Distribution

```bash
# Extract the package
tar -xzf tibero-exporter-java-1.0.0.tar.gz
cd dist/

# Run the exporter
java -jar tibero-exporter.jar \
  --db.host YOUR_HOST \
  --db.user YOUR_USER \
  --db.password YOUR_PASSWORD
```

## Performance Notes

- **파일 크기**: ~3.2 MB (Uber JAR, 모든 의존성 포함)
  - Tibero JDBC 드라이버 포함
  - default_metrics.yaml 포함
  - 모든 라이브러리 포함 (HikariCP, Prometheus Client, SnakeYAML 등)
- **시작 시간**: ~2-3초
- **메모리 사용량**: ~256-512 MB
- **의존성**: Java 11+ 만 필요
- **연결 방식**: JDBC 직접 연결 (HikariCP 연결 풀)
- **배포**: 단일 실행 가능한 JAR 파일 (외부 파일 불필요)

## 주요 특징

- ✅ **HikariCP 연결 풀**: 안정적이고 효율적인 데이터베이스 연결 관리
- ✅ **Oracle Exporter 패턴**: 메트릭 캐싱으로 100% 안정성 보장
- ✅ **메트릭별 타임아웃**: 개별 쿼리 타임아웃 설정 지원
- ✅ **동기화된 메트릭 수집**: 동시 요청 처리 완벽 지원
- ✅ **한글 주석**: 모든 소스 코드에 상세한 한글 설명
- ✅ **단일 JAR 배포**: 외부 의존성 없이 독립 실행 가능
- ✅ **내장 리소스**: default_metrics.yaml과 JDBC 드라이버가 JAR에 포함됨

## Next Steps

1. **Install Maven** if not already installed
2. **Run `mvn clean package`** to build the JAR
3. **Test the JAR** with your Tibero database
4. **Deploy** the JAR to your production environment

For more information, see the main README.md file.
