package com.coalblend.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.entity.RuleKnowledge;
import com.coalblend.mapper.RuleKnowledgeMapper;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RuleMatchServiceImpl implements RuleMatchService {

    private static final BigDecimal SULFUR_STRICT = new BigDecimal("0.8");
    private static final BigDecimal CAL_HIGH = new BigDecimal("5000");
    private static final BigDecimal ASH_STRICT = new BigDecimal("18");
    private static final BigDecimal STOCK_WARN = new BigDecimal("3000");

    private final RuleKnowledgeMapper ruleKnowledgeMapper;

    @Override
    public List<MatchedRuleVO> match(Orders order, List<Long> candidateCoalIds, Map<Long, Inventory> invMap) {
        List<RuleKnowledge> all = ruleKnowledgeMapper.selectList(new LambdaQueryWrapper<RuleKnowledge>()
                .eq(RuleKnowledge::getStatus, 1)
                .orderByDesc(RuleKnowledge::getPriorityLevel)
                .orderByAsc(RuleKnowledge::getId));

        List<MatchedRuleVO> out = new ArrayList<>();
        for (RuleKnowledge r : all) {
            String reason = evaluate(r, order, candidateCoalIds, invMap);
            if (reason != null) {
                out.add(MatchedRuleVO.fromEntity(r, reason));
            }
        }
        return out;
    }

    private String evaluate(RuleKnowledge r, Orders order, List<Long> candidateCoalIds, Map<Long, Inventory> invMap) {
        String code = r.getRuleCode() == null ? "" : r.getRuleCode().trim();
        return switch (code) {
            case "R001" -> evalR001(order);
            case "R002" -> evalR002(order);
            case "R003" -> evalR003(order, candidateCoalIds, invMap);
            case "R004" -> evalR004(order);
            case "R005" -> evalR005(order);
            case "R006" -> evalR006(order);
            case "R007" -> evalR007(candidateCoalIds, invMap);
            case "R008" -> evalR008(order, candidateCoalIds, invMap);
            case "R009" -> evalR009(order);
            case "R010" -> evalR010(order);
            case "R011" -> evalR011(order);
            case "R012" -> evalR012(order);
            default -> evalByNameHeuristic(r, order, candidateCoalIds, invMap);
        };
    }

    private String evalR001(Orders order) {
        if (order.getTargetSulfur() != null && order.getTargetSulfur().compareTo(SULFUR_STRICT) <= 0) {
            return "因订单目标硫分不高于 0.8%，需执行高硫煤限配策略。";
        }
        return null;
    }

    private String evalR002(Orders order) {
        boolean highCal = order.getTargetCalorific() != null && order.getTargetCalorific().compareTo(CAL_HIGH) >= 0;
        boolean strictS = order.getTargetSulfur() != null && order.getTargetSulfur().compareTo(SULFUR_STRICT) <= 0;
        if (highCal && strictS) {
            return "因目标热值不低于 5000 kcal/kg 且硫分要求较严，优先参考低硫煤组合经验。";
        }
        if (strictS) {
            return "因订单硫分要求较严，优先参考低硫煤调用规则。";
        }
        return null;
    }

    private String evalR003(Orders order, List<Long> candidateCoalIds, Map<Long, Inventory> invMap) {
        boolean lowStock = false;
        if (candidateCoalIds != null && invMap != null) {
            for (Long cid : candidateCoalIds) {
                Inventory inv = invMap.get(cid);
                if (inv != null && inv.getAvailableQuantity() != null
                        && inv.getAvailableQuantity().compareTo(STOCK_WARN) < 0) {
                    lowStock = true;
                    break;
                }
            }
        }
        if (!lowStock) {
            return null;
        }
        Integer p = order.getPriorityLevel();
        if (p != null && p <= 1) {
            return "存在煤种可用库存低于 3000 吨；当前为高优先级订单，允许在库存保护前提下参与配煤。";
        }
        return "存在煤种可用库存低于 3000 吨，需关注库存保护规则对可执行性的影响。";
    }

    private String evalR004(Orders order) {
        if (order.getTargetCalorific() != null && order.getTargetCalorific().compareTo(CAL_HIGH) >= 0) {
            return "因订单目标热值不低于 5000 kcal/kg，建议参考高热值补偿类配煤策略。";
        }
        return null;
    }

    private String evalR005(Orders order) {
        if (order.getTargetAsh() != null && order.getTargetAsh().compareTo(ASH_STRICT) < 0) {
            return "因订单灰分上限低于 18%，需严控高灰煤种作为主配比例。";
        }
        return null;
    }

    /** 低成本优先：非高优先级订单 */
    private String evalR006(Orders order) {
        Integer p = order.getPriorityLevel();
        if (p != null && p >= 3) {
            return "订单优先级为一般/偏低，可侧重成本可控的配煤路径。";
        }
        return null;
    }

    /** 高库存优先调用 */
    private String evalR007(List<Long> candidateCoalIds, Map<Long, Inventory> invMap) {
        if (candidateCoalIds == null || invMap == null) {
            return null;
        }
        long high = candidateCoalIds.stream()
                .map(invMap::get)
                .filter(inv -> inv != null && inv.getAvailableQuantity() != null)
                .filter(inv -> inv.getAvailableQuantity().compareTo(new BigDecimal("8000")) >= 0)
                .count();
        if (high >= 1) {
            return "候选煤种中存在较高可用库存，可兼顾消化库存的配煤策略。";
        }
        return null;
    }

    /** 库存不足预警 */
    private String evalR008(Orders order, List<Long> candidateCoalIds, Map<Long, Inventory> invMap) {
        if (order.getDemandQuantity() == null || candidateCoalIds == null || invMap == null) {
            return null;
        }
        for (Long cid : candidateCoalIds) {
            Inventory inv = invMap.get(cid);
            if (inv == null || inv.getAvailableQuantity() == null) {
                continue;
            }
            if (inv.getAvailableQuantity().compareTo(order.getDemandQuantity()) < 0) {
                return "部分煤种单库可用量低于本单需求量，存在分仓调拨或增量采购风险。";
            }
        }
        return null;
    }

    /** 水分控制 */
    private String evalR009(Orders order) {
        if (order.getTargetMoisture() != null && order.getTargetMoisture().compareTo(new BigDecimal("10")) < 0) {
            return "订单水分上限较严，需关注高水分煤种配比与堆放周期。";
        }
        return null;
    }

    private String evalR010(Orders order) {
        if (order.getTargetAsh() == null) {
            return null;
        }
        if (order.getTargetAsh().compareTo(new BigDecimal("18")) >= 0
                && order.getTargetAsh().compareTo(new BigDecimal("20")) <= 0) {
            return "订单灰分上限处于中等区间，应避免灰分过高的煤种作为主配。";
        }
        return null;
    }

    private String evalR011(Orders order) {
        if (order.getTargetSulfur() == null) {
            return null;
        }
        if (order.getTargetSulfur().compareTo(SULFUR_STRICT) > 0
                && order.getTargetSulfur().compareTo(new BigDecimal("1.5")) <= 0) {
            return "订单硫分要求为常规水平，宜在成本与硫分达标风险之间综合权衡。";
        }
        return null;
    }

    private String evalR012(Orders order) {
        if (order.getDemandQuantity() != null && order.getDemandQuantity().compareTo(new BigDecimal("5000")) >= 0) {
            return "本单需求量大，应校核多仓合计可用量与物流执行能力。";
        }
        return null;
    }

    private String evalByNameHeuristic(RuleKnowledge r, Orders order, List<Long> candidateCoalIds,
                                         Map<Long, Inventory> invMap) {
        String name = r.getRuleName() == null ? "" : r.getRuleName();
        if (name.contains("水分") && evalR009(order) != null) {
            return evalR009(order);
        }
        if (name.contains("库存") && (evalR003(order, candidateCoalIds, invMap) != null || evalR008(order, candidateCoalIds, invMap) != null)) {
            return evalR008(order, candidateCoalIds, invMap) != null
                    ? evalR008(order, candidateCoalIds, invMap)
                    : evalR003(order, candidateCoalIds, invMap);
        }
        if (name.contains("成本") && evalR006(order) != null) {
            return evalR006(order);
        }
        return null;
    }
}
