package com.example.probe.internal;

/**
 * Non-exported internal helper, reachable for tests only by patching them into the module.
 */
public final class Secret {
    private Secret() {}

    public static String reveal() {
        return "42";
    }
}
