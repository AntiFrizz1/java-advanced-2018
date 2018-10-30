package ru.ifmo.rain.glukhov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders);
        this.extractorsPool = Executors.newFixedThreadPool(extractors);
    }

    private WebCrawler(int downloaders, int extractors) throws IOException {
        this(new CachingDownloader(), downloaders, extractors, 0);
    }

    private WebCrawler(int downloaders) throws IOException {
        this(downloaders, 8);
    }

    private WebCrawler() throws IOException {
        this(8);
    }

    private void extractLinks(final Document page, final int depth, final Set<String> good,
                              final Map<String, IOException> bad, final Set<String> visited, final Phaser sync) {
        try {
            page.extractLinks().forEach(
                    link -> {
                        if (visited.add(link)) {
                            sync.register();
                            downloadersPool.submit(getRunnableDownloadImpl(link, depth, good, bad, visited, sync));
                        }
                    }
            );
        } catch (IOException ignored) {
        } finally {
            sync.arrive();
        }

    }

    private Runnable getRunnableExtractLinks(final Document page, final int depth, final Set<String> good,
                                             final Map<String, IOException> bad, final Set<String> visited, final Phaser sync) {
        return () -> extractLinks(page, depth, good, bad, visited, sync);
    }

    private void downloadImpl(final String url, final int depth, final Set<String> good,
                              final Map<String, IOException> bad, final Set<String> visited, final Phaser sync) {
        final Runnable task = () -> {
            try {
                final Document page = downloader.download(url);
                good.add(url);
                if (depth > 1) {
                    sync.register();
                    extractorsPool.submit(getRunnableExtractLinks(page, depth, good, bad, visited, sync));
                }
            } catch (IOException e) {
                bad.put(url, e);
            } finally {
                sync.arrive();
            }
        };
        sync.register();
        downloadersPool.submit(task);
        sync.arrive();
    }

    private Runnable getRunnableDownloadImpl(final String url, final int depth, final Set<String> good,
                                             final Map<String, IOException> bad, final Set<String> visited, final Phaser sync) {
        return () -> downloadImpl(url, depth, good, bad, visited, sync);
    }

    @Override
    public Result download(String url, int depth) {
        final Set<String> good = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final Map<String, IOException> bad = new ConcurrentHashMap<>();
        final Set<String> visited = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final Phaser sync = new Phaser(2);
        visited.add(url);
        downloadImpl(url, depth, good, bad, visited, sync);
        sync.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(good), bad);
    }

    @Override
    public void close() {
        downloadersPool.shutdownNow();
        extractorsPool.shutdownNow();
    }

    public static void main(String[] args) {
        if (args == null || args.length < 2 || args.length > 5) {
            System.out.println("Error: incorrect amount of arguments");
        } else {
            if (Arrays.stream(args).anyMatch(Objects::isNull)) {
                System.out.println("Error: some arg is null");
            } else {
                int[] numericArgs = new int[args.length - 1];
                for (int i = 1; i < args.length; i++) {
                    try {
                        numericArgs[i - 1] = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        System.out.println("Error: numeric arg is not numeric");
                        return;
                    }
                }
                WebCrawler crawler = null;
                try {
                    switch (numericArgs.length) {
                        case 4:
                        case 3:
                            crawler = new WebCrawler(numericArgs[1], numericArgs[2]);
                            break;
                        case 2:
                            crawler = new WebCrawler(numericArgs[1]);
                            break;
                        case 1:
                            crawler = new WebCrawler();
                    }
                    Objects.requireNonNull(crawler).download(args[0], numericArgs[0]);
                    crawler.close();
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }
}
