package org.apache.maven.surefire.util;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kristian Rosenvold
 */
public class RunOrderCalculatorTest extends TestCase {

  public void testOrderTestClasses() throws Exception {
    getClassesToRun();
    TestsToRun testsToRun = new TestsToRun(getClassesToRun());
    RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator(RunOrder.ALPHABETICAL);
    final TestsToRun testsToRun1 = runOrderCalculator.orderTestClasses(testsToRun);
    assertEquals( A.class, testsToRun1.iterator().next());

  }

  private List getClassesToRun() {
    List classesToRun = new ArrayList();
    classesToRun.add( B.class);
    classesToRun.add( A.class);
    return classesToRun;
  }

  class A {

  }

  class B {

  }


}
