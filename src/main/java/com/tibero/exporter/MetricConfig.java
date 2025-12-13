package com.tibero.exporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 개별 메트릭의 설정 정보를 담는 클래스
 * YAML 파일에서 로드한 메트릭 정의를 저장하고 관리합니다.
 */
public class MetricConfig {
    private String name;                              // 메트릭 이름
    private String context;                           // 메트릭 컨텍스트 (그룹명)
    private String help;                              // 메트릭 설명
    private String request;                           // 실행할 SQL 쿼리
    private List<String> labels = new ArrayList<>();  // Prometheus 레이블 목록
    private String metrictype = "gauge";              // 메트릭 타입 (gauge 또는 counter)
    private Map<String, String> fieldtoname = new HashMap<>();  // 필드명과 메트릭명 매핑
    private boolean ignoreZero = false;               // 0 값 무시 여부
    private int queryTimeout = 0;                     // 쿼리 타임아웃 (0이면 기본값 사용)

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getMetrictype() {
        return metrictype;
    }

    public void setMetrictype(String metrictype) {
        this.metrictype = metrictype;
    }

    public Map<String, String> getFieldtoname() {
        return fieldtoname;
    }

    public void setFieldtoname(Map<String, String> fieldtoname) {
        this.fieldtoname = fieldtoname;
    }

    public boolean isIgnoreZero() {
        return ignoreZero;
    }

    public void setIgnoreZero(boolean ignoreZero) {
        this.ignoreZero = ignoreZero;
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    @Override
    public String toString() {
        return "MetricConfig{" +
                "name='" + name + '\'' +
                ", context='" + context + '\'' +
                ", metrictype='" + metrictype + '\'' +
                ", labels=" + labels.size() +
                '}';
    }
}
