# Modules

## dependency tree

- sonar-kotlin-plugin
  - sonar-kotlin-checks
    - sonar-kotlin-api + sonar-kotlin-frontend
      - sonar-kotlin-test-api
  - sonar-kotlin-external-linters
  - sonar-kotlin-surefire
  - sonar-kotlin-metrics
    - sonar-kotlin-api

## sonar-kotlin-frontend

**Contents:**

- grammar, parser, AST model et c. 
- so far we have only AST the model

**Packages:** 

- `com.sonarsource.kotlin.ast`

## sonar-kotlin-checks

**Contents:**

- the checks
- maybe split into submodules:
  - `sonar-kotlin-checks-regex`
  - `sonar-kotlin-checks-typing`
  - `sonar-kotlin-checks-conventions`
  - `sonar-kotlin-checks-spring`
  - ...

**Packages:**

- `org.sonarsource.kotlin.checks`
  - `org.sonarsource.kotlin.checks.regex`
  - `org.sonarsource.kotlin.checks.typing`
  - `org.sonarsource.kotlin.checks.conventions`
  - ...

## sonar-kotlin-api

**Contents:**

- internal API required to implement the checks
- Not limited to checks though! This might serve as a crosscutting module
  also for other layers, such as `external-linters` et c.

## sonar-kotlin-plugin
## sonar-kotlin-external-linters
## sonar-kotlin-surefire



TODO:
- remove ./src
- why "kotlin" in "java" subfolder?
- https://sonarsource.atlassian.net/browse/SONARKT-336 -> text from yesterday was not saved
- what about "utils-kotlin" -> "sonar-kotlin-utils"?
- Impact of renaming packages to community?