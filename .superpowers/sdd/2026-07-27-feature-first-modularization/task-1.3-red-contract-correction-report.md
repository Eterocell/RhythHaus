# Task 1.3 RED Contract Correction Report

Status: documentation-only user-approved correction. Historical reports and rejected
checker commits remain intact; this does not claim architecture-gate acceptance.

## Corrected RED Contract

- Retain the rejected checker as the baseline; do not delete or revert it.
- Add focused TestKit/KSP integration regressions first.
- RED proves missing `:architecture-processor` and supported core/API production KSP wiring
  leave package-root and public-KDoc compilation checks absent or failing as specified.
- Gradle-model regressions may fail against the legacy checker only where it violates the
  approved stable public-model contract.
- No fixture may assume a nonexistent plugin or allow-list, and no broad rewrite is needed
  solely to manufacture RED.

## Next Gate

Task 1.3 remains rejected/in progress. OpenSpec 2.1, 2.3, and 2.4 remain unchecked. The
corrected written specification still requires user review before implementation.
