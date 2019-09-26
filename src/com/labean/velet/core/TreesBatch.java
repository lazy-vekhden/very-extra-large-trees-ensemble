package com.labean.velet.core;

import com.labean.velet.api.RecordSet;
import com.labean.velet.api.Splitter;
import com.labean.velet.api.TrainSet;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TreesBatch {
    final TrainSet trainSet;
    final RecordSet testSet;
    final int treesCount;
    final int treeSize;
    final int tryCount;
    final SplitterChoiceStrategy choiceStrategy;

    final List<Integer> allTest;
    final ThreadSafeAccumulator accumulator;

    public TreesBatch(TrainSet trainSet, RecordSet testSet, int treesCount, int treeSize,
                      int tryCount, SplitterChoiceStrategy choiceStrategy) {
        this.trainSet = trainSet;
        this.testSet = testSet;
        this.treesCount = treesCount;
        this.treeSize = treeSize;
        this.tryCount = tryCount;
        this.choiceStrategy = choiceStrategy;

        this.allTest = IntStream.range(0, testSet.size())
                .mapToObj(i -> i)
                .collect(Collectors.toList());
        this.accumulator = new ThreadSafeAccumulator(testSet.size());
    }


    public double[] predict() {
        IntStream.range(0, treesCount)
                .parallel()
                .forEach(i -> createVirtualTree());
        return accumulator.getResult();
    }

    private void createVirtualTree() {
        Random random = ThreadLocalRandom.current();
        Set<Integer> treeTrain = new HashSet<>();
        while (treeTrain.size() < treeSize) {
            treeTrain.add(random.nextInt(trainSet.size()));
        }
        runVirtualTree(new ArrayList<>(treeTrain), allTest, random);
    }

    private void runVirtualTree(final List<Integer> train, final List<Integer> test, final Random random) {
        if (test.size() == 0) return;
        int val = (int) train
                .stream()
                .filter(i -> trainSet.getResult(i))
                .count();
        if (val == 0) return;
        if (val == train.size()) {
            accumulator.addToList(test, 1.);
            return;
        }

        final Splitter splitter = SplitterChoiceStrategy.select(
                choiceStrategy,
                IntStream.range(0, tryCount)
                        .mapToObj(i -> trainSet.createSplitter(train, random))
                        .filter(s->s!=null)
        );

        if (splitter == null) {
            accumulator.addToList(test, val * 1. / train.size());
            return;
        }

        runVirtualTree(
                train
                        .stream()
                        .filter(i -> splitter.match(trainSet.get(i)))
                        .collect(Collectors.toList()),
                test
                        .stream()
                        .filter(i -> splitter.match(testSet.get(i)))
                        .collect(Collectors.toList()),
                random
        );

        runVirtualTree(
                train
                        .stream()
                        .filter(i -> !splitter.match(trainSet.get(i)))
                        .collect(Collectors.toList()),
                test
                        .stream()
                        .filter(i -> !splitter.match(testSet.get(i)))
                        .collect(Collectors.toList()),
                random
        );
    }

}
