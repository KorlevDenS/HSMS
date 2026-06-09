ALTER TABLE mission ADD COLUMN IF NOT EXISTS closed_by VARCHAR(80);
ALTER TABLE mission ADD COLUMN IF NOT EXISTS draft_missing_fields VARCHAR(1000);
ALTER TABLE mission ADD COLUMN IF NOT EXISTS monitoring_priority INTEGER NOT NULL DEFAULT 0 CHECK (monitoring_priority >= 0 AND monitoring_priority <= 100);
ALTER TABLE mission ADD COLUMN IF NOT EXISTS monitoring_context VARCHAR(1000);
ALTER TABLE mission ADD COLUMN IF NOT EXISTS risk_review_required_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE mission ADD COLUMN IF NOT EXISTS risk_review_reason VARCHAR(1000);

ALTER TABLE mission ALTER COLUMN zone_id DROP NOT NULL;
ALTER TABLE mission ALTER COLUMN harvester_id DROP NOT NULL;
ALTER TABLE mission ALTER COLUMN crew_id DROP NOT NULL;
ALTER TABLE mission ALTER COLUMN planned_start DROP NOT NULL;
ALTER TABLE mission ALTER COLUMN planned_end DROP NOT NULL;

ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS trigger_reason VARCHAR(1000);
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS trigger_p_attack DOUBLE PRECISION CHECK (trigger_p_attack IS NULL OR (trigger_p_attack >= 0 AND trigger_p_attack <= 1));
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS trigger_risk_score INTEGER CHECK (trigger_risk_score IS NULL OR (trigger_risk_score >= 0 AND trigger_risk_score <= 100));
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS trigger_risk_snapshot_id BIGINT REFERENCES risk_snapshot(id);
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS trigger_decision_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS trigger_decision_by VARCHAR(80);
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS incident_severity VARCHAR(40);
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS incident_registered_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS incident_closed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS incident_sla_breached BOOLEAN;
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS incident_operator VARCHAR(80);
ALTER TABLE insurance_case ADD COLUMN IF NOT EXISTS missing_data VARCHAR(1000);

ALTER TABLE insurance_recalculation ADD COLUMN IF NOT EXISTS event_type VARCHAR(40) NOT NULL DEFAULT 'RECALCULATION';
ALTER TABLE insurance_recalculation ALTER COLUMN risk_snapshot_id DROP NOT NULL;

DELETE FROM permission WHERE role IN (SELECT id FROM role WHERE name = 'ROLE_SUPERVISOR');
DELETE FROM role WHERE name = 'ROLE_SUPERVISOR';

CREATE OR REPLACE FUNCTION hsms_prevent_audit_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'HSMS audit log is immutable';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS audit_event_immutable_update ON audit_event;
DROP TRIGGER IF EXISTS audit_event_immutable_delete ON audit_event;
DROP TRIGGER IF EXISTS audit_detail_immutable_update ON audit_detail;
DROP TRIGGER IF EXISTS audit_detail_immutable_delete ON audit_detail;

CREATE TRIGGER audit_event_immutable_update
BEFORE UPDATE ON audit_event
FOR EACH ROW EXECUTE FUNCTION hsms_prevent_audit_mutation();

CREATE TRIGGER audit_event_immutable_delete
BEFORE DELETE ON audit_event
FOR EACH ROW EXECUTE FUNCTION hsms_prevent_audit_mutation();

CREATE TRIGGER audit_detail_immutable_update
BEFORE UPDATE ON audit_detail
FOR EACH ROW EXECUTE FUNCTION hsms_prevent_audit_mutation();

CREATE TRIGGER audit_detail_immutable_delete
BEFORE DELETE ON audit_detail
FOR EACH ROW EXECUTE FUNCTION hsms_prevent_audit_mutation();
