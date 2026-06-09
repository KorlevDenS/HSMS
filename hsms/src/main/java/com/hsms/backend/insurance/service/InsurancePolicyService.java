package com.hsms.backend.insurance.service;

import com.hsms.backend.insurance.api.InsuranceApi;
import com.hsms.backend.insurance.api.InsurancePolicyRequest;
import com.hsms.backend.insurance.api.InsurancePolicyResponse;
import org.springframework.stereotype.Service;

@Service
public class InsurancePolicyService implements InsuranceApi {

    @Override
    public InsurancePolicyResponse createInsurancePolicy(InsurancePolicyRequest insurancePolicy) {
        return null;
    }

    @Override
    public InsurancePolicyResponse updateInsurancePolicy(
            Long insurancePolicyId,
            InsurancePolicyRequest insurancePolicy
    ) {
        return null;
    }
}
