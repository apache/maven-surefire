package org.apache.maven.plugin.surefire.runorder;

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

import org.apache.maven.plugin.surefire.runorder.api.RunOrder;

/**
 * @author Dipak Pawar
 */
public class StringUtil
{

    public static String joins( RunOrder[] runOrders )
    {
        StringJoiner stringJoiner = new StringJoiner( "," );

        for ( RunOrder runOrder : runOrders )
        {
            final String runOrderName = runOrder.getName();

            if ( runOrderName != null )
            {
                stringJoiner.add( runOrderName );
            }
        }

        return stringJoiner.toString();
    }

    static class StringJoiner
    {
        private final String delimiter;
        private StringBuilder value;

        StringJoiner( CharSequence delimiter )
        {
            this.delimiter = delimiter.toString();
        }

        public String toString()
        {
            if ( this.value == null )
            {
                return "";
            }
            else
            {
                int initialLength = this.value.length();
                String result = this.value.toString();
                this.value.setLength( initialLength );
                return result;
            }
        }

        public StringJoiner add( CharSequence newElement )
        {
            this.prepareBuilder().append( newElement );
            return this;
        }

        private StringBuilder prepareBuilder()
        {
            if ( this.value != null )
            {
                this.value.append( this.delimiter );
            }
            else
            {
                this.value = new StringBuilder();
            }

            return this.value;
        }
    }

}
