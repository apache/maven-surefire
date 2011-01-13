package surefire500;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(org.junit.runners.Suite.class)
@SuiteClasses(value={ExplodingTest.class, PassingTest.class})
public class Suite {

}
