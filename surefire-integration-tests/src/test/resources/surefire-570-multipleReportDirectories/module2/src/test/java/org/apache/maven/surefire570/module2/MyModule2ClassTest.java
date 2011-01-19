package org.apache.maven.surefire570.module2;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.maven.surefire570.module2.MyModule2Class;

public class MyModule2ClassTest extends TestCase {

  public void testGetFooKO() {
    MyModule2Class mc = new MyModule2Class();
    Assert.assertEquals(18, mc.getFoo());
  }

  public void testGetFooOK() {
    MyModule2Class mc = new MyModule2Class();
    Assert.assertEquals(42, mc.getFoo());
  }
}
