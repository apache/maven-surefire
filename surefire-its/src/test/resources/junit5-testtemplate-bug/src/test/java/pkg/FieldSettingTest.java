package pkg;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldSettingTest {
    private int testValue = 42;

    // We're calling this in the provider underneath
    public void setTestValue(int testValue) {
        this.testValue = testValue;
    }

    @TestTemplate
    @ExtendWith(FieldSettingContextProvider.class)
    public void testTemplatePartiallyFails() {
        assertEquals(42, testValue);
    }
}


class FieldSettingContextProvider implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        return Stream.of(context(0), context(42));
    }

    private TestTemplateInvocationContext context(int parameter) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return "[%d] %s".formatted(invocationIndex, parameter);
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return getBeforeEachCallbacks(parameter);
            }
        };
    }

    private List<Extension> getBeforeEachCallbacks(int value) {
        return List.of(((BeforeEachCallback) ctx ->
                ((FieldSettingTest) ctx.getRequiredTestInstance()).setTestValue(value)
        ));
    }
}
