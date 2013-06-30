package org.apache.maven.surefire.crb;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: benson
 * Date: 3/16/13
 * Time: 11:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClassRuleIT extends Assert {

    @ClassRule
    public static ExampleClassRule rule = new ExampleClassRule(ExampleClassRule.someStaticFunction());

    @Test
    public void dummyTest() {

    }

}
