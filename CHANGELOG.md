# Changelog

All notable changes to AdaptiveFlow are documented here. This project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.2] - 2026-08-11

### Fixed

- Retry backoff was one doubling too long: the engine passed the 1-based
  attempt counter into the 0-based delay API, so the first retry waited
  twice the configured initial delay and every step was shifted. Retry
  waits now match the documented schedule (initial, 2x, 4x, ...).
- Cycle errors now name only the tasks actually stuck in the cycle instead
  of dumping the whole remaining-dependency map.
- Group-level `retry(...)` no longer clobbers a parallel member that
  declared its own retry policy; the per-member policy wins.
- `WorkflowResult` and `ExecutionPlan` are `final`.

## [1.0.1] - 2026-08-11

### Fixed

- Exponential backoff no longer overflows: delays saturate at the cap (or a
  bounded maximum when uncapped) instead of wrapping negative.
- A rejecting executor now fails the run fast with `AdaptiveFlowException`
  instead of hanging on a never-completing gate.
- Interrupts during backoff abort the task (recorded `FAILED`) instead of
  spinning the remaining attempts through an immediate-wakeup loop.
- `Error`-family failures are no longer retried; they fail the task and stop
  the run.
- `ExecutionPlan.ids()` now returns the true dependency order computed by
  Kahn's algorithm instead of hash order.
- `TaskSpec` moved out of the `internal` package so the public plan API no
  longer leaks an "internal" type.
- Empty `parallel()` and blank task ids are rejected at build time.
- `exponentialBackoff` rejects `maxDelay < initialDelay`.

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
- Maven Central wiring: Central Portal publishing
  (`central-publishing-maven-plugin`), GPG-signed `release` profile with
  sources + javadoc jars.