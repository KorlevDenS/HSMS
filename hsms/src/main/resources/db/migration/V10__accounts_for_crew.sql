INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT 6, 'Экипаж №1 Альфа', '+7-900-002', 'crew1@hsms.local', 'crew1', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'crew1');

INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT 7, 'Экипаж №2 Бетта2', '+7-900-002', 'crew2@hsms.local', 'crew2', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'crew2');

INSERT INTO hsms_user (id, display_name, phone_number, email, login, password)
SELECT 8, 'Экипаж №3 Гамма', '+7-900-002', 'crew3@hsms.local', 'crew3', '{bcrypt}$2b$10$ZzKPX8aJH8b8VAqlmdioTunUhCSozq2qyBboHqlmVPMgWj9W6Goq6'
WHERE NOT EXISTS (SELECT 1 FROM hsms_user WHERE login = 'crew3');

SELECT setval('hsms_user_id_seq', (SELECT COALESCE(MAX(id), 1) FROM hsms_user));

INSERT INTO permission (client, role)
SELECT u.id, r.id
FROM hsms_user u, role r
WHERE r.name = 'ROLE_HARVESTER_CREW'
ON CONFLICT DO NOTHING;

CREATE TABLE crew_member (
    client BIGINT REFERENCES hsms_user ON DELETE CASCADE,
    crew BIGINT REFERENCES crew ON DELETE CASCADE,
    PRIMARY KEY (client, crew)
);

INSERT INTO crew_member (client, crew)
SELECT 6, 1
WHERE NOT EXISTS (SELECT 1 FROM crew_member WHERE client = 6 AND crew = 1);

INSERT INTO crew_member (client, crew)
SELECT 7, 2
WHERE NOT EXISTS (SELECT 1 FROM crew_member WHERE client = 7 AND crew = 2);

INSERT INTO crew_member (client, crew)
SELECT 8, 3
WHERE NOT EXISTS (SELECT 1 FROM crew_member WHERE client = 8 AND crew = 3);

