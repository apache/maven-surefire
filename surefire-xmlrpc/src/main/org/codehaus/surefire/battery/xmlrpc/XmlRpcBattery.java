package org.codehaus.surefire.battery.xmlrpc;

import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.codehaus.surefire.battery.AbstractBattery;

public class XmlRpcBattery
    extends AbstractBattery
{

    public XmlRpcBattery()
    {
    }

    public void setupServer( String url )
        throws Exception
    {
        try
        {
            client = new XmlRpcClient( url );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public Object send( String command )
    {
        Object o = null;
        try
        {
            o = client.execute( command, new Vector() );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return o;
    }

    private XmlRpcClient client;
}
