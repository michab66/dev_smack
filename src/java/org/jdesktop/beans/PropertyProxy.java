/* $Id$
 *
 * Laboratory.
 *
 * Released under Gnu Public License
 * Copyright © 2011 Michael G. Binz
 */
package org.jdesktop.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import org.jdesktop.smack.util.ReflectionUtils;

/**
 * An instance of this class represents a property on the target object
 * in a (runtime) type-safe way.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class PropertyProxy<T,B> implements PropertyType<T,B>
{
    /**
     * The property descriptor of the proxied property.
     */
    private final PropertyDescriptor _targetProperty;



    /**
     * The target object.
     */
    private final B _targetObject;



    /**
     * Create an instance of the proxy and ensures that the property is
     * actually available on the target.  'Available' means that the
     * class of the target object follows the Java Beans conventions.
     *
     * @param propName The name of the property.
     * @param beanInstance The target object. Null not allowed.
     * @throws IllegalArgumentException of the property is not supported
     * by the target object.
     */
    public PropertyProxy( String propName, B beanInstance )
    {
        try
        {
            _targetProperty = new PropertyDescriptor( propName, beanInstance.getClass() );

            if ( _targetProperty.getReadMethod() == null )
                throw new IllegalArgumentException( "target has no read method" );
            if ( _targetProperty.getWriteMethod() == null )
                throw new IllegalArgumentException( "target has no write method" );

            _targetObject = beanInstance;
        }
        catch ( IntrospectionException e )
        {
            throw new IllegalArgumentException( e );
        }
    }

    /**
     * Get the property's type.
     *
     * @return The property's type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getType()
    {
        return (Class<T>)_targetProperty.getPropertyType();
    }

    /**
     * Get the property's value.
     *
     * @return The property's value.
     */
    @Override
    @SuppressWarnings("unchecked")
    public T get()
    {
        return (T)ReflectionUtils.invokeQuiet(
                _targetProperty.getReadMethod(),
                _targetObject );
    }

    /**
     * Set the property's value.
     *
     * @param value The value to set.
     */
    @Override
    public void set( T value )
    {
        ReflectionUtils.invokeQuiet(
                _targetProperty.getWriteMethod(),
                _targetObject,
                value );
    }

    /**
     * Get the property name.
     *
     * @return The property name.
     */
    @Override
    public String getName()
    {
        return _targetProperty.getName();
    }

    /**
     * Returns a string representation of an instance for debug purposes.
     *
     * @return A string representation of an instance for debug purposes.
     */
    @Override
    public String toString()
    {
        StringBuilder result =
            new StringBuilder( _targetObject.getClass().getSimpleName() );
        return result
            .append( '@' )
            .append( _targetProperty.getName() )
            .toString();
    }

    @Override
    public B getBean()
    {
        return _targetObject;
    }
}
