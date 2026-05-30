package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_retrieval_log")
public class RagRetrievalLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** blend_generate/chat/search */
    private String bizType;
    /** 方案ID或其他业务ID */
    private Long bizId;
    private String queryText;
    private String keywords;
    private String retrievedIds;
    private String modelName;
    private String promptText;
    private String modelOutput;
    private String retrievalMode;
    private String queryEmbeddingModel;
    private String retrievedChunksJson;
    private String rerankResultJson;
    private String usedChunkIds;
    private LocalDateTime createTime;
}
