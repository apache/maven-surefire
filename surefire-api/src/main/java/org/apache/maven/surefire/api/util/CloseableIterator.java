package org.apache.maven.surefire.api.util;

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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This iterator is marked as stopped if {@link #isClosed()} returns {@code true}.
 * If the iterator has been closed before calling {@link #hasNext()} then the method returns {@code false}.
 * If the iterator was closed after {@link #hasNext() hasNext returns true} but before {@link #next()}, the
 * method {@link #next()} throws {@link java.util.NoSuchElementException}.
 * The method {@link #remove()} throws {@link IllegalStateException} if the iterator has been closed.
 *
 * @param <T> the type of elements returned by this iterator
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19.1
 */
public abstract class CloseableIterator<T>
        implements Iterator<T>
{
    private Boolean finishCurrentIteration;

    protected abstract boolean isClosed();
    protected abstract boolean doHasNext();
    protected abstract T doNext();
    protected abstract void doRemove();

    @Override
    public boolean hasNext()
    {
        popMarker();
        return !finishCurrentIteration && doHasNext();
    }

    @Override
    public T next()
    {
        try
        {
            if ( popMarker() && finishCurrentIteration )
            {
                throw new NoSuchElementException( "iterator closed" );
            }
            return doNext();
        }
        finally
        {
            finishCurrentIteration = null;
        }
    }

    @Override
    public void remove()
    {
        try
        {
            if ( popMarker() && finishCurrentIteration )
            {
                throw new IllegalStateException( "iterator closed" );
            }
            doRemove();
        }
        finally
        {
            finishCurrentIteration = null;
        }
    }

    /**
     * @return {@code true} if marker changed from NULL to anything
     */
    private boolean popMarker()
    {
        if ( finishCurrentIteration == null )
        {
            finishCurrentIteration = isClosed();
            return true;
        }
        return false;
    }
}
