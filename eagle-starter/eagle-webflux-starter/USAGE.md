# Eagle WebFlux Starter

Add the starter to a reactive service:

```gradle
implementation project(':eagle-starter:eagle-webflux-starter')
```

It provides:

- `spring-boot-starter-webflux`
- validation and actuator support
- `X-Request-Id` propagation
- unified JSON error responses using `ErrorResult`
