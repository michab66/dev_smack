/* $Id$
 *
 * Mack.
 *
 * Released under Gnu Public License
 * Copyright © 2012 Michael G. Binz
 */
package org.jdesktop.smack;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.application.TaskMonitor;
import org.jdesktop.smack.util.StringUtils;




/**
 * A StatusBar panel that tracks a TaskMonitor. Although one could certainly
 * create a more elaborate StatusBar class, this one is sufficient for the
 * examples that need one.
 * <p>
 * This class loads resources from the ResourceBundle called {@code
 * resources.StatusBar}.
 * </p>
 * @version $Rev$
 * @author Michael Binz
 */
public class MackStatusBar extends JPanel implements PropertyChangeListener
{
    private static final long serialVersionUID = -6937637774349306436L;

    private final Insets zeroInsets =
        new Insets( 0, 0, 0, 0 );

    private final JLabel messageLabel;
    private final JProgressBar progressBar;
    private final JLabel statusAnimationLabel;

    private Timer messageTimer;
    private final Timer busyIconTimer;
    @Resource
    private Icon idleIcon;
    @Resource
    private final Icon[] busyIcons = new Icon[15];
    @Resource
    private int busyAnimationRate;
    private int busyIconIndex = 0;



    /**
     * Constructs a panel that displays messages/progress/state properties of
     * the {@code taskMonitor's} foreground task.
     *
     * @param taskMonitor
     *            the {@code TaskMonitor} whose {@code PropertyChangeEvents}
     *            {@code this StatusBar} will track.
     */
    @Deprecated
    public MackStatusBar( Application app, TaskMonitor taskMonitor )
    {
        this( taskMonitor );
    }

    /**
     * Create an instance.
     */
    public MackStatusBar()
    {
        this( null );
    }

    /**
     * Constructs a panel that displays messages/progress/state properties of
     * the {@code taskMonitor's} foreground task.
     *
     * @param taskMonitor
     *            the {@code TaskMonitor} whose {@code PropertyChangeEvents}
     *            {@code this StatusBar} will track.
     */
    public MackStatusBar( TaskMonitor taskMonitor )
    {
        super( new GridBagLayout() );
        setBorder( new EmptyBorder( 2, 0, 6, 0 ) ); // top, left, bottom, right
        messageLabel = new JLabel();
        progressBar = new JProgressBar( 0, 100 );
        statusAnimationLabel = new JLabel();

        Application.getResourceManager().injectResources( this );

        busyIconTimer = new Timer( busyAnimationRate, new UpdateBusyIcon() );
        progressBar.setEnabled( false );
        statusAnimationLabel.setIcon( idleIcon );

        GridBagConstraints c = new GridBagConstraints();
        initGridBagConstraints( c );
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        add( new JSeparator(), c );

        initGridBagConstraints( c );
        c.insets = new Insets( 6, 6, 0, 3 ); // top, left, bottom, right;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        add( messageLabel, c );

        initGridBagConstraints( c );
        c.insets = new Insets( 6, 3, 0, 3 ); // top, left, bottom, right;
        add( progressBar, c );

        initGridBagConstraints( c );
        c.insets = new Insets( 6, 3, 0, 6 ); // top, left, bottom, right;
        add( statusAnimationLabel, c );

        if ( taskMonitor != null )
            taskMonitor.addPropertyChangeListener( this );
    }



    /**
     * Display the passed message.
     *
     * @param message The message to display.
     */
    public void setMessage( String message )
    {
        if ( message == null )
            message = StringUtils.EMPTY_STRING;

        messageLabel.setText( message );
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

        messageLabel.setText( message );

        if ( messageTimer == null )
        {
            messageTimer = new Timer( displayDuration, new ClearOldMessage() );
            messageTimer.setRepeats( false );
        }

        messageTimer.setDelay( displayDuration );
        messageTimer.restart();
    }



    private void initGridBagConstraints( GridBagConstraints c )
    {
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = GridBagConstraints.RELATIVE;
        c.insets = zeroInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
    }

    private class ClearOldMessage implements ActionListener
    {
        @Override
        public void actionPerformed( ActionEvent e )
        {
            messageLabel.setText( StringUtils.EMPTY_STRING );
        }
    }

    private class UpdateBusyIcon implements ActionListener
    {
        @Override
        public void actionPerformed( ActionEvent e )
        {
            busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
            statusAnimationLabel.setIcon( busyIcons[busyIconIndex] );
        }
    }



    public void showBusyAnimation()
    {
        if ( !busyIconTimer.isRunning() )
        {
            statusAnimationLabel.setIcon( busyIcons[0] );
            busyIconIndex = 0;
            busyIconTimer.start();
        }
    }



    public void stopBusyAnimation()
    {
        busyIconTimer.stop();
        statusAnimationLabel.setIcon( idleIcon );
    }

    /**
     * The TaskMonitor (constructor arg) tracks a "foreground" task; this method
     * is called each time a foreground task property changes.
     */
    @Override
    public void propertyChange( PropertyChangeEvent e )
    {
        String propertyName = e.getPropertyName();
        if ( "started".equals( propertyName ) )
        {
            showBusyAnimation();
            progressBar.setEnabled( true );
            progressBar.setIndeterminate( true );
        }
        else if ( "done".equals( propertyName ) )
        {
            stopBusyAnimation();
            progressBar.setIndeterminate( false );
            progressBar.setEnabled( false );
            progressBar.setValue( 0 );
        }
        else if ( "message".equals( propertyName ) )
        {
            String text = (String) (e.getNewValue());
            setMessage( text );
        }
        else if ( "progress".equals( propertyName ) )
        {
            int value = (Integer) (e.getNewValue());
            progressBar.setEnabled( true );
            progressBar.setIndeterminate( false );
            progressBar.setValue( value );
        }
    }
}
