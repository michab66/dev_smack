/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2011 Michael G. Binz
 */
package org.jdesktop.smack.util;

/**
 * General utilities.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class JavaUtils
{
    private JavaUtils()
    {
        throw new AssertionError();
    }

    /**
     * Compare two objects, handle {@code null}.
     *
     * @param a The first object to compare.
     * @param b The second object to compare.
     * @return {@code true} if the objects are equal.  Note that this includes
     * that both parameters were {@code null}.
     */
    public static boolean equals( Object a, Object b )
    {
        if ( a == null )
            return b == null;
        return a.equals( b );
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
