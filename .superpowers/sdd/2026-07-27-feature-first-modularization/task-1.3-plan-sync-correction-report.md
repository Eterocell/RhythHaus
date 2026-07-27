# Task 1.3 Plan-Sync Correction Report

This documentation-only correction synchronizes the executable Task 1.3 plan, the
Superpowers feature spec, and the OpenSpec design/task references with canonical design
amendment `4732db9` (`2026-07-28-stable-architecture-checker-design.md:74-208`). The
canonical design and all historical task reports are unchanged.

- The processor dependency exemption is tooling-only and applies only to direct documented
  production `ksp`/`ksp<Target>` configurations classified from public Gradle/KMP target
  data. Every other placement remains `ARCH-EDGE`.
- `:core:model` and `:core:database` remain mutually forbidden; applicable fixtures require
  both `ARCH-EDGE` and `ARCH-CYCLE` diagnostics.
- Fixtures now explicitly cover public KMP/JVM/AGP resource discovery, documented Compose
  defaults plus the owned custom-directory registry, and public SQLDelight
  configured/default/explicit roots with deterministic unsupported/inconsistent failures.
- KSP wiring derives from the public target model, is production-only for core/API modules,
  processes initial input only, and has exact deduplicated diagnostics using the real
  processor artifact/version fixture across KMP JVM and Android/native registration.
- The required matrix includes standalone graph/model fixtures, KSP semantic fixtures, and
  a combined graph/KSP fixture. Root quality/CI wiring, shared KSP wiring, internals, and
  Task 1.4 remain excluded.

Task 1.3 remains rejected/in progress. OpenSpec 2.1, 2.3, and 2.4 remain unchecked. The
required explicit user review gate remains in force before any implementation.
