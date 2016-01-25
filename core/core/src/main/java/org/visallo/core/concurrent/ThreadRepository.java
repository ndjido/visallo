package org.visallo.core.concurrent;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Singleton
public class ThreadRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ThreadRepository.class);
    private static final int DEFAULT_TERMINATION_TIMEOUT_MILLIS = 5000;
    private static final int SHORT_TERMINATION_TIMEOUT_MILLIS = 250;

    private static final ThreadLocal<ThreadProperties> threadLocalProperties = new ThreadLocal<>();
    private final ExecutorService executorService;
    private final long terminationTimeoutMillis;
    private volatile boolean shutdown = false;

    public ThreadRepository() {
        this(DEFAULT_TERMINATION_TIMEOUT_MILLIS);
    }

    private ThreadRepository(int terminationTimeoutMillis) {
        this.terminationTimeoutMillis = terminationTimeoutMillis;

        executorService = Executors.newCachedThreadPool(new LocalThreadFactory());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    @VisibleForTesting
    public static ThreadRepository withShortTimeout() {
        return new ThreadRepository(SHORT_TERMINATION_TIMEOUT_MILLIS);
    }

    public Thread startDaemon(Runnable runnable, String name) {
        return start(runnable, true, name);
    }

    public Thread startNonDaemon(Runnable runnable,  String name) {
        return start(runnable, false, name);
    }

    private Thread start(Runnable runnable, boolean isDaemon, String name) {
        if (shutdown) {
            throw new VisalloException("already shut down");
        }
        Thread thread;
        ThreadProperties threadProperties = new ThreadProperties();
        threadProperties.isDaemon = isDaemon;
        threadProperties.name = name;
        threadLocalProperties.set(threadProperties);
        try {
            executorService.execute(runnable);
        } finally {
            thread = threadLocalProperties.get().thread;
            threadLocalProperties.remove();
        }
        return thread;
    }

    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        LOGGER.debug("shutting down and waiting %d milliseconds for threads to exit cleanly", terminationTimeoutMillis);
        try {
            executorService.shutdown();
            executorService.awaitTermination(terminationTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // doesn't matter
        } finally {
            if (executorService.isTerminated()) {
                LOGGER.debug("all threads exited cleanly");
            } else {
                LOGGER.debug("one or more threads did not exit cleanly");
            }
            executorService.shutdownNow();
        }
    }

    private static class ThreadProperties {
        boolean isDaemon;
        String name;
        Thread thread;
    }

    private static class LocalThreadFactory implements ThreadFactory {
        LocalThreadFactory() {
        }

        @Override
        public Thread newThread(Runnable runnable) {
            ThreadProperties threadProperties = threadLocalProperties.get();
            boolean isDaemon = threadProperties.isDaemon;
            String name = threadProperties.name;
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(isDaemon);
            if (name != null) {
                thread.setName(name + "-" + thread.getName());
            }
            threadProperties.thread = thread;
            LOGGER.debug("created new %s thread %s", isDaemon ? "daemon" : "non-daemon", thread);
            return thread;
        }
    }
}
