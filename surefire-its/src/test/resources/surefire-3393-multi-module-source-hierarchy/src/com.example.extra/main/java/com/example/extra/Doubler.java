package com.example.extra;

import com.example.core.Calculator;
import com.example.extra.internal.TwiceHelper;

public class Doubler {
    public long doubled(long value) {
        // cross-module call into com.example.core
        return new Calculator().add(TwiceHelper.twice(value), 0L);
    }
}
