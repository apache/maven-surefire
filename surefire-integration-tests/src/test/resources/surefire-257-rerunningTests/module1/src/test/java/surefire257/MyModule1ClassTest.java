package surefire257;

import junit.framework.Assert;
import junit.framework.TestCase;
import surefire257.MyModule1Class;

public class MyModule1ClassTest extends TestCase {

  public void testGetFooOK() {
    MyModule1Class mc = new MyModule1Class();
    Assert.assertEquals(42, mc.getFoo());
  }
}
