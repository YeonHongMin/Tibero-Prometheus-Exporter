package com.tibero.exporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tibero Exporter 설정 클래스
 * 데이터베이스 연결 정보, HTTP 서버 설정, 메트릭 파일 경로 등을 관리합니다.
 */
public class Config {
    // 데이터베이스 연결 설정
    private String dbHost = "localhost";              // DB 호스트 주소
    private int dbPort = 8629;                        // DB 포트 (Tibero 기본 포트)
    private String dbUser = "sys";                    // DB 사용자명
    private String dbPassword = "";                   // DB 비밀번호
    private String dbName = "tibero";                 // DB 이름
    private String dbDsn = "";                        // 전체 DSN 연결 문자열 (옵션)
    private String jdbcJar = "jre/tibero7-jdbc.jar";  // JDBC 드라이버 JAR 파일 경로

    // HTTP 서버 설정
    private String listenAddress = "0.0.0.0";         // HTTP 서버 바인딩 주소
    private int listenPort = 9162;                    // HTTP 서버 포트

    // 쿼리 및 스크래핑 설정
    private int queryTimeout = 30;                    // 쿼리 타임아웃 (초)
    private int scrapeInterval = 15;                  // 스크래핑 간격 (초)

    // 메트릭 파일 설정
    private String metricsFile = "default_metrics.yaml";  // 기본 메트릭 파일 경로
    private String customMetricsFile = "";                // 커스텀 메트릭 파일 경로

    // 연결 풀 설정 (HikariCP)
    private int maxPoolSize = 10;                     // 최대 연결 풀 크기
    private int minIdle = 2;                          // 최소 유휴 연결 수
    private int connectionTimeout = 30000;            // 연결 타임아웃 (밀리초)
    private int idleTimeout = 600000;                 // 유휴 타임아웃 (밀리초, 10분)
    private int maxLifetime = 1800000;                // 최대 연결 수명 (밀리초, 30분)

    // Getter 및 Setter 메소드
    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbDsn() {
        return dbDsn;
    }

    public void setDbDsn(String dbDsn) {
        this.dbDsn = dbDsn;
    }

    public String getJdbcJar() {
        return jdbcJar;
    }

