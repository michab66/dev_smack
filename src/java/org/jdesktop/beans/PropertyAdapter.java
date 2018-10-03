/* $Id$
 *
 * Released under Gnu Public License
 * Copyright © 2011-15 Michael G. Binz
 */

package org.jdesktop.beans;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;

import org.jdesktop.smack.util.ReflectionUtils;

/**
 * Wraps an object that offers a property change listener interface.
 * An object supports this if it offers the operations
 * {@code addPropertyChangeListener( PropertyChangeListener p )} and
 * {@code removePropertyChangeListener( PropertyChangeListener p )}.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class PropertyAdapter
{
    /**
     * The target object.
     */
    private final Object _bean;

    /**
     * The add operation.
     */
    private final Method _addPcl;

    /**
     * The remove operation.
     */
    private final Method _removePcl;

    /**
     * The add with name operation.
     */
    private final Method _addPclNamed;

    /**
     * The remove with name operation.
     */
    private final Method _removePclNamed;

    /**
     * Get all property change listeners.
     */
    private final Method _getPcls;

    /**
     * Get property change listeners for a certain name.
     */
    private final Method _getPclsNamed;

    /**
     * Creates an instance.  The constructor fails with an exception
     * if the passed object does not implement a partial property change
     * listener interface.
     *
     * @param bean The object to adapt.
     */
    public PropertyAdapter( Object bean )
    {
        _bean = bean;

        Class<?> beanClass = bean.getClass();

        int found = 0;

        _addPcl = ReflectionUtils.getMethod(
                beanClass,
                "addPropertyChangeListener",
                PropertyChangeListener.class );
        if ( _addPcl != null )
            found++;

        _addPclNamed = ReflectionUtils.getMethod(
                beanClass,
                "addPropertyChangeListener",
                String.class, PropertyChangeListener.class );
        if ( _addPclNamed != null )
            found++;

        _removePcl = ReflectionUtils.getMethod(
                beanClass,
                "removePropertyChangeListener",
                PropertyChangeListener.class );
        if ( _removePcl != null )
            found++;

        _removePclNamed = ReflectionUtils.getMethod(
                beanClass,
                "removePropertyChangeListener",
                String.class, PropertyChangeListener.class );
        if ( _removePclNamed != null )
            found++;

        _getPcls = ReflectionUtils.getMethod(
                beanClass,
                "getPropertyChangeListeners" );
        if ( _getPcls != null )
            found++;

        _getPclsNamed = ReflectionUtils.getMethod(
                beanClass,
                "getPropertyChangeListeners",
                String.class );
        if ( _getPclsNamed != null )
            found++;

        if ( found == 0 )
            throw new IllegalArgumentException( "Object is not a PropertyChangeSource: " + beanClass );
    }

    /**
     * Adds a <code>PropertyChange</code> listener. Containers and attached
     * components use these methods to register interest in this
     * <code>Action</code> object. When its enabled state or other property
     * changes, the registered listeners are informed of the change.
     *
     * @param listener  a <code>PropertyChangeListener</code> object
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        if ( _addPcl == null )
            throw new NoSuchMethodError();

        ReflectionUtils.invokeQuiet(
                _addPcl,
                _bean,
                listener );
    }



    /**
     * Removes a <code>PropertyChange</code> listener.
     *
     * @param listener  a <code>PropertyChangeListener</code> object
     * @see #addPropertyChangeListener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        if ( _removePcl == null )
            throw new NoSuchMethodError();

        ReflectionUtils.invokeQuiet(
                _removePcl,
                _bean,
                listener );
    }

    /**
     * Adds a <code>PropertyChange</code> listener. Containers and attached
     * components use these methods to register interest in this
     * <code>Action</code> object. When its enabled state or other property
     * changes, the registered listeners are informed of the change.
     *
     * @param propertyName The name of the property.
     * @param listener  a <code>PropertyChangeListener</code> object
     */
    public void addPropertyChangeListener( String propertyName, PropertyChangeListener listener)
    {
        if ( _addPclNamed == null )
            throw new NoSuchMethodError();

        ReflectionUtils.invokeQuiet(
                _addPclNamed,
                _bean,
                propertyName,
                listener );
    }

    /**
     * Removes a <code>PropertyChange</code> listener.
     *
     * @param propertyName The name of the property.
     * @param listener  a <code>PropertyChangeListener</code> object
     * @see #addPropertyChangeListener
     */
    public void removePropertyChangeListener( String propertyName, PropertyChangeListener listener)
    {
        if ( _removePclNamed == null )
            throw new NoSuchMethodError();

        ReflectionUtils.invokeQuiet(
                _removePclNamed,
                _bean,
                propertyName,
                listener );
    }

    public PropertyChangeListener[] getPropertyChangeListeners( String propertyName )
    {
        if ( _getPclsNamed == null )
            throw new NoSuchMethodError();

        return (PropertyChangeListener[])ReflectionUtils.invokeQuiet(
                _getPclsNamed,
                _bean,
                propertyName );
    }

    public PropertyChangeListener[] getPropertyChangeListeners()
    {
        if ( _getPcls == null )
            throw new NoSuchMethodError();

        return (PropertyChangeListener[])ReflectionUtils.invokeQuiet(
                _getPcls,
                _bean );
    }

    /**
     * Get the adapted bean object.
     *
     * @return The adapted bean object.
     */
    public Object getBean()
    {
        return _bean;
    }
}
