# Slang Checks

SLang Checks contains checks that are common for all languages.

## Partially mapped language

The different checks for a language don't always require all Slang nodes.
The following array is here to track which trees are still mapped to native node, despite the fact that
an equivalent Slang node exists.


| Node | Language(s) |
| ------ | ----------- |
| MemberSelect   | Kotlin, Scala, Ruby, Go |
| FunctionInvocation   | Kotlin, Scala, Ruby, Go |
