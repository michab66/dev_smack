package org.jdesktop.util;

import java.util.WeakHashMap;
import java.util.function.Function;

/**
 *
 *
 * @version $Revision$
 * @author Michael Binz
 */
class WeakMapWithProducer<K,V>
{
    private final WeakHashMap<K, V> _cache =
            new WeakHashMap<>();

    private final Function<K, V> _factory;

    public WeakMapWithProducer( Function<K, V> factory )
    {
        _factory = factory;
    }

    V get( K key )
    {
        V result = _cache.get( key );

        if ( result == null )
        {
            result = _factory.apply( key );
            _cache.put( key, result );
        }

        return result;
    }
}
