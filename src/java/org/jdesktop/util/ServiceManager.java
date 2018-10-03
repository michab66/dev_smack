/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2016 Michael G. Binz
 */
package org.jdesktop.util;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdesktop.smack.util.ReflectionUtils;

/**
 * Management of ApplicationServices.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public final class ServiceManager
{
    /**
     * The map of singular application services.
     */
    private static final Map<Class<?>, Object> _singletons =
            new HashMap<Class<?>, Object>();

    /**
     * Create an instance.
     */
    public ServiceManager()
    {
        // Catch ctor.
    }

    /**
     * Get an application service of the specified type.
     *
     * @param singletonType The type of the application service.
     * @return An instance of the requested service.
     */
    public static synchronized <T> T getApplicationService( Class<T> singletonType )
    {
        if ( !  _singletons.containsKey( singletonType ) )
            try
            {
                _singletons.put(
                        singletonType,
                        singletonType.newInstance() );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }

        return singletonType.cast( _singletons.get( singletonType ) );
    }

    /**
     * Get an application service of the specified type.
     *
     * @param singletonType The type of the application service.
     * @return An instance of the requested service.
     */
    public static synchronized <T> T initApplicationService(
            Class<T> singletonType,
            Object...objects  )
    {
        if ( _singletons.containsKey(  singletonType ) )
            throw new IllegalArgumentException( "Already initialized: " + singletonType.getName() );

        Constructor<T> c = ReflectionUtils.matchConstructorArguments(
                singletonType.getConstructors(),
                objects );

        if ( c == null )
            throw new IllegalArgumentException( "No matching constructor found." );

        try
        {
            if ( ! c.isAccessible() )
                c.setAccessible( true );

            T result = c.newInstance( objects );

            _singletons.put( singletonType, result );

            return result;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}
