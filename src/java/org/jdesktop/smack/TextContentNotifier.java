/* $Id$
 *
 * Mack
 *
 * Released under Gnu Public License
 * Copyright © 2010 Michael G. Binz
 */
package org.jdesktop.smack;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.jdesktop.beans.ConstrainedProperty;
import org.jdesktop.beans.PropertyProxy;
import org.jdesktop.smack.util.StringUtils;




/**
 * A notifier that allows to link the enabled property of a target component
 * against a text component in a way that the text content controls whether the
 * target is enabled.  The default implementation enables the target as soon as
 * the text component contains a non-null, and after trimming, a non-empty
 * string.
 *
 * <p>Create an instance of this class for each necessary connection.  It is
 * allowed to create several connection for a single target when the enabled
 * state of the target depends on more than one sources.</p>
 *
 * <p>The validation behavior can be adjusted by overriding
 * {@link #isValidContent(String)}.</p>
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class TextContentNotifier
{
    /**
     * A reference to the source component.
     */
    private final JTextComponent _source;

    /**
     * The handler for property changes.
     *
     * One instance of this type is placed in the client properties
     * of each target component.  It acts as a wrapper for the target
     * adding a constrained enabled Java Bean property.
     */
    private ConstrainedProperty<Boolean> _targetHandler;

    /**
     *
     */
    private final String _targetPropertyName;

    /**
     * Create an instance that is linked between source and target.
     *
     * @param source The source component.
     * @param target The target component.
     */
    public TextContentNotifier( JTextComponent source, JComponent target )
    {
        this( source, target, ENABLED_PROP_NAME );
    }

    /**
     * Create an instance that is linked between source and target.
     *
     * @param source The source component.
     * @param target The target component.
     * @param targetPropertyName The name of the target property.
     */
    @SuppressWarnings("unchecked")
    public TextContentNotifier( JTextComponent source, JComponent target, String targetPropertyName )
    {
        // Null check.
        _targetPropertyName = targetPropertyName.toString();

        PropertyProxy<Boolean,JComponent> targetProperty =
            new PropertyProxy<Boolean,JComponent>( targetPropertyName, target );

        _source = source;

        // Check if we are already cooperating with the target.
        _targetHandler = (ConstrainedProperty<Boolean>)
            target.getClientProperty( TextContentNotifier.class );

        if ( _targetHandler == null )
        {
            // No, so start cooperation.
            _targetHandler =
                new ConstrainedProperty<Boolean>( targetProperty );

            target.putClientProperty(
                    TextContentNotifier.class,
                    _targetHandler );
        }

        linkSource( source );
    }

    /**
     * Create an instance that is linked between source and target.
     *
     * @param source The source component.
     * @param target The target component.
     */
    public TextContentNotifier( JTextComponent source, Action target )
    {
        this( source, target, ENABLED_PROP_NAME );
    }

    /**
     * Create an instance that is linked between source and target.
     *
     * @param source The source component.
     * @param target The target component.
     * @param targetPropertyName The name of the target property.
     */
    @SuppressWarnings("unchecked")
    public TextContentNotifier( JTextComponent source, Action target, String targetPropertyName )
    {
        // Null check.
        _targetPropertyName = targetPropertyName.toString();

        PropertyProxy<Boolean,Action> targetProperty =
            new PropertyProxy<Boolean,Action>( _targetPropertyName, target );

        _source = source;

        // Check if we are already cooperating with the target.
        _targetHandler = (ConstrainedProperty<Boolean>)
            target.getValue( TextContentNotifier.class.getName() );

        if ( _targetHandler == null )
        {
            // No, so start cooperation.
            _targetHandler =
                new ConstrainedProperty<Boolean>( targetProperty );

            target.putValue(
                TextContentNotifier.class.getName(),
                _targetHandler );
        }

        linkSource( source );
    }

    private void linkSource( JTextComponent source )
    {
        _targetHandler.addVetoableChangeListener(
                _targetPropertyName,
                _changeListener );

        // Register with the component's document model.
        source.getDocument().addDocumentListener( _documentListener );

        // Initialize the target status.
        handleChange( source.getDocument() );
    }

    /**
     * Check if the passed content qualifies as valid.  This default
     * implementation accepts non-null, non-empty strings as valid.
     *
     * @param pContent The content to qualify.
     * @return True if the content is valid and the targets should be enabled.
     */
    protected boolean isValidContent( String pContent )
    {
        return StringUtils.hasContent( pContent, true );
    }

    /**
     * Access to the source component.  Can be used to modify the component
     * view in the {@link #isValidContent(String)} operation.
     *
     * @return The source component.
     */
    protected JTextComponent getSource()
    {
        return _source;
    }

    /**
     * Checks if the passed document should enable the target component.
     *
     * @param d The document to check.
     * @return {@code true} if the target should enable.
     */
    private Boolean shouldEnable( Document d )
    {
        String content = null;

        try
        {
            content = d.getText( 0, d.getLength() );
        }
        catch ( BadLocationException e )
        {
            content = StringUtils.EMPTY_STRING;
        }

        Boolean result = Boolean.valueOf( isValidContent( content ) );

        return result;
    }

    /**
     * Synchronizes the enabled state of the target according to the content
     * of the passed document.
     *
     * @param d The document whose content should be checked.
     */
    private void handleChange( Document d )
    {
        try
        {
            _targetHandler.set( shouldEnable( d ) );
        }
        catch ( PropertyVetoException ignore )
        {
            // Change was not accepted by another component.
        }
    }



    /**
     * The document listener for the source component.
     */
    private final DocumentListener _documentListener = new DocumentListener()
    {
        @Override
        public void insertUpdate( DocumentEvent e )
        {
            handleChange( e.getDocument() );
        }



        @Override
        public void removeUpdate( DocumentEvent e )
        {
            handleChange( e.getDocument() );
        }



        @Override
        public void changedUpdate( DocumentEvent e )
        {
            handleChange( e.getDocument() );
        }
    };



    /**
     * The change listener that is linked on the shared, per-target, bean.
     *
     * @see ConstrainedBooleanEnabled
     */
    private final VetoableChangeListener _changeListener = new VetoableChangeListener()
    {
        @Override
        public void vetoableChange( PropertyChangeEvent evt )
                throws PropertyVetoException
        {
            // Check the property name.
            if ( ! _targetPropertyName.equals( evt.getPropertyName() ) )
                return;

            // We never veto a change to not enabled.
            if ( ! Boolean.TRUE.equals( evt.getNewValue() ) )
                return;

            if ( Boolean.FALSE.equals( shouldEnable( _source.getDocument() ) ) )
                throw new PropertyVetoException( null, evt );
        }
    };



    /**
     * Unlink the listener connections.
     */
    public void dispose()
    {
        _targetHandler.removeVetoableChangeListener(
                _targetPropertyName,
                _changeListener );

        _source.getDocument().removeDocumentListener(
                _documentListener );
    }



    /**
     * The name of the enabled property.
     */
    private final static String ENABLED_PROP_NAME = "enabled";
//
//
//
//
//    public static void main( String[] argv )
//    {
//        JFrame main = new JFrame("Test");
//        JTextField t1 = new JTextField();
//        JTextField t2 = new JTextField("Gimme float...");
//        JTextField t3 = new JTextField();
//        JTextField t4 = new JTextField();
//        JButton b = new JButton( "Huh" );
//
//        new TextContentNotifier( t1, b );
//        new TextContentNotifier( t2, b )
//        {
//            @Override
//            protected boolean isValidContent(String pContent)
//            {
//                try
//                {
//                    Float.parseFloat( pContent );
//                    return true;
//                }
//                catch ( NumberFormatException e )
//                {
//                    return false;
//                }
//            };
//        };
//
//        new TextContentNotifier( t3, b );
//        new TextContentNotifier( t4, b );
//
//        Box c = new Box( BoxLayout.Y_AXIS );
//
//        c.add(  t1 );
//        c.add(  t2 );
//        c.add(  t3 );
//        c.add(  t4 );
//        c.add(  b );
//
//        main.setContentPane( c );
//        main.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//
//        main.pack();
//        main.setVisible( true );
//    }
}
