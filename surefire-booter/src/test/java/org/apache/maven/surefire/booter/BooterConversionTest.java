package org.apache.maven.surefire.booter;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import junit.framework.TestCase;

public class BooterConversionTest
    extends TestCase
{
    Method convert, constructParamObjects;
    public void setUp() throws Exception {
        convert = SurefireBooter.class.getDeclaredMethod( "convert", new Class[] { Object.class } );
        convert.setAccessible( true );
        constructParamObjects = SurefireBooter.class.getDeclaredMethod( "constructParamObjects", new Class[] { String.class, String.class } );
        constructParamObjects.setAccessible( true );
    }
    
    public void testString() throws Exception {
        doTest( "Hello world!" );
    }
    
    public void testFile() throws Exception {
        doTest( new File( "."));
    }
    
    public void testFileArray() throws Exception {
        doTestArray( new File[] {new File( ".")});
    }
    
    public void testArrayList() throws Exception {
        doTest(new ArrayList());
    }
    
    public void testBoolean() throws Exception {
        doTest(Boolean.TRUE);
        doTest(Boolean.FALSE);
    }
    
    public void testInteger() throws Exception {
        doTest(new Integer(0));
    }
    
    public void testProperties() throws Exception {
        Properties p = new Properties();
        p.setProperty( "foo", "bar" );
        doTest(p);
    }
    
    public void testPropertiesEmpty() throws Exception {
        Properties p = new Properties();
        doTest(p);
    }
    
    public void testPropertiesWithComma() throws Exception {
        Properties p = new Properties();
        p.setProperty( "foo, comma", "bar" );
        
        doTest(p);
    }
    
    public void doTest(Object o) throws Exception {
        String serialized = serialize( o );
        Object[] output = deserialize( serialized, o.getClass().getName() );
        assertEquals ( "Wrong number of output elements: " + Arrays.asList( output ), 1, output.length);
        assertEquals ( o, output[0] );
    }
    
    public void doTestArray(Object[] o) throws Exception {
        String serialized = serialize( o );
        Object[] output = deserialize( serialized, o.getClass().getName() );
        assertEquals ( "Wrong number of output elements: " + Arrays.asList( output ), 1, output.length);
        assertArrayEquals ( "Deserialized array didn't match", o, (Object[])output[0] );
    }
    
    private void assertArrayEquals(String message, Object[] expected, Object[] actual) {
        assertEquals( message + "; wrong number of elements", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals( message + "; element " + i + " differs", expected[i], actual[i]);
        }
    }

    private Object[] deserialize (String paramProperty, String typeProperty) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        return (Object[]) constructParamObjects.invoke( null, new Object[] {paramProperty, typeProperty} );
    }
    
    private String serialize(Object o) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        return (String) convert.invoke( null, new Object[] { o } );
    }
}
