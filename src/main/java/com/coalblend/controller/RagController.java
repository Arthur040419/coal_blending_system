package com.coalblend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.common.result.Result;
import com.coalblend.entity.Orders;
import com.coalblend.entity.RagChunk;
import com.coalblend.entity.RagDocument;
import com.coalblend.mapper.OrdersMapper;
import com.coalblend.mapper.RagChunkMapper;
import com.coalblend.mapper.RagDocumentMapper;
import com.coalblend.service.rag.RagIngestService;
import com.coalblend.service.rag.RagRetrieveService;
import com.coalblend.service.rag.RagVectorStoreService;
import com.coalblend.vo.rag.RagRetrieveResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagIngestService ragIngestService;
    private final RagRetrieveService ragRetrieveService;
    private final RagVectorStoreService ragVectorStoreService;
    private final OrdersMapper ordersMapper;
    private final RagDocumentMapper ragDocumentMapper;
    private final RagChunkMapper ragChunkMapper;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of("qdrantAvailable", ragVectorStoreService.available()));
    }

    @GetMapping("/documents")
    public Result<List<RagDocument>> documents(@RequestParam(defaultValue = "30") int limit) {
        return Result.ok(ragDocumentMapper.selectList(new LambdaQueryWrapper<RagDocument>()
                .orderByDesc(RagDocument::getUpdateTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100)))));
    }

    @GetMapping("/chunks")
    public Result<List<RagChunk>> chunks(@RequestParam(required = false) Long documentId,
                                         @RequestParam(defaultValue = "50") int limit) {
        LambdaQueryWrapper<RagChunk> query = new LambdaQueryWrapper<RagChunk>()
                .orderByDesc(RagChunk::getUpdateTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        if (documentId != null) {
            query.eq(RagChunk::getDocumentId, documentId);
        }
        return Result.ok(ragChunkMapper.selectList(query));
    }

    @PostMapping("/ingest/all")
    public Result<Map<String, Object>> ingestAll() {
        return Result.ok(ragIngestService.ingestAll());
    }

    @PostMapping("/ingest/knowledge/{id}")
    public Result<Map<String, Object>> ingestKnowledge(@PathVariable Long id) {
        return Result.ok(ragIngestService.ingestRagKnowledge(id));
    }

    @PostMapping("/ingest/rule/{id}")
    public Result<Map<String, Object>> ingestRule(@PathVariable Long id) {
        return Result.ok(ragIngestService.ingestRule(id));
    }

    @PostMapping("/ingest/case/{id}")
    public Result<Map<String, Object>> ingestCase(@PathVariable Long id) {
        return Result.ok(ragIngestService.ingestCase(id));
    }

    @GetMapping("/retrieve/order/{orderId}")
    public Result<RagRetrieveResultVO> retrieveByOrder(@PathVariable Long orderId,
                                                       @RequestParam(defaultValue = "8") int topK) {
        Orders order = ordersMapper.selectById(orderId);
        return Result.ok(ragRetrieveService.retrieveByOrder(order, topK));
    }
}
