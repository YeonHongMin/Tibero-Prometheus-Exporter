package com.tibero.exporter;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.CounterMetricFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Tibero 데이터베이스 메트릭을 수집하는 Prometheus Collector 클래스
 * HikariCP 연결 풀을 사용하여 데이터베이스에 연결하고,
 * YAML에 정의된 쿼리를 실행하여 메트릭을 수집합니다.
 */
public class TiberoCollector extends Collector {
    private static final Logger logger = LoggerFactory.getLogger(TiberoCollector.class);
    private static final String NAMESPACE = "tibero";  // Prometheus 메트릭 네임스페이스

    private final Config config;                        // 설정 정보
    private final List<MetricConfig> metricsConfig;     // 메트릭 설정 목록
    private HikariDataSource dataSource;                // HikariCP 데이터소스 (연결 풀)
    private boolean connectionValid = false;            // 연결 상태
    private long lastConnectionAttempt = 0;             // 마지막 연결 시도 시간
    private static final long CONNECTION_RETRY_DELAY = 5000; // 재연결 시도 간격 (5초)

    // 마지막 성공한 메트릭 수집 결과 캐싱 (Oracle Exporter 패턴)
    private List<MetricFamilySamples> lastSuccessfulMetrics = new ArrayList<>();

    /**
     * TiberoCollector 생성자
     * 설정 정보와 메트릭 설정을 받아 초기화하고 데이터베이스에 연결합니다.
     *
     * @param config 설정 정보
     * @param metricsConfig 메트릭 설정 목록
     */
    public TiberoCollector(Config config, List<MetricConfig> metricsConfig) {
        this.config = config;
        this.metricsConfig = metricsConfig;
        connect();
    }

