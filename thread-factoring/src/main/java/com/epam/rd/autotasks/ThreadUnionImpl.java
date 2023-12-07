package com.epam.rd.autotasks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ThreadUnionImpl implements ThreadUnion{
    private final String name;
    private final List<Thread> threads;
    private final List<FinishedThreadResult> finishedThreads;
    private boolean isShutdown;
    //    private final AtomicInteger threadNumber = new AtomicInteger(-1);  This way is possible, but it creates additional object
    protected ThreadUnionImpl(String name){
        this.name = name;
        this.threads = Collections.synchronizedList(new ArrayList<>());
        this.finishedThreads = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public int totalSize() {
        return threads.size();
    }

    @Override
    public int activeSize() {
        return (int) threads.stream().filter(Thread::isAlive).count();
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public void awaitTermination() {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isFinished() {
        return isShutdown && threads.stream().noneMatch(Thread::isAlive);
    }

    @Override
    public List<FinishedThreadResult> results() {
        return finishedThreads;
    }

    @Override
    public synchronized Thread newThread(@Nullable Runnable r) {
        if(isShutdown){
            throw new IllegalStateException();
        }

        Thread thread = new Thread(() -> {
            try {
                Objects.requireNonNull(r).run();
                finishedThreads.add(new FinishedThreadResult(Thread.currentThread().getName()));
            } catch (Exception e) {
                finishedThreads.add(new FinishedThreadResult(Thread.currentThread().getName(), e));
            }
        }, name + "-worker-" + totalSize());

        threads.add(thread);
        return thread;
    }
}
