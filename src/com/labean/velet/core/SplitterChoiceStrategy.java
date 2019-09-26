package com.labean.velet.core;

import com.labean.velet.api.Splitter;

import java.util.stream.Stream;

public enum SplitterChoiceStrategy {
    FIRST,
    BEST;

    static Splitter select(SplitterChoiceStrategy choiceStrategy, Stream<Splitter> splitterStream) {
        if (choiceStrategy == FIRST) {
            return splitterStream
                    .findAny()
                    .orElse(null);
        }
        if (choiceStrategy == BEST) {
            return splitterStream
                    .reduce((s1, s2) -> s1.getQuality() < s2.getQuality() ? s2 : s1)
                    .orElse(null);
        }
        return null;
    }
}
