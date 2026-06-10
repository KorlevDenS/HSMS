package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.common.HsmsDomain.AlarmRequest;
import com.hsms.backend.common.HsmsDomain.AlarmResponse;
import com.hsms.backend.common.HsmsDomain.ClassificationRequest;
import com.hsms.backend.common.HsmsDomain.EvacuationCommandDto;
import com.hsms.backend.common.HsmsDomain.EvacuationRequest;
import com.hsms.backend.common.HsmsDomain.IncidentDto;
import com.hsms.backend.security.api.SecurityApi;
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
public class SecurityController extends HsmsControllerSupport {

    private final SecurityApi securityApi;

    public SecurityController(SecurityApi securityApi) {
        this.securityApi = securityApi;
    }

    @PostMapping("/missions/{missionId}/alarms")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public AlarmResponse submitAlarm(
            Authentication authentication,
            @PathVariable long missionId,
            @RequestBody AlarmRequest request
    ) {
        return securityApi.submitAlarm(actor(authentication), missionId, request);
    }

    @GetMapping("/incidents/queue")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public List<IncidentDto> incidentQueue() {
        return securityApi.incidentQueue();
    }

    @GetMapping("/incidents/{incidentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_SUPPLY_MANAGER','ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public IncidentDto incident(@PathVariable long incidentId) {
        return securityApi.incident(incidentId);
    }

    @PatchMapping("/incidents/{incidentId}/classification")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public IncidentDto classifyIncident(
            Authentication authentication,
            @PathVariable long incidentId,
            @RequestBody ClassificationRequest request
    ) {
        return securityApi.classifyIncident(actor(authentication), incidentId, request);
    }

    @PostMapping("/incidents/{incidentId}/evacuation")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public EvacuationCommandDto issueEvacuation(
            Authentication authentication,
            @PathVariable long incidentId,
            @RequestBody(required = false) EvacuationRequest request
    ) {
        return securityApi.issueEvacuation(actor(authentication), incidentId, request);
    }

    @PostMapping("/incidents/{incidentId}/evacuation/delivered")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public EvacuationCommandDto markEvacuationDelivered(Authentication authentication, @PathVariable long incidentId) {
        return securityApi.markEvacuationDelivered(actor(authentication), incidentId);
    }

    @PostMapping("/incidents/{incidentId}/evacuation/ack")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public EvacuationCommandDto acknowledgeEvacuation(Authentication authentication, @PathVariable long incidentId) {
        return securityApi.acknowledgeEvacuation(actor(authentication), incidentId);
    }

    @PostMapping("/incidents/{incidentId}/evacuation/delivery-failed")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public EvacuationCommandDto markEvacuationDeliveryFailed(
            Authentication authentication,
            @PathVariable long incidentId,
            @RequestBody(required = false) EvacuationRequest request
    ) {
        return securityApi.markEvacuationDeliveryFailed(actor(authentication), incidentId, request);
    }

    @PostMapping("/incidents/{incidentId}/close")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public IncidentDto closeIncident(Authentication authentication, @PathVariable long incidentId) {
        return securityApi.closeIncident(actor(authentication), incidentId);
    }
}
