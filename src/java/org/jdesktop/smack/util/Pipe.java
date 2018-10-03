/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2012 Michael G. Binz
 */
package org.jdesktop.smack.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * A simple pipe.  Write to the write end, read from the read end.
 *
 * @author Michael Binz
 */
public final class Pipe
{
    private static final int BUFFER_SIZE = 10 * 1024;

    private final PipedOutputStream _writeEnd;
    private final PipedInputStream _readEnd;

    /**
     * Create an instance.
     *
     * @param bufferSize The size of the pipe's internal buffer.
     */
    public Pipe( int bufferSize )
    {
        try
        {
            _writeEnd = new PipedOutputStream();
            _readEnd = new PipedInputStream( _writeEnd, bufferSize );
        }
        catch ( IOException e )
        {
            // PipedStreams can throw 'AlreadyConnected' which should not be
            // possible in the context of this implementation.
            throw new InternalError( e.toString() );
        }
    }

    /**
     * Create an instance with a 10k buffer size.
     */
    public Pipe()
    {
        this( BUFFER_SIZE );
    }

    /**
     * Get the Pipe's write end.
     *
     * @return The write end.
     */
    public OutputStream getWriteEnd()
    {
        return _writeEnd;
    }

    /**
     * Get the pipes read end.
     *
     * @return The read end.
     */
    public InputStream getReadEnd()
    {
        return _readEnd;
    }

    /**
     * Close the pipe.
     */
    public void close()
    {
        FileUtils.forceClose(
                _readEnd );
        FileUtils.forceClose(
                _writeEnd );
    }
}
