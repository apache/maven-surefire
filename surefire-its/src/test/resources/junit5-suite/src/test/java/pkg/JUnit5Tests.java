package pkg;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({pkg.domain.AxTest.class})
public class JUnit5Tests
{
}
