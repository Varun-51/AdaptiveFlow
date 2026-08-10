# Changelog

All notable changes to AdaptiveFlow are documented here. This project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-08-10

### Added

- Fluent `WorkflowBuilder` API: `task`, `then`, `parallel(TaskRef...)`,
  `retry`, `execute`, `execute(Executor)`, `build`.
- Implicit dependency semantics: chained tasks form a chain; `parallel`
  groups fan out and fan in; always a valid DAG.
- DAG construction and validation: duplicate ids, unknown dependencies, and
  cycles (Kahn's algorithm); multi-root graphs supported.
- Virtual Thread execution by default (`newVirtualThreadPerTaskExecutor`)
  with pluggable `java.util.concurrent.Executor`.
- Type-safe `ExecutionContext`: `ctx.<T>result("taskId")`; thread-safe
  during the run, immutable snapshot afterwards.
- Immutable `WorkflowResult` and `TaskResult` (output, error, attempts,
  duration).
- Per-task retry: `none()`, `fixedDelay(attempts, delay)`,
  `exponentialBackoff(attempts, firstDelay, maxDelay)`; `maxAttempts` capped
  at 63 to keep exponential delay math in range.
- Full jitter on exponential backoff waits (`delayBeforeJittered`): retry
  storms spread instead of pounding the same deadline.
- Fail-fast failure semantics: once a task exhausts its attempts, downstream
  dispatch stops; never-run tasks are reported as `SKIPPED` and stay
  queryable through `taskResult(id)`.
- Task outcomes carry a terminal `TaskStatus` (`COMPLETED` / `FAILED` /
  `SKIPPED`).
- Unexpected engine-internal errors surface as `AdaptiveFlowException`
  instead of being silently masked.
- Zero runtime dependencies (test-only JUnit 5 + JMH).
- Quality gates: JaCoCo (90 % line / 80 % branch), Checkstyle, SpotBugs
  (max effort), Maven Wrapper.
- JMH benchmarks: linear chain and parallel fan-in (end-to-end throughput).
- Maven Central wiring: `distributionManagement`, GPG-signed `release`
  profile with sources + javadoc jars and Nexus staging.