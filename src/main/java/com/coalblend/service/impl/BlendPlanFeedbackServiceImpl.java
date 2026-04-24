package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.entity.BlendPlanFeedback;
import com.coalblend.entity.CaseSample;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Orders;
import com.coalblend.mapper.BlendPlanDetailMapper;
import com.coalblend.mapper.BlendPlanFeedbackMapper;
import com.coalblend.mapper.BlendPlanMapper;
import com.coalblend.mapper.CaseSampleMapper;
import com.coalblend.mapper.CoalTypeMapper;
import com.coalblend.mapper.OrdersMapper;
import com.coalblend.service.BlendPlanFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlendPlanFeedbackServiceImpl implements BlendPlanFeedbackService {

    private final BlendPlanFeedbackMapper feedbackMapper;
    private final BlendPlanMapper blendPlanMapper;
    private final BlendPlanDetailMapper detailMapper;
    private final OrdersMapper ordersMapper;
    private final CoalTypeMapper coalTypeMapper;
    private final CaseSampleMapper caseSampleMapper;

    @Override
    public IPage<BlendPlanFeedback> page(long current, long size, Long planId, Long orderId, Integer qualifiedFlag) {
        Page<BlendPlanFeedback> page = new Page<>(current, size);
        LambdaQueryWrapper<BlendPlanFeedback> w = new LambdaQueryWrapper<>();
        if (planId != null) {
            w.eq(BlendPlanFeedback::getPlanId, planId);
        }
        if (orderId != null) {
            w.eq(BlendPlanFeedback::getOrderId, orderId);
        }
        if (qualifiedFlag != null) {
            w.eq(BlendPlanFeedback::getQualifiedFlag, qualifiedFlag);
        }
        w.orderByDesc(BlendPlanFeedback::getCreateTime).orderByDesc(BlendPlanFeedback::getId);
        return feedbackMapper.selectPage(page, w);
    }

    @Override
    public List<BlendPlanFeedback> listByPlan(Long planId) {
        return feedbackMapper.selectList(new LambdaQueryWrapper<BlendPlanFeedback>()
                .eq(BlendPlanFeedback::getPlanId, planId)
                .orderByDesc(BlendPlanFeedback::getCreateTime)
                .orderByDesc(BlendPlanFeedback::getId));
    }

    @Override
    public BlendPlanFeedback getById(Long id) {
        BlendPlanFeedback row = feedbackMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "反馈记录不存在");
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(BlendPlanFeedback feedback) {
        BlendPlan plan = loadPlan(feedback.getPlanId());
        feedback.setOrderId(plan.getOrderId());
        normalize(feedback);
        feedbackMapper.insert(feedback);
        markPlanExecuted(plan.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BlendPlanFeedback feedback) {
        if (feedback.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        BlendPlanFeedback old = getById(feedback.getId());
        if (old.getCaseGeneratedFlag() != null && old.getCaseGeneratedFlag() == 1) {
            throw new BusinessException("已沉淀为案例的反馈不建议直接修改");
        }
        BlendPlan plan = loadPlan(feedback.getPlanId() == null ? old.getPlanId() : feedback.getPlanId());
        feedback.setPlanId(plan.getId());
        feedback.setOrderId(plan.getOrderId());
        normalize(feedback);
        feedbackMapper.updateById(feedback);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToCase(Long feedbackId) {
        BlendPlanFeedback feedback = getById(feedbackId);
        if (feedback.getCaseGeneratedFlag() != null && feedback.getCaseGeneratedFlag() == 1 && feedback.getCaseId() != null) {
            return feedback.getCaseId();
        }
        BlendPlan plan = loadPlan(feedback.getPlanId());
        Orders order = ordersMapper.selectById(plan.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "方案关联订单不存在");
        }
        List<BlendPlanDetail> details = detailMapper.selectList(new LambdaQueryWrapper<BlendPlanDetail>()
                .eq(BlendPlanDetail::getPlanId, plan.getId())
                .orderByAsc(BlendPlanDetail::getId));

        CaseSample sample = new CaseSample();
        sample.setCaseCode(buildCaseCode(feedback.getId()));
        sample.setCaseName("反馈回流案例-" + plan.getPlanCode());
        sample.setOrderDesc(buildOrderDesc(order));
        sample.setBlendDesc(buildBlendDesc(details));
        sample.setResultDesc(buildResultDesc(feedback));
        sample.setQualityResult(buildQualityResult(feedback));
        sample.setCostResult(feedback.getActualCost());
        sample.setEffectivenessEval(feedback.getEffectivenessEval());
        sample.setStatus(1);
        caseSampleMapper.insert(sample);

        BlendPlanFeedback patch = new BlendPlanFeedback();
        patch.setId(feedback.getId());
        patch.setCaseGeneratedFlag(1);
        patch.setCaseId(sample.getId());
        feedbackMapper.updateById(patch);
        return sample.getId();
    }

    private BlendPlan loadPlan(Long planId) {
        if (planId == null) {
            throw new BusinessException("planId 不能为空");
        }
        BlendPlan plan = blendPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(404, "方案不存在");
        }
        return plan;
    }

    private void normalize(BlendPlanFeedback feedback) {
        if (feedback.getActualQuantity() == null || feedback.getActualQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("实际执行量必须大于 0");
        }
        if (feedback.getQualifiedFlag() == null) {
            feedback.setQualifiedFlag(1);
        }
        if (!StringUtils.hasText(feedback.getEffectivenessEval())) {
            feedback.setEffectivenessEval(feedback.getQualifiedFlag() == 1 ? "良好" : "一般");
        }
        if (feedback.getExecuteDate() == null) {
            feedback.setExecuteDate(LocalDate.now());
        }
        if (feedback.getCaseGeneratedFlag() == null) {
            feedback.setCaseGeneratedFlag(0);
        }
        if (feedback.getStatus() == null) {
            feedback.setStatus(1);
        }
    }

    private void markPlanExecuted(Long planId) {
        BlendPlan patch = new BlendPlan();
        patch.setId(planId);
        patch.setPlanStatus("executed");
        blendPlanMapper.updateById(patch);
    }

    private String buildCaseCode(Long feedbackId) {
        return "CF" + String.format("%06d", feedbackId);
    }

    private String buildOrderDesc(Orders order) {
        return "订单" + order.getOrderCode()
                + "，客户" + order.getCustomerName()
                + "，需求量" + fmt(order.getDemandQuantity()) + "吨"
                + "，灰分≤" + fmt(order.getTargetAsh()) + "%"
                + "，硫分≤" + fmt(order.getTargetSulfur()) + "%"
                + "，热值≥" + fmt(order.getTargetCalorific()) + "。";
    }

    private String buildBlendDesc(List<BlendPlanDetail> details) {
        if (details == null || details.isEmpty()) {
            return "方案无明细。";
        }
        return details.stream().map(d -> {
            CoalType coal = coalTypeMapper.selectById(d.getCoalId());
            String name = coal == null ? ("煤种" + d.getCoalId()) : coal.getCoalName();
            BigDecimal pct = d.getBlendRatio() == null ? null : d.getBlendRatio().multiply(new BigDecimal("100"));
            return name + "配比" + fmt(pct) + "%，用量" + fmt(d.getUseQuantity()) + "吨";
        }).collect(Collectors.joining("；"));
    }

    private String buildResultDesc(BlendPlanFeedback feedback) {
        String qualified = feedback.getQualifiedFlag() != null && feedback.getQualifiedFlag() == 1 ? "达标" : "未达标";
        String desc = StringUtils.hasText(feedback.getFeedbackDesc()) ? "；" + feedback.getFeedbackDesc() : "";
        return "实际执行量" + fmt(feedback.getActualQuantity()) + "吨，执行结果" + qualified + desc;
    }

    private String buildQualityResult(BlendPlanFeedback feedback) {
        return "灰分" + fmt(feedback.getActualAsh()) + "%"
                + "，硫分" + fmt(feedback.getActualSulfur()) + "%"
                + "，水分" + fmt(feedback.getActualMoisture()) + "%"
                + "，热值" + fmt(feedback.getActualCalorific());
    }

    private static String fmt(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
