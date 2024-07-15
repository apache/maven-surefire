/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.surefire;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.KeyValueSource;
import org.apache.maven.surefire.shared.utils.StringUtils;

import static java.util.Arrays.asList;
import static java.util.Map.Entry;

/**
 * A {@link Properties} implementation that preserves insertion order.
 */
public class SurefireProperties extends Properties implements KeyValueSource {
    private static final Collection<String> KEYS_THAT_CANNOT_BE_USED_AS_SYSTEM_PROPERTIES =
            asList("java.library.path", "file.encoding", "jdk.map.althashing.threshold", "line.separator");

    private final LinkedHashSet<Object> items = new LinkedHashSet<>();

    public SurefireProperties() {}

    public SurefireProperties(Properties source) {
        if (source != null) {
            putAll(source);
        }
    }

    public SurefireProperties(KeyValueSource source) {
        if (source != null) {
            source.copyTo(this);
        }
    }

    @Override
    public synchronized void putAll(Map<?, ?> t) {
        putAllInternal(t);
    }

    private Collection<String> putAllInternal(Map<?, ?> source) {
        Collection<String> overwrittenProperties = new LinkedList<>();
        for (Entry<?, ?> entry : source.entrySet()) {
            if (put(entry.getKey(), entry.getValue()) != null) {
                overwrittenProperties.add(entry.getKey().toString());
            }
        }
        return overwrittenProperties;
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        items.add(key);
        return super.put(key, value);
    }

    @Override
    public synchronized Object remove(Object key) {
        items.remove(key);
        return super.remove(key);
    }

    @Override
    public synchronized void clear() {
        items.clear();
        super.clear();
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(items);
    }

    /**
     * Copies all keys and values from source to these properties, overwriting existing properties with same name
     * @param source
     * @return all overwritten property names (may be empty if there was no property name clash)
     */
    public Collection<String> copyPropertiesFrom(Properties source) {
        if (source != null) {
            return putAllInternal(source);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Copies all keys and values from source to these properties, overwriting existing properties with same name
     * @param source
     * @return all overwritten property names (may be empty if there was no property name clash)
     */
    public Collection<String> copyPropertiesFrom(Map<String, String> source) {
        return copyProperties(this, source);
    }

    public Iterable<Object> getStringKeySet() {
        return keySet();
    }

    public Set<Object> propertiesThatCannotBeSetASystemProperties() {
        Set<Object> result = new HashSet<>();
        for (Object key : getStringKeySet()) {
            //noinspection SuspiciousMethodCalls
            if (KEYS_THAT_CANNOT_BE_USED_AS_SYSTEM_PROPERTIES.contains(key)) {
                result.add(key);
            }
        }
        return result;
    }

    public void copyToSystemProperties() {
        for (Object o : items) {
            String key = (String) o;
            String value = getProperty(key);
            System.setProperty(key, value);
        }
    }

    private static Collection<String> copyProperties(Properties target, Map<String, String> source) {
        Collection<String> overwrittenProperties = new LinkedList<>();
        if (source != null) {
            for (String key : source.keySet()) {
                String value = source.get(key);
                if (target.setProperty(key, value == null ? "" : value) != null) {
                    overwrittenProperties.add(key);
                }
            }
        }
        return overwrittenProperties;
    }

    @Override
    public void copyTo(Map<Object, Object> target) {
        target.putAll(this);
    }

    public void setProperty(String key, File file) {
        if (file != null) {
            setProperty(key, file.toString());
        }
    }

    public void setProperty(String key, Boolean aBoolean) {
        if (aBoolean != null) {
            setProperty(key, aBoolean.toString());
        }
    }

    public void setProperty(String key, int value) {
        setProperty(key, String.valueOf(value));
    }

    public void setProperty(String key, Long value) {
        if (value != null) {
            setProperty(key, value.toString());
        }
    }

    public void addList(List<?> items, String propertyPrefix) {
        if (items != null && !items.isEmpty()) {
            int i = 0;
            for (Object item : items) {
                if (item == null) {
                    throw new NullPointerException(propertyPrefix + i + " has null value");
                }

                String[] stringArray = StringUtils.split(item.toString(), ",");

                for (String aStringArray : stringArray) {
                    setProperty(propertyPrefix + i, aStringArray);
                    i++;
                }
            }
        }
    }

    public void setClasspath(String prefix, Classpath classpath) {
        List<String> classpathElements = classpath.getClassPath();
        for (int i = 0; i < classpathElements.size(); ++i) {
            String element = classpathElements.get(i);
            setProperty(prefix + i, element);
        }
    }

    private static SurefireProperties loadProperties(InputStream inStream) throws IOException {
        try (InputStream surefirePropertiesStream = inStream) {
            Properties p = new Properties();
            p.load(surefirePropertiesStream);
            return new SurefireProperties(p);
        }
    }

    public static SurefireProperties loadProperties(File file) throws IOException {
        return file == null ? new SurefireProperties() : loadProperties(new FileInputStream(file));
    }

    public void setNullableProperty(String key, String value) {
        if (value != null) {
            super.setProperty(key, value);
        }
    }
}
