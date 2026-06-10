package com.hsms.backend.common;

import java.util.List;

public record DesertSimulationResult(
            MissionDto mission,
            List<TelemetryEventDto> telemetry,
            AlarmResponse alarm,
            RiskSnapshotDto risk
    ) {
        public DesertSimulationResult {
            telemetry = DomainCollections.immutableList(telemetry);
        }

        @Override
        public List<TelemetryEventDto> telemetry() {
            return DomainCollections.immutableList(telemetry);
        }
    }
