package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_chunk")
public class RagChunk {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String chunkCode;
    private String chunkText;
    private Integer chunkIndex;
    private Integer tokenCount;
    private String tags;
    private String sourceType;
    private Long sourceId;
    private String vectorId;
    private String embeddingModel;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
