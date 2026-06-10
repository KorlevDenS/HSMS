package com.hsms.backend.common;

public record RiskPolicyUpdateRequest(
            String version,
            Integer warningThreshold,
            Integer blockThreshold,
            String formulaDescription,
            String changeReason,
            String validatedScenarios,
            String choamImpact
    ) {
    }
