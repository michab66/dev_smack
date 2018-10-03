/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2016 Michael G. Binz
 */
package org.jdesktop.util;

import java.util.HashMap;
import java.util.Map;

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
    private ServiceManager()
    {
        throw new AssertionError();
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
        {
            try
            {
                _singletons.put(
                        singletonType,
                        ReflectionUtil.createInstanceX( singletonType ) );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }

        return singletonType.cast( _singletons.get( singletonType ) );
    }

    /**
     * Get an application service of the specified type.
     *
     * @param singletonInstance The application service.
     * @return An instance of the requested service.
     */
    public static synchronized <T> T initApplicationService( T singletonInstance )
    {
        @SuppressWarnings("unchecked")
        Class<T> c = (Class<T>)singletonInstance.getClass();

        return initApplicationService( c, singletonInstance );
    }

    /**
     * Initialize an application service with a specific instance.
     *
     * @param clazz The service class.
     * @param singletonInstance The instance to use. The instance has to be assignable
     * to the passed class.
     * @return A service instance.
     */
    public static synchronized <T> T initApplicationService( Class<T> clazz, T singletonInstance )
    {
        if ( ! clazz.isAssignableFrom( singletonInstance.getClass() ) )
            throw new ClassCastException();

        if ( _singletons.containsKey( clazz ) )
            throw new IllegalArgumentException(
                    "Already initialized: " +
                    _singletons.get( clazz ) );

        _singletons.put( clazz, singletonInstance );

        return singletonInstance;
    }
}
