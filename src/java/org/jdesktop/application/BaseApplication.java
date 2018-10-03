/* $Id$
 *
 * Smack Application.
 *
 * Released under Gnu Public License
 * Copyright © 2015 Michael G. Binz
 */
package org.jdesktop.application;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdesktop.beans.AbstractBeanEdt;

/**
 * A raw base application.  Offers management of ApplicationServices.
 *
 * @version $Rev$
 * @author Michael Binz
 */
class BaseApplication extends AbstractBeanEdt
{
    /**
     * The map of singular application services.
     */
    private final Map<Class<?>, Object> _singletons =
            new HashMap<Class<?>, Object>();

    /**
     * Create an instance.
     */
    public BaseApplication()
    {
        // Catch ctor.
    }

    /**
     * Get an application service of the specified type.
     *
     * @param singletonType The type of the application service.
     * @return An instance of the requested service.
     */
    public synchronized <T> T getApplicationService( Class<T> singletonType )
    {
        if ( !  _singletons.containsKey( singletonType ) )
            try
            {
                _singletons.put(
                        singletonType,
                        makeSingletonInstance( singletonType ) );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }

        return singletonType.cast( _singletons.get( singletonType ) );
    }

    private <T> Object makeSingletonInstance( Class<T> singletonType )
        throws Exception
    {
        // First check if we have a constructor accepting an application instance.
        Constructor<T> ctor = getConstructor( singletonType, Application.class );
        if ( ctor != null )
            return ctor.newInstance( this );

        // Fall back to the default constructor.
        return singletonType.newInstance();
    }

    private <T> Constructor<T> getConstructor( Class<T> klass, Class<?> ... parameterTypes  )
    {
        try
        {
            return klass.getDeclaredConstructor( parameterTypes );
        }
        catch ( Exception e )
        {
            // Ignored.
        }

        return null;
    }
}
