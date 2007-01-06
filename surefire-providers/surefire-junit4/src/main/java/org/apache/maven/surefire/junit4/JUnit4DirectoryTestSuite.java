package org.apache.maven.surefire.junit4;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.surefire.suite.AbstractDirectoryTestSuite;
import org.apache.maven.surefire.testset.SurefireTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * Test suite for JUnit4 based on a directory of Java test classes. This is
 * capable of running both JUnit3 and JUnit4 test classes (I think).
 * 
 * @author Karl M. Davis
 */
public class JUnit4DirectoryTestSuite extends AbstractDirectoryTestSuite
{
	/**
	 * Constructor.
	 */
	public JUnit4DirectoryTestSuite(File basedir, ArrayList includes,
			ArrayList excludes)
	{
		super(basedir, includes, excludes);
	}

	/**
	 * This method will be called for each class to be run as a test. It returns
	 * a surefire test set that will later be executed.
	 * 
	 * @see org.apache.maven.surefire.suite.AbstractDirectoryTestSuite#createTestSet(java.lang.Class,
	 *      java.lang.ClassLoader)
	 */
	protected SurefireTestSet createTestSet(Class testClass,
			ClassLoader classLoader) throws TestSetFailedException
	{
		return new JUnit4TestSet(testClass);
	}

}
