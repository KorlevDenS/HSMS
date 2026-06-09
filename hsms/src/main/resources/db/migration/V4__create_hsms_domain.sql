INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT 1, 'Диспетчер снабжения', '+7-900-001', 'dispatcher@hsms.local', 'dispatcher', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'dispatcher');

INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT 2, 'Экипаж харвестера', '+7-900-002', 'crew@hsms.local', 'crew', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'crew');

INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT 3, 'Оператор штаба', '+7-900-003', 'security@hsms.local', 'security', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'security');

INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT 4, 'Оператор страхового контура', '+7-900-004', 'insurance@hsms.local', 'insurance', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'insurance');

INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT 5, 'Руководство операций', '+7-900-005', 'management@hsms.local', 'management', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'management');

INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT 6, 'Администратор', '+7-900-006', 'admin@hsms.local', 'admin', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'admin');

INSERT INTO permission (client, role)
SELECT u.id, r.id
FROM hsms_user u, role r
WHERE u.login = 'dispatcher' AND r.name = 'ROLE_SUPPLY_MANAGER'
ON CONFLICT DO NOTHING;

INSERT INTO permission (client, role)
SELECT u.id, r.id
FROM hsms_user u, role r
WHERE u.login = 'crew' AND r.name = 'ROLE_HARVESTER_CREW'
ON CONFLICT DO NOTHING;

INSERT INTO permission (client, role)
SELECT u.id, r.id
FROM hsms_user u, role r
WHERE u.login = 'security' AND r.name = 'ROLE_SECURITY_HEADQUARTERS_OPERATOR'
ON CONFLICT DO NOTHING;

INSERT INTO permission (client, role)
SELECT u.id, r.id
FROM hsms_user u, role r
WHERE u.login = 'insurance' AND r.name = 'ROLE_INSURANCE_CONTOUR_OPERATOR'
ON CONFLICT DO NOTHING;

INSERT INTO permission (client, role)
SELECT u.id, r.id
FROM hsms_user u, role r
WHERE u.login = 'management' AND r.name = 'ROLE_OPERATIONS_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO permission (client, role)
SELECT u.id, r.id
FROM hsms_user u, role r
WHERE u.login = 'admin' AND r.name = 'ROLE_ADMINISTRATOR'
ON CONFLICT DO NOTHING;

SELECT setval('hsms_user_id_seq', (SELECT COALESCE(MAX(id), 1) FROM hsms_user));

