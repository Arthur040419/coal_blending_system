package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_document")
public class RagDocument {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String docCode;
    private String title;
    /** rule/case/term/doc */
    private String docType;
    /** rag_knowledge/rule_knowledge/case_sample */
    private String sourceType;
    private Long sourceId;
    private String tags;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
