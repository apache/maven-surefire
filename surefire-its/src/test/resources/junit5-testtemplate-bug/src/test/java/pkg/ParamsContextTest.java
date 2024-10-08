package pkg;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParamsContextTest {

    @TestTemplate
    @ExtendWith(ParamsContextProvider.class)
    void testTemplatePartiallyFails(Integer value) {
        assertEquals(42, value);
    }
}

class ParamsContextProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return Stream.of(invocationContext(0), invocationContext(42));
    }

    private TestTemplateInvocationContext invocationContext(int parameter) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return "[%d] %s".formatted(invocationIndex, parameter);
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return Collections.singletonList(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
                        return parameterContext.getParameter().getType().equals(Integer.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
                        return parameter;
                    }
                });
            }
        };
    }
}
