/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2012 Michael G. Binz
 */
package org.jdesktop.smack.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * An output stream that decouples the writing thread from the thread that
 * forwards the data to the passed target stream.
 *
 * @version $Rev$
 * @author Michael Binz
 */
final public class OutputStreamForwarder
    extends OutputStream
{
    /**
     * The target output stream.
     */
    private final OutputStream _target;

    /**
     * If the data copy thread receives an exception this is placed here and returned
     * on the next write call.
     */
    volatile private IOException _failed;

    /**
     * The queue that holds the data packets to forward.
     */
    private final BlockingQueue<byte[]> _outgoing;

    /**
     * Create an instance.
     *
     * @param target The target output stream receiving incoming data.
     */
    public OutputStreamForwarder( OutputStream target, int capacity )
    {
        if ( null == target )
            throw new NullPointerException();

        _outgoing = new ArrayBlockingQueue<byte[]>( capacity );

        _target = target;

        _dataPumpThread = new InterruptibleThread(
                _dataPump,
                getClass().getSimpleName(),
                true );
        _dataPumpThread.start();
    }

    /**
     * The thread forwarding the data.
     */
    private final InterruptibleThread _dataPumpThread;

    /**
     * The actual forwarding algorithm.
     */
    private final Runnable _dataPump = new Runnable()
    {
        @Override
        public void run()
        {
            while ( ! Thread.currentThread().isInterrupted() )
                try
                {
                    _target.write( _outgoing.take() );
                }
                catch ( IOException e )
                {
                    _failed = e;
                    return;
                }
                catch ( InterruptedException e )
                {
                    return;
                }
        }
    };

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write( byte[] b ) throws IOException
    {
        if ( _failed != null )
            throw _failed;

        try
        {
            _outgoing.add( b.clone() );
        }
        catch ( IllegalStateException e )
        {
            throw new IOException( "Pipe broken -- no consumer." );
        }
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write( byte[] b, int off, int len ) throws IOException
    {
        if ( _failed != null )
            throw _failed;

        try
        {
            _outgoing.add( Arrays.copyOfRange( b, off, len ) );
        }
        catch ( IllegalStateException e )
        {
            throw new IOException( "Pipe broken -- no consumer." );
        }
    }

    @Override
    public void write( int b ) throws IOException
    {
        if ( _failed != null )
            throw _failed;

        try
        {
            _outgoing.add( new byte[] { (byte)b } );
        }
        catch ( IllegalStateException e )
        {
            throw new IOException( "Pipe broken -- no consumer." );
        }
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException
    {
        if ( _failed != null )
            throw _failed;

        _target.flush();
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException
    {
        _dataPumpThread.interrupt();
        _target.flush();
        _target.close();
    }
}
