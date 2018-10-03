/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package org.jdesktop.application;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.jdesktop.smack.util.FileUtils;

/**
 * An application service that offers a simple means to store short-term,
 * low-value preferences values per user and application.
 * This is intended to be used to save for example ui settings from session
 * to session.
 *
 * This is similar to the {@link Preferences} system, but simpler to use.
 *
 * All put-operations perform an implicit write of a serialized map to the
 * file system. This class is not intended as a transactional high volume
 * storage.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class ApplicationProperties
{
    private static final Logger L =
            Logger.getLogger( ApplicationProperties.class.getName() );

    private final LocalStorage _localStorage;
    private final File _fileName;

    private final Map<String, String> _storage;

    /**
     * Create an instance.
     *
     * @param application The application requesting the service.
     */
    @SuppressWarnings("unchecked")
    ApplicationProperties( Application application )
    {
        _localStorage =
                application.getApplicationService( LocalStorage.class );
        _fileName =
                new File( application.getId() + "_" + application.getVendorId() + ".aps" );

        Map<String, String> localMap;

        ObjectInputStream is = null;
        try
        {
            is = new ObjectInputStream(
                    _localStorage.openInputFile( _fileName ) );

            localMap = (Map<String, String>)is.readObject();
        }
        catch ( Exception e )
        {
            localMap = new HashMap<String, String>();
        }
        finally
        {
            FileUtils.forceClose( is );
        }

        _storage = localMap;
    }

    /**
     * Check if a key is defined.
     *
     * @param client The client class.
     * @param key The key to check for.
     * @return True if the key is defined, false otherwise.
     */
    public boolean containsKey( Class<?> client, String key )
    {
        return _storage.containsKey( makeKey( client, key ) );
    }

    /**
     *
     * @param client The client class.
     * @param key
     */
    public void remove( Class<?> client, String key )
    {
        _storage.remove( makeKey( client, key ) );
        flush();
    }

    /**
     * Store a string property.
     *
     * @param client The client class.
     * @param key The key. Must not be null.
     * @param value The value. Must not be null.s
     */
    public void put( Class<?> client, String key, String value )
    {
        if ( value == null )
            throw new NullPointerException();

        _storage.put(
                makeKey( client, key ),
                value );

        flush();
    }

    /**
     * Get a string value.
     *
     * @param client The client class.
     * @param key The key. Must not be null.
     * @param deflt A default result.
     * @return If the key exists the attached value or the default result.
     */
    public String get( Class<?> client, String key, String deflt )
    {
        String normalizedKey = makeKey( client, key );

        if ( _storage.containsKey( normalizedKey ) )
            return _storage.get( normalizedKey );

        return deflt;
    }

    /**
     * Store a long value.
     *
     * @param client The client class.
     * @param key The key. Must not be null.
     * @param value The value to store.
     */
    public void putLong( Class<?> client, String key, long value )
    {
        _storage.put(
                makeKey( client, key ),
                Long.toString( value ) );

        flush();
    }

    /**
     * Get a long value.
     *
     * @param client The client class.
     * @param key The key. Must not be null.
     * @param def A default result.
     * @return The integer value from the storage or the default value
     * if the key was not set.
     */
    public long getLong( Class<?> client, String key, long def )
    {
        String normalizedKey = makeKey( client, key );

        if ( _storage.containsKey( normalizedKey ) )
        {
            String content = _storage.get( normalizedKey );

            try
            {
                return Long.parseLong( content );
            }
            catch ( Exception ignore )
            {
                L.warning( "Unexpected content: " + content );
            }
        }

        return def;
    }

    /**
     * Store a floating point value.
     *
     * @param client The client class.
     * @param key The key. Must not be null.
     * @param value The value to store.
     */
    public void putDouble( Class<?> client, String key, double value )
    {
        _storage.put(
                makeKey( client, key ),
                Double.toString( value ) );

        flush();
    }

    /**
     * Get a floating point value.
     *
     * @param client The client class.
     * @param key The key. Must not be null.
     * @param def A default result.
     * @return A value from the storage or the passed default value
     * if the key was not set.
     */
    public double getDouble( Class<?> client, String key, double def )
    {
        String normalizedKey = makeKey( client, key );

        if ( _storage.containsKey( normalizedKey ) )
        {
            String content = _storage.get( normalizedKey );

            try
            {
                return Double.parseDouble( content );
            }
            catch ( Exception ignore )
            {
                L.warning( "Unexpected content: " + content );
            }
        }

        return def;
    }

    /**
     * Get the keys defined for the passed client.
     *
     * @param client The client class.
     * @return A newly allocated map holding the available keys. Empty
     * if no keys are defined.
     */
    public Set<String> keys(Class<?> client )
    {
        HashSet<String> result = new HashSet<String>();

        for ( String c : _storage.keySet() )
            if ( c.startsWith( client.getName() ))
                result.add( c );

        return result;
    }

    /**
     * Flushes our storage to persistent storage.
     */
    private void flush()
    {
        ObjectOutputStream oos = null;

        try
        {
            oos = new ObjectOutputStream( _localStorage.openOutputFile( _fileName ) );
            oos.writeObject( _storage );
            oos.flush();
        }
        catch ( IOException e )
        {
            L.log( Level.WARNING, "Storing application properties failed.", e );
        }
        finally
        {
            FileUtils.forceClose( oos );
        }
    }

    private String makeKey( Class<?> c, String key )
    {
        if ( key == null || c == null )
            throw new NullPointerException();

        return c.getName() + "." + key;
    }
}
