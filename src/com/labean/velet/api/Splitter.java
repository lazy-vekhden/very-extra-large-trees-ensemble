package com.labean.velet.api;

public interface Splitter {
    boolean match(Record record);

    default double getQuality() {
        return 1;
    }
}
