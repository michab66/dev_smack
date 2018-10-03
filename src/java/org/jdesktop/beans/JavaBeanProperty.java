/* $Id$
 *
 * Laboratory.
 *
 * Released under Gnu Public License
 * Copyright © 2015 Michael G. Binz
 */
package org.jdesktop.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdesktop.smack.util.JavaUtils;

/**
 * Support class for implementing java bean properties on components.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class JavaBeanProperty<T,B> implements PropertyType<T,B>
{
    private String _name;
    private T _value;
    private B _bean;
    private Class<T> _beantype;

    private final PropertyAdapter _pa;

    /**
     * Create an instance.
     *
     * @param bean The target bean instance.
     * @param initialValue
     * @param propertyName
     */
    public JavaBeanProperty( B bean, T initialValue, String propertyName )
    {
        // This internally validates if the host offers the required
        // set and get operations.
        _beantype = new PropertyProxy<T,B>( propertyName, bean ).getType();

        // This internally validates if the bean has the pcl operations
        _pa = new PropertyAdapter( bean );

        _bean = bean;
        _value = initialValue;
        _name = propertyName;
    }

    @Override
    public void set( T newValue )
    {
        if ( JavaUtils.equals( _value, newValue ) )
            return;

        T oldValue = _value;
        _value = newValue;

        PropertyChangeEvent evt =
                new PropertyChangeEvent( _bean, _name, oldValue, newValue );

        for ( PropertyChangeListener c : getPcls() )
            c.propertyChange( evt );
    }

    /**
     * Get the listeners to call.
     *
     * @return The listeners to call.
     */
    private List<PropertyChangeListener> getPcls()
    {
        List<PropertyChangeListener> result = new ArrayList<PropertyChangeListener>();

        result.addAll(
            Arrays.asList( _pa.getPropertyChangeListeners( _name ) ) );

        // Get the pcls that were added w/o a name. Since we seem to get all pcls,
        // with and w/o a name, we filter in the loop below.
        // See comment in AbstractBean#getPropertyChangeListeners.
        for ( PropertyChangeListener c : _pa.getPropertyChangeListeners() )
        {
            if ( c instanceof PropertyChangeListenerProxy )
                continue;

            if ( ! result.contains( c ) )
                result.add( c );
        }

        return result;
    }

    @Override
    public T get()
    {
        return _value;
    }

    @Override
    public Class<T> getType()
    {
        return _beantype;
    }

    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public B getBean()
    {
        return _bean;
    }
}
