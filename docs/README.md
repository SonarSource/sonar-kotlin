# Kotlin development reference

These pages expand the [agent guide](../AGENTS.md); the [README](../README.md) covers build and metadata setup.
Covers: `AGENTS.md`, `sonar-kotlin-api/`, `sonar-kotlin-checks/`, `sonar-kotlin-plugin/`, `its/`.

- [Architecture](architecture.md): K2 session and plugin boundaries.
- [Writing checks](writing-checks.md): visitors, matchers, registration and samples.
- [Testing](testing.md): targeted checks and test conventions.
- [Integration tests](integration-tests.md): ruling corpora and server-backed tests.
- [Agent analysis](agent-analysis.md): end-of-turn analyzer check.

## Writing and maintaining these docs
- Write for a new teammate: lead with the point, then short sections, bullets and tables.
- Record only what the code does not make obvious: invariants, ordering constraints, the reason for a design.
- Link to README, CONTRIBUTING and module READMEs instead of copying them. Each fact lives in one place.
- Describe only the current state. No repository history, migration notes, "previously…" or "no longer…". When behavior changes, rewrite the statement.
- Keep each page's "Covers:" line accurate and check facts against source and tests.
- Use invented, nonfunctional examples only. Never real secrets.
