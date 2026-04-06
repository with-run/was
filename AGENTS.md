# WAS MODULE GUIDE

Scope: backend-specific rules only. Follow `../AGENTS.md` for shared repo workflow and cross-project boundaries.

## OVERVIEW
`was/` is a Spring Boot 3.5 service on Java 21. The codebase is package-by-domain under `kr.withrun.was`, with business modules in `domain/` and cross-cutting infrastructure in `global/`.

## STRUCTURE
```text
was/
├── src/main/java/kr/withrun/was/
│   ├── WasApplication.java        # runtime entry and component-scan root
│   ├── domain/                    # primary ownership seam; navigate by business domain first
│   └── global/                    # cross-domain infrastructure; edits here usually affect many domains
├── src/test/java/kr/withrun/was/  # mirrored behavior map; fastest way to see current expectations
├── src/main/resources/            # application config and environment wiring
└── build.gradle                   # dependency/toolchain truth: Spring Boot, QueryDSL, AWS, security
```

Inside `domain/`, the important deep seams are not just controllers and services. Repeated `repository/query/` packages, `course/service/recommendation/`, and `navigation/service/bundle/` are real submodules with their own behavior density.

## WHERE TO LOOK
- App boot and component-scan root: `src/main/java/kr/withrun/was/WasApplication.java`
- Auth by concern: `domain/auth/controller/` for entry channels, `domain/auth/security/` for auth context/filtering, `global/config/SecurityConfig.java` for cross-cutting security rules
- Course behavior and ranking logic: `domain/course/service/`, `domain/course/repository/query/`, and `domain/course/service/recommendation/`
- Running session and ghost flow: `domain/running/service/` and `domain/running/repository/query/`
- Navigation bundle internals: `domain/navigation/service/NavigationBundleService.java`, then `service/bundle/` and `service/bundle/polyline/`
- File delivery and signed URL concerns: `domain/file/service/`
- Fastest behavior map before editing: matching tests under `src/test/java/kr/withrun/was/...`

## CONVENTIONS
- The real architectural seam is package-by-domain, not layer-first at the repo root.
- Repeated `repository/query/` packages are intentional custom-query seams.
- `global/` is shared infrastructure; changes there are cross-domain by default.
- `DB/schema.sql` and `DB/SCHEMA_GUIDE.md` describe the schema contract, but application-level enforcement still lives here.

## TESTING PATTERNS
- Repository tests commonly use `@DataJpaTest` with Querydsl/JPA config.
- Service tests commonly use `@ExtendWith(MockitoExtension.class)` and constructor-level dependency wiring.
- Controller/security tests split between slice tests and full-context Spring Boot tests depending on auth/security depth.
- Large, high-risk domains today are `course/` and `running/`; read their existing tests before changing behavior.

## ANTI-PATTERNS
- Do not assume backend work is covered by the root file alone; this module has its own test taxonomy and domain seams.
- Do not navigate by technical layer first; start from the business domain, then go inward.
- Do not bypass service-layer validation by only changing schema/docs.
- Do not treat `PRIVATE` course visibility, ghost rules, or navigation bundle generation as incidental logic.
- Do not mistake `src/test/.../runner/test-support/` for a production runtime domain.
- Do not forget that local Valhalla bootstrap lives outside this module in `infra/local/valhalla/`.

## COMMANDS
```bash
cd was && ./gradlew build
cd was && ./gradlew test

# Windows shell alternative
cd was && .\gradlew.bat build
cd was && .\gradlew.bat test
```

## NOTES
- CI builds and tests this module in `.gitlab-ci.yml`.
- `was/src/test/` is substantial; use targeted tests when changing a single domain, then rerun broader verification if the seam is shared.
