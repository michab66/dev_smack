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
import java.io.File;

import javax.swing.JOptionPane;

import org.jdesktop.application.Action;
import org.jdesktop.smack.actions.AppOpen;
import org.jdesktop.smack.util.Transformer;
import org.jdesktop.swingx.action.MackActionManager;




/**
 * A great but undocumented thing.
 *
 * @param FT This application's file type. (document type)
 * @param MC The main component.
 *
 * @version $Revision$
 * @author Michael Binz
 */
public abstract class MackAppViewer<FT, MC extends Component>
  extends
    MackApplication<MC>
  implements ArrayLoader<FT>
{
    private String[] _commandLineArgs = null;

    /**
     * The class that represents the document type of this application.
     */
    private final Class<FT> _documentClass;



    private AppOpen<FT, MC> _actAppOpen = null;



    /**
     * Create an instance with all UI elements.
     *
     * @param documentClass The class of the document to be viewed.
     */
    public MackAppViewer( Class<FT> documentClass )
    {
      _documentClass = documentClass;
    }

    /**
     * Create an instance with a selection of the application ui
     * elements that are wanted.
     *
     * @param wantMenu {@code true} if a menu should be generated.
     * @param wantToolbar {@code true} if a toolbar should be generated.
     * @param wantStatusbar {@code true} if a status bar should be generated.
     */
    public MackAppViewer( Class<FT> documentClass, boolean wantMenu, boolean wantToolbar, boolean wantStatusbar )
    {
        super( wantMenu, wantToolbar, wantToolbar );
        _documentClass = documentClass;
    }

    /**
     * The template method that has to implement the concrete load
     * functionality.
     *
     * @param files The files that have been selected for loading.
     */
    @Override
    public abstract void load( FT[] files );

    @Override
    void addActionsInternal( MackActionManager actionManager )
    {
        // Create the default app actions.
        _actAppOpen =
            new AppOpen<FT, MC>( this, _documentClass );
        actionManager.addAction( _actAppOpen );

        super.addActionsInternal( actionManager );
    }

    /**
     * Save the arguments.
     */
    @Override
    protected void initialize( String[] args )
    {
        _commandLineArgs = args;
    }

    private boolean filesValid( File[] files )
    {
        for ( File c : files )
        {
            if ( ! c.exists() )
            {
                JOptionPane.showMessageDialog( null, "Cannot load file\n" + c.getName() );
                return false;
            }
        }

        return true;
    }

    /**
     * Processes command line arguments.  These are interpreted as
     * file names and load is started.
     */
    @Override
    protected void ready()
    {
        if ( _commandLineArgs.length > 0 )
        {
            Transformer<File, String> toFile =
                new Transformer<File, String>( File.class, String.class );

            File[] f = toFile.transform( _commandLineArgs );

            if ( filesValid( f ) )
            {
                _actAppOpen.load(
                        toFile.transform( _commandLineArgs ) );
            }
        }

        show( getMainView() );
    }

    @Action
    public void actLoad( ActionEvent ae )
    {
        _actAppOpen.actionPerformed( ae );
    }
}
