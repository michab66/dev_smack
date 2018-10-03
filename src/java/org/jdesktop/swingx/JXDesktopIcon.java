/* $Id: BaseApplication.java 148 2015-04-18 13:44:12Z michab66 $
 *
 * Smack Application.
 *
 * Released under Gnu Public License
 * Copyright © 2017 Michael G. Binz
 */
package org.jdesktop.swingx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.border.AbstractBorder;

import org.jdesktop.smack.util.StringUtils;
import org.jdesktop.swingx.action.AbstractActionExt;
import org.jdesktop.swingx.util.ComponentDragHandler;

/**
 * An icon that can be placed on a {@link JXDesktop} pane.
 *
 * @version $Rev: 22573 $
 * @author micbinz
 */
public abstract class JXDesktopIcon extends Box
    implements ActionListener
{
    /**
     * Used to access the default colors of a button.
     */
    private transient final static JButton _colorPrototype =
            new JButton();

    private static transient Dimension _dimension;

    private transient final JLabel _text =
            new JLabel();

    private String _payload;

    private transient final JPopupMenu _popup =
            new JPopupMenu();

    /**
     * http://stackoverflow.com/questions/16531989/xml-serialization-and-empty-constructors
     */
    public JXDesktopIcon()
    {
        super( BoxLayout.PAGE_AXIS );

        init();
    }

    /**
     * Create an instance.
     *
     * @param position The start position.
     * @param text The displayed text.
     * @param payload The payload, ie. text that is not shown.
     */
    public JXDesktopIcon( Point position, String text, String payload )
    {
        super(
                BoxLayout.PAGE_AXIS );
        setLocation(
                position );
        _text.setText(
                text );
        setPayload(
                payload );
        init();
    }

    public void setText( String text )
    {
        _text.setText( text );
    }

    public String getText()
    {
        return _text.getText();
    }

    public void setIcon( Icon icon )
    {
        _text.setIcon( icon );
    }

    public Icon getIcon()
    {
        return _text.getIcon();
    }

    public final String getPayload()
    {
        return _payload;
    }

    public final void setPayload( String payload )
    {
        _payload = payload;

        setToolTipText( _payload );
    }

    private void init()
    {
        _text.setHorizontalTextPosition(
                JLabel.CENTER );
        _text.setVerticalTextPosition(
                JLabel.BOTTOM );

        new ComponentDragHandler( this );

        _text.setForeground(
                Color.WHITE );

        add( _text );

        addMouseListener( _mouse );

        _actRename.setText( "Rename" );
        _popup.add( _actRename );
        _actDelete.setText( "Delete" );
        _popup.add( _actDelete );

        addFocusListener(
                _focusListener );
        setFocusable(
                true );

        setMaximumSize(
                _dimension );
        setMinimumSize(
                _dimension );
        setComponentPopupMenu(
                _popup );

        setBorder(
                new FocusBorder() );

        GTools.addKeyBinding(
                this,
                KeyStroke.getKeyStroke( "DELETE" ),
                JComponent.WHEN_FOCUSED, _actDelete );
        GTools.addKeyBinding(
                this,
                KeyStroke.getKeyStroke( "control R" ),
                JComponent.WHEN_FOCUSED, _actRename );
    }

    private final MouseListener _mouse = new MouseAdapter()
    {
        @Override
        public void mouseClicked( MouseEvent e )
        {
            requestFocus();

            if ( e.getClickCount() < 2 )
                return;

            actionPerformed( new ActionEvent( JXDesktopIcon.this, e.getID(), _payload ) );
        }
    };

    @SuppressWarnings("serial")
    private final AbstractActionExt _actDelete = new AbstractActionExt()
    {
        @Override
        public void actionPerformed( ActionEvent e )
        {
            Container pa = JXDesktopIcon.this.getParent();

            if ( pa != null )
            {
                pa.remove( JXDesktopIcon.this );
                pa.repaint();
            }
        }
    };

    @SuppressWarnings("serial")
    private final AbstractActionExt _actRename = new AbstractActionExt()
    {
        @Override
        public void actionPerformed( ActionEvent e )
        {
            String s = (String)JOptionPane.showInternalInputDialog(
                    JXDesktopIcon.this,
                    "New name",
                    getName(),
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    JXDesktopIcon.this.getText() );

            if ( StringUtils.hasContent( s ) )
            {
                JXDesktopIcon.this.setText( s.trim() );
                JXDesktopIcon.this.setSize( JXDesktopIcon.this.getPreferredSize() );
            }
        }
    };

    @SuppressWarnings("serial")
    private class FocusBorder extends AbstractBorder
    {
        @Override
        public void paintBorder(
                Component comp, Graphics g, int x, int y, int w, int h )
        {

            if ( ! hasFocus() )
                return;

            Graphics2D gg = (Graphics2D) g;

            gg.setColor(
                    _colorPrototype.getBackground() );
            gg.setStroke(
                    new BasicStroke(
                            1,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_BEVEL,
                            0,
                            new float[]{1},
                            0));
            gg.drawRect(
                    x,
                    y,
                    w - 1,
                    h - 1 );
        }
    }

    private FocusListener _focusListener = new FocusListener()
    {

        @Override
        public void focusLost( FocusEvent e )
        {
            repaint();
        }

        @Override
        public void focusGained( FocusEvent e )
        {
            repaint();
        }
    };

    private static final long serialVersionUID = 5252279373893772433L;
}
