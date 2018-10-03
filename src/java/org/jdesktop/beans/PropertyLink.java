/* $Id$
 *
 * Released under Gnu Public License
 * Copyright © 2011 Michael G. Binz
 */

package org.jdesktop.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jdesktop.smack.util.JavaUtils;




/**
 * Links a bound property on a source object to a bound property on
 * a target object.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class PropertyLink
{
    private final String _propertySrcName;

    private final PropertyProxy<Object,Object> _targetProperty;

    private final PropertyAdapter _pa;

    /**
     * Creates a property update link between the source and target.
     *
     * @param source A source property. Changes on this are propagated to the
     * target object.
     * @param target The target property.
     */
    public PropertyLink(
            PropertyType<?, ?> source,
            PropertyType<?, ?> target )
    {
        this(
                source.getBean(),
                source.getName(),
                target.getBean(),
                target.getName() );
    }

    /**
     * Creates a property update link between the source and target.
     * The property is expected to exist on both objects.
     *
     * @param source The source object. Changes on this are propagated to the
     * target object.
     * @param propName The name of source and target property.
     * @param target The target object.
     */
    public PropertyLink(
            Object source,
            String propName,
            Object target )
    {
        this( source, propName, target, propName );
    }

    /**
     * Creates a property update link between the source and target.
     *
     * @param source The source object. Changes on this are propagated to the
     * target object.
     * @param propSrcName The name of the source property.
     * @param target The target object.
     * @param propTgtName The name of the target property.
     */
    public PropertyLink(
            Object source,
            String propSrcName,
            Object target,
            String propTgtName )
    {
        _propertySrcName = propSrcName;

        _pa =
            new PropertyAdapter( source );

        _pa.addPropertyChangeListener( _listener );

        _targetProperty =
            new PropertyProxy<Object,Object>( propTgtName, target );
    }

    /**
     * Allows to manually trigger a property update.
     *
     * @return The PropertyLink for chained calls.
     */
    public PropertyLink update()
    {
        PropertyProxy<Object, Object> sourceProperty =
                new PropertyProxy<Object, Object>( _propertySrcName, _pa.getBean() );

        _targetProperty.set( sourceProperty.get() );

        return this;
    }

    /**
     * Remove the internal listener registrations.  This is only needed if the
     * linked beans have a different life cycle.
     */
    public void dispose()
    {
        _pa.removePropertyChangeListener( _listener );
    }

    /**
     * A listener for source changes.
     */
    private final PropertyChangeListener _listener = new PropertyChangeListener()
    {
        @Override
        public void propertyChange( PropertyChangeEvent evt )
        {
            // Ignore change events for other properties.
            if ( ! _propertySrcName.equals( evt.getPropertyName() ) )
                return;

            Object newValue = evt.getNewValue();

            // If the new value and the value on the target are already the
            // same we ignore the call.
            if ( JavaUtils.equals( _targetProperty.get(), newValue ) )
                return;

            // Set the value.
            _targetProperty.set( newValue );
        }
    };
}
