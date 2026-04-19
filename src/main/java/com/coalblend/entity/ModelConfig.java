package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("model_config")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelName;
    private String modelType;
    private String apiUrl;
    private String apiKey;
    private BigDecimal temperature;
    @TableField("top_p")
    private BigDecimal topP;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
