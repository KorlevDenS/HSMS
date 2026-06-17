INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT next_user_id.id, 'Экипаж №1 Альфа', '+7-900-002', 'crew1@hsms.local', 'crew1', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
FROM (SELECT COALESCE(MAX(id), 0) + 1 AS id FROM hsms_user) next_user_id
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'crew1');

INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT next_user_id.id, 'Экипаж №2 Бетта', '+7-900-007', 'crew2@hsms.local', 'crew2', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
FROM (SELECT COALESCE(MAX(id), 0) + 1 AS id FROM hsms_user) next_user_id
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'crew2');

INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT next_user_id.id, 'Экипаж №3 Гамма', '+7-900-008', 'crew3@hsms.local', 'crew3', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
FROM (SELECT COALESCE(MAX(id), 0) + 1 AS id FROM hsms_user) next_user_id
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'crew3');

INSERT INTO permission (client, role)
SELECT u.id, r.id
FROM hsms_user u, role r
WHERE u.login IN ('crew1', 'crew2', 'crew3')
  AND r.name = 'ROLE_HARVESTER_CREW'
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS crew_member (
    client BIGINT REFERENCES hsms_user ON DELETE CASCADE,
    crew BIGINT REFERENCES crew ON DELETE CASCADE,
    PRIMARY KEY (client, crew)
);

INSERT INTO crew_member (client, crew)
SELECT u.id, 1
FROM hsms_user u
WHERE u.login = 'crew1'
ON CONFLICT DO NOTHING;

INSERT INTO crew_member (client, crew)
SELECT u.id, 2
FROM hsms_user u
WHERE u.login = 'crew2'
ON CONFLICT DO NOTHING;

INSERT INTO crew_member (client, crew)
SELECT u.id, 3
FROM hsms_user u
WHERE u.login = 'crew3'
ON CONFLICT DO NOTHING;

UPDATE crew SET assigned_login = 'crew1' WHERE id = 1;
UPDATE crew SET assigned_login = 'crew2' WHERE id = 2;
UPDATE crew SET assigned_login = 'crew3' WHERE id = 3;
