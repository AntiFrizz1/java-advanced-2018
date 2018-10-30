package ru.ifmo.rain.glukhov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * This implementation of interface {@link ParallelMapper}
 * @author antifrizz
 * @version 1.0
 * @see ParallelMapper
 */
public class ParallelMapperImpl implements ParallelMapper{
    private final ArrayList<Thread> thread;
    private final Queue<Runnable> queue;

    /**
     * Main constructor for {@link ParallelMapperImpl}. Make <tt>threads</tt> threads
     * @param threads Amount of threads
     * @throws InterruptedException If <tt>threads</tt> <= 0
     */
    public ParallelMapperImpl(int threads) throws InterruptedException {
        if (threads <= 0) {
            throw new InterruptedException("Error: argument threads <= 0");
        }
        thread = new ArrayList<>();
        queue = new ArrayDeque<>();
        for (int i = 0; i < threads; i++) {
            Thread tmp = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Runnable function;
                        synchronized (queue) {
                            while (queue.isEmpty()) {
                                queue.wait();
                            }
                            function = queue.poll();
                        }
                        try {
                            function.run();
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }
                    }
                } catch (InterruptedException ignored) {
                }
            });
            tmp.start();
            thread.add(tmp);
        }
    }

    /**
     * Check arguments given to {@link #map(Function, List)}
     * @param f Function
     * @param args List arguments
     * @param <T> Type of list's arguments
     * @param <R> Function's return type
     * @throws InterruptedException Incorrect arguments
     */
    private <T, R> void check_args(Function<?super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        if (f == null) {
            throw new InterruptedException("Error: f is null");
        }
        if (args == null) {
            throw new InterruptedException("Error: args is null");
        }
    }

    /**
     * For all arguments in <tt>args</tt> execute function <tt>f</tt> and return answers list
     * @param f Function
     * @param args List arguments
     * @param <T> Type of list's arguments
     * @param <R> Function's return type
     * @return {@link java.util.List<R>} List answers
     * @throws InterruptedException Errors with threads na incorrect arguments
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        check_args(f, args);
        List<R> ans = new ArrayList<>(Collections.nCopies(args.size(), null));
        final int[] amount = {0};
        for (int i = 0; i < args.size(); i++) {
            final T arg = args.get(i);
            final int index = i;
            synchronized (queue) {
                queue.add(() -> {
                    R result = f.apply(arg);
                    synchronized (ans) {
                        ans.set(index, result);
                        synchronized (amount) {
                            amount[0]++;
                            if (amount[0] == ans.size()) {
                                ans.notify();
                            }
                        }
                    }
                });
                queue.notifyAll();
            }
        }
        while (amount[0] != ans.size()) {
            synchronized (ans) {
                ans.wait();
            }
        }
        return ans;
    }

    /**
     * Close all threads
     */
    @Override
    public void close() {
        thread.forEach(Thread::interrupt);
        for (Thread aThread : thread) {
            try {
                aThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
