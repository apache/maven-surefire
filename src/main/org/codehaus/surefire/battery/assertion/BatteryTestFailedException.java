/*
 * Copyright (C) 2001-2003 Artima Software, Inc. All rights reserved.
 * Licensed under the Open Software License version 1.0.
 *
 * A copy of the Open Software License version 1.0 is available at:
 *     http://www.artima.com/surefire/osl10.html
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Artima Software, Inc. For more
 * information on Artima Software, Inc., please see:
 *     http://www.artima.com/
 */
package org.codehaus.surefire.battery.assertion;

/**
 * Exception that indicates a test failed.
 *
 * @author Bill Venners
 */
public class BatteryTestFailedException extends RuntimeException
{

    private boolean causeInitialized;
    private Throwable cause;

    /**
     * Create a <code>TestFailedException</code> with no detail message.
     */
    public BatteryTestFailedException()
    {

        super();
    }

    /**
     * Create a <code>TestFailedException</code> with a detail message.
     *
     * @param message A detail message for this <code>TestFailedException</code>, or
     *     <code>null</code>. If <code>null</code> is passed, the <code>getMessage</code>
     *     method will return an empty <code>String</code>.
     */
    public BatteryTestFailedException( String message )
    {

        super( message );
    }

    /**
     * Create a <code>TestFailedException</code> with the specified detail
     * message and cause.
     *
     * <p>Note that the detail message associated with cause is
     * <em>not</em> automatically incorporated in this throwable's detail
     * message.
     *
     * @param message A detail message for this <code>TestFailedException</code>, or <code>null</code>.
     * @param cause the cause, which is saved for later retrieval by the <code>getCause</code> method.
     *     (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public BatteryTestFailedException( String message, Throwable cause )
    {

        super( message );

        this.cause = cause;
        causeInitialized = true;
    }

    /**
     * Create a <code>TestFailedException</code> with the specified cause.  The
     * <code>getMessage</code> method of this exception object will return
     * <code>(cause == null ? "" : cause.toString())</code>.
     */
    public BatteryTestFailedException( Throwable cause )
    {

        super( cause == null ? "" : cause.toString() );

        this.cause = cause;
        causeInitialized = true;
    }

    /**
     * Returns the cause of this <code>TestFailedException</code>, or <code>null</code> if
     * the cause is nonexistent or unknown. (The cause is the <code>Throwable</code> that caused
     * this <code>TestFailedException</code> to get thrown.)
     */
    public Throwable getCause()
    {
        return cause;
    }

    /**
     * Initializes the cause of this <code>TestFailedException</code> to the specified value.
     * (The cause is the <code>Throwable</code> that caused this <code>TestFailedException</code>
     * to get thrown.)
     *
     * This method can be called at most once. It is generally called immediately after creating
     * the <code>TestFailedException</code>. If this <code>TestFailedException</code> was created
     * with <code>TestFailedException(Throwable)</code> or
     * <code>TestFailedException(String, Throwable)</code>, this method cannot be called even once.
     *
     * @param cause the cause (which is saved for later retrieval by the <code>getCause()</code>
     *     method). A <code>null</code> value is permitted, and indicates that the cause is
     *     nonexistent or unknown.)
     * @throws java.lang.IllegalArgumentException - if cause is this <code>TestFailedException</code>. (A
     *     <code>TestFailedException</code> cannot be its own cause.)
     * @throws java.lang.IllegalStateException if this <code>TestFailedException</code> was created
     *     with <code>TestFailedException(Throwable)</code> or
     *     <code>TestFailedException(String, Throwable)</code>, or this method has already been
     *     called on this <code>TestFailedException</code>.
     */
    public synchronized Throwable initCause( Throwable cause )
    {

        if ( cause != null && cause == this )
        {
            throw new IllegalArgumentException();
        }

        if ( causeInitialized )
        {
            throw new IllegalStateException();
        }

        this.cause = cause;
        causeInitialized = true;
        return this;
    }
}
