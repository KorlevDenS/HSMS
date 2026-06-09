ALTER TABLE crew ADD COLUMN IF NOT EXISTS assigned_login VARCHAR(80);

UPDATE crew
SET assigned_login = 'crew'
WHERE assigned_login IS NULL OR assigned_login = '';

ALTER TABLE crew ALTER COLUMN assigned_login SET NOT NULL;

CREATE OR REPLACE VIEW route_point AS
SELECT
    id,
    lat,
    lon
FROM mission_route_point;

CREATE OR REPLACE VIEW mission_route AS
SELECT
    mission_id,
    id AS route_point_id,
    seq_no
FROM mission_route_point;
