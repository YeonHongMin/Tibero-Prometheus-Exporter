# Tibero Database Prometheus Exporter

Tibero 데이터베이스용 Prometheus Exporter입니다. Oracle DB Exporter([oracle-db-appdev-monitoring](https://github.com/oracle/oracle-db-appdev-monitoring))의 패턴을 참고하여 Java로 작성되었습니다.

## 주요 기능

- **HikariCP 연결 풀**: 안정적이고 효율적인 데이터베이스 연결 관리
- **Oracle Exporter 패턴**: 메트릭 캐싱으로 100% 안정성 보장
- **메트릭별 타임아웃**: 개별 쿼리에 대한 타임아웃 설정 지원
- **동기화된 메트릭 수집**: 동시 요청 처리 완벽 지원
- **YAML 기반 메트릭 정의**: 유연한 커스텀 메트릭 설정
- **한글 주석**: 모든 소스 코드에 상세한 한글 설명

## 요구사항

- **Java Runtime**: JRE 11 이상
- **Tibero JDBC 드라이버**: tibero7-jdbc.jar (JAR 파일에 내장됨)
- **메트릭 정의 파일**: default_metrics.yaml (JAR 파일에 내장됨, 선택적으로 외부 파일 사용 가능)

## 빌드

### Maven을 사용한 빌드

```bash
# 빌드 스크립트 실행 (Windows)
build.bat

# 빌드 스크립트 실행 (Linux/macOS)
./build.sh

# 또는 Maven 직접 실행
mvn clean package
```

빌드가 완료되면 `target/tibero-exporter.jar` (3.2MB) 파일이 생성됩니다.

자세한 빌드 방법은 [BUILD.md](BUILD.md)를 참고하세요.

## 실행

### 기본 실행

```bash
# JAR에 내장된 default_metrics.yaml 사용 (권장)
java -jar target/tibero-exporter.jar \
    --db.host 192.168.0.140 \
    --db.port 8629 \
    --db.user mon \
    --db.password mon \
    --db.name prod \
    --web.listen-port 9162

# 외부 default_metrics.yaml 파일 사용 (선택사항)
java -jar target/tibero-exporter.jar \
    --db.host 192.168.0.140 \
    --db.port 8629 \
    --db.user mon \
    --db.password mon \
    --db.name prod \
    --web.listen-port 9162 \
    --default.metrics /path/to/default_metrics.yaml
```

### 환경 변수 사용

```bash
export DB_HOST=192.168.0.140
export DB_PORT=8629
export DB_USER=mon
export DB_PASSWORD=mon
export DB_NAME=prod
export LISTEN_PORT=9162

java -jar target/tibero-exporter.jar
```

## 설정

### 명령줄 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--db.host` | Tibero 호스트 주소 | localhost |
| `--db.port` | Tibero 포트 | 8629 |
| `--db.user` | 데이터베이스 사용자 | sys |
| `--db.password` | 데이터베이스 비밀번호 | - |
| `--db.name` | 데이터베이스 이름/SID | tibero |
| `--db.dsn` | 전체 DSN 연결 문자열 | - |
| `--web.listen-address` | HTTP 서버 바인딩 주소 | 0.0.0.0 |
| `--web.listen-port` | HTTP 서버 포트 | 9162 |
| `--query.timeout` | 쿼리 타임아웃 (초) | 30 |
| `--default.metrics` | 기본 메트릭 파일 경로 (외부 파일 지정 시 사용, 미지정 시 JAR 내장 버전 사용) | default_metrics.yaml (JAR 내장) |
| `--custom.metrics` | 커스텀 메트릭 파일 경로 | - |
| `--scrape.interval` | 스크래핑 간격 (초) | 15 |

### 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `DB_HOST` | Tibero 호스트 | localhost |
| `DB_PORT` | Tibero 포트 | 8629 |
| `DB_USER` | 데이터베이스 사용자 | sys |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | - |
| `DB_NAME` | 데이터베이스 이름/SID | tibero |
| `DATA_SOURCE_NAME` | 전체 DSN 연결 문자열 | - |
| `LISTEN_ADDRESS` | 리슨 주소 | 0.0.0.0 |
| `LISTEN_PORT` | 리슨 포트 | 9162 |
| `QUERY_TIMEOUT` | 쿼리 타임아웃 (초) | 30 |
| `SCRAPE_INTERVAL` | 스크래핑 간격 (초) | 15 |
| `DEFAULT_METRICS_FILE` | 기본 메트릭 파일 | default_metrics.yaml |
| `CUSTOM_METRICS_FILE` | 커스텀 메트릭 파일 | - |
| `TIBERO_JDBC_JAR` | JDBC JAR 경로 | jre/tibero7-jdbc.jar |

### 연결 풀 설정 (HikariCP)

코드 내에서 다음 기본값으로 설정되어 있습니다:
- **최대 연결 수**: 10
- **최소 유휴 연결**: 2
- **연결 타임아웃**: 30초
- **유휴 타임아웃**: 10분
- **최대 연결 수명**: 30분

## 수집 메트릭

### 기본 메트릭

| 메트릭 | 설명 | 타입 |
|--------|------|------|
| `tibero_up` | Exporter 연결 상태 (1=성공, 0=실패) | Gauge |
| `tibero_scrape_duration_seconds` | 메트릭 수집 소요 시간 | Gauge |
| `tibero_instance_info_value` | 인스턴스 정보 (이름, 버전, 상태) | Gauge |
| `tibero_database_status_value` | 데이터베이스 상태 | Gauge |
| `tibero_sessions_value` | 세션 수 (상태별, 타입별) | Gauge |
| `tibero_sessions_total_value` | 전체 세션 수 | Gauge |
| `tibero_sessions_active_value` | 활성 세션 수 | Gauge |
| `tibero_process_count_value` | 프로세스 수 | Gauge |
| `tibero_sga_size_total_bytes` | SGA 전체 크기 (컴포넌트별) | Gauge |
| `tibero_sga_size_used_bytes` | SGA 사용량 (컴포넌트별) | Gauge |
| `tibero_pga_stat_value` | PGA 통계 | Gauge |
| `tibero_lock_count_value` | 락 수 (타입별) | Gauge |
| `tibero_transactions_active_value` | 활성 트랜잭션 수 | Gauge |
| `tibero_uptime_seconds` | 데이터베이스 업타임 (초) | Counter |

### 커스텀 메트릭 정의

`default_metrics.yaml` 또는 별도의 YAML 파일로 메트릭을 정의할 수 있습니다:

```yaml
metrics:
  - name: my_custom_metric
    context: custom
    help: "커스텀 메트릭 설명"
    labels: [label1, label2]
    metrictype: gauge
    request: |
      SELECT
          COL1 as LABEL1,
          COL2 as LABEL2,
          VALUE_COL as VALUE
      FROM MY_TABLE
```

## Prometheus 설정

```yaml
scrape_configs:
  - job_name: 'tibero'
    static_configs:
      - targets: ['localhost:9162']
    scrape_interval: 15s
    scrape_timeout: 10s
```

## 필요 권한

모니터링 사용자에게 다음 뷰에 대한 SELECT 권한이 필요합니다:

```sql
GRANT SELECT ON V$INSTANCE TO monitoring_user;
GRANT SELECT ON V$DATABASE TO monitoring_user;
GRANT SELECT ON V$SYSSTAT TO monitoring_user;
GRANT SELECT ON V$SESSION TO monitoring_user;
GRANT SELECT ON V$SYSTEM_EVENT TO monitoring_user;
GRANT SELECT ON V$PROCESS TO monitoring_user;
GRANT SELECT ON V$LOCK TO monitoring_user;
GRANT SELECT ON V$TRANSACTION TO monitoring_user;
GRANT SELECT ON V$SGA TO monitoring_user;
GRANT SELECT ON V$SGASTAT TO monitoring_user;
GRANT SELECT ON V$PGASTAT TO monitoring_user;
GRANT SELECT ON V$RESOURCE_LIMIT TO monitoring_user;
GRANT SELECT ON DBA_DATA_FILES TO monitoring_user;
GRANT SELECT ON DBA_FREE_SPACE TO monitoring_user;
GRANT SELECT ON DBA_TABLESPACES TO monitoring_user;
```

## 아키텍처

### 주요 컴포넌트

- **Config.java**: 설정 관리 (환경변수, 명령줄 옵션)
- **MetricConfig.java**: 메트릭 정의 데이터 클래스
- **MetricsLoader.java**: YAML 파일에서 메트릭 로드
- **TiberoCollector.java**: 메트릭 수집 및 Prometheus 형식 변환
  - HikariCP 연결 풀 관리
  - Oracle Exporter 패턴 캐싱
  - 동기화된 메트릭 수집
- **TiberoExporter.java**: HTTP 서버 및 애플리케이션 진입점

### 기술 스택

- **Java 11**: 컴파일 대상
- **HikariCP 5.0.1**: 데이터베이스 연결 풀
- **Prometheus Client 0.16.0**: 메트릭 노출
- **SnakeYAML 2.2**: YAML 파싱
- **SLF4J + Logback**: 로깅
- **Maven**: 빌드 도구

## 문제 해결

### 연결 오류

1. JDBC 드라이버 확인: `jre/tibero7-jdbc.jar` 존재 여부
2. 네트워크 연결 확인: `telnet <host> <port>`
3. 사용자 계정 및 비밀번호 확인
4. 권한 확인: V$ 뷰 SELECT 권한

### 메트릭이 수집되지 않음

1. 로그 확인: `tibero_up` 메트릭 값 확인 (1이어야 함)
2. 쿼리 타임아웃 증가: `--query.timeout 60`
3. 권한 확인: 해당 V$ 뷰 SELECT 권한
4. 외부 default_metrics.yaml 파일이 있으면 삭제하여 JAR 내장 버전 사용

### 성능 문제

1. 연결 풀 크기 조정 (Config.java 수정)
2. 스크래핑 간격 증가: `--scrape.interval 30`
3. 불필요한 메트릭 비활성화

## 로그 확인

기본 로그 레벨은 INFO입니다. 상세 로그가 필요한 경우 `src/main/resources/logback.xml`에서 DEBUG로 변경 후 재빌드하세요.

```xml
<logger name="com.tibero.exporter" level="DEBUG"/>
```

## 라이선스

MIT License

## 참고 자료

- [Oracle DB Exporter](https://github.com/oracle/oracle-db-appdev-monitoring)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
- [Prometheus Java Client](https://github.com/prometheus/client_java)
- [Tibero Documentation](https://technet.tmaxsoft.com/upload/download/online/tibero/)
