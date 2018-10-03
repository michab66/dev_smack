/* $Id$
 *
 * Mp3 tagger.
 *
 * Released under Gnu Public License
 * Copyright © 2008-2010 Michael G. Binz
 */
package org.jdesktop.smack.actions;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;
import org.jdesktop.smack.MackAppViewer;
import org.jdesktop.smack.util.FileUtils;
import org.jdesktop.smack.util.StringUtils;
import org.jdesktop.smack.util.Transformer;
import org.jdesktop.swingx.JXErrorPane;




/**
 * The general application open action.  This encapsulates classic
 * dialog-based loading as well as drag and drop driven loading.
 *
 * @param FT Document type
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class AppOpen<FT, MC extends Component>
  extends
    MackApplicationAction
  implements
    DropTargetListener
{
    private static final long serialVersionUID =
        -7923176272415547062L;


    private static final Logger _log =
        Logger.getLogger( AppOpen.class.getName() );



    /**
     * The file chooser used for selecting the files to open.
     */
    private JFileChooser _fc;



    /**
     * The used file filter.
     */
    private FileFilter _filter;



    /**
     * Flag for directory resolution.  If this is true, then
     * directories are allowed on drop operations.  If a directory is
     * dropped its contents is scanned for files matching the filter.
     */
    private boolean _resolveDirs;



    /**
     * A transformer from File to FT,
     */
    private Transformer<FT,File> _transformer;



    /**
     * A reference to the document class.
     */
    private final Class<FT> _documentClass;



    /**
     * Create an instance.
     */
    public AppOpen( MackAppViewer<FT, MC> host, Class<FT> documentClass )
    {
        super( AppActionKey.appOpen );

        _documentClass = documentClass;

        _transformer = new Transformer<FT, File>( documentClass, File.class );

        // TODO here we decide the drag and drop target for
        // loading.  Make sure that the main frame is cool.  Note that
        // this has to take into account that an application may
        // decide internally to use also dnd and we do not want to
        // interfere with this.
        new DropTarget(
            host.getMainFrame(),
            this );

        setEnabled( true );

        configureFrom( getResourceMap() );
    }



    /**
     * The public mack api that allows to load files.  Encapsulates
     * transforming these files to FTs.
     *
     * @param files The files to load.
     */
    public void load( File[] files )
    {
        execute( new Worker(
                Application.getInstance(),
                files ) );
    }



    @Override
    public void actionPerformed(ActionEvent e)
    {
        if ( _fc == null )
        {
            _fc = createFileChooser(
                    makeResourceKeyName( "fileChooser" ) );
        }

        // If we have a filter...
        if ( _filter != null )
          // ...set it.
          _fc.setFileFilter( _filter );

        int option = _fc.showOpenDialog(
            Application.getInstance(
                    SingleFrameApplication.class ).getMainFrame()
        );

        if ( option != JFileChooser.APPROVE_OPTION )
            return;

        if ( _fc.isMultiSelectionEnabled() )
            load( _fc.getSelectedFiles() );
        else
            load( new File[]{ _fc.getSelectedFile() } );
     }



    /**
     * TODO note that this needs to be configurable from the
     * application resources.
     */
    private void configureFrom( ResourceMap rm )
    {
        String ALLOW_DIRS =
            makeResourceKeyName( "allowDirectories" );
        String FILTER_DESCRIPTION =
            makeResourceKeyName( "filter.description" );
        String FILTER_EXTENSION =
            makeResourceKeyName( "filter.extensions" );

        if ( rm.containsKey( ALLOW_DIRS ) )
        {
            _resolveDirs = rm.getBoolean(
                ALLOW_DIRS );
        }

        String filterDisplayText = StringUtils.EMPTY_STRING;
        if ( rm.containsKey( FILTER_DESCRIPTION ) )
        {
            filterDisplayText = rm.getString(
                FILTER_DESCRIPTION );
        }

        String filterExtension = StringUtils.EMPTY_STRING;
        if ( rm.containsKey( FILTER_EXTENSION ) )
        {
            filterExtension = rm.getString(
                FILTER_EXTENSION );
        }

        if ( StringUtils.hasContent( filterExtension ) )
        {
          if ( ! StringUtils.hasContent( filterDisplayText ) )
            _log.warning( "No description for filename filter." );

          _filter = new FileNameExtensionFilter(
                filterDisplayText,
                filterExtension.split( "\\s" ) );
        }
    }



    /**
     *
     * @param name
     * @return
     */
    private JFileChooser createFileChooser(String name)
    {
        JFileChooser fc = new JFileChooser();
        fc.setName( name );

        if ( _resolveDirs )
            fc.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );


        Application.getInstance()
            .getContext()
            .getResourceMap()
            .injectComponents( fc );

        return fc;
    }



    /**
     *
     */
    private class Worker extends Task<FT[],Void>
    {
        private final File[] _files;



        /**
         *
         * @param application
         */
        public Worker( Application application, File[] fs )
        {
            super( application, makeResourceKeyName( "task" ) );
            _files = fs;
        }



        /* (non-Javadoc)
         * @see org.jdesktop.application.Task#succeeded(java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void succeeded( FT[] result )
        {
            Application.getInstance(
                    MackAppViewer.class).load( result );
        }



        private FT loadSingleFile( File f )
            throws Exception
        {
            try
            {
                return _transformer.transformX( f );
            }
            catch ( Exception e )
            {
                JXErrorPane.showDialog( e );
                throw e;
            }
        }

        @Override
        protected FT[] doInBackground() throws Exception
        {
            Vector<FT> transformedFiles = new Vector<FT>( _files.length );

            setProgress( 0.0f );

            for ( int i = 0 ; i < _files.length ; i++ )
            {
                message( "loadFile", _files[i].getName() );

                transformedFiles.add( loadSingleFile( _files[i] ) );

                float progress = (i+1) / (float)_files.length;

                setProgress( progress );
            }

            // Create the properly typed result array.
            @SuppressWarnings("unchecked")
            FT[] result = (FT[])
              Array.newInstance(
                  _documentClass,
                  transformedFiles.size() );

            // Fill and return the array.
            return transformedFiles.toArray( result );
        }
    }



    @Override
    public void dragEnter(DropTargetDragEvent dtde)
    {
        if ( isDropAcceptable( dtde ) )
            dtde.acceptDrag( DnDConstants.ACTION_COPY_OR_MOVE );
        else
            dtde.rejectDrag();
    }



    @Override
    public void dragExit(DropTargetEvent dte)
    {
    }



    @Override
    public void dragOver(DropTargetDragEvent dtde)
    {
    }



    /*
     * Inherit javadoc.
     */
    @Override
    public void drop( DropTargetDropEvent dtde )
    {
        // Used on dnd protocol completion in 'finally' below.
        boolean status = false;

        try
        {
            if ( dtde.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) )
            {
                // First we accept the drop to get the dnd protocol into the
                // needed state for getting the data.
                dtde.acceptDrop( DnDConstants.ACTION_COPY_OR_MOVE );

                File[] files = getTransferFiles( dtde.getTransferable() );

                if ( files.length > 0)
                {
                    load( files );
                }

                // Everything went fine.
                status = true;
            }
            else
                dtde.rejectDrop();
        }
        // Catch potential IO exceptions and keep dnd protocol in sync.
        catch (Exception e)
        {
            _log.log( Level.WARNING, e.getLocalizedMessage(), e );
            dtde.rejectDrop();
        }
        // And again: Last step in dnd protocol. After that we are ready to
        // accept the next drop.
        finally
        {
            dtde.dropComplete( status );
        }
    }



    /**
     *
     * @param x
     * @return
     */
    @SuppressWarnings("unchecked")
    private File[] getTransferFiles( Transferable x )
    {
        File[] result;

        try
        {
            // Now get the transferred data and be as defensive as possible.
            // We expect a java.util.List of java.io.Files.
            List<File> fileList = (List<File>)
                x.getTransferData( DataFlavor.javaFileListFlavor );
            result = fileList.toArray( new File[fileList.size()] );
        }
        catch ( Exception  e )
        {
            _log.log( Level.FINE, e.getLocalizedMessage(), e );
            return new File[0];
        }

        if ( _resolveDirs )
            result = FileUtils.resolveDirectories( result );

        if ( _filter != null )
            result = FileUtils.filterFiles( result, _filter );

        return result;
    }



    @Override
    public void dropActionChanged(DropTargetDragEvent dtde)
    {
    }



   /**
    *
    * @param dtde
    * @return
    */
   private boolean isDropAcceptable( DropTargetDragEvent dtde )
   {
       if (!dtde.isDataFlavorSupported( DataFlavor.javaFileListFlavor ))
           return false;

       File[] files = getTransferFiles( dtde.getTransferable() );

       if ( files.length < 1 )
           return false;

       return true;
   }
}
