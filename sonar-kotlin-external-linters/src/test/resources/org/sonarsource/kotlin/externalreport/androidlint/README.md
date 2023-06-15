`lint-results.xml` file can be generated using:
```
android-sdk/tools/bin/lint --xml lint-results.xml path/to/project
```

or using gradle (the report is generated in `build/reports/lint-results.xml`):
```
gradle lint
```
