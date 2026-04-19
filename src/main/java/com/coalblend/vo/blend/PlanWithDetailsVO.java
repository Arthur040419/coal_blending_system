package com.coalblend.vo.blend;

import com.coalblend.entity.BlendPlan;
import lombok.Data;

import java.util.List;

@Data
public class PlanWithDetailsVO {

    private BlendPlan plan;
    private List<PlanDetailVO> details;
}
