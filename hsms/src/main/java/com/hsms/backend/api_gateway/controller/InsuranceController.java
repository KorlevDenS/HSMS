package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.common.InsuranceCaseDto;
import com.hsms.backend.common.InsuranceCloseRequest;
import com.hsms.backend.common.InsuranceRecalculateRequest;
import com.hsms.backend.common.InsuranceRecalculationDto;
import com.hsms.backend.common.InsuranceRejectRequest;
import com.hsms.backend.common.InsuranceTermsRequest;
import com.hsms.backend.insurance.api.InsuranceApi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InsuranceController extends HsmsControllerSupport {

    private final InsuranceApi insuranceApi;

    public InsuranceController(InsuranceApi insuranceApi) {
        this.insuranceApi = insuranceApi;
    }

    @GetMapping("/insurance-cases")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public List<InsuranceCaseDto> insuranceCases() {
        return insuranceApi.insuranceCases();
    }

    @GetMapping("/insurance-cases/{caseId}")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto insuranceCase(@PathVariable long caseId) {
        return insuranceApi.insuranceCase(caseId);
    }

    @GetMapping("/insurance-cases/{caseId}/history")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public List<InsuranceRecalculationDto> insuranceHistory(@PathVariable long caseId) {
        return insuranceApi.insuranceHistory(caseId);
    }

    @PostMapping("/insurance-cases/{caseId}/recalculate")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto recalculateInsurance(
            Authentication authentication,
            @PathVariable long caseId,
            @RequestBody(required = false) InsuranceRecalculateRequest request
    ) {
        return insuranceApi.recalculateInsurance(actor(authentication), caseId, request);
    }

    @PatchMapping("/insurance-cases/{caseId}/terms")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto updateInsuranceTerms(
            Authentication authentication,
            @PathVariable long caseId,
            @RequestBody InsuranceTermsRequest request
    ) {
        return insuranceApi.updateInsuranceTerms(actor(authentication), caseId, request);
    }

    @PostMapping("/insurance-cases/{caseId}/reject-recalculation")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto rejectInsurance(
            Authentication authentication,
            @PathVariable long caseId,
            @RequestBody(required = false) InsuranceRejectRequest request
    ) {
        return insuranceApi.rejectInsuranceRecalculation(actor(authentication), caseId, request);
    }

    @PostMapping("/insurance-cases/{caseId}/close")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto closeInsurance(
            Authentication authentication,
            @PathVariable long caseId,
            @RequestBody(required = false) InsuranceCloseRequest request
    ) {
        return insuranceApi.closeInsuranceCase(actor(authentication), caseId, request);
    }
}
