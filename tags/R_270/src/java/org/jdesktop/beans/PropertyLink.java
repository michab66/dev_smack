/* $Id$
 *
 * Released under Gnu Public License
 * Copyright © 2011 Michael G. Binz
 */

package org.jdesktop.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import javafx.beans.property.adapter.ReadOnlyJavaBeanObjectProperty;
import javafx.beans.property.adapter.ReadOnlyJavaBeanObjectPropertyBuilder;

/**
 * Links a bound property on a source object to a bound property on
 * a target object.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class PropertyLink
{
    private final ReadOnlyJavaBeanObjectProperty<Object>
        _sourceProperty;
    private final PropertyProxy<Object,Object>
        _targetProperty;
    private final PropertyAdapter
        _pa;

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
        try
        {
            _sourceProperty = ReadOnlyJavaBeanObjectPropertyBuilder
                    .create()
                    .bean( source )
                    .name( propSrcName )
                    .build();
        }
        catch ( NoSuchMethodException e )
        {
            throw new IllegalArgumentException( propSrcName );
        }


        _pa =
            new PropertyAdapter( source );

        // Note that the javafx bindings use weak listeners.  This is the reason
        // why we can't use them here.  pcl listeners are kept using a strong
        // reference.
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
        _targetProperty.set( _sourceProperty.get() );

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
            if ( ! _sourceProperty.getName().equals( evt.getPropertyName() ) )
                return;

            Object newValue = evt.getNewValue();

            // If the new value and the value on the target are already the
            // same we ignore the call.
            if ( Objects.equals( _targetProperty.get(), newValue ) )
                return;

            // Set the value.
            _targetProperty.set( newValue );
        }
    };

    public static PropertyLink bind(
            Object source,
            String propSrcName,
            Object target,
            String propTgtName )
    {
        return new PropertyLink( source, propSrcName, target, propTgtName );
    }

    public static PropertyLink bind(
            Object source,
            String propSrcName,
            Object target )
    {
        return new PropertyLink( source, propSrcName, target );
    }
}