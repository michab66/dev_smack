/* $Id$
 *
 * Utilities
 *
 * Released under Gnu Public License
 * Copyright (c) 2008 Michael G. Binz
 */
package org.jdesktop.smack.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.filechooser.FileFilter;



/**
 * A library of utility routines for file handling.
 *
 * @version $Rev$s
 * @author Michael Binz
 */
public class FileUtils
{
    /**
     * Instances cannot be created.
     */
    private FileUtils()
    {
        throw new AssertionError();
    }



    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(
        FileUtils.class.getName() );



    /**
     * Resolves directories in the passed file list. That is, normal file
     * entries in the returned list are returned unmodified in the result. If a
     * directory is contained in the passed file list, then this is replaced
     * recursively by its contained files.
     *
     * @param files The file list including directories.
     * @return A list of files. No directories are part of this list.
     */
    public static File[] resolveDirectories( File[] files )
    {
        Vector<File> fs = new Vector<File>();

        for ( File file : files )
        {
            if ( file.isDirectory() )
            {
                File[] dirContents = file.listFiles();
                if ( dirContents != null && dirContents.length > 0 )
                {
                    for ( File c : resolveDirectories( dirContents ) )
                        fs.add( c );
                }
            }
            else
            {
                fs.add( file );
            }
        }

        return fs.toArray( new File[fs.size()] );
    }



    /**
     * Silently closes the passed closeable.  In case the close
     * operation fails the exception is written into the log.
     * If the passed object does not offer a close operation
     * an {@link IllegalArgumentException} is thrown.
     *
     * @param closeable The object to close.  If {@code null} is passed
     *          this operation does nothing.
     */
    public static void forceClose( Object closeable )
    {
        if ( closeable == null )
            return;

        try {
            Method closeOperation = closeable.getClass().getMethod( "close" );

            if ( ! closeOperation.isAccessible() )
                closeOperation.setAccessible( true );

            closeOperation.invoke(closeable);
        }
        catch (NoSuchMethodException e) {

            throw new IllegalArgumentException(e.getMessage());
        }
        catch (Exception e) {

            if ( ! LOG.isLoggable(Level.FINE) )
                return;

            Throwable t = e;

            if (t instanceof InvocationTargetException)
                t = t.getCause();

            LOG.log(Level.FINE, t.getMessage(), t);
        }
    }

    /**
     * Filters a list of files.
     *
     * @param files The files to filter.
     * @param filter The filter to use.
     * @return The files that have been accepted by the filter.
     */
   public static File[] filterFiles( File[] files, FileFilter filter )
   {
       Vector<File> collector = new Vector<File>( files.length );

       for ( File f : files )
       {
           if ( filter.accept( f ) )
               collector.add( f );
       }

       return collector.toArray( new File[ collector.size() ] );
   }
}
