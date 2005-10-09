package org.codehaus.surefire;

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

/**
 * @author <a href="mailto:andyglick@acm.org">Andy Glick</a>
 * @version $Id$
 */
public class SurefireBooterForkException extends Exception
{
  /**
   * Constructs a new exception with <code>null</code> as its detail message.
   * The cause is not initialized, and may subsequently be initialized by a call
   * to {@link #initCause}.
   */
  public SurefireBooterForkException()
  {
  }

  /**
   * Constructs a new exception with the specified detail message.  The cause is
   * not initialized, and may subsequently be initialized by a call to {@link
   * #initCause}.
   *
   * @param message the detail message. The detail message is saved for later
   *                retrieval by the {@link #getMessage()} method.
   */
  public SurefireBooterForkException(String message)
  {
    super(message);
  }

  /**
   * Constructs a new exception with the specified cause and a detail message of
   * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains
   * the class and detail message of <tt>cause</tt>). This constructor is useful
   * for exceptions that are little more than wrappers for other throwables (for
   * example, {@link java.security.PrivilegedActionException}).
   *
   * @param cause the cause (which is saved for later retrieval by the {@link
   *              #getCause()} method).  (A <tt>null</tt> value is permitted,
   *              and indicates that the cause is nonexistent or unknown.)
   */
  public SurefireBooterForkException(Throwable cause)
  {
    super(cause);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   * <p>Note that the detail message associated with <code>cause</code> is
   * <i>not</i> automatically incorporated in this exception's detail message.
   *
   * @param message the detail message (which is saved for later retrieval by
   *                the {@link #getMessage()} method).
   * @param cause   the cause (which is saved for later retrieval by the {@link
   *                #getCause()} method).  (A <tt>null</tt> value is permitted,
   *                and indicates that the cause is nonexistent or unknown.)
   */
  public SurefireBooterForkException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
