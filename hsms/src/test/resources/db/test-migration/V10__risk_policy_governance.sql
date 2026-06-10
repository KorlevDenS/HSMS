ALTER TABLE risk_policy
    ADD COLUMN IF NOT EXISTS changed_by VARCHAR(80) NOT NULL DEFAULT 'system';

ALTER TABLE risk_policy
    ADD COLUMN IF NOT EXISTS change_reason VARCHAR(1000) NOT NULL DEFAULT 'Initial HSMS risk policy baseline';

ALTER TABLE risk_policy
    ADD COLUMN IF NOT EXISTS validated_scenarios VARCHAR(1000) NOT NULL DEFAULT 'normal launch, warning launch, blocking risk, degraded telemetry, incident, CHOAM insurance';

ALTER TABLE risk_policy
    ADD COLUMN IF NOT EXISTS choam_impact VARCHAR(1000) NOT NULL DEFAULT 'Risk policy version is persisted in risk snapshots and referenced by insurance decisions';
