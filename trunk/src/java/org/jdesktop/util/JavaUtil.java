/* $Id: JavaUtils.java 323 2017-06-13 20:56:07Z michab66 $
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2011 Michael G. Binz
 */
package org.jdesktop.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * General utilities.
 *
 * @version $Rev: 323 $
 * @author Michael Binz
 */
public class JavaUtil
{
    private static final Logger LOG =
            Logger.getLogger( JavaUtil.class.getName() );

    private JavaUtil()
    {
        throw new AssertionError();
    }

    public static void Assert( boolean condition, String message )
    {
        if ( condition )
            return;
        throw new AssertionError( message );
    }

    public static void Assert( boolean condition )
    {
        if ( condition )
            return;
        throw new AssertionError();
    }

    /**
     * Sleep for an amount of milliseconds or until interrupted.
     * @param millis The time to sleep.
     */
    public void sleep( long millis )
    {
        try
        {
            Thread.sleep( millis );
        }
        catch ( InterruptedException ignore )
        {
        }
    }

    public interface Fx
    {
        void call()
            throws Exception;
    }

    /**
     * Calls the passed operation, ignoring an exception.
     * @param f The operation to call.
     */
    public void force( Fx operation )
    {
        try
        {
            operation.call();
        }
        catch ( Exception e )
        {
            LOG.log( Level.INFO, "Force exception.", e );
        }
    }


    /**
     * Test if an array is empty.
     *
     * @param array The array to test. {@code null} is allowed.
     * @return {@code true} if the array is not null and has a length greater
     * than zero.
     */
    public static <T> boolean isEmptyArray( T[] array )
    {
        return array == null || array.length == 0;
    }
}
