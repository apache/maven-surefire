package failingbuilds;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.*;

import junit.framework.TestCase;

public class ExceptionsTest
    extends TestCase
{
    public void testWithMultiLineExceptionBeingThrown()
    {
        throw new RuntimeException( "A very very long exception message indeed, which is to demonstrate truncation. It will be truncated somewhere\nA cat\nAnd a dog\nTried to make a\nParrot swim" );
    }
}