package org.codehaus.surefire.battery.xmlrpc;

/*
 * Copyright 2001-2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
