package org.apache.maven.surefire.its;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

public class Junit47StaticInnerClassTestsIT extends SurefireJUnit4IntegrationTestCase
{

    @Test
	public void testStaticInnerClassTests() {
		executeErrorFreeTest( "junit47-static-inner-class-tests", 3 );
	}
}
