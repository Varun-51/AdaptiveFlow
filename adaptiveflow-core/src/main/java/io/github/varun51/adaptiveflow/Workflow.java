package io.github.varun51.adaptiveflow;

/**
 * Immutable workflow definition, produced by {@link WorkflowBuilder#build()}.
 *
 * @param name name given to the workflow
 * @param plan validated, ordered execution plan
 */
public record Workflow(String name, ExecutionPlan plan) {
}
