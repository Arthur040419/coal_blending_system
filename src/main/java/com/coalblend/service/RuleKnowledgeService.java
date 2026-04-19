package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.entity.RuleKnowledge;

import java.util.List;

public interface RuleKnowledgeService {

    IPage<RuleKnowledge> page(long current, long size, String ruleType, Integer status, String keyword);

    List<RuleKnowledge> listByType(String ruleType);

    List<RuleKnowledge> listEnabled();

    RuleKnowledge getById(Long id);

    void add(RuleKnowledge entity);

    void update(RuleKnowledge entity);

    void delete(Long id);
}
