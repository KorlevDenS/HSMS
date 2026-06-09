CREATE SEQUENCE IF NOT EXISTS hsms_domain_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS hsms_user_id_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS role_id_seq START WITH 100 INCREMENT BY 1;

CREATE INDEX IF NOT EXISTS idx_mission_status_planned_start ON mission(status, planned_start);
CREATE INDEX IF NOT EXISTS idx_mission_harvester_status ON mission(harvester_id, status);
CREATE INDEX IF NOT EXISTS idx_mission_crew_status ON mission(crew_id, status);
CREATE INDEX IF NOT EXISTS idx_risk_snapshot_mission_calculated ON risk_snapshot(mission_id, calculated_at DESC);
CREATE INDEX IF NOT EXISTS idx_incident_status_sla_deadline ON incident(status, sla_deadline_at);
CREATE INDEX IF NOT EXISTS idx_insurance_case_status_mission ON insurance_case(status, mission_id);
CREATE INDEX IF NOT EXISTS idx_audit_object ON audit_event(object_type, object_id);
