package org.codehaus.surefire.battery;

import org.codehaus.surefire.report.ReportManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Modifier;

public class JUnitBattery
    extends AbstractBattery
{
    public static final String TEST_CASE = "junit.framework.TestCase";

    public static final String TEST_RESULT = "junit.framework.TestResult";

    public static final String TEST_LISTENER = "junit.framework.TestListener";

    public static final String TEST = "junit.framework.Test";

    public static final String ADD_LISTENER_METHOD = "addListener";

    public static final String RUN_METHOD = "run";

    public static final String COUNT_TEST_CASES_METHOD = "countTestCases";

    public static final String SETUP_METHOD = "setUp";

    public static final String TEARDOWN_METHOD = "tearDown";

    private static final String TEST_SUITE = "junit.framework.TestSuite";

    private Object instanceOfJunitFrameworkTest;

    private Class[] interfacesImplementedByDynamicProxy;

    private Class testResultClass;

    private ClassLoader classLoader;

    private Class testClass;

    private Method addListenerMethod;

    private Method countTestCasesMethod;

    private Method runMethod;

    public JUnitBattery( final String testClass, ClassLoader loader )
        throws Exception
    {
        this( loader.loadClass( testClass ), loader );
    }

    public JUnitBattery( final Class testClass, ClassLoader loader )
        throws Exception
    {
        if ( testClass == null )
        {
            throw new NullPointerException( "testClass is null" );
        }

        if ( loader == null )
        {
            throw new NullPointerException( "classLoader is null" );
        }

        this.classLoader = loader;

        this.testClass = testClass;

        testResultClass = loader.loadClass( TEST_RESULT );

        Class testCaseClass = loader.loadClass( TEST_CASE );

        Class testSuiteClass = loader.loadClass( TEST_SUITE );

        Class testListenerInterface = loader.loadClass( TEST_LISTENER );

        Class testInterface = loader.loadClass( TEST );

        // Make sure passed instance is actually a TestCase
        if ( !testCaseClass.isAssignableFrom( testClass ) )
        {
            throw new IllegalArgumentException( "testClass is not a " + TEST_CASE );
        }

        // If a TestCase defines a static suite() method, use that to get
        // a instanceOfJunitFrameworkTest case.
        try
        {
            Class[] emptyArgs = new Class[0];

            Method suiteMethod = testClass.getMethod( "suite", emptyArgs );

            if ( Modifier.isPublic( suiteMethod.getModifiers() )
                 &&
                 Modifier.isStatic( suiteMethod.getModifiers() )
                 &&
                 suiteMethod.getReturnType() == testInterface )
            {
                instanceOfJunitFrameworkTest = suiteMethod.invoke( null, emptyArgs );
            }
        }
        catch ( NoSuchMethodException e )
        {
        }

        if ( instanceOfJunitFrameworkTest == null )
        {
            Class[] constructorParamTypes = {Class.class};

            Constructor constructor = testSuiteClass.getConstructor( constructorParamTypes );

            Object[] constructorParams = {testClass};

            instanceOfJunitFrameworkTest = constructor.newInstance( constructorParams );
        }

        interfacesImplementedByDynamicProxy = new Class[1];

        interfacesImplementedByDynamicProxy[0] = testListenerInterface;

        // The interface implemented by the dynamic proxy (TestListener), happens to be
        // the same as the param types of TestResult.addTestListener
        Class[] addListenerParamTypes = interfacesImplementedByDynamicProxy;

        addListenerMethod = testResultClass.getMethod( ADD_LISTENER_METHOD, addListenerParamTypes );

        countTestCasesMethod = testInterface.getMethod( COUNT_TEST_CASES_METHOD, new Class[0] );

        Class[] runParamTypes = {testResultClass};

        runMethod = testInterface.getMethod( RUN_METHOD, runParamTypes );
    }

    protected Class getTestClass()
    {
        return testClass;
    }

    public void execute( ReportManager reportManager )
    {
        try
        {
            Object instanceOfTestResult = testResultClass.newInstance();

            TestListenerInvocationHandler invocationHandler =
                new TestListenerInvocationHandler( reportManager, instanceOfTestResult, classLoader );

            Object testListener =
                Proxy.newProxyInstance( classLoader, interfacesImplementedByDynamicProxy, invocationHandler );

            Object[] addTestListenerParams = {testListener};

            addListenerMethod.invoke( instanceOfTestResult, addTestListenerParams );

            Object[] runParams = {instanceOfTestResult};

            runMethod.invoke( instanceOfJunitFrameworkTest, runParams );
        }
        catch ( IllegalArgumentException e )
        {
            throw new org.codehaus.surefire.battery.assertion.BatteryTestFailedException( e );
        }
        catch ( InstantiationException e )
        {
            throw new org.codehaus.surefire.battery.assertion.BatteryTestFailedException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new org.codehaus.surefire.battery.assertion.BatteryTestFailedException( e );
        }
        catch ( InvocationTargetException e )
        {
            throw new org.codehaus.surefire.battery.assertion.BatteryTestFailedException( e );
        }
    }

    public int getTestCount()
    {
        try
        {
            Integer integer = (Integer) countTestCasesMethod.invoke( instanceOfJunitFrameworkTest, new Class[0] );

            return integer.intValue();
        }
        catch ( IllegalAccessException e )
        {
            throw new org.codehaus.surefire.battery.assertion.BatteryTestFailedException( e );
        }
        catch ( IllegalArgumentException e )
        {
            throw new org.codehaus.surefire.battery.assertion.BatteryTestFailedException( e );
        }
        catch ( InvocationTargetException e )
        {
            throw new org.codehaus.surefire.battery.assertion.BatteryTestFailedException( e );
        }
    }

    public String getBatteryName()
    {
        return testClass.getName();
    }
}
