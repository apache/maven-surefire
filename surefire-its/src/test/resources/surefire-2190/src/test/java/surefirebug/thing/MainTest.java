package surefirebug.thing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Nullable;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MainTest {

    @Test
    public void testDefault() {
        Main main = new Main();
        assertEquals("Hello, World", main.call());
    }

    @Test
    public void testNonStandard() {
        Main main = new Main(new NonstandardSupplier());
        assertEquals("Hello, People", main.call());
    }

    @Test
    public void testNullSupplierWithNullable() {
        Main main = new Main(new NullSupplierWithNullable());
        assertEquals("<null value>", main.call());
    }

    @Test
    public void testNullSupplierWithoutNullable() {
        Main main = new Main(new NullSupplierWithoutNullable());
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, main::call);
        assertEquals("null without @Nullable annotation", e.getMessage());
    }

    public static class NonstandardSupplier implements Supplier<String> {
        public String get() {
            return "Hello, People";
        }
    }

    @Nullable
    public static class NullSupplierWithNullable implements Supplier<String> {
        public String get() {
            return null;
        }
    }

    public static class NullSupplierWithoutNullable implements Supplier<String> {
        public String get() {
            return null;
        }
    }
}
