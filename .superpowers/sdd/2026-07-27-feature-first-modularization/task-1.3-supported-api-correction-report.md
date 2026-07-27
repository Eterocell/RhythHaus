# Task 1.3 Supported API Correction Report

Status: documentation-only. This correction preserves historical reports and does not
claim architecture-gate acceptance.

## Corrected Lifecycle

- Standalone `architectureCheck` validates only public Gradle model facts and never reads
  KSP outputs, task providers, generated directories, or internals.
- KSP is configured through supported `ksp { arg(...) }` and target-specific dependency
  configurations. It reports package-root and KDoc violations as production compilation
  errors, not architectureCheck diagnostics.
- Strict explicit API is the module-level public
  `KotlinBaseExtension.explicitApi == ExplicitApiMode.Strict` setting. Effective
  per-compilation/task introspection is unsupported and out of scope.
- Verification includes both standalone architectureCheck/cache invocations and relevant
  production compilation/KSP integration commands. Root check/quality/CI wiring remains
  Task 1.4.

## Next Gate

Task 1.3 remains rejected/in progress. OpenSpec 2.1/2.3 and 2.4 remain unchecked. The
user must review the corrected written specification before any implementation begins.
