package com.coalblend.vo.blend;

import lombok.Data;

@Data
public class DecisionSuggestionItemVO {
    private String type;
    private String action;
    private String message;
    private Integer priority;
}
