package com.coalblend.vo.experiment;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExperimentRadarVO {

    private String experimentCode;
    private Long orderId;
    private String modelName;
    private List<ExperimentRadarItemVO> items = new ArrayList<>();
}
