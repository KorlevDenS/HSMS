package com.hsms.backend.common;

import java.util.List;

public record BootstrapDto(
            HsmsUserDto user,
            List<HsmsUserDto> users,
            List<MiningZoneDto> zones,
            List<HarvesterDto> harvesters,
            List<CrewDto> crews,
            List<MissionDto> missions,
            List<IncidentDto> incidents,
            List<InsuranceCaseDto> insuranceCases,
            List<AuditEventDto> audit,
            RiskPolicyDto activeRiskPolicy,
            DashboardDto dashboard
    ) {
        public BootstrapDto {
            users = DomainCollections.immutableList(users);
            zones = DomainCollections.immutableList(zones);
            harvesters = DomainCollections.immutableList(harvesters);
            crews = DomainCollections.immutableList(crews);
            missions = DomainCollections.immutableList(missions);
            incidents = DomainCollections.immutableList(incidents);
            insuranceCases = DomainCollections.immutableList(insuranceCases);
            audit = DomainCollections.immutableList(audit);
        }

        @Override
        public List<HsmsUserDto> users() {
            return DomainCollections.immutableList(users);
        }

        @Override
        public List<MiningZoneDto> zones() {
            return DomainCollections.immutableList(zones);
        }

        @Override
        public List<HarvesterDto> harvesters() {
            return DomainCollections.immutableList(harvesters);
        }

        @Override
        public List<CrewDto> crews() {
            return DomainCollections.immutableList(crews);
        }

        @Override
        public List<MissionDto> missions() {
            return DomainCollections.immutableList(missions);
        }

        @Override
        public List<IncidentDto> incidents() {
            return DomainCollections.immutableList(incidents);
        }

        @Override
        public List<InsuranceCaseDto> insuranceCases() {
            return DomainCollections.immutableList(insuranceCases);
        }

        @Override
        public List<AuditEventDto> audit() {
            return DomainCollections.immutableList(audit);
        }
    }
