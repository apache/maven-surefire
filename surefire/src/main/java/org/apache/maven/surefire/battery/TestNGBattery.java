package org.apache.maven.surefire.battery;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.surefire.report.ReporterManager;

/**
 * Main plugin point for running testng tests within the Surefire runtime
 * infrastructure.
 * 
 * @author jkuhnert
 */
public class TestNGBattery implements Battery {
	
    protected List testMethods;
    
    protected List subBatteryClassNames;
    
    private Class testClass;
	
    /**
     * Creates a new test battery that will process the class being
     * passed in to determine the testing configuration.
     * @param testClass
     * @param loader
     * @throws Exception
     */
	public TestNGBattery(final Class testClass, ClassLoader loader)
			throws Exception {
		processTestClass(testClass, loader);
	}
	
	/**
	 * Parses and configures this battery based on the test class
	 * being passed in.
	 * @param testClass
	 * @param loader
	 * @throws Exception
	 */
	public void processTestClass(final Class testClass, ClassLoader loader)
			throws Exception {
		if (testClass == null) {
			throw new NullPointerException("testClass is null");
		}
		
		if (loader == null) {
			throw new NullPointerException("classLoader is null");
		}
		
		this.testClass = testClass;
	}
	
    public Class getTestClass() 
    {
        return testClass;
    }
    
	protected void discoverTestMethods()
    {
        if ( testMethods != null )
        {
            return;
        }
        
        testMethods = new ArrayList();
        	
        Method[] methods = testClass.getMethods();
        
        for ( int i = 0; i < methods.length; ++i )
        {
            Method m = methods[i];
            
            Class[] paramTypes = m.getParameterTypes();
            
            boolean isInstanceMethod = !Modifier.isStatic( m.getModifiers() );

            boolean returnsVoid = m.getReturnType() == void.class;

            boolean hasNoParams = paramTypes.length == 0;

            if ( isInstanceMethod && returnsVoid && hasNoParams )
            {
                String simpleName = m.getName();

                if ( simpleName.length() <= 4 )
                {
                    // name must have 5 or more chars
                    continue;
                }
                
                testMethods.add( m );
            }
        }
    }
	
	public void discoverBatteryClassNames() throws Exception {
	}

	public void execute(ReporterManager reportManager) throws Exception {
		// TODO Auto-generated method stub
	}
	
	public String getBatteryName() {
		return testClass.getName();
	}
	
	public void addSubBatteryClassName( String batteryClassName )
    {
        getSubBatteryClassNames().add( batteryClassName );
    }
	
    public List getSubBatteryClassNames()
    {
        if ( subBatteryClassNames == null )
        {
            subBatteryClassNames = new ArrayList();
        }

        return subBatteryClassNames;
    }
    
	public int getTestCount() {
		discoverTestMethods();
		
        return testMethods.size();
	}
}
