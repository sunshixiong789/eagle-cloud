# Eagle Data R2DBC Starter

Add the starter to a reactive PostgreSQL service:

```gradle
implementation project(':eagle-starter:eagle-data-r2dbc-starter')
```

Configure PostgreSQL through Spring Boot R2DBC properties:

```yaml
spring:
  r2dbc:
    url: r2dbc:pool:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:eagle}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:}
```

Disable the Eagle marker auto-configuration if needed:

```yaml
eagle:
  r2dbc:
    enabled: false
```
