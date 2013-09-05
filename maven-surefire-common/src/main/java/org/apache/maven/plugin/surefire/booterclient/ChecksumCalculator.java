package org.apache.maven.plugin.surefire.booterclient;

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
 * Unless required| by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * @author Kristian Rosenvold
 */
public class ChecksumCalculator
{
    private static final String HEX = "0123456789ABCDEF";

    private final List<Object> checksumItems = new ArrayList<Object>();

    private void appendObject( Object item )
    {
        checksumItems.add( item );
    }

    public void add( boolean value )
    {
        checksumItems.add( value ? Boolean.TRUE : Boolean.FALSE );
    }

    public void add( int value )
    {
        checksumItems.add( value );
    }

    public void add( double value )
    {
        checksumItems.add( value );
    }

    public void add( Map<?, ?> map )
    {
        if ( map != null )
        {
            appendObject( map.toString() );
        }
    }

    public void add( String string )
    {
        appendObject( string );
    }

    public void add( File workingDirectory )
    {
        appendObject( workingDirectory );
    }

    public void add( ArtifactRepository localRepository )
    {
        appendObject( localRepository );
    }

    public void add( List<?> items )
    {
        if ( items != null )
        {
            for ( Object item : items )
            {
                appendObject( item );
            }
        }
        else
        {
            appendObject( null );
        }

    }

    public void add( Object[] items )
    {
        if ( items != null )
        {
            for ( Object item : items )
            {
                appendObject( item );
            }
        }
        else
        {
            appendObject( null );
        }
    }

    public void add( Artifact artifact )
    {
        appendObject( artifact != null ? artifact.getId() : null );
    }

    public void add( Boolean aBoolean )
    {
        appendObject( aBoolean );
    }

    private static String asHexString( byte[] bytes )
    {
        if ( bytes == null )
        {
            return null;
        }
        final StringBuilder result = new StringBuilder( 2 * bytes.length );
        for ( byte b : bytes )
        {
            result.append( HEX.charAt( ( b & 0xF0 ) >> 4 ) ).append( HEX.charAt( ( b & 0x0F ) ) );
        }
        return result.toString();
    }

    private String getConfig()
    {
        StringBuilder result = new StringBuilder();
        for ( Object checksumItem : checksumItems )
        {
            result.append( checksumItem != null ? checksumItem.toString() : "null" );
        }
        return result.toString();
    }

    public String getSha1()
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance( "SHA-1" );
            String configValue = getConfig();
            md.update( configValue.getBytes( "iso-8859-1" ), 0, configValue.length() );
            byte[] sha1hash = md.digest();
            return asHexString( sha1hash );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RuntimeException( e );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new RuntimeException( e );
        }
    }

}