    public void setJdbcJar(String jdbcJar) {
        this.jdbcJar = jdbcJar;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public int getScrapeInterval() {
        return scrapeInterval;
    }

    public void setScrapeInterval(int scrapeInterval) {
        this.scrapeInterval = scrapeInterval;
    }

    public String getMetricsFile() {
        return metricsFile;
    }

    public void setMetricsFile(String metricsFile) {
        this.metricsFile = metricsFile;
    }

    public String getCustomMetricsFile() {
        return customMetricsFile;
    }

    public void setCustomMetricsFile(String customMetricsFile) {
        this.customMetricsFile = customMetricsFile;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(int maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    /**
     * 설정 정보로부터 JDBC URL을 생성합니다.
     * DSN이 설정되어 있으면 DSN을 사용하고, 없으면 호스트/포트/DB명으로 URL을 생성합니다.
     *
     * @return JDBC 연결 URL
     */
    public String getJdbcUrl() {
        if (dbDsn != null && !dbDsn.isEmpty()) {
            return dbDsn;
        }
        return String.format("jdbc:tibero:thin:@%s:%d:%s", dbHost, dbPort, dbName);
    }

    /**
     * JDBC 드라이버 클래스가 클래스패스에서 로드 가능한지 확인합니다.
     * fat JAR에 드라이버가 포함되어 있으면 true를 반환합니다.
     *
     * @return 드라이버를 로드할 수 있으면 true, 아니면 false
     */
    private boolean isJdbcDriverAvailable() {
        try {
            Class.forName("com.tmax.tibero.jdbc.TbDriver");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 설정 값의 유효성을 검증합니다.
     * 포트 범위, 필수 필드, 파일 존재 여부, 타임아웃 값 등을 확인합니다.
     *
     * @return 검증 오류 목록 (유효한 경우 빈 리스트)
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        // 포트 번호 유효성 검사
        if (dbPort < 1 || dbPort > 65535) {
            errors.add("Invalid db_port: " + dbPort + " (must be between 1 and 65535)");
        }

        if (listenPort < 1 || listenPort > 65535) {
            errors.add("Invalid listen_port: " + listenPort + " (must be between 1 and 65535)");
        }

        // 필수 필드 검사
        if (dbHost == null || dbHost.isEmpty()) {
            errors.add("db_host is required");
        }

        if (dbUser == null || dbUser.isEmpty()) {
            errors.add("db_user is required");
        }

        if ((dbName == null || dbName.isEmpty()) && (dbDsn == null || dbDsn.isEmpty())) {
            errors.add("Either db_name or db_dsn must be provided");
        }

        // JDBC 드라이버 클래스 로드 가능 여부 확인 (fat JAR에 포함되어 있거나 외부 파일 존재)
        if (!isJdbcDriverAvailable()) {
            // fat JAR에 드라이버가 포함되어 있지 않고, 외부 파일도 없는 경우만 에러
            if (!new File(jdbcJar).exists()) {
                errors.add("JDBC driver not available. Either include it in the JAR or provide path via --jdbc.jar: " + jdbcJar);
            }
        }

        // 메트릭 파일 존재 여부 확인 (외부 파일 또는 내장 리소스)
        if (!MetricsLoader.isMetricsFileAvailable(metricsFile)) {
            errors.add("Metrics file not found (external or embedded): " + metricsFile);
        }

        // 타임아웃 값 유효성 검사
        if (queryTimeout < 1) {
            errors.add("Invalid query_timeout: " + queryTimeout + " (must be >= 1)");
        }

        if (scrapeInterval < 1) {
            errors.add("Invalid scrape_interval: " + scrapeInterval + " (must be >= 1)");
        }

        return errors;
    }

    /**
     * 환경 변수로부터 설정 값을 로드합니다.
     * 설정된 환경 변수가 있으면 해당 값으로 설정을 덮어씁니다.
     */
    public void loadFromEnv() {
        String envValue;

        // 데이터베이스 연결 정보
        if ((envValue = System.getenv("DB_HOST")) != null) {
            dbHost = envValue;
        }
        if ((envValue = System.getenv("DB_PORT")) != null) {
            dbPort = Integer.parseInt(envValue);
        }
        if ((envValue = System.getenv("DB_USER")) != null) {
            dbUser = envValue;
        }
        if ((envValue = System.getenv("DB_PASSWORD")) != null) {
            dbPassword = envValue;
        }
        if ((envValue = System.getenv("DB_NAME")) != null) {
            dbName = envValue;
        }
        if ((envValue = System.getenv("DATA_SOURCE_NAME")) != null) {
            dbDsn = envValue;
        }

        // HTTP 서버 설정
        if ((envValue = System.getenv("LISTEN_ADDRESS")) != null) {
            listenAddress = envValue;
        }
        if ((envValue = System.getenv("LISTEN_PORT")) != null) {
            listenPort = Integer.parseInt(envValue);
        }

        // 쿼리 및 스크래핑 설정
        if ((envValue = System.getenv("QUERY_TIMEOUT")) != null) {
            queryTimeout = Integer.parseInt(envValue);
        }
        if ((envValue = System.getenv("SCRAPE_INTERVAL")) != null) {
            scrapeInterval = Integer.parseInt(envValue);
        }

        // 메트릭 파일 설정
        if ((envValue = System.getenv("DEFAULT_METRICS_FILE")) != null) {
            metricsFile = envValue;
        }
        if ((envValue = System.getenv("CUSTOM_METRICS_FILE")) != null) {
            customMetricsFile = envValue;
        }

        // JDBC JAR 파일 경로
        if ((envValue = System.getenv("TIBERO_JDBC_JAR")) != null) {
            jdbcJar = envValue;
        }
    }
}
