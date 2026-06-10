package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.common.RiskPolicyDto;
import com.hsms.backend.common.RiskPolicyUpdateRequest;
import com.hsms.backend.risk.api.RiskApi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RiskController extends HsmsControllerSupport {

    private final RiskApi riskApi;

    public RiskController(RiskApi riskApi) {
        this.riskApi = riskApi;
    }

    @PatchMapping("/risk-policy")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public RiskPolicyDto updateRiskPolicy(
            Authentication authentication,
            @RequestBody RiskPolicyUpdateRequest request
    ) {
        return riskApi.updateRiskPolicy(actor(authentication), request);
    }
}
