/* $Id$
 *
 * Mack II -- Michael's Application Construction Kit.
 *
 * Released under Gnu Public License
 * Copyright © 2008-2010 Michael G. Binz
 */
package org.jdesktop.smack;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.EventObject;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.TaskMonitor;
import org.jdesktop.smack.actions.AppExit;
import org.jdesktop.smack.util.StringUtils;
import org.jdesktop.swingx.action.MackActionManager;




/**
 * The basic Mack application. Offers an action manager and
 * the creation of common ui components like menu, toolbar
 * and status bar.
 *
 * @version $Revision$
 * @author Michael Binz
 */
public abstract class MackApplication<MC extends Component>
    extends
        SingleFrameApplication
{
    private MackStatusBar _statusBar = null;

    private boolean _wantMenuBar;
    private boolean _wantToolBar;
    private boolean _wantStatusBar;

    /**
     * The applications action manger.
     */
    private final MackActionManager _actionManager =
        new MackActionManager();

    /**
     * Create an instance.
     */
    public MackApplication()
    {
        this( true, true, true );
    }
    /**
     * Create an instance with a selection of the application ui
     * elements that are wanted.
     *
     * @param wantMenu {@code true} if a menu should be generated.
     * @param wantToolbar {@code true} if a toolbar should be generated.
     * @param wantStatusbar {@code true} if a status bar should be generated.
     */
    public MackApplication( boolean wantMenu, boolean wantToolbar, boolean wantStatusbar )
    {
        _wantMenuBar = wantMenu;
        _wantToolBar = wantToolbar;
        _wantStatusBar = wantStatusbar;
        addExitListener( _exitListener );
    }

    /**
     * Get the applications action manager.
     *
     * @return The applications action manager.
     */
    public MackActionManager getActionManager()
    {
        return _actionManager;
    }

    /**
     * Create the JToolBar for this application.  Name is 'mainToolBar'.
     * The tool bar is populated with the application actions whose
     * toolbar group is set.
     * <p>To customize the toolbar override this operation.</p>
     */
    protected JToolBar createJToolBar()
    {
        JToolBar result =
            _actionManager.getToolbar();

        result.setName( "mainToolBar" );

        return result;
    }

    /**
     * Creates the application menu bar.  Override and return null if no
     * menu bar is needed.
     *
     * @return The application menu bar.
     */
    protected JMenuBar createJMenuBar()
    {
        JMenuBar result =
            _actionManager.getMenuBar();

        result.setName( "mainMenuBar" );

        return result;
    }

    /**
     * Implements the creation of the main component.  Note that Mack sets
     * the main component name to 'mainComponent'.
     *
     * @return The main component instance.
     */
    protected abstract MC createMainComponent();

    /**
     * Get a reference to the main component.
     *
     * @return A reference to the main component.
     */
    @SuppressWarnings("unchecked")
    public final MC getMainComponent()
    {
        return (MC)getMainView().getComponent();
    }

    /**
     * Ensures that the component name is set to 'mainComponent'.
     *
     * @return The main component with a valid component name.
     */
    private Component createMainInternal()
    {
        MC _mainComponent = createMainComponent();
        _mainComponent.setName( "mainComponent" );
        return _mainComponent;
    }

    /**
     * Creates the application status bar.  Override and return null if no
     * status bar is needed.
     *
     * @return The application status bar.
     */
    protected JComponent createStatusBar()
    {
        _statusBar =
            new MackStatusBar( getApplicationService( TaskMonitor.class ) );
        _statusBar.setName( "mainStatusBar" );

        return _statusBar;
    }

    /**
     * Allows an application to access the status bar.
     *
     * @return The application status bar.
     */
    protected final MackStatusBar getStatusBar()
    {
        return _statusBar;
    }

    /**
     * Get the application's toolbar.
     *
     * @return The application toolbar.
     */
    protected final JToolBar getToolbar()
    {
        return getMainView().getToolBar();
    }

    /**
     * A default implementation that simply shows the main view.
     */
    @Override
    protected void ready()
    {
        show( getMainView() );
    }

    /**
     * To be overridden by the application to place the application
     * actions in a central place in the action manager.  This is
     * called by {@link #startup()}.
     *
     * @param actionManager The action manager to be used for action
     * registration.
     */
    protected void addActions( MackActionManager actionManager )
    {
    }

    /**
     * Add the Mack-internal actions. If overriding call super.
     *
     * @param actionManager The action manager to add actions.
     */
    void addActionsInternal( MackActionManager actionManager )
    {
        actionManager.addAction( new AppExit() );
        // Forward to user code.
        addActions( actionManager );
    }

    /**
     * Creates the standard view components.  These are the tool bar,
     * the status bar and the main component as returned by
     * {@link #getMainComponent()}.
     */
    @Override
    protected void startup()
    {
        FrameView view = getMainView();

        // First create the main component.  This ensures that
        // the required initialisations for the actions are done
        // before the menu and tool bars are created.
        view.setComponent( createMainInternal() );

        addActionsInternal( _actionManager );

        if ( _wantMenuBar )
            view.setMenuBar(
                    createJMenuBar() );
        if ( _wantToolBar )
            view.setToolBar(
                    createJToolBar() );
        if ( _wantStatusBar )
            view.setStatusBar(
                    createStatusBar() );
    }

    /**
     * An exit listener that forwards the exit calls onto
     * the respective protected methods.
     */
    private ExitListener _exitListener = new ExitListener()
    {
        @Override
        public void willExit( EventObject event )
        {
            MackApplication.this.willExit( event );
        }

        @Override
        public boolean canExit( EventObject event )
        {
            return MackApplication.this.canExit( event );
        }
    };

    /**
     * This operation is called if the user requested application
     * exit. The default implementation requests a user confirmation.
     */
    protected boolean canExit( EventObject event )
    {
        ResourceMap r =
                Application.getResourceManager().getApplicationResourceMap();

        String confirmText =
                r.getString( "Application.exitMessage" );

        // If no confirm text is configured, simply leave.
        if ( ! StringUtils.hasContent( confirmText ) )
            return true;

        int option = JOptionPane.showConfirmDialog(
                getMainFrame(),
                confirmText,
                getMainFrame().getTitle(),
                JOptionPane.OK_CANCEL_OPTION );

        return option == JOptionPane.YES_OPTION;
    }

    /**
     * Override to release resources on application exit. This
     * default implementation is empty.
     */
    protected void willExit( EventObject event )
    {
    }

    /**
     * The application exit action.
     *
     * @param ae
     */
    @Action
    public void actExit( ActionEvent ae )
    {
        exit( ae );
    }
}
