# Source provenance

The application started from a Git archive of [NexoraPe's historical SafeWork backend](https://github.com/NexoraPe-1ASI0732/backend-safework/tree/778fe1ec8e999b27e8d0340eb26fef50d1a49683), snapshot `778fe1ec8e999b27e8d0340eb26fef50d1a49683`. Its Git history was not imported. The original checkout, database and historical runtime evidence remain outside this repository and were not modified.

This is the UPC 1ACC0238, period 202620, NRC 4945 backend. Historical success DTOs and routes are retained where suitable; historical security behavior is not a target requirement. The foundation changes registration, authorization, validation, transaction handling, notifications and secret configuration.

No repository-level LICENSE or NOTICE file was present in the pinned source. Its empty Maven license element and generic Apache 2.0 OpenAPI metadata do not establish a source-code license. We preserve existing file-level notices, including the Maven wrapper's Apache license headers, and do not invent a license or authorship. The repository owners must decide any project-wide licensing and confirm redistribution rights before external reuse.

Java 25, Spring Boot 4.0.5, MySQL 8.4 and the inherited library versions are retained. The HTTP mapper is Jackson 3 supplied by Spring Boot; date feature configuration follows the [Spring Boot 4 documentation](https://docs.spring.io/spring-boot/4.0/how-to/spring-mvc.html). No framework migration is part of this foundation.

No historical signing key, database credentials, runtime logs, local database, compiled output or fixture secrets belong in commits. The new main initialization is separate from the corrected feature branch.
