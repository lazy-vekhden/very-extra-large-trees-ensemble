package com.labean.velet.core;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class ThreadSafeAccumulator {
    private static final int PFS = 100;

    private final int testSize;
    private final int rowsCount;
    private final double[][] accumulator;

    private final AtomicInteger initialMaker;
    private final ThreadLocal<Integer> rowForThread;

    public ThreadSafeAccumulator(int testSize) {
        this.testSize = testSize;
        this.rowsCount = Runtime.getRuntime().availableProcessors();
        this.accumulator = new double[rowsCount][testSize + PFS];

        this.initialMaker = new AtomicInteger(0);
        rowForThread = ThreadLocal.withInitial(() -> initialMaker.getAndIncrement());
    }

    public void addToList(List<Integer> ls, double value) {
        if (value == 0) return;
        if (ls.size() == 0) return;
        int row = rowForThread.get();
        for (int i : ls) {
            if (i >= 0 && i < testSize) {
                accumulator[row][i] += value;
            }
        }
    }

    public double[] getResult() {
        return IntStream.range(0, testSize)
                .parallel()
                .mapToDouble(
                        i -> IntStream.range(0, rowsCount)
                                .mapToDouble(j -> accumulator[j][i])
                                .sum())
                .toArray();
    }
}
