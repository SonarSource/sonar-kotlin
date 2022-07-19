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
If you are not using the Maven or Gradle Sonar plugins, it is strongly recommended to provide the paths of all dependency binaries used by
the project in order to improve the analysis accuracy. You can provide these using the `sonar.java.libraries` property. This is a list of 
comma-separated paths to files with third-party libraries (JAR or Zip files) used by your project. Wildcards can be used: 
`sonar.java.libraries=path/to/Library.jar,directory/**/*.jar`

Note that if you use the Gradle or Maven Sonar plugins to scan your code, these plugins will usually auto-detect the value for the property.

## Specifying the Kotlin Source Code Version
You can explicitly define which Kotlin version the analyzer should analyze your code based on. Provide the desired version in the format
`X.Y` as value to the `sonar.kotlin.source.version` property (e.g. `1.7`).

## Multi-Threaded Parsing
As of August 2022, the SonarKotlin analyzer supports multi-threaded parsing. This is an experimental feature and is disabled by default.
You can enable it by providing an appropriate number of threads with the property key `sonar.kotlin.threads`.

## Related Pages
* [Importing External Issues](/analysis/external-issues/) (AndroidLint, Detekt and Ktlint)
* [Test Coverage & Execution](/analysis/coverage/) (JaCoCo)
