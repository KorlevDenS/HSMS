package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.reporting.api.ReportingApi;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReportingController {

    private final ReportingApi reportingApi;

    public ReportingController(ReportingApi reportingApi) {
        this.reportingApi = reportingApi;
    }

    @GetMapping(value = "/reports/missions.csv", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public ResponseEntity<String> missionReportCsv() {
        return csv("missions.csv", reportingApi.missionReportCsv());
    }

    @GetMapping(value = "/reports/incidents.csv", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public ResponseEntity<String> incidentReportCsv() {
        return csv("incidents.csv", reportingApi.incidentReportCsv());
    }

    private ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.valueOf("text/csv; charset=UTF-8"))
                .body(body);
    }
}
