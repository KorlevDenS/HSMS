package com.hsms.backend.common;

import java.math.BigDecimal;

public record InsuranceTermsRequest(BigDecimal premium, String reason) {
    }
