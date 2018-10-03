/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2013 Michael G. Binz
 */
package org.jdesktop.swingx;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.JSplitPane;

import org.jdesktop.smack.util.MathExt;


/**
 * A JSplitPane that allows setting the proportional divider location
 * in the constructor. This simplifies handling of the location a lot
 * compared to the original {@link JSplitPane}.
 *
 * @version $Rev$
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class JXSplitPane
    extends JSplitPane
{
    private double _proportionalLeftTopx;

    /**
     * Create an instance with a proportion of 0.5.
     */
    public JXSplitPane()
    {
        this( .5 );
    }
    /**
     * Create an instance with the passed proportion.
     *
     * @param proportion The proportion of the left pane in range
     * [0.0..1.0].
     */
    public JXSplitPane( double proportion )
    {
        if ( proportion < 0 || proportion > 1 )
            throw new IllegalArgumentException();

        setResizeWeight( 0.5 );

        _proportionalLeftTopx = proportion;
        addComponentListener( _cl );
    }

    /**
     * This is not allowed. Use the constructor to set the proportional
     * location.
     * @see javax.swing.JSplitPane#setDividerLocation(double)
     * @deprecated Use the constructor to set the proportional location.
     * @throws IllegalStateException In all cases.
     */
    public void setDividerLocation( double proportionalLocation )
    {
        throw new IllegalStateException(
                "Not allowed on " + getClass().getSimpleName() );
    }

    private ComponentListener _cl = new ComponentAdapter()
    {
        @Override
        public void componentResized( ComponentEvent e )
        {
            assert this == _cl;
            removeComponentListener( this );
            _cl = null;
            setDividerLocation(
                    MathExt.round( getWidth() * _proportionalLeftTopx ) );
        }
    };
}
