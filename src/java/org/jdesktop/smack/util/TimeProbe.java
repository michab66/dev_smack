/* $Id$
 *
 * Utilities
 *
 * Released under Gnu Public License
 * Copyright (c) 2009 Michael G. Binz
 */
package org.jdesktop.smack.util;



/**
 * A class used for execution time profiling.  Instances encapsulate
 * a millisecond timer that can be started and stopped, measuring the time
 * between these events.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class TimeProbe
{
    /**
     * The probe's name.
     */
    private final String _name;



    /**
     * The UTC ms time of timer start. Zero means not started yet.
     */
    private long _start = 0;



    /**
     * The UTC ms time of timer stop.  Zero means not stopped.
     */
    private long _stop = 0;



    /**
     * A counter used for generating default names;
     */
    private static int _count = 0;



    /**
     * Create a named instance.
     *
     * @param name The probe's name.  Null is allowed, resulting
     * in an unnamed probe.
     */
    public TimeProbe( String name )
    {
        _name = name;
    }



    /**
     * Create an unnamed instance.
     */
    public TimeProbe()
    {
        this( Integer.toString( ++_count ) );
    }



    /**
     * Start the timer.  Note that a timer can be reused.
     *
     * @return The TimeProbe for call chaining.
     */
    public TimeProbe start()
    {
        _stop = 0;
        _start = System.currentTimeMillis();

        return this;
    }



    /**
     * Stop the timer.
     *
     * @return The TimeProbe for call chaining.
     */
    public TimeProbe stop()
    {
        _stop = System.currentTimeMillis();

        return this;
    }


    /**
     * Get the time duration between the last start and stop
     * calls.
     *
     * @return Measured time in ms.
     */
    public long duration()
    {
        // Not started.
        if ( _start == 0 )
            return 0;
        // Not stopped (= running).
        if ( _stop == 0 )
            return System.currentTimeMillis() - _start;

        // Stopped and stable.
        return _stop - _start;
    }



    /**
     * Get whether this timer is running, i.e. has been started.
     *
     * @return {@code true} if started.
     */
    public boolean isRunning()
    {
        return _stop == 0;
    }



    /**
     * Get a printable representation of the timer.  This looks like:
     * [TimeProbe:{name} - 99:99:99.999{...}]. The ellipsis (...) is added if
     * the timer is currently running.  If the timer is unnamed, then the name
     * is represented by a number.
     *
     * @return A printable representation of this timer.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( '[' );
        sb.append( this.getClass().getSimpleName() );

        if ( _name != null )
        {
            sb.append( ':' );
            sb.append( _name );
        }

        sb.append( " - " );
        sb.append( new Duration( duration() ) );

        if ( isRunning() )
            sb.append( "..." );

        sb.append( ']' );

        return sb.toString();
    }
}
