/* $Id$
 *
 * Unpublished work.
 * Copyright © 2016 Michael G. Binz
 */
package org.jdesktop.beans;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.util.StringConverter;

import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationProperties;
import org.jdesktop.beans.JavaBeanProperty;

/**
 * A JavaBean property that uses ApplicationProperties for persistence.
 *
 * @param <T> The property's type.
 * @param <B> The bean the property is part of.
 *
 * @version $Rev$
 * @author Michael G. Binz
 */
public class PersistentJavaBeanProperty<T,B> extends JavaBeanProperty<T,B>
{
    private static final Logger LOG =
            Logger.getLogger( PersistentJavaBeanProperty.class.getName() );

    private Application _app;

    private final StringConverter<T> _converter;

    /**
     * Create an instance.
     *
     * @param bean A reference to our bean.
     * @param initialValue The property's initial value.
     * @param propertyName The name of the property.
     * @param converter A string converter for the property type.
     */
    public PersistentJavaBeanProperty(
            B bean,
            T initialValue,
            String propertyName,
            StringConverter<T> converter )
    {
        super( bean, initialValue, propertyName );

        try
        {
            // If the passed object is an application, then this
            // means we are an application property defined on the
            // application object. In this case the .getInstance
            // call is not working. By using the passed object
            // we allow persistent java bean properties defined
            // on the application object using inline initialization.
            // This is cool, cool, cool...
            _app = (Application)bean;
        }
        catch ( ClassCastException e )
        {
            _app = Application.getInstance();
        }

        _converter = converter;

        String value =
                getAps().get( bean.getClass(), propertyName, null );

        if ( value != null )
        {
            try
            {
                super.set( converter.fromString( value ) );
                // All worked, object is initialized, we're done.
                return;
            }
            catch ( Exception e )
            {
                // Could not convert what we found in persistence.
                // Continue and re-initialize...
                LOG.log( Level.WARNING, propertyName, e );
            }
        }

        // Either the value was not in the persistence or what was
        // found there could not be converted. Initialize the entry
        // now.
        set( initialValue );
    }

    @Override
    public void set( T newValue )
    {
        // Put the value into the persistence.
        getAps().put(
                getBean().getClass(),
                getName(),
                _converter.toString( newValue ) );

        super.set( newValue );
    }

    private ApplicationProperties getAps()
    {
        return _app.getApplicationService(
                ApplicationProperties.class );
    }
}
