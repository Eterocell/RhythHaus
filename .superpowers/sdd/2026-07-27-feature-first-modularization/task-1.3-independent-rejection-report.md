# Task 1.3 Independent Rejection Report

Status: rejected/in progress. This report records the independent rejection of
`b2e2700` (`fix: complete architecture checker ownership policy`). It does not alter
the historical Task 1.3 reports or claim architecture-gate acceptance.

## Independent Findings

1. The allow-list permits both `:core:model -> :core:database` and
   `:core:database -> :core:model`, weakening the real dependency DAG so that an
   otherwise forbidden core cycle can produce only a cycle diagnostic.
2. Resource validation accepts any file in a recognized source-set location and does
   not validate owning-module resource policy; it therefore does not enforce resource
   ownership.
3. The Kotlin declaration index is restricted to top-level declarations, so declared
   nested/member providers are absent from import ownership resolution.
4. Declaration providers are reduced to a set of module paths. Multiple declarations
   in one module collapse to one provider, so ambiguous imports are not rejected
   conservatively as the policy requires.
5. SQLDelight ownership accepts plugin application or a driver dependency without
   proving a configured database. It does not distinguish a physical configured owner
   from an unconfigured plugin/driver consumer strongly enough for the sole-owner rule.
6. Explicit API is accepted from the module-level `KotlinBaseExtension` setting rather
   than validated for every applicable Android/JVM/Native production compilation; it
   cannot prove the required compilation-level coverage.

## Consequence And Next Action

OpenSpec 2.1 and 2.3 are unchecked and rejected/in progress. OpenSpec 2.4 remains
unchecked. A separately approved repair must add regressions for all six findings and
establish new RED/GREEN evidence before any architecture-gate completion claim.
