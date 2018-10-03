/* $Id$
 *
 * Copyright © 2011 Michael G. Binz
 */
package org.jdesktop.swingx;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.jdesktop.application.Action;
import org.jdesktop.beans.JavaBeanProperty;
import org.jdesktop.beans.PropertyLink;

/**
 * A special desktop pane, providing the ability to tile the existing child
 * windows and helping with some bright placement of new child frames. Tries to
 * be as similar as possible to what is usually implemented in Windows for MDI
 * support.
 *
 * @version $Rev$
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class JXDesktop extends JDesktopPane
{
    private static Logger LOG = Logger.getLogger( JXDesktop.class.getName() );

    // TODO sorting window array
    // TODO calculating cascade offset
    /**
     *
     */
    private static final int TILE_HORIZONTALLY = 0;

    /**
     *
     */
    private static final int TILE_VERTICALLY = 1;

    /**
     * Maps menu items to their associated child windows.
     */
    private final HashMap<JMenuItem, JInternalFrame> _mi2frameMap =
        new HashMap<JMenuItem, JInternalFrame>();

    /**
     * The button group used to manage the selected window in the window menu.
     */
    private final ButtonGroup _group = new ButtonGroup();

    /**
     * Defines the maximum number of windows represented as menu items in the
     * windows menu. If there are more windows, a selection dialog is added.
     */
    private static final int showWindowsMenuCount = 10;

    /**
     * The window menu. This is completely managed by the MdiFrame.
     */
    private final JPopupMenu _windowMenu = new JPopupMenu( "Window" );

    public final javax.swing.Action ACTION_CASCADE =
            GTools.getAction( this, "actCascade" );
    public final javax.swing.Action ACTION_TILE_VERTICALLY =
            GTools.getAction( this, "actTileVertically" );
    public final javax.swing.Action ACTION_TILE_HORIZONTALLY =
            GTools.getAction( this, "actTileHorizontally" );

