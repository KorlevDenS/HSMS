package com.hsms.backend.insurance.api;

import com.hsms.backend.common.InsuranceCaseDto;
import com.hsms.backend.common.InsuranceCaseOpenRequest;
import com.hsms.backend.common.InsuranceCloseRequest;
import com.hsms.backend.common.InsuranceRecalculateRequest;
import com.hsms.backend.common.InsuranceRecalculationDto;
import com.hsms.backend.common.InsuranceRejectRequest;
import com.hsms.backend.common.InsuranceTermsRequest;
import com.hsms.backend.common.InsuranceTrigger;

import java.util.List;

public interface InsuranceApi {
    List<InsuranceCaseDto> insuranceCases();

    InsuranceCaseDto insuranceCase(long caseId);

    InsuranceCaseDto openInsuranceCase(String actorLogin, InsuranceCaseOpenRequest request);

    default InsuranceCaseDto openInsuranceCase(String actorLogin, long missionId, Long incidentId, InsuranceTrigger trigger) {
        return openInsuranceCase(actorLogin, new InsuranceCaseOpenRequest(
                missionId,
                incidentId,
                trigger,
                null,
                null,
                null,
                null,
                null,
                actorLogin,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    InsuranceCaseDto recalculateInsurance(String actorLogin, long caseId, InsuranceRecalculateRequest request);

    InsuranceCaseDto updateInsuranceTerms(String actorLogin, long caseId, InsuranceTermsRequest request);

    InsuranceCaseDto rejectInsuranceRecalculation(String actorLogin, long caseId, InsuranceRejectRequest request);

    InsuranceCaseDto closeInsuranceCase(String actorLogin, long caseId, InsuranceCloseRequest request);

    List<InsuranceRecalculationDto> insuranceHistory(long caseId);
}