CREATE TABLE mining_zone (
    id BIGINT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    risk_level DOUBLE PRECISION NOT NULL CHECK (risk_level >= 0 AND risk_level <= 1),
    coordinates VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE harvester (
    id BIGINT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(60) NOT NULL,
    status VARCHAR(40) NOT NULL,
    noise_level DOUBLE PRECISION NOT NULL CHECK (noise_level >= 0 AND noise_level <= 1),
    capacity INTEGER NOT NULL CHECK (capacity > 0)
);

CREATE TABLE crew (
    id BIGINT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(40) NOT NULL,
    contact_channel VARCHAR(160) NOT NULL,
    member_count INTEGER NOT NULL CHECK (member_count > 0)
);

CREATE TABLE risk_policy (
    id BIGINT PRIMARY KEY,
    version VARCHAR(40) NOT NULL UNIQUE,
    warning_threshold INTEGER NOT NULL CHECK (warning_threshold >= 0 AND warning_threshold <= 100),
    block_threshold INTEGER NOT NULL CHECK (block_threshold >= 0 AND block_threshold <= 100),
    formula_description VARCHAR(1000) NOT NULL,
    active_from TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE mission (
    id BIGINT PRIMARY KEY,
    title VARCHAR(240) NOT NULL,
    status VARCHAR(60) NOT NULL,
    zone_id BIGINT REFERENCES mining_zone(id),
    harvester_id BIGINT REFERENCES harvester(id),
    crew_id BIGINT REFERENCES crew(id),
    planned_start TIMESTAMP WITH TIME ZONE,
    planned_end TIMESTAMP WITH TIME ZONE,
    actual_start TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    close_reason VARCHAR(1000),
    closed_by VARCHAR(80),
    draft_missing_fields VARCHAR(1000),
    monitoring_priority INTEGER NOT NULL DEFAULT 0 CHECK (monitoring_priority >= 0 AND monitoring_priority <= 100),
    monitoring_context VARCHAR(1000),
    risk_review_required_at TIMESTAMP WITH TIME ZONE,
    risk_review_reason VARCHAR(1000),
    route_version INTEGER NOT NULL CHECK (route_version > 0),
    risk_snapshot_id BIGINT,
    report_id BIGINT,
    insurance_case_id BIGINT,
    created_by VARCHAR(80) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (planned_start IS NULL OR planned_end IS NULL OR planned_end > planned_start)
);

CREATE TABLE mission_route_point (
    id BIGSERIAL PRIMARY KEY,
    mission_id BIGINT NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    seq_no INTEGER NOT NULL CHECK (seq_no > 0),
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    UNIQUE (mission_id, seq_no)
);

CREATE TABLE mission_plan (
    id BIGINT PRIMARY KEY,
    mission_id BIGINT NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    route_version INTEGER NOT NULL,
    safety_contact VARCHAR(160) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by VARCHAR(80),
    UNIQUE (mission_id)
);

CREATE TABLE risk_snapshot (
    id BIGINT PRIMARY KEY,
    mission_id BIGINT NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    policy_version VARCHAR(40) NOT NULL REFERENCES risk_policy(version),
    p_attack DOUBLE PRECISION NOT NULL CHECK (p_attack >= 0 AND p_attack <= 1),
    risk_score INTEGER NOT NULL CHECK (risk_score >= 0 AND risk_score <= 100),
    launch_allowed BOOLEAN NOT NULL,
    decision_zone VARCHAR(40) NOT NULL,
    blocking_reason VARCHAR(1000),
    data_quality VARCHAR(40) NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_for_route_version INTEGER NOT NULL,
    stale BOOLEAN NOT NULL
);

CREATE TABLE risk_factor (
    id BIGSERIAL PRIMARY KEY,
    risk_snapshot_id BIGINT NOT NULL REFERENCES risk_snapshot(id) ON DELETE CASCADE,
    factor_name VARCHAR(80) NOT NULL,
    factor_value DOUBLE PRECISION NOT NULL,
    UNIQUE (risk_snapshot_id, factor_name)
);

CREATE TABLE telemetry_event (
    id BIGINT PRIMARY KEY,
    external_event_id VARCHAR(160) NOT NULL,
    mission_id BIGINT NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    crew_id BIGINT NOT NULL REFERENCES crew(id),
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    equipment_status VARCHAR(120) NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    freshness_status VARCHAR(40) NOT NULL,
    UNIQUE (mission_id, external_event_id)
);

CREATE TABLE incident (
    id BIGINT PRIMARY KEY,
    mission_id BIGINT NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    alarm_signal_id BIGINT NOT NULL,
    status VARCHAR(60) NOT NULL,
    severity VARCHAR(40) NOT NULL,
    classification_reason VARCHAR(1000),
    sla_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sla_deadline_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sla_breached BOOLEAN NOT NULL,
    sla_breached_notified_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    closed_by VARCHAR(80),
    operator_login VARCHAR(80),
    evacuation_command_id BIGINT
);

CREATE TABLE alarm_signal (
    id BIGINT PRIMARY KEY,
    external_event_id VARCHAR(160) NOT NULL,
    mission_id BIGINT NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    sender VARCHAR(80) NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    incident_id BIGINT NOT NULL REFERENCES incident(id),
    UNIQUE (mission_id, external_event_id)
);

CREATE TABLE evacuation_command (
    id BIGINT PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incident(id) ON DELETE CASCADE,
    mission_id BIGINT NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    status VARCHAR(60) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE,
    sent_by VARCHAR(80) NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by VARCHAR(80),
    delivery_error VARCHAR(1000),
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE mission_report (
    id BIGINT PRIMARY KEY,
    mission_id BIGINT NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    actual_start TIMESTAMP WITH TIME ZONE,
    actual_end TIMESTAMP WITH TIME ZONE NOT NULL,
    spice_amount NUMERIC(18, 2) NOT NULL CHECK (spice_amount >= 0),
    harvester_final_status VARCHAR(80) NOT NULL,
    abnormal_situations VARCHAR(2000) NOT NULL,
    submitted_by VARCHAR(80) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (mission_id)
);

CREATE TABLE insurance_case (
    id BIGINT PRIMARY KEY,
    mission_id BIGINT NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    incident_id BIGINT REFERENCES incident(id),
    status VARCHAR(60) NOT NULL,
    trigger_type VARCHAR(60) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    opened_by VARCHAR(80) NOT NULL,
    trigger_reason VARCHAR(1000),
    trigger_p_attack DOUBLE PRECISION CHECK (trigger_p_attack IS NULL OR (trigger_p_attack >= 0 AND trigger_p_attack <= 1)),
    trigger_risk_score INTEGER CHECK (trigger_risk_score IS NULL OR (trigger_risk_score >= 0 AND trigger_risk_score <= 100)),
    trigger_risk_snapshot_id BIGINT REFERENCES risk_snapshot(id),
    trigger_decision_at TIMESTAMP WITH TIME ZONE,
    trigger_decision_by VARCHAR(80),
    incident_severity VARCHAR(40),
    incident_registered_at TIMESTAMP WITH TIME ZONE,
    incident_closed_at TIMESTAMP WITH TIME ZONE,
    incident_sla_breached BOOLEAN,
    incident_operator VARCHAR(80),
    missing_data VARCHAR(1000),
    final_risk_score INTEGER CHECK (final_risk_score >= 0 AND final_risk_score <= 100),
    final_premium NUMERIC(18, 2),
    closed_at TIMESTAMP WITH TIME ZONE,
    closed_by VARCHAR(80)
);

CREATE TABLE insurance_recalculation (
    id BIGINT PRIMARY KEY,
    insurance_case_id BIGINT NOT NULL REFERENCES insurance_case(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL DEFAULT 'RECALCULATION',
    risk_snapshot_id BIGINT REFERENCES risk_snapshot(id),
    old_premium NUMERIC(18, 2),
    new_premium NUMERIC(18, 2) NOT NULL,
    old_risk_score INTEGER,
    new_risk_score INTEGER NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    calculated_by VARCHAR(80) NOT NULL,
    rejected_reason VARCHAR(1000)
);

CREATE TABLE audit_event (
    id BIGINT PRIMARY KEY,
    actor_login VARCHAR(80) NOT NULL,
    actor_role VARCHAR(80) NOT NULL,
    action VARCHAR(120) NOT NULL,
    object_type VARCHAR(80) NOT NULL,
    object_id BIGINT NOT NULL,
    mission_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE audit_detail (
    id BIGSERIAL PRIMARY KEY,
    audit_event_id BIGINT NOT NULL REFERENCES audit_event(id) ON DELETE CASCADE,
    detail_key VARCHAR(120) NOT NULL,
    detail_value VARCHAR(2000) NOT NULL
);

CREATE INDEX idx_mission_status ON mission(status);
CREATE INDEX idx_incident_queue ON incident(sla_breached, sla_deadline_at, severity, status);
CREATE INDEX idx_telemetry_mission_time ON telemetry_event(mission_id, event_time DESC);
CREATE INDEX idx_audit_mission_time ON audit_event(mission_id, created_at DESC);
CREATE INDEX idx_insurance_status ON insurance_case(status);

INSERT INTO mining_zone (id, name, risk_level, coordinates, active) VALUES
    (1, 'Северный пояс Арракиса', 0.25, '24.42, 54.20', TRUE),
    (2, 'Котловина Карфаг', 0.55, '22.11, 53.91', TRUE),
    (3, 'Глубокая пустыня', 0.92, '20.01, 51.74', TRUE);

INSERT INTO harvester (id, name, type, status, noise_level, capacity) VALUES
    (1, 'HV-17 Раббан', 'STANDARD', 'READY', 0.30, 120),
    (2, 'HV-22 Глоссу', 'HEAVY', 'READY', 0.72, 180),
    (3, 'HV-09 Резерв', 'LIGHT', 'MAINTENANCE', 0.20, 90);

INSERT INTO crew (id, name, status, contact_channel, member_count) VALUES
    (1, 'Экипаж Альфа', 'READY', 'arrakis-alpha', 6),
    (2, 'Экипаж Бета', 'READY', 'arrakis-beta', 5),
    (3, 'Экипаж Гамма', 'READY', 'arrakis-gamma', 7);

INSERT INTO risk_policy (id, version, warning_threshold, block_threshold, formula_description, active_from, active)
VALUES (1, '1.0', 50, 75, 'Детерминированная формула MVP: P(attack), risk-score, conservative penalties', CURRENT_TIMESTAMP, TRUE);
