package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.RuleKnowledge;
import com.coalblend.mapper.RuleKnowledgeMapper;
import com.coalblend.service.RuleKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleKnowledgeServiceImpl implements RuleKnowledgeService {

    private final RuleKnowledgeMapper ruleKnowledgeMapper;

    @Override
    public IPage<RuleKnowledge> page(long current, long size, String ruleType, Integer status, String keyword) {
        Page<RuleKnowledge> page = new Page<>(current, size);
        LambdaQueryWrapper<RuleKnowledge> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(ruleType)) {
            w.eq(RuleKnowledge::getRuleType, ruleType);
        }
        if (status != null) {
            w.eq(RuleKnowledge::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(RuleKnowledge::getRuleName, keyword)
                    .or().like(RuleKnowledge::getRuleCode, keyword)
                    .or().like(RuleKnowledge::getRuleContent, keyword));
        }
        w.orderByDesc(RuleKnowledge::getId);
        return ruleKnowledgeMapper.selectPage(page, w);
    }

    @Override
    public List<RuleKnowledge> listByType(String ruleType) {
        return ruleKnowledgeMapper.selectList(new LambdaQueryWrapper<RuleKnowledge>()
                .eq(RuleKnowledge::getRuleType, ruleType)
                .orderByDesc(RuleKnowledge::getPriorityLevel));
    }

    @Override
    public List<RuleKnowledge> listEnabled() {
        return ruleKnowledgeMapper.selectList(new LambdaQueryWrapper<RuleKnowledge>()
                .eq(RuleKnowledge::getStatus, 1)
                .orderByDesc(RuleKnowledge::getPriorityLevel)
                .last("limit 200"));
    }

    @Override
    public RuleKnowledge getById(Long id) {
        RuleKnowledge row = ruleKnowledgeMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "规则不存在");
        }
        return row;
    }

    @Override
    public void add(RuleKnowledge entity) {
        Long cnt = ruleKnowledgeMapper.selectCount(
                new LambdaQueryWrapper<RuleKnowledge>().eq(RuleKnowledge::getRuleCode, entity.getRuleCode()));
        if (cnt != null && cnt > 0) {
            throw new BusinessException("规则编号已存在");
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        ruleKnowledgeMapper.insert(entity);
    }

    @Override
    public void update(RuleKnowledge entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        getById(entity.getId());
        if (StringUtils.hasText(entity.getRuleCode())) {
            Long cnt = ruleKnowledgeMapper.selectCount(new LambdaQueryWrapper<RuleKnowledge>()
                    .eq(RuleKnowledge::getRuleCode, entity.getRuleCode())
                    .ne(RuleKnowledge::getId, entity.getId()));
            if (cnt != null && cnt > 0) {
                throw new BusinessException("规则编号已存在");
            }
        }
        ruleKnowledgeMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        ruleKnowledgeMapper.deleteById(id);
    }
}
