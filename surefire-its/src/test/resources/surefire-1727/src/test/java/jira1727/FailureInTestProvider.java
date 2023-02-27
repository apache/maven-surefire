package jira1727;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

class FailureInTestProvider
    implements TestTemplateInvocationContextProvider
{
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext context) {
        fail( "Encountered failure in TestTemplate provideTestTemplateInvocationContexts()" );
        return Stream.of();
    }
}
