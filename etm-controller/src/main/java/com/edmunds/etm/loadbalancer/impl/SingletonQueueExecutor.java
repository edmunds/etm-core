package com.edmunds.etm.loadbalancer.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An executor that maintains a queue with one or zero pending tasks.
 * <p/>
 * When a task is passed to the {@link #execute(Runnable)} method it either replaces the currently
 * queued task or, if the queue is empty, it is executed immediately.
 * <p/>
 * All tasks are executed sequentially within a single thread.
 *
 * @author David Trott
 */
public class SingletonQueueExecutor extends ThreadPoolExecutor {
    public SingletonQueueExecutor() {
        super(1, 1, 1, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<Runnable>(1));
    }

    @Override
    public synchronized void execute(Runnable runnable) {
        getQueue().clear();
        super.execute(runnable);
    }

    public static void main(String[] args) {
        final SingletonQueueExecutor executor = new SingletonQueueExecutor();

        for (int i = 0; i < 10; i++) {
            try {
                executor.execute(new TestTask(i));
                Thread.sleep(99);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
    }

    private static class TestTask implements Runnable {
        private final int value;

        TestTask(int value) {
            this.value = value;
        }

        @Override
        public void run() {
            System.out.println(value);
            System.out.println(Thread.currentThread());
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
