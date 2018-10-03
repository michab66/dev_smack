/* $Id$
 *
 * Released under Gnu Public License
 * Copyright © 2011 Michael G. Binz
 */

package org.jdesktop.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

/**
 * Links properties of all types to a boolean target.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class PropertyLinkToBoolean
{
    private final String _propertySrcName;

    private final PropertyProxy<Object,Object> _targetProperty;

    private final PropertyAdapter _pa;

    private final boolean _invert;

    /**
     * Creates a property update link between the source and target.
     *
     * @param source The source object. Changes on this are propagated to the
     * target object.
     * @param propSrcName The name of the source property.
     * @param target The target object.
     * @param propTgtName The name of the target property.
     */
    public PropertyLinkToBoolean(
            Object source,
            String propSrcName,
            Object target,
            String propTgtName )
    {
        this( source, propSrcName, target, propTgtName, false );
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
    public PropertyLinkToBoolean(
            Object source,
            String propSrcName,
            Object target,
            String propTgtName,
            boolean invert )
    {
        _invert = invert;

        _targetProperty =
                new PropertyProxy<Object,Object>( propTgtName, target );

        if ( ! Boolean.TYPE.equals( _targetProperty.getType() ) )
            throw new IllegalArgumentException( "Supports only boolean primitves." );

        _propertySrcName = propSrcName;

        _pa =
            new PropertyAdapter( source );

        _pa.addPropertyChangeListener( _listener );
    }

    /**
     * Allows to manually trigger a property update.
     *
     * @return The PropertyLink for chained calls.
     */
    public PropertyLinkToBoolean update()
    {
        PropertyProxy<Object, Object> sourceProperty =
                new PropertyProxy<Object, Object>( _propertySrcName, _pa.getBean() );

        Object newValue = sourceProperty.get();

        _listener.propertyChange(
                new PropertyChangeEvent(
                        _pa.getBean(),
                        _propertySrcName,
                        newValue,
                        newValue ) );

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
     * Allows to override the target value creation.
     * <p>The default implementation returns true if the passed object is
     * not null or is a number that is not equal to zero.</p>
     * <p>Note that the value returned by this operation is
     * inverted by the base class if this was specified in the
     * constructor.
     *
     * @param newValue The value that was set on the source property.
     */
    protected boolean computeTargetValue( Object newValue )
    {
        try
        {
            Number n =
                    (Number)newValue;
            return n.intValue() != 0;
        }
        catch ( ClassCastException e )
        {
            return newValue != null;
        }
    }

    private void handleChange( PropertyChangeEvent evt )
    {
        // Ignore change events for other properties.
        if ( ! _propertySrcName.equals( evt.getPropertyName() ) )
            return;

        boolean newValue = computeTargetValue( evt.getNewValue() );

        if ( _invert )
            newValue = ! newValue;

        Boolean newBooleanTargetValue =
                Boolean.valueOf( newValue );

        // If the new value and the value on the target are already the
        // same we ignore the call.
        if ( Objects.equals( _targetProperty.get(), newBooleanTargetValue ) )
            return;

        // Set the value.
        _targetProperty.set( newBooleanTargetValue );
    }

    /**
     * A listener for source changes.
     */
    private final PropertyChangeListener _listener = new PropertyChangeListener()
    {
        @Override
        public void propertyChange( PropertyChangeEvent evt )
        {
            handleChange( evt );
        }
    };
}
