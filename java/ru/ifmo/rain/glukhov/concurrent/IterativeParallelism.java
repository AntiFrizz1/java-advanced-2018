package ru.ifmo.rain.glukhov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.min;

/**
 * This class implements {@link info.kgeorgiy.java.advanced.concurrent.ScalarIP}
 * @author antifrizz
 * @version 1.0
 * @see info.kgeorgiy.java.advanced.concurrent.ScalarIP
 */
public class IterativeParallelism implements ListIP {
    private final ParallelMapper mapper;

    /**
     * Constructor with some {@link ParallelMapper}
     * @param mapper Example of {@link ParallelMapper}
     * @throws InterruptedException If <tt>mapper</tt> is null
     */
    public IterativeParallelism(ParallelMapper mapper) throws InterruptedException {
        if (mapper == null) {
            throw new InterruptedException("Error: argument mapper is null");
        }
        this.mapper = mapper;
    }

    /**
     * Standard constructor
     */
    public IterativeParallelism() {
        mapper = null;
    }
    /**
     * Abstract class for execution main methods from {@link info.kgeorgiy.java.advanced.concurrent.ScalarIP}
     * @param threads Count of threads which method must use
     * @param values List of arguments for which method must work
     * @param func Function which work with <tt>values</tt> for get result
     * @param selector Function which selected right answer from threads
     * @param <T> Type of data with which method must work
     * @param <R> Return type for functions which use this method({@link #maximum(int, List, Comparator)}
     *           , {@link #minimum(int, List, Comparator)}, {@link #all(int, List, Predicate)}, {@link #any(int, List, Predicate)})
     * @return R Information which we want to get
     * @throws java.lang.InterruptedException Errors with threads
     */
    private <T, R, F> F impl(int threads, List<? extends T> values, Function<Stream<? extends T>, R> func, Function<Stream<R>, F> selector) throws InterruptedException {
        final int countThreads = min(values.size(), threads);
        int n = values.size() / countThreads;
        if (values.size() % countThreads != 0) {
            n++;
        }

        List<Stream<? extends T>> streams = new ArrayList<>();
        for (int i = 0; i < countThreads; i++) {
            int l = min(i * n, values.size());
            int r = min((i + 1) * n, values.size());
            streams.add(values.subList(l, r).stream());
        }
        List<R> q;
        if (mapper != null) {
            q = mapper.map(func, streams);
        } else {
            q = new ArrayList<>(Collections.nCopies(countThreads, null));
            List<Thread> threadArrayList = new ArrayList<>();
            for (int i = 0; i < countThreads; i++) {
                final int index = i;
                final Stream<? extends T> stream = streams.get(i);
                Thread thread = new Thread(() -> {
                    R result = func.apply(stream);
                    synchronized (q) {
                        q.set(index, result);
                    }
                });
                thread.start();
                threadArrayList.add(thread);
            }
            InterruptedException error = null;
            for (Thread aThreadArrayList : threadArrayList) {
                try {
                    aThreadArrayList.join();
                } catch (InterruptedException e) {
                    if (Objects.isNull(error)) {
                        error = new InterruptedException();
                    }
                    error.addSuppressed(e);
                }
            }
            if (Objects.nonNull(error)) {
                throw error;
            }
        }
        ArrayList<R> ans = new ArrayList<>();
        for (R aQ : q) {
            if (aQ != null) {
                ans.add(aQ);
            }
        }
        return selector.apply(ans.stream());

    }

    /**
     * This method general arguments from methods {@link #maximum(int, List, Comparator)}, {@link #minimum(int, List, Comparator)}, {@link #all(int, List, Predicate)}, {@link #any(int, List, Predicate)}
     * @param threads Count of threads
     * @param values List of arguments for which method must work
     * @param <T> Arguments type
     * @throws InterruptedException If <tt>threads</tt> <= 0 or/and <tt>values</tt> is null
     */
    private <T> void checkMainArgs(int threads, List<? extends T> values) throws InterruptedException{
        if (threads <= 0) {
            throw new InterruptedException("Error: argument threads is incorrect (<=0)");
        }
        if (values == null) {
            throw new InterruptedException("Error: argument values is null");
        }
        if (values.isEmpty()) {
            throw new InterruptedException("Error: argument values is empty");
        }
    }

