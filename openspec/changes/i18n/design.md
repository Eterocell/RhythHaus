# Design: i18n

## Summary

Use Compose Multiplatform resources in the `shared` module to provide localized UI strings. English remains the default resource set and Simplified Chinese is added as the first translated locale.

## Scope

Shared Compose UI copy and accessibility descriptions move to resources. Runtime user data such as track titles, artist names, album names, file/folder names, and query text is not translated.

## Verification

Generated resources must compile for shared JVM and existing JVM tests must pass.
