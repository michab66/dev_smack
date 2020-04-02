/* $Id$
 *
 * Utilities
 *
 * Released under Gnu Public License
 * Copyright (c) 2008 Michael G. Binz
 */
package org.smack.util;

/**
 * An exception that offers constructors that can handle string
 * formatting.
 *
 * @author micbinz
 */
@SuppressWarnings("serial")
public class SmackException extends Exception
{
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param   message   the detail message. The detail message is saved for
     *          later retrieval by the {@link #getMessage()} method.
     */
    public SmackException( String message )
    {
        super( message );
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public SmackException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Creates an exception with a formatted message.
     *
     * @param message The format specifier.
     * @param args The format arguments
     */
    public SmackException( String message, Object... args )
    {
        super( String.format( message, args ) );
    }

    /**
     * Creates an exception with a formatted message.
     *
     * @param cause The cause of this exception.
     * @param message The format specifier.
     * @param args The format arguments
     */
    public SmackException( Throwable cause, String message, Object... args )
    {
        super( String.format( message, args ), cause );
    }
}
