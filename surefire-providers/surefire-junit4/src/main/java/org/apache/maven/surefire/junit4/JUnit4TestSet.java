package org.apache.maven.surefire.junit4;

import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.testset.AbstractTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

public class JUnit4TestSet extends AbstractTestSet
{
	// Member Variables
	private Runner junitTestRunner;

	/**
	 * Constructor.
	 * 
	 * @param testClass the class to be run as a test
	 */
	protected JUnit4TestSet(Class testClass)
	{
		super(testClass);
		
		junitTestRunner = Request.aClass(testClass).getRunner();
	}

	/**
	 * Actually runs the test and adds the tests results to the <code>reportManager</code>.
	 * 
	 * @see org.apache.maven.surefire.testset.SurefireTestSet#execute(org.apache.maven.surefire.report.ReporterManager, java.lang.ClassLoader)
	 */
	public void execute(ReporterManager reportManager, ClassLoader loader)
			throws TestSetFailedException
	{
		RunNotifier fNotifier = new RunNotifier();
		RunListener listener = new JUnit4TestSetReporter(this, reportManager);
		fNotifier.addListener(listener);
		
		try
		{
			junitTestRunner.run(fNotifier);
		}
		finally
		{
			fNotifier.removeListener(listener);
		}
	}

	/**
	 * Returns the number of tests to be run in this class.
	 * 
	 * @see org.apache.maven.surefire.testset.SurefireTestSet#getTestCount()
	 */
	public int getTestCount() throws TestSetFailedException
	{
		return junitTestRunner.testCount();
	}
}
