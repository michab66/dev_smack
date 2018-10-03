/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright � 2015 Michael G. Binz
 */
package org.jdesktop.smack.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import org.jdesktop.util.Pipe;
import org.jdesktop.util.PowerPipe;

/**
 * A simple pipe.  Write to the write end, read from the read end.
 *
 * @author Michael Binz
 * @deprecated Use {@link PowerPipe}
 */
@Deprecated
public final class Pipe2
    implements Pipe, AutoCloseable
{
    private static final int BUFFER_SIZE = 10 * 1024;

    private final byte[] _buffer;

    private int _currentWritePosition = 0;
    private int _currentReadPosition = 0;

    private volatile boolean _closed = false;

    /**
     * The pipe's singular write end.
     */
    private final OutputStream _writeEnd = new OutputStream()
    {
        private void writeImpl( int b ) throws IOException, InterruptedException
        {
            synchronized ( _writeEnd )
            {
                while ( true )
                {
                    if ( _closed )
                        throw new IOException( "Pipe broken." );

                    if ( ! isFull() )
                        break;

                    wait();
                }

                _buffer[ _currentWritePosition ] =
                        (byte)b;
                _currentWritePosition =
                        boundIdx( _currentWritePosition + 1 );
            }

            synchronized ( _readEnd )
            {
                _readEnd.notify();
            }
        }

        @Override
        public void write( int b ) throws IOException
        {
            try
            {
                System.err.println( ">w " + _currentWritePosition );
                writeImpl( b );
                System.err.println( "<w " + _currentWritePosition );
            }
            catch ( InterruptedException e )
            {
                throw new InterruptedIOException();
            }
        }

        @Override
        public void close() throws IOException
        {
            // A close propagates to both ends.
            Pipe2.this.close();
        };
    };

    /**
     * The pipe's read end.
     */
    private final InputStream _readEnd = new InputStream()
    {
        private int readImpl() throws InterruptedException
        {
            int result = 0;

            synchronized ( _readEnd )
            {
                while ( true )
                {
                    if ( _closed )
                        // TODO better ex pipe broken?
                        return -1;

                    if ( ! isEmpty() )
                        break;

                    // We block here on an empty read stream, waiting for
                    // data to be written or an asynchronous close of the
                    // channel.
                    wait();
                }

                result =
                        _buffer[ _currentReadPosition ];
                _currentReadPosition =
                        boundIdx( _currentReadPosition+1 );
            }

            // We read a byte, kick the write end.
            synchronized ( _writeEnd )
            {
                _writeEnd.notify();
            }

            // Mask to a byte.
            return result & 0xff;
        }

        @Override
        public int read() throws IOException
        {
            try
            {
                System.err.println( ">r " + _currentReadPosition );
                int result = readImpl();
                System.err.println( "<r " + _currentReadPosition );
                return result;
            }
            catch ( InterruptedException e )
            {
                throw new InterruptedIOException();
            }
        }

        @Override
        public void close() throws IOException
        {
            // A close propagates to both ends.
            Pipe2.this.close();
        };
    };

    /**
     * Create an instance.
     *
     * @param bufferSize The size of the pipe's internal buffer.
     */
    public Pipe2( int bufferSize )
    {
        if ( bufferSize <= 0 )
            throw new IllegalArgumentException( "bufferSize must be > 0" );

        // Allocate one larger since our invariant requires that
        _buffer = new byte[ bufferSize+1 ];
    }

    /**
     * Create an instance with a 10k buffer size.
     */
    public Pipe2()
    {
        this( BUFFER_SIZE );
    }

    /**
     * Get the Pipe's write end.
     *
     * @return The write end.
     */
    @Override
    public OutputStream getWriteEnd()
    {
        return _writeEnd;
    }

    /**
     * Get the pipes read end.
     *
     * @return The read end.
     */
    @Override
    public InputStream getReadEnd()
    {
        return _readEnd;
    }

    /**
     *
     * @return
     */
    private boolean isFull()
    {
        return
                boundIdx( _currentWritePosition + 1 )
                ==
                _currentReadPosition;
    }

    /**
     *
     * @param idx
     * @return
     */
    private int boundIdx( int idx )
    {
        return idx % _buffer.length;
    }

    /**
     *
     * @return
     */
    private boolean isEmpty()
    {
        return _currentReadPosition == _currentWritePosition;
    }

    @Override
    public void close()
    {
        if ( _closed )
            return;

        _closed = true;

        // Ensure wake-up and termination of all waiting threads.
        synchronized ( _writeEnd )
        {
            _writeEnd.notifyAll();
        }
        synchronized ( _readEnd )
        {
            _readEnd.notifyAll();
        }
    }
}
