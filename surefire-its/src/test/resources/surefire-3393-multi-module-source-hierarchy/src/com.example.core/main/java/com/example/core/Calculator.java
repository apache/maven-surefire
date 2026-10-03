package com.example.core;

import com.example.core.internal.MathHelper;
import jakarta.json.JsonValue;

public class Calculator {
    public long add(long a, long b) {
        return MathHelper.add(a, b);
    }

    public String kindOf(JsonValue value) {
        return value.getValueType().name();
    }
}
