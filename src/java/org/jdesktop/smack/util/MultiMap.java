/* $Id$
 *
 * Released under Gnu Public License
 * Copyright © 2008-2015 Michael G. Binz
 */
package org.jdesktop.smack.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



/**
 * A double key map.  The map allows to store a single value for key pairs.
 *
 * @version $Revision$
 * @author Michael Binz
 * @deprecated Use org.jdesktop.util.MultiMap
 */
@Deprecated
public class MultiMap<KT1, KT2, VT>
{
    /**
     * The main map.  This contains for each K1 a secondary map mapping K2 to
     * its corresponding value.
     */
    private final Map<KT1, Map<KT2, VT>> _primaryMap =
        new HashMap< KT1, Map<KT2, VT>>();

    /**
     * Put a value into the map.  No argument must be null.
     *
     * @param k1 The first key.
     * @param k2 The second key.
     * @param pValue The value.
     * @throws NullPointerException If one of the arguments was null.
     */
    public VT put( KT1 k1, KT2 k2, VT pValue )
    {
        if ( pValue == null )
            throw new NullPointerException( "null value." );

        Map<KT2, VT> secondaryMap;

        if ( _primaryMap.containsKey( k1 ) )
        {
            // If the key was contained, we get a reference on the
            // existing map.
            secondaryMap = _primaryMap.get( k1 );
        }
        else
        {
            // If the key was not contained, we create the sub map
            // and add it to our main map.
            secondaryMap = new HashMap<KT2, VT>();
            _primaryMap.put( k1, secondaryMap );
        }

        return secondaryMap.put( k2, pValue );
    }

    /**
     * Remove a key pair from the map.
     *
     * @param k1 The primary key.
     * @param k2 The secondary key.
     * @return The removed value. If there was no value in the
     * map for the passed keys then {@code null} is returned.
     */
    public VT remove( KT1 k1, KT2 k2 )
    {
        if ( ! _primaryMap.containsKey( k1 ) )
            return null;

        Map<KT2,VT> secondaryMap = _primaryMap.get( k1 );

        VT result = secondaryMap.remove( k2 );

        if ( secondaryMap.isEmpty() )
            _primaryMap.remove( secondaryMap );

        return result;
    }

    /**
     * Get a value from the map.
     *
     * @param k1 The primary key.
     * @param k2 The secondary key.
     * @return The value or {@code null} if no value is available for the key
     * pair.
     */
    public VT get( KT1 k1, KT2 k2 )
    {
        if ( ! _primaryMap.containsKey( k1 ) )
            return null;

        Map<KT2, VT> _secondaryMap = _primaryMap.get( k1 );

        if ( ! _secondaryMap.containsKey( k2 ) )
            return null;

        return _secondaryMap.get( k2 );
    }



    /**
     * Get all values for the passed primary key.
     *
     * @param k1 The primary key to look up.
     * @return A map holding the secondary key/value mappings.  This map is
     * empty if none are found.  Null is never returned.
     */
    public Map<KT2, VT> getAll( KT1 k1 )
    {
        if ( ! _primaryMap.containsKey( k1 ) )
            return Collections.emptyMap();

        return Collections.unmodifiableMap( _primaryMap.get( k1 ) );
    }



    /**
     * Get the set of primary keys.
     *
     * @return The set of primary keys.
     */
    public Set<KT1> getPrimaryKeys()
    {
        return Collections.unmodifiableSet( _primaryMap.keySet() );
    }

    /**
     * Get all values from this map.
     *
     * @return All contained values in a newly allocated map.
     */
    public Set<VT> getValues()
    {
        Set<VT> result = new HashSet<VT>();

        for ( KT1 c : getPrimaryKeys() )
            result.addAll( getAll( c ).values() );

        return result;
    }

    /**
     * Remove all values from this map.
     */
    public void clear()
    {
        _primaryMap.clear();
    }
}
