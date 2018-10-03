/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2010 Michael G. Binz
 */

package org.jdesktop.smack.util;

import java.text.MessageFormat;



/**
 * A time duration.  Used for printing and for splitting millisecond times
 * into their day, hour, minute etc. units.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public final class Duration
{
    /**
     * The number of milliseconds in a second.
     */
    public static final short MS_SEC = 1000;

    /**
     * The number of milliseconds in a minute.
     */
    public static final int MS_MIN = MS_SEC * 60;

    /**
     * The number of milliseconds in an hour.
     */
    public static final int MS_HOUR = MS_MIN * 60;

    /**
     * The number of milliseconds in a day.
     */
    public static final int MS_DAY = MS_HOUR * 24;

    /**
     * The original microsecond value as received in the constructor.
     */
    private final long _durationMs;

    /**
     * True if the passed duration was negative.
     */
    private final boolean _negative;

    /**
     * The day fraction.
     */
    private final long _days;

    /**
     * The hour fraction.
     */
    private final int _hours;

    /**
     * The minute fraction.
     */
    private final int _mins;

    /**
     * The second fraction.
     */
    private final int _secs;

    /**
     * The millisecond fraction.
     */
    private final int _millis;



    /**
     * Create a duration instance.
     *
     * @param ms Duration in milliseconds.
     */
    public Duration( long ms )
    {
        _negative = ms < 0;

        if ( _negative )
            ms = Math.abs( ms );

        _durationMs = ms;

        _days = ms / MS_DAY;
        ms %= MS_DAY;

        _hours = (int)(ms / MS_HOUR);
        ms %= MS_HOUR;

        _mins = (int)(ms / MS_MIN);
        ms %= MS_MIN;

        _secs = (int)(ms / MS_SEC);

        _millis = (int)(ms % MS_SEC);
    }



    /**
     * Get the original microsecond value as received in the constructor.
     *
     * @return The original microsecond value.
     */
    public long getDurationMs()
    {
        return _durationMs;
    }



    /**
     * Get the day fraction.
     *
     * @return The day fraction.
     */
    public long getDays()
    {
        return _days;
    }



    /**
     * Get the hour fraction.
     *
     * @return The hours fraction in the range [0..23].
     */
    public int getHours()
    {
        return _hours;
    }



    /**
     * Get the minute fraction.
     *
     * @return The minute fraction in the range [0..59].
     */
    public int getMinutes()
    {
        return _mins;
    }



    /**
     * Get the second fraction.
     *
     * @return The second fraction in the range [0..59].
     */
    public int getSeconds()
    {
        return _secs;
    }



    /**
     * Get the millisecond fraction.
     *
     * @return The number of milliseconds in the range [0..999].
     */
    public int getMilliseconds()
    {
        return _millis;
    }



    /**
     * Get the duration represented as a string. This has the format
     * '-9999d:99:99:99.9999', the sign prefix and days are optional
     * and only added if needed.
     *
     * @return The string representation of the duration.
     */
    @Override
    public String toString()
    {
        String sign = _negative ? "-" : StringUtils.EMPTY_STRING;

        String yearDayPrefix = StringUtils.EMPTY_STRING;

        if ( _days > 0 )
            yearDayPrefix = _days + "d:";

        String formatted = MessageFormat.format(
                "{0,number,00}:{1,number,00}:{2,number,00}.{3,number,000}",
                _hours,
                _mins,
                _secs,
                _millis );

        return sign + yearDayPrefix + formatted;
    }



    /**
     * Get a string representation of the passed millisecond duration.
     *
     * @param ms The duration to be formatted in milliseconds.
     * @return a string representation of the passed millisecond duration.
     * @see #toString()
     */
    public static String toString( long ms )
    {
        return new Duration( ms ).toString();
    }
}
