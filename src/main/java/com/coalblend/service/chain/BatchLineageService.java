package com.coalblend.service.chain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.entity.BatchLineage;
import com.coalblend.mapper.BatchLineageMapper;
import com.coalblend.vo.chain.TraceNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BatchLineageService {

    private final BatchLineageMapper batchLineageMapper;

    public void record(String parentNo, String parentType, String childNo, String childType,
                       String stage, BigDecimal quantity, BigDecimal ratio, String operator, String remark) {
        if (!StringUtils.hasText(parentNo) || !StringUtils.hasText(childNo)) {
            return;
        }
        BatchLineage row = new BatchLineage();
        row.setParentBatchNo(parentNo);
        row.setParentBatchType(parentType);
        row.setChildBatchNo(childNo);
        row.setChildBatchType(childType);
        row.setProcessStage(stage);
        row.setQuantity(quantity);
        row.setRatio(ratio);
        row.setOperatorName(operator);
        row.setRemark(remark);
        batchLineageMapper.insert(row);
    }

    public List<BatchLineage> upstream(String batchNo) {
        List<BatchLineage> out = new ArrayList<>();
        collectUpstream(batchNo, out, new HashSet<>(), 0);
        return out;
    }

    public List<BatchLineage> downstream(String batchNo) {
        List<BatchLineage> out = new ArrayList<>();
        collectDownstream(batchNo, out, new HashSet<>(), 0);
        return out;
    }

    public TraceNodeVO upstreamTree(String batchNo, String batchType) {
        TraceNodeVO root = node(batchNo, batchType, null, null, null);
        fillUpstream(root, new HashSet<>(), 0);
        return root;
    }

    private void collectUpstream(String childNo, List<BatchLineage> out, Set<String> seen, int depth) {
        if (!StringUtils.hasText(childNo) || depth > 12 || !seen.add("U:" + childNo)) {
            return;
        }
        List<BatchLineage> rows = batchLineageMapper.selectList(new LambdaQueryWrapper<BatchLineage>()
                .eq(BatchLineage::getChildBatchNo, childNo)
                .orderByAsc(BatchLineage::getId));
        for (BatchLineage r : rows) {
            out.add(r);
            collectUpstream(r.getParentBatchNo(), out, seen, depth + 1);
        }
    }

    private void collectDownstream(String parentNo, List<BatchLineage> out, Set<String> seen, int depth) {
        if (!StringUtils.hasText(parentNo) || depth > 12 || !seen.add("D:" + parentNo)) {
            return;
        }
        List<BatchLineage> rows = batchLineageMapper.selectList(new LambdaQueryWrapper<BatchLineage>()
                .eq(BatchLineage::getParentBatchNo, parentNo)
                .orderByAsc(BatchLineage::getId));
        for (BatchLineage r : rows) {
            out.add(r);
            collectDownstream(r.getChildBatchNo(), out, seen, depth + 1);
        }
    }

    private void fillUpstream(TraceNodeVO node, Set<String> seen, int depth) {
        if (node == null || depth > 12 || !seen.add(node.getBatchNo())) {
            return;
        }
        List<BatchLineage> rows = batchLineageMapper.selectList(new LambdaQueryWrapper<BatchLineage>()
                .eq(BatchLineage::getChildBatchNo, node.getBatchNo())
                .orderByAsc(BatchLineage::getId));
        for (BatchLineage r : rows) {
            TraceNodeVO child = node(r.getParentBatchNo(), r.getParentBatchType(), r.getProcessStage(),
                    r.getQuantity(), r.getRatio());
            node.getChildren().add(child);
            fillUpstream(child, seen, depth + 1);
        }
    }

    private TraceNodeVO node(String batchNo, String batchType, String stage, BigDecimal quantity, BigDecimal ratio) {
        TraceNodeVO n = new TraceNodeVO();
        n.setBatchNo(batchNo);
        n.setBatchType(batchType);
        n.setProcessStage(stage);
        n.setQuantity(quantity);
        n.setRatio(ratio);
        n.setName(batchTypeName(batchType) + " " + batchNo);
        return n;
    }

    private String batchTypeName(String type) {
        if ("mine_source".equals(type)) return "矿区来源";
        if ("raw_coal".equals(type)) return "原煤批次";
        if ("wash_batch".equals(type)) return "洗选批次";
        if ("product_batch".equals(type)) return "产品批次";
        if ("final_product".equals(type)) return "最终产品";
        if ("shipment".equals(type)) return "发运批次";
        return StringUtils.hasText(type) ? type : "批次";
    }
}
