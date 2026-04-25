package com.coalblend.vo.chain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TraceNodeVO {
    private String batchNo;
    private String batchType;
    private String processStage;
    private BigDecimal quantity;
    private BigDecimal ratio;
    private String name;
    private List<TraceNodeVO> children = new ArrayList<>();
}
