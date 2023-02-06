package pkg.junit5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ParameterizedTest
{
    static class ParameterSource implements ArgumentsProvider
    {
        @Override
        public Stream<? extends Arguments> provideArguments( ExtensionContext context ) throws Exception
        {
            return Arrays.asList(0, 1).stream().map( Arguments::of );
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @ArgumentsSource(value = ParameterSource.class)
    public void notFlaky(int expected)
    {
        assertEquals( expected, 0 );
    }
}
