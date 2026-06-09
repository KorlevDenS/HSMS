package com.hsms.backend.api_gateway;

import com.hsms.backend.insurance.api.InsuranceApi;
import com.hsms.backend.insurance.api.InsurancePolicyRequest;
import com.hsms.backend.insurance.api.InsurancePolicyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/insurance")
public class InsuranceController {

    private final InsuranceApi insuranceApi;

    public InsuranceController(InsuranceApi insuranceApi) {
        this.insuranceApi = insuranceApi;
    }

    @PostMapping("/")
    public ResponseEntity<InsurancePolicyResponse> createInsurancePolicy(
            @RequestBody InsurancePolicyRequest insurancePolicy
    ) {
        return ResponseEntity.ok(insuranceApi.createInsurancePolicy(insurancePolicy));
    }


    @PatchMapping("/{insurancePolicyId}")
    public ResponseEntity<InsurancePolicyResponse> updateInsurancePolicy(
            @PathVariable Long insurancePolicyId,
            @RequestBody InsurancePolicyRequest insurancePolicy) {
        return ResponseEntity.ok(insuranceApi.updateInsurancePolicy(insurancePolicyId, insurancePolicy));
    }


}
