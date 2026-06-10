package com.hsms.backend.common;

import java.time.Instant;

public record RiskPolicyDto(
            long id,
            String version,
            int warningThreshold,
            int blockThreshold,
            String formulaDescription,
            Instant activeFrom
    ) {
    }
