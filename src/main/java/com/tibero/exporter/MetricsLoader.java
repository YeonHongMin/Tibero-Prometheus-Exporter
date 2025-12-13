package com.tibero.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * YAML 파일에서 메트릭 설정을 로드하는 클래스
 * SnakeYAML 라이브러리를 사용하여 메트릭 정의를 파싱합니다.
 */
public class MetricsLoader {
    private static final Logger logger = LoggerFactory.getLogger(MetricsLoader.class);
    
    // 기본 메트릭 파일명 (JAR 내장 리소스)
    private static final String DEFAULT_METRICS_RESOURCE = "default_metrics.yaml";

    /**
     * YAML 파일에서 메트릭 설정을 로드합니다.
     * 먼저 외부 파일을 확인하고, 없으면 JAR 내장 리소스에서 로드합니다.
     *
     * @param filename 로드할 YAML 파일 경로
     * @return 로드된 메트릭 설정 리스트
     */
    @SuppressWarnings("unchecked")
    public static List<MetricConfig> loadMetricsConfig(String filename) {
        List<MetricConfig> metrics = new ArrayList<>();

        InputStream input = null;
        boolean fromResource = false;
        
        try {
            // 1. 먼저 외부 파일 확인
            File file = new File(filename);
            if (file.exists()) {
                input = new FileInputStream(file);
                logger.info("Loading metrics from external file: {}", filename);
            } else {
                // 2. 외부 파일이 없으면 클래스패스 리소스에서 로드 시도
                String resourceName = new File(filename).getName();
                input = MetricsLoader.class.getClassLoader().getResourceAsStream(resourceName);
                
                if (input == null && DEFAULT_METRICS_RESOURCE.equals(resourceName)) {
                    // 기본 리소스 이름으로 재시도
                    input = MetricsLoader.class.getClassLoader().getResourceAsStream(DEFAULT_METRICS_RESOURCE);
                }
                
                if (input != null) {
                    fromResource = true;
                    logger.info("Loading metrics from embedded resource: {}", resourceName);
                } else {
                    logger.warn("Metrics file not found (external or embedded): {}", filename);
                    return metrics;
                }
            }

            // YAML 파일 파싱
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            // metrics 키가 있는지 확인
            if (data == null || !data.containsKey("metrics")) {
                logger.warn("No metrics found in {}", filename);
                return metrics;
            }

            // 메트릭 데이터 추출
            List<Map<String, Object>> metricsData = (List<Map<String, Object>>) data.get("metrics");

            // 각 메트릭을 MetricConfig 객체로 변환
            for (Map<String, Object> metricData : metricsData) {
                MetricConfig metric = new MetricConfig();

                // 기본 필드 설정
                metric.setName(getString(metricData, "name", ""));
                metric.setContext(getString(metricData, "context", ""));
                metric.setHelp(getString(metricData, "help", ""));
                metric.setRequest(getString(metricData, "request", ""));
                metric.setMetrictype(getString(metricData, "metrictype", "gauge"));
                metric.setIgnoreZero(getBoolean(metricData, "ignorezeroresult", false));

                // 레이블 목록 설정
                if (metricData.containsKey("labels")) {
                    Object labelsObj = metricData.get("labels");
                    if (labelsObj instanceof List) {
                        metric.setLabels((List<String>) labelsObj);
                    }
                }

                // 필드명-메트릭명 매핑 설정
                if (metricData.containsKey("fieldtoname")) {
                    Object fieldtonameObj = metricData.get("fieldtoname");
                    if (fieldtonameObj instanceof Map) {
                        metric.setFieldtoname((Map<String, String>) fieldtonameObj);
                    }
                }

                metrics.add(metric);
            }

            logger.info("Loaded {} metrics from {}", metrics.size(), fromResource ? "(embedded)" : filename);

        } catch (IOException e) {
            logger.error("Error reading metrics file {}: {}", filename, e.getMessage());
        } catch (Exception e) {
            logger.error("Error parsing metrics file {}: {}", filename, e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // 무시
                }
            }
        }

        return metrics;
    }
    
    /**
     * 메트릭 파일이 존재하는지 확인합니다 (외부 파일 또는 내장 리소스).
     *
     * @param filename 확인할 파일 경로
     * @return 파일이 존재하면 true
     */
    public static boolean isMetricsFileAvailable(String filename) {
        // 외부 파일 확인
        if (new File(filename).exists()) {
            return true;
        }
        
        // 내장 리소스 확인
        String resourceName = new File(filename).getName();
        try (InputStream is = MetricsLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is != null) {
                return true;
            }
        } catch (IOException e) {
            // 무시
        }
        
        // 기본 리소스명으로 확인
        if (DEFAULT_METRICS_RESOURCE.equals(resourceName)) {
            try (InputStream is = MetricsLoader.class.getClassLoader().getResourceAsStream(DEFAULT_METRICS_RESOURCE)) {
                return is != null;
            } catch (IOException e) {
                // 무시
            }
        }
        
        return false;
    }

    /**
     * Map에서 문자열 값을 가져옵니다. 없으면 기본값을 반환합니다.
     */
    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Map에서 boolean 값을 가져옵니다. 없으면 기본값을 반환합니다.
     */
    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
