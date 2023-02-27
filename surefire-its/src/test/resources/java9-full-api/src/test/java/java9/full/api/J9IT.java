package java9.full.api;

import org.junit.Test;

public class J9IT
{
    @Test
    public void testMiscellaneousAPI() throws java.sql.SQLException
    {
        System.out.println( "loaded class " + java.sql.SQLException.class.getName() );
        System.out.println( "loaded class " + javax.xml.ws.Holder.class.getName() );
        System.out.println( "loaded class " + javax.xml.bind.JAXBException.class.getName() );
        System.out.println( "loaded class " + javax.transaction.InvalidTransactionException.class.getName() );
        System.out.println( "from classloader " + javax.transaction.InvalidTransactionException.class.getClassLoader() );
        System.out.println( "loaded class " + javax.transaction.TransactionManager.class.getName() );
        System.out.println( "loaded class " + javax.xml.xpath.XPath.class.getName() );
        System.out.println( "java.specification.version=" + System.getProperty( "java.specification.version" ) );
    }

}
