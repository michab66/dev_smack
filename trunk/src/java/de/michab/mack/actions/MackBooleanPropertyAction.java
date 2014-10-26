/* $Id: MackBooleanPropertyAction.java 695 2013-12-22 18:41:18Z michab $
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2010 Michael G. Binz
 */
package de.michab.mack.actions;

import java.awt.event.ActionEvent;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.michab.mack.MackAction;
import de.michab.util.ReflectionUtils;



/**
 * A state action that can be linked against a Java Beans boolean
 * property.
 *
 * @version $Rev: 695 $
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class MackBooleanPropertyAction
    extends
        MackAction
{
    private static final Logger _log =
        Logger.getLogger( MackBooleanPropertyAction.class.getName() );



    /**
     * The setter to call.
     */
    private final Method _setter;



    /**
     * The target object.
     */
    private final Object _target;



    /**
     * Creates an instance.
     *
     * @param key The action id,
     * @param propertyName The name of the target property.
     * @param target The target object.
     * @param initialState The initial state of the property.  This will
     * be set in the constructor.
     */
    public MackBooleanPropertyAction(
            String key,
            String propertyName,
            Object target,
            boolean initialState )
    {
        super( key );

        setStateAction();
        setSelected( initialState );

        _target = target;

        try
        {
            BeanInfo bi = Introspector.getBeanInfo(
                target.getClass() );

            PropertyDescriptor targetProperty = null;

            for ( PropertyDescriptor c : bi.getPropertyDescriptors() )
            {
                if ( propertyName.equals( c.getName() ) )
                {
                    targetProperty = c;
                    break;
                }
            }

            if ( targetProperty == null )
                throw new IllegalArgumentException( propertyName );

            // Get the write method...
            _setter = targetProperty.getWriteMethod();

            // ..and validate that it is a single boolean arg operation.
            if ( _setter == null )
                throw new IllegalArgumentException( "setter null" );
            Class<?>[] params = _setter.getParameterTypes();
            if ( params.length != 1 )
                throw new IllegalArgumentException( "not a single argument" );
            if ( Boolean.class !=
                    ReflectionUtils.normalizePrimitives( params[0] ) )
                throw new IllegalArgumentException( "not boolean property" );
        }
        catch ( IntrospectionException e )
        {
            throw new IllegalArgumentException( e );
        }

        // Set the initial value.
        try
        {
            _setter.invoke( _target, initialState );
        }
        catch ( IllegalArgumentException e )
        {
            _log.log( Level.WARNING, e.getMessage(), e );
        }
        catch ( IllegalAccessException e )
        {
            _log.log( Level.WARNING, e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            _log.log( Level.WARNING, e.getMessage(), e );
        }
    }



    /**
     * Invokes the operation, passing the selected status.
     *
     * @param e The action event.  This is not used.
     */
    @Override
    public void actionPerformed( ActionEvent e )
    {
        try
        {
            _setter.invoke( _target, isSelected() );
        }
        catch ( IllegalArgumentException ex )
        {
            _log.log( Level.WARNING, ex.getMessage(), ex );
        }
        catch ( IllegalAccessException ex )
        {
            _log.log( Level.WARNING, ex.getMessage(), ex );
        }
        catch ( InvocationTargetException ex )
        {
            _log.log( Level.WARNING, ex.getMessage(), ex );
        }
     }
}
