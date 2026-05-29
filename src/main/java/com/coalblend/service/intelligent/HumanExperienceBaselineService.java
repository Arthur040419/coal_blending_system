package com.coalblend.service.intelligent;

import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.model.BlendGenerationRuntimeConfig;
import com.coalblend.service.intelligent.model.HumanExperiencePlan;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;

import java.util.List;

/**
 * 人工经验配煤基线服务。
 * <p>
 * 作为对照基线模拟工程师常规经验决策，<b>仅生成单个对照方案</b>，
 * 不参与系统主决策流程的 Pareto 排序与最终推荐。
 * <p>
 * 算法法则（V1）：
 * <ol>
 *   <li><b>Step 1 经验综合分排序</b>：对每个候选物料按工程师常用偏好计算综合分
 *       Score = 0.4 × 热值得分 + 0.2 × 灰分得分 + 0.2 × 硫分得分 + 0.2 × 价格得分；
 *       质量优先、价格次要。</li>
 *   <li><b>Step 2 经验配比规则</b>：按综合分降序<b>同时生成</b>两组候选——
 *       N=2（主煤 60% + 辅煤 40%，对应"主辅 6:4"经验）与
 *       N=3（主煤 50% + 副煤 30% + 辅煤 20%，对应"主副辅 5:3:2"经验）；
 *       不进行枚举搜索、不进行小步长试错调整、不进行多目标 Pareto 排序。
 *       候选物料不足 3 种时，仅生成 N=2 候选。</li>
 *   <li><b>Step 3 硬约束粗校验</b>：将每组候选的配比代入线性加权计算配合煤指标，
 *       仅检查灰/硫/水/热值四项是否未超订单上下限：通过则标记为 feasible，
 *       任一超限则保留方案并标记为 infeasible，记录超限项。</li>
 *   <li><b>Step 4 择优 + 显式缺失</b>：在两组候选间按"硬约束可行优先 → 方案级综合分更高 →
 *       吨煤成本更低"的优先级择优，<b>仅输出一个最终方案</b>。
 *       算法不查规则知识库、不查历史案例、不查 RAG、不做安全余量分级、
 *       不做 Pareto 多目标优化、不做枚举试错、不做库存调拨判断、
 *       不参与最终推荐决策；与系统主流程形成清晰对比。</li>
 * </ol>
 *
 * <p>本服务的目的是把"人工经验"从一个不可量化的描述固化为一段可复现的算法，
 * 使得「系统推荐方案 vs 人工经验方案」对比有可披露、可重跑的统一基线。
 */
public interface HumanExperienceBaselineService {

    /**
     * 基于订单约束与候选物料生成单一人工经验对照方案。
     *
     * @param order      订单
     * @param candidates 已经过候选筛选的物料快照列表（与 AI 通道、系统枚举通道使用同一份）
     * @return 人工经验对照方案；若候选物料不足，返回 generated=false 的失败结果
     */
    HumanExperiencePlan generate(Orders order, List<PlanCoalSnapshot> candidates,
                                 BlendGenerationRuntimeConfig runtimeConfig);
}
