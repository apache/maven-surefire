package jiras.surefire1082;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RunWith( Parameterized.class )
public final class Jira1264Test extends Jira1082Test
{
    public Jira1264Test( int x )
    {
        super( x );
    }
}
