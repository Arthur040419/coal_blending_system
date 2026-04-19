package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("case_sample")
public class CaseSample {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String caseCode;
    private String caseName;
    private String orderDesc;
    private String blendDesc;
    private String resultDesc;
    private String qualityResult;
    private BigDecimal costResult;
    private String effectivenessEval;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
