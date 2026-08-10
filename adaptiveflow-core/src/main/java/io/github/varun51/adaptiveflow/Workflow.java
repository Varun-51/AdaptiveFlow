package io.github.varun51.adaptiveflow;

/**
 * Immutable workflow definition, produced by {@link WorkflowBuilder#build()}.
 */
public record Workflow(String name, ExecutionPlan plan) {
}
