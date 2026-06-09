package com.hsms.backend.insurance.api;

public interface InsuranceApi {
    InsurancePolicyResponse createInsurancePolicy(InsurancePolicyRequest insurancePolicy);
    // with Patch logic
    InsurancePolicyResponse updateInsurancePolicy(Long insurancePolicyId, InsurancePolicyRequest insurancePolicy);
}
