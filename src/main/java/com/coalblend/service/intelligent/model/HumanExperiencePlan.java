package com.coalblend.service.intelligent.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 人工经验对照方案（baseline）。
 * <p>
 * 仅作为对照展示与论文实验对比使用，不参与系统主决策流程的 Pareto 排序与持久化。
 * 法则定义详见 {@code HumanExperienceBaselineService} 接口注释。
 */
@Data
public class HumanExperiencePlan {

    /** 是否成功生成（候选物料不足等情况会失败） */
    private boolean generated;
    /** 生成失败时的原因 */
    private String errorMessage;

    /** 选中物料明细（按经验综合分降序，固定取前 2 名） */
    private List<HumanExperienceItem> items = new ArrayList<>();

    /** 加权配煤指标 */
    private BigDecimal predictedAsh;
    private BigDecimal predictedSulfur;
    private BigDecimal predictedMoisture;
    private BigDecimal predictedVolatile;
    private BigDecimal predictedCalorific;

    /** 单位成本（元/吨） */
    private BigDecimal costPerTon;
    /** 总成本（元） */
    private BigDecimal totalCost;
    /** 订单需求量（吨） */
    private BigDecimal demandQuantity;

    /** 是否通过四项硬约束（灰/硫/水≤上限，热值≥下限） */
    private boolean hardConstraintsPassed;
    /** 超限项明细（未通过时使用） */
    private List<String> violations = new ArrayList<>();

    /** 综合标签：feasible / infeasible / failed */
    private String status;
    /** 摘要说明（含算法步骤回放，便于前端展示与论文披露） */
    private String summary;

    /**
     * 入选物料明细。
     */
    @Data
    public static class HumanExperienceItem {
        private Long coalId;
        private String coalCode;
        private String coalName;
        /** 经验综合分（详见 Step 1） */
        private BigDecimal experienceScore;
        /** 经验配比（如 0.6 / 0.4） */
        private BigDecimal ratio;
        /** 单价（元/吨） */
        private BigDecimal purchasePrice;
        /** 灰分 */
        private BigDecimal ash;
        /** 硫分 */
        private BigDecimal sulfur;
        /** 水分 */
        private BigDecimal moisture;
        /** 挥发分 */
        private BigDecimal volatileMatter;
        /** 热值 */
        private BigDecimal calorific;
    }
}
