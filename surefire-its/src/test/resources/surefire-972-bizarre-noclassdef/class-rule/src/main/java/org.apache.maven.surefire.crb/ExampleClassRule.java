package org.apache.maven.surefire.crb;

import org.junit.rules.ExternalResource;

/**
 * Created with IntelliJ IDEA.
 * User: benson
 * Date: 3/16/13
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExampleClassRule extends ExternalResource {

    public ExampleClassRule(String dummy) {
        //
    }

    protected void before() throws Throwable {
        System.err.println("ExampleClassRule.before()");
    }

    protected void after() {
        System.err.println("ExampleClassRule.after()");
    }

    public static String someStaticFunction() {
        throw new RuntimeException("Surprise!");
    }
}
