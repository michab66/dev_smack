/* $Id: ReflectionUtil.java 252 2016-10-03 11:32:23Z michab66 $
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2010 Michael G. Binz
 */
package org.jdesktop.util;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A one-to-n relationship. Allows to customize the used map
 * implementation as well as the conrete to-n holder.
 *
 * @version $Id$
 * @author micbinz
 *
 * @param <K> The key type.
 * @param <V> The value type.
 * @param <C> A container factory.
 */
public class  OneToN<K,V,C extends Collection<V> >
{
    private final AbstractMap<K,C> _map;

    private final Supplier<C> _supplier;

    /**
     * Create an instance.
     *
     * @param s A factory for the to-n collection.
     */
    public OneToN(
            Supplier<AbstractMap<K,C>> m,
            Supplier<C> s )
    {
        _map =
                m.get();
        _supplier =
                s;
    }

    /**
     * Create an instance.
     *
     * @param s A factory for the to-n collection.
     */
    public OneToN( Supplier<C> s )
    {
        this( HashMap<K, C>::new, s );
    }

    /**
     * Put a single value into the collection for the passed key.
     *
     * @param key The key.
     * @param value The value to add.
     */
    public void putValue( K key, V value )
    {
        C c = _map.get( key );

        if ( c == null )
            c = _supplier.get();

        c.add( value );

        _map.put( key, c );
    }

    /**
     * Put n values for the passed key into the collection.
     *
     * @param key The key.
     * @param values The values to add.
     */
    public void putValues( K key, C values )
    {
        values.forEach( v -> putValue( key, v ) );
    }

    /**
     * For each key put values into the collection.
     *
     * @param keys The key set.
     * @param values The values to add.
     */
    public void putValues( Collection<K> keys, C values )
    {
        keys.forEach( k -> putValues( k, values ) );
    }

    /**
     * Remove the value for the passed key.
     *
     * @param key The key.
     * @param value The value to remove.
     */
    public void removeValue( K key, V value )
    {
        Collection<V> c = _map.get( key );

        if ( c == null )
            return;

        c.remove( value );
    }

    /**
     * @param key The key.
     * @return The values associated with the key.  If no values are available
     * for the key an empty collection is returned.
     */
    public C getValues( K key )
    {
        C result = _map.get( key );

        // If we have no result, we return an empty collection.
        if ( result == null )
            result = _supplier.get();

        return result;
    }

    /**
     * @return The keys in the map.
     */
    public Set<K> keySet()
    {
        return _map.keySet();
    }
}
