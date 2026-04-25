package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rule_knowledge")
public class RuleKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String ruleContent;
    private String applicableScope;
    private Integer priorityLevel;
    private Integer status;
    private String sourceDesc;
    private String businessStage;
    private String qualityIndicator;
    private String materialType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
