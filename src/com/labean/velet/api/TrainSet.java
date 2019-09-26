package com.labean.velet.api;

import java.util.List;
import java.util.Random;

public interface TrainSet extends RecordSet {
    boolean getResult(int i);

    Splitter createSplitter(List<Integer> train, Random random);
}
