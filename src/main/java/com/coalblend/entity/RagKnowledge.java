package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_knowledge")
public class RagKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String knowledgeCode;
    private String title;
    /** rule/case/term/doc */
    private String knowledgeType;
    private String content;
    private String sourceTable;
    private Long sourceId;
    private String tags;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
