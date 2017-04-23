package org.apache.maven.plugin.failsafe.util;

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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import static javax.xml.bind.JAXBContext.newInstance;
import static javax.xml.bind.Marshaller.JAXB_ENCODING;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static javax.xml.bind.Marshaller.JAXB_FRAGMENT;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public final class JAXB
{
    private JAXB()
    {
        throw new IllegalStateException( "Not instantiated constructor." );
    }

    public static <T> T unmarshal( File source, Class<T> rootXmlNode ) throws JAXBException
    {
        return unmarshal( source, rootXmlNode, Collections.<String, Object>emptyMap() );
    }

    public static <T> T unmarshal( File source, Class<T> rootXmlNode, Map<String, ?> props )
            throws JAXBException
    {
        Class<?>[] classesToBeBound = { rootXmlNode };
        JAXBContext ctx = newInstance( classesToBeBound );
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        properties( props, unmarshaller );
        unmarshaller.setEventHandler( new DefaultValidationEventHandler() );
        JAXBElement<T> element = unmarshaller.unmarshal( new StreamSource( source ), rootXmlNode );
        return element.getValue();
    }

    @SuppressWarnings( "unchecked" )
    public static <T> void marshal( T bean, Charset encoding, File destination ) throws JAXBException, IOException
    {
        Class<T> type = (Class<T>) bean.getClass();
        JAXBElement<T> rootElement = buildJaxbElement( bean, type );
        Class<?>[] classesToBeBound = { type };
        JAXBContext context = newInstance( classesToBeBound );
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty( JAXB_ENCODING, encoding.name() );
        marshaller.setProperty( JAXB_FORMATTED_OUTPUT, true );
        marshaller.setProperty( JAXB_FRAGMENT, true );
        marshaller.marshal( rootElement, destination );
    }

    private static <T> JAXBElement<T> buildJaxbElement( T bean, Class<T> type )
    {
        XmlRootElement xmlRootElement = type.getAnnotation( XmlRootElement.class );
        if ( xmlRootElement == null )
        {
            return null;
        }
        QName root = new QName( "", xmlRootElement.name() );
        return new JAXBElement<T>( root, type, bean );
    }

    private static void properties( Map<String, ?> props, Unmarshaller unmarshaller ) throws PropertyException
    {
        for ( Entry<String, ?> e : props.entrySet() )
        {
            unmarshaller.setProperty( e.getKey(), e.getValue() );
        }
    }
}
