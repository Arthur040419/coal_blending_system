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
public class BlendPlanDecisionSchemaInitializer implements ApplicationRunner {

    private static final String TABLE_NAME = "blend_plan";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("decision_status", "VARCHAR(32) NULL COMMENT '决策状态：FEASIBLE/RISKY/INFEASIBLE'");
        columns.put("recommendation_mode", "VARCHAR(32) NULL COMMENT '推荐模式：NORMAL/RISK_REFERENCE/NO_SOLUTION'");
        columns.put("score_strategy", "VARCHAR(32) NULL COMMENT '评分策略'");
        columns.put("pareto_rank", "INT NULL COMMENT 'Pareto非支配排序等级'");
        columns.put("objective_cost_per_ton", "DECIMAL(18,4) NULL COMMENT '目标1：吨煤成本'");
        columns.put("objective_quality_deviation", "DECIMAL(18,4) NULL COMMENT '目标2：质量偏差'");
        columns.put("objective_execution_risk", "DECIMAL(18,4) NULL COMMENT '目标3：执行风险'");
        columns.put("problem_items_json", "JSON NULL COMMENT '结构化问题项'");
        columns.put("suggestion_items_json", "JSON NULL COMMENT '结构化建议项'");
        columns.put("generation_config_json", "JSON NULL COMMENT '生成参数快照'");

        for (Map.Entry<String, String> entry : columns.entrySet()) {
            ensureColumn(entry.getKey(), entry.getValue());
        }
    }

    private void ensureColumn(String columnName, String definition) {
        if (columnExists(columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnName + " " + definition);
        log.info("Added missing {}.{} column for P0 decision upgrade", TABLE_NAME, columnName);
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
