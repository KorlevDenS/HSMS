package com.hsms.backend.security.api;

import com.hsms.backend.common.HsmsDomain.AlarmRequest;
import com.hsms.backend.common.HsmsDomain.AlarmResponse;
import com.hsms.backend.common.HsmsDomain.ClassificationRequest;
import com.hsms.backend.common.HsmsDomain.EvacuationCommandDto;
import com.hsms.backend.common.HsmsDomain.EvacuationRequest;
import com.hsms.backend.common.HsmsDomain.IncidentDto;

import java.util.List;

public interface SecurityApi {
    AlarmResponse submitAlarm(String actorLogin, long missionId, AlarmRequest request);

    List<IncidentDto> incidentQueue();

    IncidentDto incident(long incidentId);

    IncidentDto classifyIncident(String actorLogin, long incidentId, ClassificationRequest request);

    EvacuationCommandDto issueEvacuation(String actorLogin, long incidentId, EvacuationRequest request);

    EvacuationCommandDto markEvacuationDelivered(String actorLogin, long incidentId);

    EvacuationCommandDto acknowledgeEvacuation(String actorLogin, long incidentId);

    EvacuationCommandDto markEvacuationDeliveryFailed(String actorLogin, long incidentId, EvacuationRequest request);

    IncidentDto closeIncident(String actorLogin, long incidentId);

    String incidentReportCsv();
}