    /**
     * This method check arguments of methods {@link #maximum(int, List, Comparator)} and {@link #minimum(int, List, Comparator)}
     * @param threads Count of threads
     * @param values List of arguments for which method must work
     * @param comparator Comparator which set order for <tt>values</tt>
     * @param <T> Arguments type
     * @throws InterruptedException If <tt>threads</tt> <= 0 or/ans <tt>values</tt> is null or/and <tt>comparator</tt> is null
     */
    private <T> void checkMinMaxArgs(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        checkMainArgs(threads, values);
        if (comparator == null) {
            throw new InterruptedException("Error: argument comparator is null");
        }
    }

    /**
     * This method check arguments of methods {@link #all(int, List, Predicate)} and {@link #any(int, List, Predicate)}
     * @param threads Count of threads
     * @param values List of arguments for which method must work
     * @param predicate Select arguments from <tt>values</tt>
     * @param <T> Arguments type
     * @throws InterruptedException If <tt>threads</tt> <= 0 or/ans <tt>values</tt> is null or/and <tt>comparator</tt> is null
     */
    private <T> void checkAllAnyArgs(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        checkMainArgs(threads, values);
        if (predicate == null) {
            throw new InterruptedException("Error: argument predicate is null");
        }
    }

    /**
     * This method select from <tt>values</tt> max argument by <tt>comparator</tt> with using <tt>threads</tt> count threads
     * @param threads Count of threads
     * @param values List of arguments
     * @param comparator Comparator which set order for <tt>values</tt>
     * @param <T> Arguments type
     * @return T Max argument from <tt>values</tt>
     * @throws InterruptedException If errors with threads, <tt>threads</tt> <= 0 or/ans <tt>values</tt> is null or/and <tt>comparator</tt> is null
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        checkMinMaxArgs(threads, values, comparator);
        return impl(threads, values, x -> x.max(comparator).orElse(null), x -> x.max(comparator).orElse(null));
    }

    /**
     * This method select from <tt>values</tt> min argument by <tt>comparator</tt> with using <tt>threads</tt> count threads
     * @param threads Count of threads
     * @param values List of arguments
     * @param comparator Comparator which set order for <tt>values</tt>
     * @param <T> Arguments type
     * @return T Min argument from <tt>values</tt>
     * @throws InterruptedException If errors with threads, <tt>threads</tt> <= 0 or/ans <tt>values</tt> is null or/and <tt>comparator</tt> is null
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        checkMinMaxArgs(threads, values, comparator);
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * This method check all arguments of <tt>values</tt> and if they satisfied with <tt>predicate</tt> return true else false
     * @param threads Count of threads
     * @param values List of arguments
     * @param predicate Predicate
     * @param <T> Arguments type
     * @return {@link java.lang.Boolean} True if all arguments satisfied with <tt>predicate</tt> else false
     * @throws InterruptedException If errors with threads, <tt>threads</tt> <= 0 or/ans <tt>values</tt> is null or/and <tt>comparator</tt> is null
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        checkAllAnyArgs(threads, values, predicate);
        return impl(threads, values, x -> x.allMatch(predicate), x -> x.allMatch(f -> f));
    }
    /**
     * This method check all arguments of <tt>values</tt> and if one of them satisfied with <tt>predicate</tt> return true else false
     * @param threads Count of threads
     * @param values List of arguments
     * @param predicate Predicate
     * @param <T> Arguments type
     * @return {@link java.lang.Boolean} Return true if any argument satisfied with <tt>predicate</tt> else false
     * @throws InterruptedException If errors with threads, <tt>threads</tt> <= 0 or/ans <tt>values</tt> is null or/and <tt>comparator</tt> is null
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        checkAllAnyArgs(threads, values, predicate);
        return !all(threads, values, predicate.negate());
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return impl(threads, values, x -> x.map(Objects::toString).collect(Collectors.joining()), x -> x.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return impl(threads, values, x -> x.filter(predicate), x -> x.flatMap(Function.identity()).collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return impl(threads, values, x -> x.map(f), x -> x.flatMap(Function.identity()).collect(Collectors.toCollection(ArrayList::new)));
    }
}
