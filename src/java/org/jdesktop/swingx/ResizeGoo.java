/* $Id$
 *
 * Copyright © 2011 Michael G. Binz
 */
package org.jdesktop.swingx;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JInternalFrame;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

/**
 * Connects the relative sizes of a master and slave component.
 *
 * @version $Rev$
 * @author Michael Binz
 */
final class ResizeGoo
{
    private final Component _master;
    private final JInternalFrame _servant;

    private float _masterOldSizeW = -1.0f;
    private float _masterOldSizeH = -1.0f;
    private float _masterNewSizeW = -1.0f;
    private float _masterNewSizeH = -1.0f;

    /**
     * High precision servant dimensions.  These have priority over the actual integer
     * property values on the servant component.
     */
    private float _servantX, _servantY, _servantW, _servantH;

    /**
     * Create an instance.
     *
     * @param master The master, i.e. the controlling component.
     * @param servant The servant, i.e. the controlled component.
     */
    public ResizeGoo( Component master, JInternalFrame servant )
    {
        _master = master;
        _servant = servant;

        _servantX = servant.getX();
        _servantY = servant.getY();
        _servantW = servant.getWidth();
        _servantH = servant.getHeight();

        servant.addComponentListener(
                _servantCl );
        servant.addInternalFrameListener(
                _servantFrameListener );
        master.addComponentListener(
                _masterCl );

        if ( master.isShowing() )
        {
            _masterNewSizeW = master.getWidth();
            _masterNewSizeH = master.getHeight();
        }
    }

    /**
     * Handles master resizes and re-computes the servant size.
     */
    private final ComponentListener _masterCl = new ComponentAdapter()
    {
        @Override
        public void componentResized( ComponentEvent e )
        {
            _masterOldSizeW = _masterNewSizeW;
            _masterOldSizeH = _masterNewSizeH;
            _masterNewSizeW = e.getComponent().getWidth();
            _masterNewSizeH = e.getComponent().getHeight();

            if ( _masterOldSizeW < 0.0 )
                return;

            float wGrowth = _masterNewSizeW / _masterOldSizeW;
            float hGrowth = _masterNewSizeH / _masterOldSizeH;

            _servantX *= wGrowth;
            _servantY *= hGrowth;
            _servantW *= wGrowth;
            _servantH *= hGrowth;

            _servant.setBounds(
                    Math.round( _servantX ),
                    Math.round( _servantY ),
                    Math.round( _servantW ),
                    Math.round( _servantH ) );
        }
    };

    /**
     * Handles manual interaction of the user with the servant component.
     */
    private final ComponentListener _servantCl = new ComponentAdapter()
    {
        @Override
        public void componentResized( ComponentEvent e )
        {
            // Only update our local values if the deltas left the rounding
            // precision.
            if ( e.getComponent().getWidth() != Math.round( _servantW ) )
                _servantW = e.getComponent().getWidth();
            if ( e.getComponent().getHeight() != Math.round( _servantH ) )
                _servantH = e.getComponent().getHeight();
        }
        @Override
        public void componentMoved( ComponentEvent e )
        {
            // Only update our local values if the deltas left the rounding
            // precision.
            if ( e.getComponent().getX() != Math.round( _servantX ) )
                _servantX = e.getComponent().getX();
            if ( e.getComponent().getY() != Math.round( _servantY ) )
                _servantY = e.getComponent().getY();
        }
    };

    /**
     * Handles close events on the servant.  Takes care for removing the component
     * listener from the master.
     */
    private final InternalFrameListener _servantFrameListener = new InternalFrameAdapter()
    {
        @Override
        public void internalFrameClosing( InternalFrameEvent e )
        {
            JInternalFrame frame = e.getInternalFrame();
            // Only remove the listener if the window is disposed.  Do not remove if
            // the window is only hidden.
            if ( frame.getDefaultCloseOperation() == WindowConstants.DISPOSE_ON_CLOSE )
                _master.removeComponentListener( _masterCl );
        }
    };
}
