create table hsms_user (
                           id bigserial primary key,
                           display_name varchar not null default '',
                           phone_number varchar not null,
                           email varchar not null,
                           login varchar unique not null,
                           password varchar not null
);

create table role (
    id bigserial primary key,
    name varchar unique not null
);

create table permission (
    client bigint references hsms_user on delete cascade,
    role bigint references role,
    primary key(client, role)
);
