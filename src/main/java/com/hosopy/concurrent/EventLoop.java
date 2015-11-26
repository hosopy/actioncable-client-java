package com.hosopy.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class EventLoop extends Thread {

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        @Override
        public Thread newThread(@SuppressWarnings("NullableProblems") Runnable runnable) {
            eventLoop = new EventLoop(runnable);
            eventLoop.setName("EventLoop");
            return eventLoop;
        }
    };

    private static EventLoop eventLoop;

    private static ExecutorService executorService;

    private static int counter = 0;

    private EventLoop(Runnable runnable) {
        super(runnable);
    }

    /**
     * Check if the current thread is EventLoop.
     *
     * @return true if the current thread is EventLoop.
     */
    public static boolean isCurrentThread() {
        return currentThread() == eventLoop;
    }

    /**
     * Execute a task in EventLoop thread.
     *
     * @param task A task to be executed.
     */
    public static void execute(Runnable task) {
        if (isCurrentThread()) {
            task.run();
        } else {
            post(task);
        }
    }

    /**
     * Post a next task in EventLoop thread.
     *
     * @param task A task to be post.
     */
    public static void post(final Runnable task) {
        ExecutorService executor;
        synchronized (EventLoop.class) {
            counter++;
            if (executorService == null) {
                executorService = Executors.newSingleThreadExecutor(THREAD_FACTORY);
            }
            executor = executorService;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } finally {
                    synchronized (EventLoop.class) {
                        counter--;
                        if (counter == 0) {
                            executorService.shutdown();
                            executorService = null;
                            eventLoop = null;
                        }
                    }
                }
            }
        });
    }
}