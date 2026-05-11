package com.coalblend.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExperimentRecordSchemaInitializer implements ApplicationRunner {

    private static final String TABLE_NAME = "experiment_record";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("ai_candidate_plan_count", "INT NULL COMMENT '大模型返回候选方案数'");
        columns.put("accepted_ai_candidate_count", "INT NULL COMMENT '通过基础校验并进入评分的AI候选数'");
        columns.put("total_candidate_count", "INT NULL COMMENT '本次参与决策候选总数'");
        columns.put("feasible_candidate_count", "INT NULL COMMENT '可执行候选数'");
        columns.put("risky_candidate_count", "INT NULL COMMENT '风险参考候选数'");
        columns.put("infeasible_candidate_count", "INT NULL COMMENT '不可执行候选数'");
        columns.put("generated_plan_count", "INT NULL COMMENT '本次落库展示方案数'");
        columns.put("llm_success_flag", "TINYINT NULL COMMENT '大模型是否返回可解析候选：1是0否'");
        columns.put("ai_candidate_error", "TEXT NULL COMMENT 'AI候选生成错误'");
        columns.put("effective_candidate_rate", "DECIMAL(8,4) NULL COMMENT '有效候选率'");
        columns.put("model_effect_score", "DECIMAL(8,2) NULL COMMENT '模型综合效果分'");

        for (Map.Entry<String, String> entry : columns.entrySet()) {
            ensureColumn(entry.getKey(), entry.getValue());
        }
    }

    private void ensureColumn(String columnName, String definition) {
        if (columnExists(columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnName + " " + definition);
        log.info("Added missing {}.{} column for experiment evaluation", TABLE_NAME, columnName);
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, TABLE_NAME, columnName);
        return count != null && count > 0;
    }
}
