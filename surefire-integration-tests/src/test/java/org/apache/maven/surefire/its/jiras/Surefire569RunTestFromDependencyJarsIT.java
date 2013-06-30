package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * SUREFIRE-569 Add support for scanning Dependencies for TestClasses
 *
 * @author Aslak Knutsen
 */
public class Surefire569RunTestFromDependencyJarsIT
	extends	SurefireJUnit4IntegrationTestCase {

	@Test
	public void shouldScanAndRunTestsInDependencyJars() throws Exception {
		SurefireLauncher launcher = unpack( "surefire-569-RunTestFromDependencyJars" );
		launcher.addGoal("test").addGoal("install");
		launcher.executeCurrentGoals();

		OutputValidator module1 = launcher.getSubProjectValidator("module1");
		module1.assertTestSuiteResults(1, 0, 0, 0);
	}
}
