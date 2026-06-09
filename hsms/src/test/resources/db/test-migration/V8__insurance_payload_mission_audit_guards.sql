ALTER TABLE mission ADD COLUMN IF NOT EXISTS closed_by VARCHAR(80);
ALTER TABLE mission ADD COLUMN IF NOT EXISTS draft_missing_fields VARCHAR(1000);
ALTER TABLE mission ADD COLUMN IF NOT EXISTS monitoring_priority INTEGER NOT NULL DEFAULT 0 CHECK (monitoring_priority >= 0 AND monitoring_priority <= 100);
ALTER TABLE mission ADD COLUMN IF NOT EXISTS monitoring_context VARCHAR(1000);
ALTER TABLE mission ADD COLUMN IF NOT EXISTS risk_review_required_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE mission ADD COLUMN IF NOT EXISTS risk_review_reason VARCHAR(1000);

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

DELETE FROM permission WHERE role IN (SELECT id FROM role WHERE name = 'ROLE_SUPERVISOR');
DELETE FROM role WHERE name = 'ROLE_SUPERVISOR';
