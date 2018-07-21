package org.apache.maven.surefire.util;

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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 * @since 2018-06-13
 */
final class ClassesShufflerImpl implements ClassesShuffler
{
    private final Randomizer randomizer;

    ClassesShufflerImpl( Randomizer randomizer )
    {
        this.randomizer = randomizer;
    }

    @Override
    public void shuffle( List<Class<?>> classes )
    {
        Collections.sort( classes, new ClassNameComparator() );
        Collections.shuffle( classes, randomizer.getRandom() );
    }

    private static final class ClassNameComparator implements Comparator<Class<?>>
    {
        @Override
        public int compare( Class<?> cls1, Class<?> cls2 )
        {
            return cls1.getName().compareTo( cls2.getName() );
        }
    }
}
