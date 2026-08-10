package io.github.varun51.adaptiveflow.bench;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import io.github.varun51.adaptiveflow.WorkflowBuilder;
import io.github.varun51.adaptiveflow.WorkflowResult;

@Fork(value = 2)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class DagEngineBenchmark {

    @Benchmark
    public WorkflowResult linearChain() {
        return WorkflowBuilder.builder("chain")
                .task("a", ctx -> 1)
                .task("b", ctx -> ctx.<Integer>result("a") + 1)
                .task("c", ctx -> ctx.<Integer>result("b") * 2)
                .task("d", ctx -> ctx.<Integer>result("c") + 3)
                .task("e", ctx -> ctx.<Integer>result("d") * 4)
                .execute();
    }

    @Benchmark
    public WorkflowResult parallelFanIn() {
        return WorkflowBuilder.builder("fan")
                .task("source", ctx -> 1)
                .parallel(
                        WorkflowBuilder.TaskRef.of("p1", ctx -> ctx.<Integer>result("source") + 1),
                        WorkflowBuilder.TaskRef.of("p2", ctx -> ctx.<Integer>result("source") + 2),
                        WorkflowBuilder.TaskRef.of("p3", ctx -> ctx.<Integer>result("source") + 3),
                        WorkflowBuilder.TaskRef.of("p4", ctx -> ctx.<Integer>result("source") + 4),
                        WorkflowBuilder.TaskRef.of("p5", ctx -> ctx.<Integer>result("source") + 5),
                        WorkflowBuilder.TaskRef.of("p6", ctx -> ctx.<Integer>result("source") + 6),
                        WorkflowBuilder.TaskRef.of("p7", ctx -> ctx.<Integer>result("source") + 7),
                        WorkflowBuilder.TaskRef.of("p8", ctx -> ctx.<Integer>result("source") + 8))
                .task("sink", ctx -> ctx.<Integer>result("p1") + ctx.<Integer>result("p8"))
                .execute();
    }
}