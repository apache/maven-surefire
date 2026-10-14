package com.example.probe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Probes that the test-scope dependencies referenced by module-info-patch.args run as
 * NAMED modules on the module path (#3090) instead of classpath jars in the unnamed
 * module.
 */
class ModulePlacementTest {
    @Test
    void junitApiIsNamedModuleOnModulePath() {
        Module junitApi = Test.class.getModule();
        assertTrue(junitApi.isNamed(), "junit-jupiter-api must run as a named module, but was: " + junitApi);
        assertEquals("org.junit.jupiter.api", junitApi.getName());
    }

    @Test
    void testRunsInsidePatchedModule() {
        Module own = ModulePlacementTest.class.getModule();
        assertTrue(own.isNamed(), "the test must run inside the patched module, but was: " + own);
        assertEquals("com.example.probe", own.getName());
        assertTrue(own.canRead(Test.class.getModule()), "the patched module must read the moved JUnit API module");
        assertEquals(1, new Widget().size());
    }
}
