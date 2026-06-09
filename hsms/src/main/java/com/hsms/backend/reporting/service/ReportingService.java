package com.hsms.backend.reporting.service;

import com.hsms.backend.readmodel.HsmsDtoAssembler;
import com.hsms.backend.reporting.api.ReportingApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportingService implements ReportingApi {

    private final HsmsDtoAssembler dto;

    public ReportingService(HsmsDtoAssembler dto) {
        this.dto = dto;
    }

    @Override
    public String missionReportCsv() {
        return dto.missionReportCsv();
    }

    @Override
    public String incidentReportCsv() {
        return dto.incidentReportCsv();
    }
}
