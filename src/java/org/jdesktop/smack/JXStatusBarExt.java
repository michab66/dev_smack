/* $Id$
 *
 * Mack.
 *
 * Released under Gnu Public License
 * Copyright © 2012 Michael G. Binz
 */
package org.jdesktop.smack;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.jdesktop.beans.PropertyLink;
import org.jdesktop.smack.util.StringUtils;
import org.jdesktop.swingx.JXToolbar;

/**
 * Strategy:  Three areas:
 *  - Message : Displays messages, possibly temporary.
 *  (Optional: A persistent message can be shadowed by a temporary
 *  message. A history of messages is kept.)
 *  - Center area: Can be used for temporary display of components, normally
 *  ones that are alive displaying progress or short term messages to the user.
 *  - Application area: Right part of the bar: Used by the application to install
 *  additional components, like a button for a debug screen or a version number
 *  display. This area can handle Components as well as Actions. Adding to this
 *  area is by default from right to left. It is possible to add WEST or EAST
 *  explicitly.

 * @version $Rev$
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class JXStatusBarExt extends JPanel
{
    private final JXToolbar _right = new JXToolbar();

    private final JLabel _messageLabel = new JLabel();

    private Timer messageTimer;

    /**
     * Create an instance.
     */
    public JXStatusBarExt()
    {
        super( new BorderLayout() );

        add( _messageLabel, BorderLayout.LINE_START );

        add( _right, BorderLayout.LINE_END );

        new PropertyLink( this, "background", _right );
        new PropertyLink( this, "foreground", _right );
        new PropertyLink( this, "background", _messageLabel );
        new PropertyLink( this, "foreground", _messageLabel );
        new PropertyLink( this, "font", _messageLabel );
    }

    /**
     * Display the passed message.
     *
     * @param message The message to display.
     */
    public void setMessage( String message )
    {
        if ( StringUtils.isEmpty( message ) )
            // Set a space to ensure we keep our height.
            message = " ";

        _messageLabel.setText( message );
    }

    /**
     * Display the passed message for a certain duration.  After the duration
     * passed, the status bar message is cleared.
     *
     * @param message The message to display.
     * @param displayDuration The time in milliseconds the message is to be
     * displayed.
     */
    public void setMessage( String message, int displayDuration )
    {
        if ( message == null )
            throw new NullPointerException( "message == null" );
        if ( displayDuration <= 0 )
            throw new IllegalArgumentException( "displayDuration <= 0" );

        _messageLabel.setText( message );

        if ( messageTimer == null )
        {
            messageTimer = new Timer( 0, new ClearOldMessage() );
            messageTimer.setRepeats( false );
        }

        messageTimer.setInitialDelay( displayDuration );
        messageTimer.restart();
    }

    /**
     * The component currently displayed in the center of the status bar.
     * If no component is displayed this is null.
     */
    private Component _centerComponent = null;

    /**
     * Add a component in the center of the status bar.
     *
     * @param component The component to add. null removes the center component.
     */
    public void addCenter( Component component )
    {
        if ( component == null && _centerComponent != null )
            remove( _centerComponent );
        else
            add( component, BorderLayout.CENTER );

        _centerComponent = component;
    }

    /**
     * Add a component to the right-side status bar area.
     *
     * @param component The component to add.
     */
    public void addRight( Component component )
    {
        addRight( component, BorderLayout.LINE_END );
    }

    /**
     * Add a component to the right-side status bar area.
     *
     * @param component The component to add.
     * @param linePosition One of {@link BorderLayout#LINE_START} or
     * {@link BorderLayout#LINE_END}.
     * @throws IllegalArgumentException if the linePostion is unknown or null.
     */
    public void addRight( Component component, String linePosition )
    {
        if ( BorderLayout.LINE_END.equals( linePosition ) )
            _right.add( component );
        else if ( BorderLayout.LINE_START.equals( linePosition ) )
            _right.add( component, 0 );
        else
            throw new IllegalArgumentException(
                    "Unknown position hint: " + linePosition );
    }

    /**
     * Add an {@link Action} to the right side status bar area.
     *
     * @param action The Action to add.
     * @return The component that was added for the passed Action.
     */
    public JComponent addRight( Action action )
    {
        return addRight( action, BorderLayout.LINE_END );
    }

    /**
     * Add an {@link Action} to the right-side status bar area.
     *
     * @param action The component to add.
     * @param linePosition One of {@link BorderLayout#LINE_START} or
     * {@link BorderLayout#LINE_END}.
     * @return The component that was added for the passed Action.
     * @throws IllegalArgumentException if the linePostion is unknown or null.
     */
    public JComponent addRight( Action action, String linePosition )
    {
        if ( BorderLayout.LINE_END.equals( linePosition ) )
            return _right.add( action );
        else if ( BorderLayout.LINE_START.equals( linePosition ) )
            return _right.add( action, 0 );

        throw new IllegalArgumentException(
                "Unknown position hint: " + linePosition );
    }

    /**
     * Called for timed messages, clears the message area.
     */
    private class ClearOldMessage implements ActionListener
    {
        @Override
        public void actionPerformed( ActionEvent e )
        {
            _messageLabel.setText( " " );
        }
    }
}
