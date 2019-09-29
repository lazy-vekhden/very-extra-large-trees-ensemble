package com.labean.velet.core;

import com.labean.velet.api.RecordSet;
import com.labean.velet.api.Splitter;
import com.labean.velet.api.TrainSet;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TreesBatch {
    private final TrainSet trainSet;
    private final RecordSet testSet;
    private final int treesCount;
    private final int treeSize;
    private final int tryCount;
    private final SplitterChoiceStrategy choiceStrategy;

    private final List<Integer> allTest;
    private final ThreadSafeAccumulator accumulator;

    public TreesBatch(TrainSet trainSet, RecordSet testSet, int treesCount, int treeSize,
                      int tryCount, SplitterChoiceStrategy choiceStrategy) {
        this.trainSet = trainSet;
        this.testSet = testSet;
        this.treesCount = treesCount;
        this.treeSize = treeSize;
        this.tryCount = tryCount;
        this.choiceStrategy = choiceStrategy;

        this.allTest = IntStream.range(0, testSet.size())
                .boxed()
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
        System.out.println("another virtual tree");
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
                .filter(trainSet::getResult)
                .count();
        if (val == 0) return;
        if (val == train.size()) {
            accumulator.addToList(test, 1.);
            return;
        }


        final Splitter splitter = selectSplitter(train, random);

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

    private Splitter selectSplitter(List<Integer> train, Random random) {
        Stream<Splitter> creator = IntStream.range(0, tryCount)
                .mapToObj(i -> trainSet.createSplitter(train, random))
                .filter(Objects::nonNull);
        if (choiceStrategy.equals(SplitterChoiceStrategy.FIRST)) return creator.findAny().orElse(null);
        if (choiceStrategy.equals(SplitterChoiceStrategy.BEST)) {
            SplitterWrapper wrapper = creator
                    .map(s -> new SplitterWrapper(s, train))
                    .reduce((a, b) -> a.quality < b.quality ? b : a)
                    .orElse(null);
            if (wrapper == null) return null;
            return wrapper.splitter;
        }
        throw new RuntimeException("not implemented strategy");
    }

    private class SplitterWrapper {
        final Splitter splitter;
        final double quality;

        SplitterWrapper(Splitter splitter, List<Integer> train) {
            this.splitter = splitter;
            int[][] distr = new int[2][2];
            for (int i : train) {
                int x = splitter.match(trainSet.get(i)) ? 0 : 1;
                int y = trainSet.getResult(i) ? 0 : 1;
                ++distr[x][y];

            }

            quality = Math.abs(
                    distr[0][1] / 1. / (distr[0][1] + distr[0][0]) -
                            distr[1][1] / 1. / (distr[1][1] + distr[1][0])
            );
        }
    }

}
