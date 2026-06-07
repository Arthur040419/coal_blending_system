package com.coalblend.vo.rag;

import com.coalblend.entity.RagKnowledge;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RagKnowledgeHitVO {

    private Long id;
    private Long chunkId;
    private Long documentId;
    private String knowledgeCode;
    private String title;
    private String knowledgeType;
    private String content;
    private String sourceTable;
    private Long sourceId;
    private String tags;
    private double score;
    private Double vectorScore;
    private Double keywordScore;
    private Double businessScore;
    private String retrievalMode;
    private List<String> hitKeywords = new ArrayList<>();
    private String hitReason;

    public static RagKnowledgeHitVO fromEntity(RagKnowledge k) {
        RagKnowledgeHitVO vo = new RagKnowledgeHitVO();
        vo.setId(k.getId());
        vo.setKnowledgeCode(k.getKnowledgeCode());
        vo.setTitle(k.getTitle());
        vo.setKnowledgeType(k.getKnowledgeType());
        vo.setContent(k.getContent());
        vo.setSourceTable(k.getSourceTable());
        vo.setSourceId(k.getSourceId());
        vo.setTags(k.getTags());
        return vo;
    }
}