//    private final ContainerListener _containerListener = new ContainerListener()
//    {
//        @Override
//        public void componentRemoved( ContainerEvent e )
//        {
//            setActionsEnabled( e.getContainer().getComponentCount() );
//        }
//
//        @Override
//        public void componentAdded( ContainerEvent e )
//        {
//            setActionsEnabled( e.getContainer().getComponentCount() );
//        }
//    };
//
    private void enableActions( boolean enabled )
    {
        ACTION_CASCADE.setEnabled( enabled );
        ACTION_TILE_HORIZONTALLY.setEnabled( enabled );
        ACTION_TILE_VERTICALLY.setEnabled( enabled );
    }

    /**
     * Create an instance.
     */
    public JXDesktop()
    {
        ACTION_CASCADE.setEnabled( false );
        ACTION_TILE_HORIZONTALLY.setEnabled( false );
        ACTION_TILE_VERTICALLY.setEnabled( false );

        _windowMenu.add( new JMenuItem( ACTION_CASCADE ) );
        _windowMenu.add( new JMenuItem( ACTION_TILE_VERTICALLY ) );
        _windowMenu.add( new JMenuItem( ACTION_TILE_HORIZONTALLY ) );

        // The static entries in the windows menu are delimited by a separator.
        // Everything below that separator will be managed automatically.
        _windowMenu.addSeparator();
        setComponentPopupMenu( _windowMenu );
//
//        addContainerListener( _containerListener );
    }

    /**
     * Add an mdi child to the frame. There's no way back, after adding the
     * frame takes over responsibility for the lifecycle of the child.
     */
    private void add( JInternalFrame child )
    {
        super.add( child );

        JMenuItem mi = new JRadioButtonMenuItem( child.getTitle() );

        new PropertyLink( child, JInternalFrame.TITLE_PROPERTY, mi, "text" );

        mi.addActionListener( childsMenuItemListener );
        child.putClientProperty( CP_MENUITEM, mi );
        _mi2frameMap.put( mi, child );
        _group.add( mi );

        // Bookkeeping.
        int mapSize = _mi2frameMap.size();

        if ( mapSize < showWindowsMenuCount )
            _windowMenu.add( mi );
        else if ( mapSize == showWindowsMenuCount )
            _windowMenu.add( getWindowsDialogMenuItem() );

        _windowMenu.setEnabled( true );
    }



    @Action
    public void actTileHorizontally( ActionEvent ae )
    {
        tile( TILE_VERTICALLY );
    }
    @Action
    public void actTileVertically( ActionEvent ae )
    {
        tile( TILE_HORIZONTALLY );
    }
    @Action
    public void actCascade( ActionEvent ae )
    {
        cascade();
    }

    /**
     * Tile the contained internal frames horizontally or vertically as defined
     * by the passed constant.
     */
    private void tile( int how )
    {
        Dimension availableDesktop = arrangeIcons();

        JInternalFrame[] children = getOpenFrames();

        // Check if there are really child windows to arrange. Iconified child
        // windows have been handled in arrangeIcons().
        if ( children.length < 1 )
            return;

        // Switch off the maximized status of all windows since the look and feel
        // does not handle setDimension() on maximized frames properly.
        for ( JInternalFrame c : children )
            unMaximizeFrame( c );

        int numOfRows = (int)Math.sqrt( children.length );
        int childsPerRow = children.length / numOfRows;
        int remaining = children.length % numOfRows;

        if ( how == TILE_VERTICALLY )
            tileVertically( children, numOfRows, childsPerRow, remaining,
                    availableDesktop );
        else
            tileHorizontally( children, numOfRows, childsPerRow, remaining,
                    availableDesktop );
    }

    /**
     *
     */
    private Dimension arrangeIcons()
    {
        Point currentPosition = null;

        for ( JInternalFrame c : getAllFrames() )
        {
            if ( c.isIcon() )
            {
                JComponent icon = c.getDesktopIcon();

                // Init the current placement position.
                if ( currentPosition == null )
                    currentPosition = new Point( 0, getHeight()
                            - icon.getHeight() );

                // If we are not in the leftmost position and
                // the icon won't fit on this line...
                if ( currentPosition.x > 0
                        && currentPosition.x + icon.getWidth() > getWidth() )
                {
                    // ...we go one line up and to the left.
                    currentPosition.x = 0;
                    currentPosition.y -= icon.getHeight();
                }

                // We are ready to set the icon location.
                icon.setLocation( currentPosition );

                // At last set the position for the next item to the right of
                // this one.
                currentPosition.x += icon.getWidth();
            }
        }

        // If the current position is valid and any icon has been placed...
        if ( currentPosition != null && currentPosition.x > 0 )
            // ...return the remaining free space for further tiling.
            return new Dimension( getWidth(), currentPosition.y );
        else
            // In the other case all real estate may be used.
            return new Dimension( getWidth(), getHeight() );
    }

    /**
     * Implements cascading the child windows.
     */
    private void cascade()
    {
        Rectangle windowPosition = new Rectangle();

        Dimension availableDesktop = arrangeIcons();

        JInternalFrame[] children = getOpenFrames();

        int cascadeOffset = calculateCascadeOffset( children );

        // Childs will be created at 4/5 of our width.
        int childWidth = (4 * availableDesktop.width) / 5;

        // Calculate the number of childs we can position for one run...
        int childsPerRun = (availableDesktop.width - childWidth)
                / cascadeOffset;
        // ...and fix the width so we use all of the available width.
        childWidth += (availableDesktop.width - childWidth) % cascadeOffset;

        // Now calculate the height.
        int childHeight = availableDesktop.height
                - (childsPerRun * cascadeOffset);

        for ( JInternalFrame c : children )
        {
            // Check if we can create a window given that width at the current
            // position...
            if ( (windowPosition.x + childWidth) > availableDesktop.width )
                // ...and reset the current position to the upper left corner if
                // not.
                windowPosition.setLocation( 0, 0 );

            // Seems fine so far. Set the height of the new window to the
            // maximum possible for the current y position.
            windowPosition.setSize( childWidth, childHeight );

            // Set our result on the frame.
            c.setBounds( windowPosition );
            c.toFront();

            // Translate the current position for the next call.
            windowPosition.translate( cascadeOffset, cascadeOffset );
        }
    }

    /**
     * Implements horizontal tiling.
     */
    private void tileHorizontally( JInternalFrame[] childs, int numOfRows,
            int childsPerRow, int remaining, Dimension availableDesktop )
    {
        int childWidth = availableDesktop.width / numOfRows;

        int childIdx = 0;

        // For each row...
        for ( int i = 0; i < numOfRows; i++ )
        {
            // ...calculate the number of children in this row...
            int childsInThisRow = childsPerRow + (i < remaining ? 1 : 0);
            // ...and each child's height...
            int childHeight = availableDesktop.height / childsInThisRow;
            // ...and for each child in this row...
            for ( int j = 0; j < childsInThisRow; j++ )
            {
                // ...calculate the position.
                childs[childIdx++].setBounds( i * childWidth, j * childHeight,
                        childWidth, childHeight );
            }
        }
    }

    /**
     * Implements vertical tiling.
     */
    private void tileVertically( JInternalFrame[] childs, int numOfRows,
            int childsPerRow, int remaining, Dimension availableDesktop )
    {
        int childHeight = availableDesktop.height / numOfRows;

        int childIdx = 0;

        // For each row...
        for ( int i = 0; i < numOfRows; i++ )
        {
            // ...calculate the number of childs in this row...
            int childsInThisRow = childsPerRow + (i < remaining ? 1 : 0);
            // ...and each child's width...
            int childWidth = availableDesktop.width / childsInThisRow;
            // ...and for each child in this row...
            for ( int j = 0; j < childsInThisRow; j++ )
            {
                // ...calculate the position.
                childs[childIdx++].setBounds( j * childWidth, i * childHeight,
                        childWidth, childHeight );
            }
        }
    }

    /**
     *
     */
    private int calculateCascadeOffset( JInternalFrame[] childs )
    {
        // The following code isn't working yet.
        /*
         * Insets insets = new Insets( 0, 0, 0, 0 ); int result = 1;
         *
         * for ( int i = 0 ; i < childs.length ; i++ ) { int currentTop =
         * childs[i].getContentPane().getY(); //Insets( insets )).top;
         * System.err.println(currentTop); //Insets( insets )); if ( currentTop
         * < result ) result = currentTop; }
         *
         * if ( result == 0 ) result = 1; System.err.println(
         * "calculateCascadeOffset: " + result ); return result;
         */
        return 30;
    }

    /**
     * Returns an array of open -- that means 'not iconified' -- frames.
     */
    private JInternalFrame[] getOpenFrames()
    {
        Vector<JInternalFrame> openChildren = new Vector<JInternalFrame>();

        for ( JInternalFrame c : getAllFrames() )
        {
            if ( c.isVisible() && !c.isIcon() )
                openChildren.add( c );
        }

        JInternalFrame[] result =
                openChildren.toArray( new JInternalFrame[ openChildren.size() ] );

        Arrays.sort( result, ifComparator );

        return result;
    }

    /**
     * Sorts internal frames according to their title.
     */
    private final Comparator<JInternalFrame> ifComparator = new Comparator<JInternalFrame>()
    {
        @Override
        public int compare( JInternalFrame o1, JInternalFrame o2 )
        {
            return o1.getTitle().compareTo( o2.getTitle() );
        }
    };

    /**
     *
     */
    private final ActionListener windowsDialogListener = new ActionListener()
    {
        @Override
        public void actionPerformed( ActionEvent dummy )
        {
            WindowsDialog.showFor( JXDesktop.this );
        }
    };

    /**
     *
     */
    private JMenuItem windowsDialogMenuItem = null;

    /**
     *
     */
    private JMenuItem getWindowsDialogMenuItem()
    {
        if ( windowsDialogMenuItem == null )
        {
            windowsDialogMenuItem = new JMenuItem( "Windows..." );
            windowsDialogMenuItem.addActionListener( windowsDialogListener );
        }

        return windowsDialogMenuItem;
    }

    /**
     * Listens for a child window's MenuItem. On selection the child window gets
     * deiconified if it was iconified and activated.
     */
    private final ActionListener childsMenuItemListener = new ActionListener()
    {
        @Override
        public void actionPerformed( ActionEvent event )
        {
            JInternalFrame child = _mi2frameMap.get( event.getSource() );

            activate( child );
        }
    };

    /**
     * Sets the focus to a specific child window. If the child is iconified it
     * gets deiconified.
     */
    private void activate( JInternalFrame child )
    {
        try
        {
            if ( child.isIcon() )
                child.setIcon( false );
            else if ( ! child.isVisible() )
                child.setVisible( true );

            child.setSelected( true );
        }
        catch ( Throwable e )
        {
            System.err.println( "activate() failed." );
        }
    }

    /**
     * Swing client property key.
     */
    private static final String CP_MENUITEM = "mdiCpMenuitem";

    /**
     *
     */
    private void remove( JInternalFrame child )
    {
        LOG.fine( "removeChild" );

        JRadioButtonMenuItem mi = (JRadioButtonMenuItem)child.getClientProperty( CP_MENUITEM );
        _mi2frameMap.remove( mi );

        if ( mi == null )
            throw new RuntimeException( "Child didn't exist: " + child );

        // TODO: don't know why this is not working. Maybe related to the
        // DO_NOTHING_ON_CLOSE problem in JInternalFrames.
        // desktop.remove( child );

        _group.remove( mi );

        // If the menu item was actually in the menu remove it, but remember if
        // it really was there.
        boolean wasInMenu = -1 !=_windowMenu.getComponentIndex( mi );
        if ( wasInMenu )
            _windowMenu.remove( mi );

        // If the special dialog action is in our windows menu...
        if ( -1 != _windowMenu.getComponentIndex( getWindowsDialogMenuItem() ) )
        {
            // ...remove it.
            _windowMenu.remove( getWindowsDialogMenuItem() );

            // If the window we closed was contained in the menu...
            if ( wasInMenu )
            {
                // ...look for a component *not* contained in the menu...
                for ( JInternalFrame c : _mi2frameMap.values() )
                {
                    JMenuItem tmi = (JMenuItem)c.getClientProperty( CP_MENUITEM );

                    if ( -1 == _windowMenu.getComponentIndex( tmi ) )
                    {
                        // ...and put it there.
                        _windowMenu.add( tmi );
                        break;
                    }
                }
            }
        }

        // As long as we have more children than we show in the menu keep the
        // windows dialog menu item.
        if ( (_mi2frameMap.size()) >= showWindowsMenuCount )
            _windowMenu.add( getWindowsDialogMenuItem() );

        // If we have no child frames disable the window menu.
        _windowMenu.setEnabled( _mi2frameMap.size() > 0 );
    }



    /**
     * Processes the context menu.
     */
    private final InternalFrameAdapter childListener = new InternalFrameAdapter()
    {
        private final Set<JInternalFrame> openFrames = new HashSet<JInternalFrame>();

        @Override
        public void internalFrameActivated( InternalFrameEvent e )
        {
            LOG.fine( "InternalFrameListener: Activated" );
            JInternalFrame activeChild = e.getInternalFrame();
            openFrames.add( activeChild );

            // Activate the window's menu item.
            JRadioButtonMenuItem mi = (JRadioButtonMenuItem)
                activeChild.getClientProperty( CP_MENUITEM );
            if ( mi != null )
                _group.setSelected( mi.getModel(), true );

            enableActions( openFrames.size() > 0 );
        }

        /**
         *
         */
        @Override
        public void internalFrameClosing( InternalFrameEvent e )
        {
            LOG.fine( "InternalFrameListener: Closing" );
            JInternalFrame internalFrame = e.getInternalFrame();
            openFrames.remove( internalFrame );

            int closeOperation =
                    internalFrame.getDefaultCloseOperation();

            // Note that there's already a listener that peforms and handles
            // the default close operation, i.e. performs the hide on
            // HIDE_ON_CLOSE.
            if ( closeOperation == WindowConstants.DO_NOTHING_ON_CLOSE )
                ;
            else if ( closeOperation == WindowConstants.HIDE_ON_CLOSE )
                ;
            else if ( closeOperation == WindowConstants.DISPOSE_ON_CLOSE )
            {
                remove( internalFrame );
            }

            enableActions( openFrames.size() > 0 );
        }

        /**
         *
         */
        @Override
        public void internalFrameOpened( InternalFrameEvent e )
        {
            LOG.warning( "InternalFrameListener: Opened" );
            JInternalFrame internalFrame = e.getInternalFrame();
            openFrames.add( internalFrame );

            add( internalFrame );

            enableActions( openFrames.size() > 0 );
        }
    };

    /**
     * Registers a new ChildWindow.  On registration child windows are set
     * to an invisible state.  To display the child window call setVisible( true )
     * on the returned instance.  This call triggers registration -- nothing more
     * has to be done.
     *
     * @param child The new child window to register.
     */
    public JInternalFrame registerChild( JInternalFrame child )
    {
        return registerChild( child, false );
    }


    /**
     * Registers a new ChildWindow.  On registration child windows are set
     * to an invisible state.  To display the child window call setVisible( true )
     * on the returned instance.  This call triggers registration -- nothing
     * more has to be done.
     *
     * @param child The new child window to register.
     * @param followResize If true then the child window size is adjusted if
     * the desktop resizes.
     */
    public JInternalFrame registerChild( JInternalFrame child, boolean followResize )
    {
        // Set the child to invisible state.
        child.setVisible( false );
        // Add our internal management listener.
        child.addInternalFrameListener( childListener );

        child.setInheritsPopupMenu( true );

        if ( followResize )
            new ResizeGoo( this, child );

        return child;
    }

    /**
     * Switches a frame into the unmaximized state.
     *
     * @param frame The frame to change.
     */
    private void unMaximizeFrame( JInternalFrame frame )
    {
        if ( frame.isMaximum() )
        {
            try
            {
                frame.setMaximum( false );
            }
            catch ( PropertyVetoException e )
            {
                return;
            }
        }
    }

    public final JavaBeanProperty<Image,JXDesktop>  P_BACKGROUND_IMAGE =
            new JavaBeanProperty<Image,JXDesktop>( this, null, "backgroundImage" );

    /**
     * Set a new background image.  This property is bound.
     *
     * @param newValue The new background image.
     */
    public void setBackgroundImage( Image newValue )
    {
        P_BACKGROUND_IMAGE.set( newValue );
    }

    /**
     * Get the currently set background image.
     *
     * @return The currently set background image or {@code null} if none is
     * set.
     */
    public Image getBackgroundImage()
    {
        return P_BACKGROUND_IMAGE.get();
    }

    @Override
    protected void paintComponent( Graphics g )
    {
        super.paintComponent( g );

        Image image = P_BACKGROUND_IMAGE.get();

        // If there's a background image ...
        if ( image == null )
            return;

        // ... paint it centered.
        g.drawImage(
                image,
                (getWidth() - image.getWidth( null ) ) / 2,
                (getHeight() - image.getHeight( null ) ) / 2,
                null );
    }
}
