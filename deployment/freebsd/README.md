# HSMS FreeBSD deployment

This deployment path runs the Spring Boot standalone JAR on FreeBSD and terminates TLS at a local reverse proxy.

## Runtime prerequisites

- FreeBSD 14.x or newer.
- OpenJDK 17.
- PostgreSQL 16.4.
- Caddy or nginx with TLS 1.2+.
- `HSMS_TOKEN_SECRET` with at least 32 characters.

## Build

```sh
cd hsms
env -u DEBUG ./mvnw -q -DskipTests package
```

Copy `target/backend-*.jar` to `/usr/local/hsms/app.jar`.

## `/etc/rc.conf`

```sh
hsms_enable="YES"
hsms_java_home="/usr/local/openjdk17"
hsms_env="POSTGRES_USER=hsms POSTGRES_PASSWORD=change-me JDBC_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/hsms HSMS_TOKEN_SECRET=change-me-at-least-32-characters HSMS_REQUIRE_HTTPS=true HSMS_SSL_ENABLED=false"
```

## `/usr/local/etc/rc.d/hsms`

```sh
#!/bin/sh

# PROVIDE: hsms
# REQUIRE: LOGIN postgresql
# KEYWORD: shutdown

. /etc/rc.subr

name="hsms"
rcvar="hsms_enable"
load_rc_config "$name"

: ${hsms_enable:="NO"}
: ${hsms_java_home:="/usr/local/openjdk17"}

pidfile="/var/run/${name}.pid"
command="/usr/sbin/daemon"
command_args="-f -p ${pidfile} /usr/bin/env ${hsms_env} ${hsms_java_home}/bin/java -jar /usr/local/hsms/app.jar"

run_rc_command "$1"
```

Use a reverse proxy in front of port `8177` and pass `X-Forwarded-Proto: https`; HSMS rejects non-HTTPS forwarded requests when `HSMS_REQUIRE_HTTPS=true`.
