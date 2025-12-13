package com.tibero.exporter;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Tibero Prometheus Exporter 메인 클래스
 * HTTP 서버를 시작하고 메트릭을 수집하여 Prometheus 형식으로 노출합니다.
 */
public class TiberoExporter {
    private static final Logger logger = LoggerFactory.getLogger(TiberoExporter.class);
    private static final String VERSION = "1.0.0";
    private static final String BUILD_DATE = "2026-01-01";

    private final Config config;                    // 설정 정보
    private TiberoCollector collector;              // 메트릭 수집기
    private HTTPServer httpServer;                  // Prometheus HTTP 서버
    private volatile boolean running = true;        // 실행 상태 플래그

    /**
     * TiberoExporter 생성자
     *
     * @param config 설정 정보
     */
    public TiberoExporter(Config config) {
        this.config = config;
    }

    /**
     * Exporter를 시작합니다.
     * 메트릭 설정을 로드하고, 수집기를 초기화하며, HTTP 서버를 시작합니다.
     *
     * @throws IOException HTTP 서버 시작 실패 시
     */
    public void start() throws IOException {
        logger.info("Starting Tibero Exporter v{}", VERSION);
        logger.info("Connecting to {}:{}", config.getDbHost(), config.getDbPort());

        // 메트릭 설정 로드
        List<MetricConfig> metricsConfig = MetricsLoader.loadMetricsConfig(config.getMetricsFile());
        logger.info("Loaded {} metrics from {}", metricsConfig.size(), config.getMetricsFile());

        // 커스텀 메트릭이 지정된 경우 추가 로드
        if (config.getCustomMetricsFile() != null && !config.getCustomMetricsFile().isEmpty()) {
            File customFile = new File(config.getCustomMetricsFile());
            if (customFile.exists()) {
                List<MetricConfig> customMetrics = MetricsLoader.loadMetricsConfig(config.getCustomMetricsFile());
                metricsConfig.addAll(customMetrics);
                logger.info("Loaded {} custom metrics", customMetrics.size());
            }
        }

        // 수집기 초기화
        collector = new TiberoCollector(config, metricsConfig);

        // 수집기 등록
        CollectorRegistry.defaultRegistry.register(collector);

        // JVM 메트릭 등록 (Tibero 메트릭만 표시하기 위해 비활성화)
        // DefaultExports.initialize();

        // HTTP 서버 시작
        logger.info("Starting HTTP server at {}:{}", config.getListenAddress(), config.getListenPort());
        InetSocketAddress address = new InetSocketAddress(config.getListenAddress(), config.getListenPort());
        httpServer = new HTTPServer(address, CollectorRegistry.defaultRegistry, true);

        logger.info("Tibero Exporter started");
        logger.info("Metrics endpoint: http://{}:{}/metrics", config.getListenAddress(), config.getListenPort());

        // 실행 유지 (인터럽트 될 때까지)
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.info("Exporter interrupted");
                break;
            }
        }
    }

    /**
     * Exporter를 중지합니다.
     * HTTP 서버와 데이터베이스 연결 풀을 종료합니다.
     */
    public void stop() {
        logger.info("Stopping Tibero Exporter...");
        running = false;

        if (httpServer != null) {
            httpServer.close();
        }

        if (collector != null) {
            collector.close();
        }

        logger.info("Tibero Exporter stopped");
    }

    /**
     * 명령줄 인자를 파싱하여 Config 객체를 생성합니다.
     * 환경 변수를 먼저 로드한 후, 명령줄 인자로 덮어씁니다.
     *
     * @param args 명령줄 인자 배열
     * @return 파싱된 Config 객체
     */
    private static Config parseArgs(String[] args) {
        Config config = new Config();
        config.loadFromEnv();  // 먼저 환경 변수 로드

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            try {
                switch (arg) {
                    case "--version":
                    case "-v":
                        System.out.println("tibero_exporter v" + VERSION + " (" + BUILD_DATE + ")");
                        System.exit(0);
                        break;

                    case "--web.listen-address":
                        config.setListenAddress(args[++i]);
                        break;

                    case "--web.listen-port":
                        config.setListenPort(Integer.parseInt(args[++i]));
                        break;

                    case "--db.host":
                        config.setDbHost(args[++i]);
                        break;

                    case "--db.port":
                        config.setDbPort(Integer.parseInt(args[++i]));
                        break;

                    case "--db.user":
                        config.setDbUser(args[++i]);
                        break;

                    case "--db.password":
                        config.setDbPassword(args[++i]);
                        break;

                    case "--db.name":
                        config.setDbName(args[++i]);
                        break;

                    case "--db.dsn":
                        config.setDbDsn(args[++i]);
                        break;

                    case "--query.timeout":
                        config.setQueryTimeout(Integer.parseInt(args[++i]));
                        break;

                    case "--default.metrics":
                        config.setMetricsFile(args[++i]);
                        break;

                    case "--custom.metrics":
                        config.setCustomMetricsFile(args[++i]);
                        break;

                    case "--jdbc.jar":
                        config.setJdbcJar(args[++i]);
                        break;

                    case "--scrape.interval":
                        config.setScrapeInterval(Integer.parseInt(args[++i]));
                        break;

                    case "--help":
                    case "-h":
                        printHelp();
                        System.exit(0);
                        break;

                    default:
                        if (arg.startsWith("--")) {
                            logger.warn("Unknown argument: {}", arg);
                        }
                        break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.error("No value provided for argument {}", arg);
                System.exit(1);
            } catch (NumberFormatException e) {
                logger.error("Invalid number format for argument {}", arg);
                System.exit(1);
            }
        }

        return config;
    }

    /**
     * 도움말 메시지를 출력합니다.
     * 사용 가능한 모든 명령줄 옵션과 환경 변수를 안내합니다.
     */
    private static void printHelp() {
        System.out.println("Tibero Database Prometheus Exporter v" + VERSION);
        System.out.println("\nUsage: java -jar tibero-exporter.jar [options]");
        System.out.println("\nOptions:");
        System.out.println("  --version, -v                Show version info");
        System.out.println("  --help, -h                   Show help");
        System.out.println("  --web.listen-address ADDR    HTTP server bind address (default: 0.0.0.0)");
        System.out.println("  --web.listen-port PORT       HTTP server port (default: 9162)");
        System.out.println("  --db.host HOST               Tibero database host (default: localhost)");
        System.out.println("  --db.port PORT               Tibero database port (default: 8629)");
        System.out.println("  --db.user USER               Database user (default: sys)");
        System.out.println("  --db.password PASS           Database password");
        System.out.println("  --db.name NAME               Database name/SID (default: tibero)");
        System.out.println("  --db.dsn DSN                 Full DSN connection string");
        System.out.println("  --query.timeout SECONDS      Query timeout in seconds (default: 30)");
        System.out.println("  --default.metrics FILE       Default metrics file path (default: default_metrics.yaml)");
        System.out.println("  --custom.metrics FILE        Custom metrics file path");
        System.out.println("  --jdbc.jar FILE              Tibero JDBC driver JAR path");
        System.out.println("  --scrape.interval SECONDS    Metrics scrape interval in seconds (default: 15)");
        System.out.println("\nEnvironment variables:");
        System.out.println("  DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME");
        System.out.println("  LISTEN_ADDRESS, LISTEN_PORT");
        System.out.println("  QUERY_TIMEOUT, SCRAPE_INTERVAL");
        System.out.println("  DEFAULT_METRICS_FILE, CUSTOM_METRICS_FILE");
        System.out.println("  TIBERO_JDBC_JAR, DATA_SOURCE_NAME");
    }

    /**
     * 메인 진입점
     * 명령줄 인자를 파싱하고, 설정을 검증한 후 Exporter를 시작합니다.
     *
     * @param args 명령줄 인자
     */
    public static void main(String[] args) {
        // 명령줄 인자 파싱
        Config config = parseArgs(args);

        // 설정 검증
        List<String> errors = config.validate();
        if (!errors.isEmpty()) {
            logger.error("Configuration validation failed:");
            for (String error : errors) {
                logger.error("  - {}", error);
            }
            System.exit(1);
        }

        // 비밀번호가 제공되지 않은 경우 경고
        if (config.getDbPassword() == null || config.getDbPassword().isEmpty()) {
            logger.warn("Password not provided; connection may fail.");
        }

        // Exporter 생성 및 시작
        final TiberoExporter exporter = new TiberoExporter(config);

        // 종료 훅 설정 (Ctrl+C 등의 종료 신호 처리)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received");
            exporter.stop();
        }));

        try {
            exporter.start();
        } catch (IOException e) {
            logger.error("Failed to start exporter: {}", e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
