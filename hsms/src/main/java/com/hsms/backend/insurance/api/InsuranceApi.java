package com.hsms.backend.insurance.api;

import com.hsms.backend.common.HsmsDomain.InsuranceCaseDto;
import com.hsms.backend.common.HsmsDomain.InsuranceCaseOpenRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceCloseRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceRecalculateRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceRecalculationDto;
import com.hsms.backend.common.HsmsDomain.InsuranceRejectRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceTermsRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceTrigger;

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
