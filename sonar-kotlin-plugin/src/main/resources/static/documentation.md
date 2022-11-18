---
title: Kotlin
key: kotlin
---

<!-- static -->
<!-- update_center:kotlin -->
<!-- /static -->


## Language-Specific Properties

You can discover and update Kotlin-specific [properties](/analysis/analysis-parameters/) in:  <!-- sonarcloud -->Project <!-- /sonarcloud -->**[Administration > General Settings > Kotlin](/#sonarqube-admin#/admin/settings?category=kotlin)**.

## Kotlin Analysis and Bytecode
If you are not using the SonarScanner for Gradle or SonarScanner for Maven, it is strongly recommended to provide the paths of all 
dependency binaries used by the project in order to improve the analysis accuracy. You can provide these using the `sonar.java.libraries` 
property (note that this property is shared with the Java analyzer and as such has `java` in its name). This is a list of comma-separated
paths to files with third-party libraries (JAR or Zip files) used by your project. Wildcards can be used: 
`sonar.java.libraries=path/to/Library.jar,directory/**/*.jar`

Note that if you use the SonarScanner for Gradle or SonarScanner for Maven to scan your code, these scanners will auto-detect the value for
this property. Thus, you don't need to provide it.

## Specifying the Kotlin Source Code Version
You can explicitly define which Kotlin version the analyzer should analyze your code based on. Provide the desired version in the format
`X.Y` as value to the `sonar.kotlin.source.version` property (e.g. `1.7`).

## Multi-Threaded Parsing
As of August 2022, the Kotlin analyzer supports multi-threaded parsing. This is an experimental feature and is disabled by default.
You can enable it by providing an appropriate number of threads with the property key `sonar.kotlin.threads`.

## Skipping unchanged files
Starting from November 2022, and by default, the Kotlin analyzer optimizes the analysis of unchanged files in pull requests.
In practice, this means that the analyzer does not perform an analysis on any file that is the same as on the PR's target branch.
As long as the project is configured in such a way that the analyzer is able to find the project's binaries, this should not impact the
analysis results.

If you wish to disable this optimization, you can set the value of the analysis parameter `sonar.kotlin.skipUnchanged` to `false`.
Leaving the parameter unset lets the server decide whether the optimization should be enabled.

## Related Pages
* [Importing External Issues](/analysis/external-issues/) (AndroidLint, Detekt and Ktlint)
* [Test Coverage & Execution](/analysis/coverage/) (JaCoCo)
