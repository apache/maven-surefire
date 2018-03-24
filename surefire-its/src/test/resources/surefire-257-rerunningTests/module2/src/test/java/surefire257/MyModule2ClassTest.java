package surefire257;

import junit.framework.Assert;
import junit.framework.TestCase;
import surefire257.MyModule2Class;

public class MyModule2ClassTest extends TestCase {

  public void testGetFooOK() {
    MyModule2Class mc = new MyModule2Class();
    Assert.assertEquals(42, mc.getFoo());
  }
}
