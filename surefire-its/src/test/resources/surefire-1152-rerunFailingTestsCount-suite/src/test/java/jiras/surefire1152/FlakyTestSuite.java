package jiras.surefire1152;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith( Suite.class )
@Suite.SuiteClasses( { FlakyTest.class, FlakyParent.class } )
public class FlakyTestSuite
{
}
