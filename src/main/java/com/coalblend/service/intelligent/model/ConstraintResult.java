package com.coalblend.service.intelligent.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ConstraintResult {

    private boolean feasible = true;
    private List<String> violations = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private BigDecimal predictedAsh;
    private BigDecimal predictedSulfur;
    private BigDecimal predictedMoisture;
    private BigDecimal predictedVolatile;
    private BigDecimal predictedCalorific;

    public void addViolation(String message) {
        feasible = false;
        violations.add(message);
    }

    public void addWarning(String message) {
        warnings.add(message);
    }

    public String riskLevel() {
        if (!violations.isEmpty()) {
            return "high";
        }
        if (!warnings.isEmpty()) {
            return "medium";
        }
        return "low";
    }
}
