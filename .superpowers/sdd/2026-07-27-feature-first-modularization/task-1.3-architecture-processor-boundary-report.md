# Task 1.3 Architecture Processor Boundary Report

Status: documentation-only. Historical reports remain unchanged; this does not claim
architecture-gate acceptance.

## Approved Boundary

- The existing `build-logic:convention` JAR remains Gradle plugin classpath and is not a
  KSP processor artifact.
- A future Task 1.3 implementation may add root JVM `:architecture-processor`, register
  it in root `settings.gradle.kts`, and use the existing Kotlin 2.4.10/KSP 2.3.10 lines
  without a toolchain change or Maven publication.
- That module owns `symbol-processing-api`, provider implementation, and the required
  Java SPI descriptor; it does not apply the KSP Gradle plugin.
- Only core/API production consumers receive public KSP wiring. `:shared`, tests, root
  architectureCheck/KSP output integration, and Task 1.4 root/quality/CI wiring remain
  excluded.

## Next Gate

Task 1.3 remains rejected/in progress. OpenSpec 2.1, 2.3, and 2.4 remain unchecked. The
user must review this corrected written specification before implementation begins.
