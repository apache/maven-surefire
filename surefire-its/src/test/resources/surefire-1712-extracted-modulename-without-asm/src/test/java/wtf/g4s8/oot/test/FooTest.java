package wtf.g4s8.oot.test;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import wtf.g4s8.oot.Foo;

import static org.hamcrest.MatcherAssert.assertThat;

public class FooTest
{
    @Test
    public void addTest()
    {
        assertThat( new Foo( 1 ).add( 1 ), Matchers.equalTo( 2 ) );
    }
}
