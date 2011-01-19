package org.apache.maven.surefire570;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.maven.surefire570.MyModule1Class;

public class MyModule1ClassTest extends TestCase {

  public void testGetFooKO() {
    MyModule1Class mc = new MyModule1Class();
    Assert.assertEquals(18, mc.getFoo());
  }

  public void testGetFooOK() {
    MyModule1Class mc = new MyModule1Class();
    Assert.assertEquals(42, mc.getFoo());
  }
}
