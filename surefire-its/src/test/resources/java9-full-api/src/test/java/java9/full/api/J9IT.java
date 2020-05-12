package java9.full.api;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
