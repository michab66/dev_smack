/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2012 Michael G. Binz
 */

package org.jdesktop.smack.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Collection utilities.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class CollectionUtils
{
    /**
     * Creates the intersection of the passed sets.
     *
     * @param a The first operand for the intersection.
     * @param b The second operand for the intersection.
     * @return A newly allocated set containing the intersections of
     * the passed sets.
     */
    public static <T> Set<T> intersection( Set<T> a, Set<T> b )
    {
        Set<T> result = new HashSet<T>( a );
        result.retainAll( b );
        return result;
    }

    /**
     * Convert a collection to an array.
     * @param p The collection to convert.
     * @param clazz The element type.
     * @return A newly allocated array of element type clazz.
     */
    @SuppressWarnings("unchecked")
    static <T> T[] toArray( Collection<T> p, Class<T> clazz )
    {
        return p.toArray(
                (T[]) Array.newInstance( clazz, p.size() ) );
    }



    private CollectionUtils()
    {
        throw new AssertionError();
    }
}