    /**
     * 데이터베이스 연결 풀을 초기화합니다.
     * HikariCP를 사용하여 연결 풀을 생성하고, 연결 유효성을 검증합니다.
     * 빠른 재연결 시도를 방지하기 위해 5초 지연 시간을 두고 있습니다.
     */
    private synchronized void connect() {
        long currentTime = System.currentTimeMillis();

        // 빠른 재연결 시도 방지 (5초 이내 재시도 무시)
        if (currentTime - lastConnectionAttempt < CONNECTION_RETRY_DELAY) {
            logger.debug("Skipping connect attempt (retry delay not elapsed)");
            return;
        }

        lastConnectionAttempt = currentTime;

        try {
            // 기존 데이터소스가 있으면 닫기
            if (dataSource != null) {
                try {
                    dataSource.close();
                } catch (Exception e) {
                    // 무시
                }
                dataSource = null;
                connectionValid = false;
            }

            // Tibero JDBC 드라이버 로드
            Class.forName("com.tmax.tibero.jdbc.TbDriver");
            logger.debug("Tibero JDBC driver loaded successfully");

            String jdbcUrl = config.getJdbcUrl();
            logger.info("Initializing Tibero connection pool: {}", jdbcUrl);

            // HikariCP 설정
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName("com.tmax.tibero.jdbc.TbDriver");  // 드라이버 클래스 명시
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(config.getDbUser());
            hikariConfig.setPassword(config.getDbPassword());
            hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());      // 최대 연결 수
            hikariConfig.setMinimumIdle(config.getMinIdle());              // 최소 유휴 연결 수
            hikariConfig.setConnectionTimeout(config.getConnectionTimeout()); // 연결 타임아웃
            hikariConfig.setIdleTimeout(config.getIdleTimeout());          // 유휴 타임아웃
            hikariConfig.setMaxLifetime(config.getMaxLifetime());          // 최대 연결 수명
            hikariConfig.setConnectionTestQuery("SELECT 1 FROM DUAL");     // 연결 검증 쿼리
            hikariConfig.setPoolName("TiberoExporterPool");
            hikariConfig.setInitializationFailTimeout(-1);                 // 초기화 실패해도 시작 허용

            // 데이터소스 생성
            dataSource = new HikariDataSource(hikariConfig);

            // 연결 유효성 검증
            validateConnection();
            connectionValid = true;

            logger.info("Tibero database connection pool initialized");
            logger.info("Connection pool config - max: {}, min: {}, timeout: {}ms",
                    config.getMaxPoolSize(), config.getMinIdle(), config.getConnectionTimeout());

        } catch (ClassNotFoundException e) {
            connectionValid = false;
            logger.error("Tibero JDBC driver not found: {}", e.getMessage());
            logger.error("Make sure tibero7-jdbc.jar is included in the classpath");
        } catch (Exception e) {
            connectionValid = false;
            logger.error("Connection pool initialization failed: {}", e.getMessage());
            logger.debug("Connection error details:", e);
        }
    }
    
    /**
     * 강제로 데이터베이스에 재연결을 시도합니다.
     * 지연 시간을 무시하고 즉시 재연결합니다.
     */
    private synchronized void forceReconnect() {
        logger.warn("Force reconnecting to database...");
        lastConnectionAttempt = 0;  // 지연 시간 리셋
        connectionValid = false;
        connect();
    }

    /**
     * 간단한 쿼리를 실행하여 데이터베이스 연결을 검증합니다.
     * SELECT 1 FROM DUAL 쿼리를 실행하여 연결이 정상인지 확인합니다.
     *
     * @throws SQLException 연결이 유효하지 않거나 쿼리 실행 실패 시
     */
    private void validateConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("No database connection pool");
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM DUAL")) {
            if (!rs.next()) {
                throw new SQLException("Connection validation query returned no result");
            }
        }
    }

    /**
     * 연결 풀이 아직 유효한지 확인합니다.
     * 데이터소스가 닫혀있지 않은지 검사합니다.
     *
     * @return 연결이 유효하면 true, 그렇지 않으면 false
     */
    private boolean isConnectionValid() {
        if (dataSource == null || !connectionValid) {
            return false;
        }

        try {
            return !dataSource.isClosed();
        } catch (Exception e) {
            logger.warn("Connection pool validation failed: {}", e.getMessage());
            connectionValid = false;
            return false;
        }
    }

    /**
     * 데이터베이스에 재연결을 시도합니다.
     * 연결 상태를 무효화하고 connect() 메소드를 호출하여 재연결합니다.
     */
    private void reconnect() {
        logger.warn("Attempting database reconnect...");
        connectionValid = false;
        connect();
    }

    /**
     * 쿼리를 실행하고 결과를 반환합니다 (기본 타임아웃 사용).
     * 지정된 타임아웃이 있는 오버로드 메소드를 호출합니다.
     *
     * @param query 실행할 SQL 쿼리
     * @return 쿼리 결과 (행마다 Map<컬럼명, 값> 형태)
     * @throws SQLException 쿼리 실행 실패 시
     */
    private List<Map<String, Object>> executeQuery(String query) throws SQLException {
        return executeQuery(query, config.getQueryTimeout());
    }

    /**
     * 지정된 타임아웃으로 쿼리를 실행하고 결과를 반환합니다.
     * 연결 풀에서 연결을 가져와 쿼리를 실행하고, 실패 시 한 번 재시도합니다.
     *
     * @param query 실행할 SQL 쿼리
     * @param timeout 쿼리 타임아웃 (초)
     * @return 쿼리 결과 (행마다 Map<컬럼명, 값> 형태)
     * @throws SQLException 쿼리 실행 실패 시
     */
    private List<Map<String, Object>> executeQuery(String query, int timeout) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        // 쿼리 실행 전 연결 풀 유효성 확인
        if (!isConnectionValid()) {
            forceReconnect();  // 강제 재연결 (지연 시간 무시)
            if (!isConnectionValid()) {
                logger.error("Database connection pool is not valid after reconnect attempt");
                logger.error("Check database connectivity: host={}, port={}, user={}", 
                    config.getDbHost(), config.getDbPort(), config.getDbUser());
                throw new SQLException("Database connection pool is not valid");
            }
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(timeout);

            try (ResultSet rs = stmt.executeQuery(query)) {
                ResultSetMetaData metadata = rs.getMetaData();
                int columnCount = metadata.getColumnCount();

                // 컬럼명 가져오기 (소문자로 변환)
                List<String> columnNames = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metadata.getColumnName(i).toLowerCase());
                }

                // 모든 결과 행 가져오기
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(columnNames.get(i - 1), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Query execution failed: {}", e.getMessage());
            logger.debug("Query: {}", query);

            // 재연결 후 한 번 재시도
            try {
                logger.warn("Retrying query after reconnect...");
                reconnect();
                if (isConnectionValid()) {
                    // 추가 재시도 없이 한 번만 재시도
                    return executeQueryNoRetry(query);
                }
            } catch (SQLException reconnectError) {
                logger.error("Reconnect and retry failed: {}", reconnectError.getMessage());
            }
            throw e;
        }

        return results;
    }

    /**
     * 재시도 없이 쿼리를 실행합니다 (재연결 후 사용).
     * executeQuery()에서 재연결 후 호출되며, 추가 재시도를 하지 않습니다.
     *
     * @param query 실행할 SQL 쿼리
     * @return 쿼리 결과 (행마다 Map<컬럼명, 값> 형태)
     * @throws SQLException 쿼리 실행 실패 시
     */
    private List<Map<String, Object>> executeQueryNoRetry(String query) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(config.getQueryTimeout());

            try (ResultSet rs = stmt.executeQuery(query)) {
                ResultSetMetaData metadata = rs.getMetaData();
                int columnCount = metadata.getColumnCount();

                List<String> columnNames = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metadata.getColumnName(i).toLowerCase());
                }

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(columnNames.get(i - 1), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }

        return results;
    }

    /**
     * Prometheus로부터 호출되는 메트릭 수집 메소드 (synchronized).
     * 모든 메트릭을 수집하고 Prometheus 형식으로 반환합니다.
     * Oracle Exporter 패턴을 따라 성공한 메트릭을 캐싱하고,
     * 연결 실패 시 캐시된 메트릭을 반환하여 안정성을 높입니다.
     *
     * @return Prometheus 메트릭 패밀리 샘플 리스트
     */
    @Override
    public synchronized List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Exporter 상태 메트릭 (tibero_up)
        GaugeMetricFamily upMetric = new GaugeMetricFamily(
                NAMESPACE + "_up",
                "Whether the last Tibero scrape succeeded",
                Collections.emptyList()
        );

        // 스크래핑 소요 시간 메트릭 (tibero_scrape_duration_seconds)
        GaugeMetricFamily scrapeDurationMetric = new GaugeMetricFamily(
                NAMESPACE + "_scrape_duration_seconds",
                "Tibero scrape duration",
                Collections.emptyList()
        );

        try {
            // 연결 테스트 (간단한 쿼리 실행)
            executeQuery("SELECT 1 FROM DUAL");
            upMetric.addMetric(Collections.emptyList(), 1);
        } catch (SQLException e) {
            logger.debug("Database connectivity check failed, returning cached metrics: {}", e.getMessage());
            // 캐시된 메트릭이 있으면 반환 (Oracle Exporter 패턴)
            if (!lastSuccessfulMetrics.isEmpty()) {
                logger.debug("Returning {} cached metrics from previous scrape", lastSuccessfulMetrics.size());
                return new ArrayList<>(lastSuccessfulMetrics);
            }
            // 캐시가 없으면 오류 메트릭 반환
            upMetric.addMetric(Collections.emptyList(), 0);
            mfs.add(upMetric);
            scrapeDurationMetric.addMetric(Collections.emptyList(), (System.currentTimeMillis() - startTime) / 1000.0);
            mfs.add(scrapeDurationMetric);
            return mfs;
        }

        // 모든 메트릭 수집
        for (MetricConfig metricConfig : metricsConfig) {
            try {
                mfs.addAll(collectMetric(metricConfig));
            } catch (Exception e) {
                logger.error("Error collecting metric {}: {}", metricConfig.getName(), e.getMessage());
            }
        }

        // 상태 메트릭 추가
        mfs.add(upMetric);
        scrapeDurationMetric.addMetric(Collections.emptyList(), (System.currentTimeMillis() - startTime) / 1000.0);
        mfs.add(scrapeDurationMetric);

        // 성공한 수집 결과 캐싱
        lastSuccessfulMetrics = new ArrayList<>(mfs);
        logger.debug("Cached {} metrics for future use", mfs.size());

        return mfs;
    }

    /**
     * 단일 메트릭을 수집합니다.
     * MetricConfig에 정의된 쿼리를 실행하고 결과를 Prometheus 형식으로 변환합니다.
     * 메트릭별 타임아웃이 설정되어 있으면 해당 값을 사용하고, 없으면 기본값을 사용합니다.
     *
     * @param metricConfig 수집할 메트릭 설정
     * @return Prometheus 메트릭 패밀리 샘플 리스트
     */
    private List<MetricFamilySamples> collectMetric(MetricConfig metricConfig) {
        logger.debug("Collecting metric: {}", metricConfig.getName());

        List<MetricFamilySamples> mfs = new ArrayList<>();
        List<Map<String, Object>> results;

        try {
            // 메트릭별 타임아웃이 설정되어 있으면 사용, 아니면 기본값 사용
            int timeout = metricConfig.getQueryTimeout() > 0 ?
                         metricConfig.getQueryTimeout() : config.getQueryTimeout();
            results = executeQuery(metricConfig.getRequest(), timeout);
        } catch (SQLException e) {
            logger.error("{} query execution failed: {}", metricConfig.getName(), e.getMessage());
            return mfs;
        }

        if (results.isEmpty()) {
            logger.debug("No results for metric {}", metricConfig.getName());
            if (metricConfig.isIgnoreZero()) {
                return mfs;
            }
        }

        // 중복된 메트릭 패밀리를 피하기 위해 컬럼명 기준으로 그룹화
        Map<String, MetricFamilySamples> metricsByColumn = new HashMap<>();

        // 결과 처리
        for (Map<String, Object> row : results) {
            // 레이블 값 추출
            List<String> labelValues = new ArrayList<>();
            for (String label : metricConfig.getLabels()) {
                Object value = row.get(label.toLowerCase());
                labelValues.add(value != null ? value.toString() : "");
            }

            // 값 컬럼 찾기(레이블 컬럼 제외)
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String colName = entry.getKey();
                Object colValue = entry.getValue();

                // 레이블 컬럼은 건너뜀
                boolean isLabel = false;
                for (String label : metricConfig.getLabels()) {
                    if (colName.equalsIgnoreCase(label)) {
                        isLabel = true;
                        break;
                    }
                }
                if (isLabel) continue;

                // 숫자 값인지 확인
                if (!isNumeric(colValue)) {
                    continue;
                }

                // fieldtoname 매핑이 있으면 사용하고 없으면 컬럼명을 메트릭 이름으로 사용
                String metricName = metricConfig.getFieldtoname().get(colName.toUpperCase());
                if (metricName == null) {
                    metricName = colName.toLowerCase();
                }

                // 값을 double로 변환
                double value;
                try {
                    value = convertToDouble(colValue);
                } catch (NumberFormatException e) {
                    logger.debug("Could not convert value {} to double", colValue);
                    continue;
                }

                // 설정에 따라 0 값은 건너뜀
                if (metricConfig.isIgnoreZero() && value == 0) {
                    continue;
                }

                // 중복을 피하기 위해 컨텍스트 대신 메트릭 이름을 사용해 전체 이름 생성
                String fullName = NAMESPACE + "_" + metricConfig.getName() + "_" + metricName;

                // 메트릭 패밀리 생성 또는 가져오기
                if (!metricsByColumn.containsKey(fullName)) {
                    if ("counter".equalsIgnoreCase(metricConfig.getMetrictype())) {
                        metricsByColumn.put(fullName, new CounterMetricFamily(
                                fullName,
                                metricConfig.getHelp(),
                                metricConfig.getLabels()
                        ));
                    } else {
                        metricsByColumn.put(fullName, new GaugeMetricFamily(
                                fullName,
                                metricConfig.getHelp(),
                                metricConfig.getLabels()
                        ));
                    }
                }

                // 메트릭 값 추가
                MetricFamilySamples metricFamily = metricsByColumn.get(fullName);
                if (metricFamily instanceof GaugeMetricFamily) {
                    ((GaugeMetricFamily) metricFamily).addMetric(labelValues, value);
                } else if (metricFamily instanceof CounterMetricFamily) {
                    ((CounterMetricFamily) metricFamily).addMetric(labelValues, value);
                }
            }
        }

        mfs.addAll(metricsByColumn.values());
        return mfs;
    }

    /**
     * 값이 숫자인지 확인합니다.
     * Number 타입이거나 숫자 형식의 문자열인 경우 true를 반환합니다.
     *
     * @param value 확인할 값
     * @return 숫자이면 true, 아니면 false
     */
    private boolean isNumeric(Object value) {
        if (value == null) return false;
        if (value instanceof Number) return true;
        if (value instanceof String) {
            String str = (String) value;
            return str.matches("-?\\d+(\\.\\d+)?");
        }
        return false;
    }

    /**
     * 값을 double 타입으로 변환합니다.
     * Number 타입이면 doubleValue()를 호출하고,
     * 문자열이면 Double.parseDouble()을 사용합니다.
     *
     * @param value 변환할 값
     * @return double로 변환된 값
     * @throws NumberFormatException 숫자로 변환할 수 없는 경우
     */
    private double convertToDouble(Object value) throws NumberFormatException {
        if (value == null) return 0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * 데이터베이스 연결 풀을 종료합니다.
     * Exporter 종료 시 호출되어 모든 연결을 정리합니다.
     */
    public void close() {
        if (dataSource != null) {
            try {
                dataSource.close();
                logger.info("Database connection pool closed");
            } catch (Exception e) {
                logger.error("Error while closing connection pool: {}", e.getMessage());
            } finally {
                dataSource = null;
                connectionValid = false;
            }
        }
    }
}
