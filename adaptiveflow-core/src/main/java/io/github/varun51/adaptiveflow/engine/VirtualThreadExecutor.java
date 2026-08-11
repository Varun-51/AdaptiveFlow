package io.github.varun51.adaptiveflow.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default execution backend: one virtual thread per task, carrier threads
 * owned by the JVM.
 */
public final class VirtualThreadExecutor {

    private VirtualThreadExecutor() {
    }

    /** Creates a fresh executor that spawns one virtual thread per submitted task.
     *
     * @return an executor backed by virtual threads
     */
    public static ExecutorService newVirtualThreadPerTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
