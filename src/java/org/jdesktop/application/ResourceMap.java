
/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package org.jdesktop.application;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import org.jdesktop.application.ResourceConverter.ResourceConverterException;
import org.jdesktop.application.util.PlatformType;
import org.jdesktop.smack.util.ReflectionUtils;
import org.jdesktop.smack.util.ResourceUtils;
import org.jdesktop.smack.util.StringUtils;

/**
 * A read-only encapsulation of one or more ResourceBundles that adds
 * automatic string conversion, support for field and Swing component
 * property injection, string resource variable substitution, and chaining.
 * <p>
 * ResourceMaps are typically obtained with the {@code ApplicationContext}
 * {@link ApplicationContext#getResourceMap getResourceMap} method
 * which lazily creates per Application, package, and class ResourceMaps that
 * are linked together with the ResourceMap <tt>parent</tt> property.
 * <p>
 * An individual ResourceMap provides read-only access to all of the
 * resources defined by the ResourceBundles named when the ResourceMap
 * was created as well as all of its parent ResourceMaps.  Resources
 * are retrieved with the <tt>getObject</tt> method which requires both
 * the name of the resource and its expected type.  The latter is used
 * to convert strings if necessary.
 * Converted values are cached.  As a convenience, <tt>getObject</tt>
 * wrapper methods for common GUI types, like <tt>getFont</tt>,
 * and <tt>getColor</tt>, are provided.
 * <p>
 * The <tt>getObject</tt> method scans raw string resource values
 * for <tt>${resourceName}</tt> variable substitutions before
 * performing string conversion.  Variables named this way can
 * refer to String resources defined anywhere in their ResourceMap
 * or any parent ResourceMap.  The special variable <tt>${null}</tt>
 * means that the value of the resource will be null.
 * <p>
 * ResourceMaps can be used to "inject" resource values into Swing
 * component properties and into object fields.  The
 * <tt>injectComponents</tt> method uses Component names ({@link
 * Component#setName}) to match resources names with properties.  The
 * <tt>injectFields</tt> method sets fields that have been tagged with
 * the <tt>&#064;Resource</tt> annotation to the value of resources
 * with the same name.
 *
 * @version $Rev$
 * @author Michael Binz
 * @author Hans Muller (Hans.Muller@Sun.COM)
 * @see #injectComponents
 * @see #injectFields
 * @see ResourceConverter
 * @see ResourceBundle
 */
public class ResourceMap
{
    private static final Logger LOG =
            Logger.getLogger(ResourceMap.class.getName());

    public static final String KEY_PLATFORM = "platform";

    private final static Object NULL_RESOURCE = "null resource";
    private final ClassLoader _classLoader;
    private final ResourceMap _parent;
    private final List<String> _bundleNames;
    private final String _resourcesDir;
    private Map<String, Object> _bundlesMap = null;
    private final Locale _locale;    // ...
    private Set<String> bundlesMapKeysP = null;     // set getBundlesMapKeys()
    private PlatformType platform;

    /**
     * Creates a ResourceMap that contains all of the resources
     * defined in the named {@link ResourceBundle}s as well as
     * (recursively) the <tt>parent</tt> ResourceMap.  The <tt>parent</tt>
     * may be null.  Typically just one ResourceBundle is specified
     * however one might name additional ResourceBundles that contain
     * platform or Swing look and feel specific resources.  When multiple
     * bundles are named, a resource defined in bundle<sub>n</sub> will
     * override the same resource defined in bundles<sub>0..n-1</sub>.
     * In other words bundles named later in the argument list take
     * precedence over the bundles named earlier.
     * <p>
     * ResourceBundles are loaded with the specified ClassLoader.  If
     * <tt>classLoader</tt> is null, an IllegalArgumentException is
     * thrown.
     * <p>
     * At least one bundleName must be specified and all of the
     * bundleNames must be non-empty strings, or an
     * IllegalArgumentException is thrown.  The bundles are
     * listed in priority order, highest priority first.  In other
     * words, resources in the the first ResourceBundle named first,
     * shadow resources with the same name later in the list.
     * <p>
     * All of the bundleNames
     * must share a common package prefix.  The package prefix
     * implicitly specifies the resources directory
     * (see {@link #getResourcesDir}). For example, the resources
     * directory for bundle names "myapp.resources.foo" and
     * "myapp.resources.bar", would be "myapp/resources/".  If
     * bundle names don't share a common package prefix, then
     * an IllegalArgumentException is thrown.
     *
     * @param parent parent ResourceMap or null
     * @param classLoader the ClassLoader to be used to load the ResourceBundle
     * @param bundleNames names of the ResourceBundle to be loaded
     * @throws IllegalArgumentException if classLoader or any bundleName is
     *   null, if no bundleNames are specified, if any bundleName is an
     *   empty (zero length) String, or if all of the bundleNames don't
     *   have a common package prefix
     * @see ResourceBundle
     * @see #getParent
     * @see #getClassLoader
     * @see #getResourcesDir
     * @see #getBundleNames
     */
    ResourceMap( Locale locale, ResourceMap parent, ClassLoader classLoader, List<String> bundleNames) {
        if (classLoader == null) {
            throw new IllegalArgumentException("null ClassLoader");
        }
        if ((bundleNames == null) || (bundleNames.size() == 0)) {
            throw new IllegalArgumentException("no bundle specified");
        }
        for (String bn : bundleNames) {
            if ( ! StringUtils.hasContent( bn )) {
                throw new IllegalArgumentException("invalid bundleName: \"" + bn + "\"");
            }
        }

        _locale = locale;
        String bpn = bundlePackageName(bundleNames.get(0));
        for (String bn : bundleNames) {
            if (!bpn.equals(bundlePackageName(bn))) {
                throw new IllegalArgumentException("bundles not colocated: \"" + bn + "\" != \"" + bpn + "\"");
            }
        }
        _parent = parent;
        _classLoader = classLoader;
        _bundleNames = Collections.unmodifiableList(new ArrayList<String>(bundleNames));
        _resourcesDir = bpn.replace(".", "/") + "/";
    }

    /**
     *
     * @param bundleName
     * @return
     */
    private String bundlePackageName(String bundleName) {
        int i = bundleName.lastIndexOf(".");
        return (i == -1) ? StringUtils.EMPTY_STRING : bundleName.substring(0, i);
    }

    /**
     * Returns the parent ResourceMap, or null.  Logically, this ResourceMap
     * contains all of the resources defined here and (recursively) in the
     * parent.
     *
     * @return the parent ResourceMap or null
     */
    private ResourceMap getParent() {
        return _parent;
    }

//    /**
//     * Returns the names of the ResourceBundles that define the
//     * resources contained by this ResourceMap.
//     *
//     * @return the names of the ResourceBundles in this ResourceMap
//     */
//    private List<String> getBundleNames() {
//        return _bundleNames;
//    }

    /**
     * Returns the ClassLoader used to load the ResourceBundles for this
     * ResourceMap.
     *
     * @return the classLoader constructor argument
     */
    private ClassLoader getClassLoader() {
        return _classLoader;
    }

    /**
     * Returns the resources directory that contains all of the ResourceBundles
     * in this ResourceMap.  It can be used with the the classLoader property
     * to load files from the resources directory.  For example:
     * <pre>
     * String filename = myResourceMap.getResourcesDir() + "myIcon.png";
     * URL url = myResourceMap.getClassLoader().getResource(filename);
     * new ImageIcon(iconURL);
     * </pre>
     *
     * @return the the resources directory for this ResourceMap
     */
    private String getResourcesDir() {
        return _resourcesDir;
    }

    /**
     * Lazily flattens all of the ResourceBundles named in bundleNames
     * into a single Map - bundlesMapP.  The bundleNames list is in
     * priority order, the first entry shadows later entries.
     */
    private synchronized Map<String, Object> getBundlesMap() {
        if (_bundlesMap == null) {
            String resourceSuffix = getPlatform().getResourceSuffix();
            Map<String, Object> bundlesMap = new ConcurrentHashMap<String, Object>();
            for (int i = _bundleNames.size() - 1; i >= 0; i--) {
                populateResourceMap(_bundleNames.get(i), bundlesMap);
                if (!resourceSuffix.isEmpty())
                    populateResourceMap(_bundleNames.get(i)+"_"+resourceSuffix, bundlesMap);
            }
            _bundlesMap = bundlesMap;
        }
        return _bundlesMap;
    }

    /**
     * Populates the passed Map with the preprocessed values from the
     * named resource bundle.
     *
     * @param bundleName The resource bundle whose entries are processed.
     * @param bundlesMap The map to populate.
     */
    private void populateResourceMap(String bundleName, Map<String, Object> bundlesMap) {

        Map<String,String> ss = ResourceUtils.getPreprocessedResourceBundle( bundleName, _locale, _classLoader );

        if ( ss != null )
            bundlesMap.putAll( ss );

        // This allows to have additional keys defined in a properties
        // file named like the bundle.  This is experimental.

        File propertiesOverride = new File( bundleName + ".properties" );

        if ( ! propertiesOverride.exists() )
            return;
        if ( ! propertiesOverride.canRead() )
            return;

        LOG.info( "Reading resource override file: " + propertiesOverride.getPath() );

        Properties p = new Properties();

        try
        {
            p.load( new FileReader( propertiesOverride ) );
        }
        catch ( Exception e )
        {
            LOG.log(
                    Level.WARNING,
                    "Failed reading: " + propertiesOverride.getPath(),
                    e );
            return;
        }

        for ( Object c : p.keySet() )
        {
            String key = c.toString();
            Object v0 = p.get( key );
            String value = v0 == null ? null : v0.toString();

            bundlesMap.put( key, value );
        }
    }

    /**
     *
     * @param key
     */
    private void checkNullKey(String key)
    {
        if ( StringUtils.isEmpty( key ) )
            throw new IllegalArgumentException("empty key");
    }

    /**
     *
     * @return
     */
    private synchronized Set<String> getBundlesMapKeys() {
        if (bundlesMapKeysP == null) {
            Set<String> allKeys = new HashSet<String>(getResourceKeySet());
            ResourceMap parent = getParent();
            if (parent != null) {
                allKeys.addAll(parent.keySet());
            }
            bundlesMapKeysP = Collections.unmodifiableSet(allKeys);
        }
        return bundlesMapKeysP;
    }

    /**
     * Return a unmodifiable {@link Set} that contains all of the keys in
     * this ResourceMap and (recursively) its parent ResourceMaps.
     *
     * @return all of the keys in this ResourceMap and its parent
     * @see #getParent
     */
    private Set<String> keySet() {
        return getBundlesMapKeys();
    }

    /**
     * Returns true if this resourceMap or its parent (recursively) contains
     * the specified key.
     *
     * @return true if this resourceMap or its parent contains the specified key.
     * @see #getParent
     * @see #keySet
     */
    public boolean containsKey(String key) {
        checkNullKey(key);
        if (containsResourceKey(key)) {
            return true;
        } else {
            ResourceMap parent = getParent();
            return (parent != null) && parent.containsKey(key);
        }
    }

    public PlatformType getPlatform() {
        if (platform != null) return platform;
        if (_parent != null) return _parent.getPlatform();
        return PlatformType.DEFAULT;
    }

    public void setPlatform(PlatformType platform) {
        if(platform == null) throw new IllegalArgumentException("Platform could not be null.");
        if (this.platform != null) throw new IllegalStateException("The platform attribute is already set for this resource map.");
        this.platform = platform;
    }

    /**
     * Unchecked exception thrown by {@link #getObject} when resource lookup
     * fails, for example because string conversion fails.  This is
     * not a missing resource exception.  If a resource isn't defined
     * for a particular key, getObject does not throw an exception.
     *
     * @see #getObject
     */
    @SuppressWarnings("serial")
    public static class LookupException extends RuntimeException {

        private final Class<?> type;
        private final String key;

        /**
         * Constructs an instance of this class with some useful information
         * about the failure.
         *
         * @param msg the detail message
         * @param type the type of the resource
         * @param key the name of the resource
         */
        public LookupException(String msg, String key, Class<?> type) {
            super(String.format("%s: resource %s, type %s", msg, key, type));
            this.key = key;
            this.type = type;
        }

        /**
         * Returns the type of the resource for which lookup failed.
         * @return the resource type
         */
        public Class<?> getType() {
            return type;
        }

        /**
         * Returns the type of the name of resource for which lookup failed.
         * @return the resource name
         */
        public String getKey() {
            return key;
        }
    }

    /**
     * By default this method is used by {@code keySet} to
     * get the names of the resources defined in this ResourceMap.
     * This method lazily loads the ResourceBundles named
     * by the constructor.
     * <p>
     * The protected {@code getResource}, {@code putResource}, and
     * {@code containsResourceKey}, {@code getResourceKeySet} abstract
     * the internal representation of this ResourceMap's list of
     * {@code ResourceBundles}.  Most applications can ignore them.
     *
     * @return the names of the resources defined in this ResourceMap
     * @see #getResource
     * @see #putResource
     * @see #containsResourceKey
     */
    private Set<String> getResourceKeySet() {
        Map<String, Object> bundlesMap = getBundlesMap();
        if (bundlesMap == null) {
            return Collections.emptySet();
        } else {
            return bundlesMap.keySet();
        }
    }

    /**
     * By default this method is used by {@code getObject} to see
     * if a resource is defined by this ResourceMap. This method lazily
     * loads the ResourceBundles named by the constructor.
     * <p>
     * The protected {@code getResource}, {@code putResource}, and
     * {@code containsResourceKey}, {@code getResourceKeySet} abstract
     * the internal representation of this ResourceMap's list of
     * {@code ResourceBundles}.  Most applications can ignore them.
     * <p>
     * If {@code key} is null, an IllegalArgumentException is thrown.
     *
     * @param key the name of the resource
     * @return true if a resource named {@code key} is defined in this ResourceMap
     * @see #getResource
     * @see #putResource
     * @see #getResourceKeySet
     */
    private boolean containsResourceKey(String key) {
        checkNullKey(key);
        Map<String, Object> bundlesMap = getBundlesMap();
        return (bundlesMap != null) && bundlesMap.containsKey(key);
    }

    /**
     * By default this method is used by {@code getObject} to look up
     * resource values in the internal representation of the {@code
     * ResourceBundles} named when this ResourceMap was constructed.
     * If a resource named {@code key} is {@link #containsResourceKey defined}
     * then its value is returned, otherwise null.
     * The {@code getResource} method lazily loads the
     * ResourceBundles named by the constructor.
     * <p>
     * The protected {@code getResource}, {@code putResource}, and
     * {@code containsResourceKey}, {@code getResourceKeySet} abstract
     * the internal representation of this ResourceMap's list of
     * {@code ResourceBundles}.  Most applications can ignore them.
     * <p>
     * If {@code key} is null, an IllegalArgumentException is thrown.
     *
     * @param key the name of the resource
     * @return the value of the resource named {@code key} (can be null)
     * @see #putResource
     * @see #containsResourceKey
     * @see #getResourceKeySet
     */
    private Object getResource(String key) {
        checkNullKey(key);
        Map<String, Object> bundlesMap = getBundlesMap();
        Object value = (bundlesMap != null) ? bundlesMap.get(key) : null;
        return (value == NULL_RESOURCE) ? null : value;
    }

    /**
     * By default this method is used by {@code getObject} to cache
     * values that have been retrieved, evaluated (as in ${key}
     * expressions), and string converted.  A subclass could override
     * this method to defeat caching or to refine the caching strategy.
     * The {@code putResource} method lazily loads ResourceBundles.
     * <p>
     * The protected {@code getResource}, {@code putResource}, and
     * {@code containsResourceKey}, {@code getResourceKeySet} abstract
     * the internal representation of this ResourceMap's list of
     * {@code ResourceBundles}.   Most applications can ignore them.
     * <p>
     * If {@code key} is null, an IllegalArgumentException is thrown.
     *
     * @param key the name of the resource
     * @param value the value of the resource (can be null)
     * @see #getResource
     * @see #containsResourceKey
     * @see #getResourceKeySet
     */
    void putResource(String key, Object value) {
        checkNullKey(key);

        if (KEY_PLATFORM.equals(key)) {
            setPlatform((PlatformType) value);
        } else {
            Map<String, Object> bundlesMap = getBundlesMap();
            if (bundlesMap != null) {
                bundlesMap.put(key, (value == null) ? NULL_RESOURCE : value);
            }
        }
    }

    /**
     * Returns the value of the resource named <tt>key</tt>, or null
     * if no resource with that name exists.  A resource exists if
     * it's defined in this ResourceMap or (recursively) in the
     * ResourceMap's parent.
     * <p>
     * String resources may contain variables that name other
     * resources.  Each <tt>${variable-key}</tt> variable is replaced
     * with the value of a string resource named
     * <tt>variable-key</tt>.  For example, given the following
     * resources:
     * <pre>
     * Application.title = My Application
     * ErrorDialog.title = Error: ${application.title}
     * WarningDialog.title = Warning: ${application.title}
     * </pre>
     * The value of <tt>"WarningDialog.title"</tt> would be
     * <tt>"Warning: My Application"</tt>.  To include "${" in a
     * resource, insert a backslash before the "$".  For example, the
     * value of <tt>escString</tt> in the example below, would
     * be <tt>"${hello}"</tt>:
     * <pre>
     * escString = \\${hello}
     * </pre>
     * Note that, in a properties file, the backslash character is
     * used for line continuation, so we've had to escape that too.
     * If the value of a resource is the special variable <tt>${null}</tt>,
     * then the resource will be removed from this ResourceMap.
     * <p>
     * The value returned by getObject will be of the specified type.  If a
     * string valued resource exists for <tt>key</tt>, and <tt>type</tt> is not
     * String.class, the value will be converted using a
     * ResourceConverter and the ResourceMap entry updated with the
     * converted value.
     * <p>
     * If the named resource exists and an error occurs during lookup,
     * then a ResourceMap.LookupException is thrown.  This can
     * happen if string conversion fails, or if resource parameters
     * can't be evaluated, or if the existing resource is of the wrong
     * type.
     * <p>
     * An IllegalArgumentException is thrown if key or type are null.
     *
     * @param key resource name
     * @param type resource type
     * @return the value of the resource
     * @see #getParent
     * @see ResourceConverter#forType
     * @see ResourceMap.LookupException
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> or <tt>type</tt> are null
     */
    private Object getObject(String key, Class<?> type)
    {
        checkNullKey(key);
        if (type == null) {
            throw new IllegalArgumentException("null type");
        }

        type = ReflectionUtils.normalizePrimitives( type );

        if ( ! containsResourceKey( key ) )
        {
            ResourceMap parent = getParent();
            if ( parent == null )
                return null;

            return parent.getObject( key, type );
        }

        Object value = getResource( key );
        // TODO worth a warning?
        if ( value == null )
            return null;

        // If the value we've found is
        // the expected type, then we're done.  If the expected
        // type is primitive and the value is the corresponding
        // object type then we're done too.  Otherwise,
        // if it's a String, then try and convert the String
        // and replace the original entry,
        // otherwise return null.
        if ( type.isAssignableFrom( value.getClass() ))
            return value;

        if ( ! ( value instanceof String ) )
            throw new LookupException( "named resource has wrong type", key, type);

        ResourceConverter stringConverter = ResourceConverter.forType(type);
        if ( stringConverter == null )
            throw new LookupException( "no StringConverter for required type", key, type);

        try {
            value = stringConverter.parseString((String)value, this);
            putResource(key, value);
        } catch (ResourceConverterException e) {
            // todo better error message.
            String msg = "Conversion failed";
            LookupException lfe = new LookupException(msg, key, type);
            lfe.initCause(e);
            throw lfe;
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T)getObject( key, type );
    }

    /**
     * If no arguments are specified, return the String value
     * of the resource named <tt>key</tt>.  This is
     * equivalent to calling <tt>getObject(key, String.class)</tt>
     * If arguments are provided, then the type of the resource
     * named <tt>key</tt> is assumed to be
     * {@link String#format(String, Object...) format} string,
     * which is applied to the arguments if it's non null.
     * For example, given the following resources
     * <pre>
     * hello = Hello %s
     * </pre>
     * then the value of <tt>getString("hello", "World")</tt> would
     * be <tt>"Hello World"</tt>.
     *
     * @param key
     * @param args
     * @return the String value of the resource named <tt>key</tt>
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     * @see String#format(String, Object...)
     */
    public String getString(String key, Object... args) {
        if (args.length == 0) {
            return (String) getObject(key, String.class);
        } else {
            String format = (String) getObject(key, String.class);
            return (format == null) ? null : String.format(format, args);
        }
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Boolean.class)</tt>.
     *
     * @param key the name of the resource
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @return the Boolean value of the resource named key
     * @see #getObject
     */
    public final Boolean getBoolean(String key) {
        return (Boolean) getObject(key, Boolean.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Integer.class)</tt>.
     *
     * @param key the name of the resource
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @return the Integer value of the resource named key
     * @see #getObject
     */
    public final Integer getInteger(String key) {
        return (Integer) getObject(key, Integer.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Long.class)</tt>.
     *
     * @param key the name of the resource
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @return the Long value of the resource named key
     * @see #getObject
     */
    public final Long getLong(String key) {
        return (Long) getObject(key, Long.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Short.class)</tt>.
     *
     * @param key the name of the resource
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @return the Short value of the resource named key
     * @see #getObject
     */
    public final Short getShort(String key) {
        return (Short) getObject(key, Short.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Byte.class)</tt>.
     *
     * @param key the name of the resource
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @return the Byte value of the resource named key
     * @see #getObject
     */
    public final Byte getByte(String key) {
        return (Byte) getObject(key, Byte.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Float.class)</tt>.
     *
     * @param key the name of the resource
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @return the Float value of the resource named key
     * @see #getObject
     */
    public final Float getFloat(String key) {
        return (Float) getObject(key, Float.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Double.class)</tt>.
     *
     * @param key the name of the resource
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @return the Double value of the resource named key
     * @see #getObject
     */
    public final Double getDouble(String key) {
        return (Double) getObject(key, Double.class);
    }

    /**
     *
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Icon.class)</tt>.  This method
     * relies on the ImageIcon ResourceConverter that's registered
     * by this class.  See {@link #getImageIcon} for more information.
     *
     *
     * @param key the name of the resource
     * @return the Icon value of the resource named key
     * @see #getObject
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     */
    public final Icon getIcon(String key) {
        return (Icon) getObject(key, Icon.class);
    }

    /**
     *
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, ImageIcon.class)</tt>.  This method
     * relies on the ImageIcon ResourceConverter that's registered
     * by this class.
     * <p>
     * If the resource named <tt>key</tt> is a String, it should name
     * an image file to be found in the resources subdirectory that
     * also contains the ResourceBundle (typically a ".properties"
     * file) that was used to create the corresponding ResourceMap.
     * <p>
     * For example, given the ResourceMap produced by
     * <tt>Application.getClass(com.mypackage.MyClass.class)</tt>,
     * and a ResourceBundle called <tt>MyClass.properties</tt>
     * in <tt>com.mypackage.resources</tt>:
     * <pre>
     * openIcon = myOpenIcon.png
     * </pre>
     * then <tt>resourceMap.getIcon("openIcon")</tt> would load
     * the image file called "myOpenIcon.png" from the resources
     * subdirectory, effectively like this:
     * <pre>
     * String filename = myResourceMap.getResourcesDir() + "myOpenIcon.png";
     * URL url = myResourceMap.getClassLoader().getResource(filename);
     * new ImageIcon(iconURL);
     * </pre>
     *
     *
     * @param key the name of the resource
     * @return the ImageIcon value of the resource named key
     * @see #getObject
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     */
    public final ImageIcon getImageIcon(String key) {
        return (ImageIcon) getObject(key, ImageIcon.class);
    }

    /**
     *
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Font.class)</tt>.   This method relies
     * on the Font ResourceConverter that's registered by this class.
     * Font resources may be defined with strings that are
     * recognized by {@link Font#decode},
     * <tt><i>face</i>-<i>STYLE</i>-<i>size</i></tt>.
     * For example:
     * <pre>
     * myFont = Arial-PLAIN-12
     * </pre>
     *
     *
     * @param key the name of the resource
     * @return the Font value of the resource named key
     * @see #getObject
     * @see ResourceConverter#forType
     * @see Font#decode
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalResourceConverteron if <tt>key</tt> is null
     */
    public final Font getFont(String key) {
        return (Font) getObject(key, Font.class);
    }

    /**
     *
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Color.class)</tt>.  This method relies on the
     * Color ResourceConverter that's registered by this class.  It defines
     * an improved version of <tt>Color.decode()</tt>
     * that supports colors with an alpha channel and comma
     * separated RGB[A] values. Legal format for color resources are:
     * <pre>
     * myHexRGBColor = #RRGGBB
     * myHexAlphaRGBColor = #AARRGGBB
     * myRGBColor = R, G, B
     * myAlphaRGBColor = R, G, B, A
     * </pre>
     * The first two examples, with the leading "#" encode the color
     * with 3 or 4 hex values and the latter with integer values between
     * 0 and 255.  In both cases the value represented by "A" is the
     * color's (optional) alpha channel.
     *
     *
     * @param key the name of the resource
     * @return the Color value of the resource named key
     * @see #getObject
     * @see ResourceConverter#forType
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException ResourceConverter is null
     */
    public final Color getColor(String key) {
        return (Color) getObject(key, Color.class);
    }

    /**
     *
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, KeyStroke.class)</tt>.  This method relies on the
     * KeyStroke ResourceConverter that's registered by this class and
     * uses {@link KeyStroke#getKeyStroke(String s)} to convert strings.
     *
     * For example, <tt>pressed F</tt> reports the "F" key, and <tt>control
     * pressed F</tt> reports Control-F. See the <tt>KeyStroke</tt> JavaDoc for
     * more information.
     *
     * @param key the name of the resource
     * @return the KeyStroke value of the resource named key
     * @see #getObject
     * @see KeyStroke#getKeyStroke
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     */
    public final KeyStroke getKeyStroke(String key) {
        return (KeyStroke) getObject(key, KeyStroke.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getKeyStroke(key).getKeyCode()</tt>.  If there's
     * no resource named <tt>key</tt> then null is returned.
     *
     * @param key the name of the resource
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @return the KeyCode value of the resource named key
     * @see #getObject
     */
    public Integer getKeyCode(String key) {
        KeyStroke ks = getKeyStroke(key);
        return (ks != null) ? ks.getKeyCode() : null;
    }

    /**
     * Unchecked exception thrown by {@link #injectComponent} and
     * {@link #injectComponents} when a property value specified by
     * a resource can not be set.
     *
     * @see #injectComponent
     * @see #injectComponents
     */
    @SuppressWarnings("serial")
    public static class PropertyInjectionException extends RuntimeException {

        private final String key;
        private final Object component;
        private final String propertyName;

        /**
         * Constructs an instance of this class with some useful information
         * about the failure.
         *
         * @param msg the detail message
         * @param key the name of the resource
         * @param target the component whose property couldn't be set
         * @param propertyName the name of the component property
         */
        public PropertyInjectionException(String msg, String key, Object target, String propertyName) {
            super(String.format("%s: resource %s, property %s, component %s", msg, key, propertyName, target));
            this.key = key;
            this.component = target;
            this.propertyName = propertyName;
        }

        /**
         * Returns the the name of the resource whose value was to be used to set the property.
         * @return the resource name
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns the bean whose property could not be set.
         * @return The target bean.
         */
        public Object getTarget() {
            return component;
        }

        /**
         * Returns the the name of property that could not be set
         * @return the property name
         */
        public String getPropertyName() {
            return propertyName;
        }
    }

    /**
     *
     * @param component
     * @param pd
     * @param key
     */
    private void injectComponentProperty(Component component, PropertyDescriptor pd, String key) {
        Method setter = pd.getWriteMethod();
        Class<?> type = pd.getPropertyType();
        if ((setter != null) && (type != null) && containsKey(key)) {
            Object value = getObject(key, type);
            String propertyName = pd.getName();
            try {
                // Note: this could be generalized, we could delegate
                // to a component property injector.
                if ("text".equals(propertyName) && (component instanceof AbstractButton)) {
                    MnemonicText.configure(component, (String) value);
                } else if ("text".equals(propertyName) && (component instanceof JLabel)) {
                    MnemonicText.configure(component, (String) value);
                } else {
                    setter.invoke(component, value);
                }
            } catch (Exception e) {
                String pdn = pd.getName();
                String msg = "property setter failed";
                RuntimeException re = new PropertyInjectionException(msg, key, component, pdn);
                re.initCause(e);
                throw re;
            }
        } else if (type != null) {
            String pdn = pd.getName();
            String msg = "no value specified for resource";
            throw new PropertyInjectionException(msg, key, component, pdn);
        } else if (setter == null) {
            String pdn = pd.getName();
            String msg = "can't set read-only property";
            throw new PropertyInjectionException(msg, key, component, pdn);
        }
    }

    /**
     *
     * @param componentName
     * @param component
     */
    private void injectComponentProperties(String componentName, Component component) {
        if ( componentName == null )
            return;

        /* Optimization: punt early if componentName doesn't
         * appear in any componentName.propertyName resource keys
         */
        boolean matchingResourceFound = false;
        for (String key : keySet()) {
            int i = key.lastIndexOf(".");
            if ((i != -1) && componentName.equals(key.substring(0, i))) {
                matchingResourceFound = true;
                break;
            }
        }
        if (!matchingResourceFound) {
            return;
        }
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(component.getClass());
        } catch (IntrospectionException e) {
            String msg = "introspection failed";
            RuntimeException re = new PropertyInjectionException(msg, null, component, null);
            re.initCause(e);
            throw re;
        }
        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        if ((pds != null) && (pds.length > 0)) {
            for (String key : keySet()) {
                int i = key.lastIndexOf(".");
                String keyComponentName = (i == -1) ? null : key.substring(0, i);
                if (componentName.equals(keyComponentName)) {
                    if ((i + 1) == key.length()) {
                        /* key has no property name suffix, e.g. "myComponentName."
                         * This is probably a mistake.
                         */
                        String msg = "component resource lacks property name suffix";
                        LOG.warning(msg);
                        break;
                    }
                    String propertyName = key.substring(i + 1);
                    boolean matchingPropertyFound = false;
                    for (PropertyDescriptor pd : pds) {
                        if (pd.getName().equals(propertyName)) {
                            injectComponentProperty(component, pd, key);
                            matchingPropertyFound = true;
                            break;
                        }
                    }
                    if (!matchingPropertyFound) {
                        String msg = String.format(
                                "[resource %s] component named %s doesn't have a property named %s",
                                key, componentName, propertyName);
                        LOG.warning(msg);
                    }
                }
            }
        }
    }

    /**
     * Set each property in <tt>target</tt> to the value of
     * the resource named <tt><i>componentName</i>.propertyName</tt>,
     * where  <tt><i>componentName</i></tt> is the value of the
     * target component's name property, i.e. the value of
     * <tt>target.getName()</tt>.  The type of the resource must
     * match the type of the corresponding property.  Properties
     * that aren't defined by a resource aren't set.
     * <p>
     * For example, given a button configured like this:
     * <pre>
     * myButton = new JButton();
     * myButton.setName("myButton");
     * </pre>
     * And a ResourceBundle properties file with the following
     * resources:
     * <pre>
     * myButton.text = Hello World
     * myButton.foreground = 0, 0, 0
     * myButton.preferredSize = 256, 256
     * </pre>
     * Then <tt>injectComponent(myButton)</tt> would initialize
     * myButton's text, foreground, and preferredSize properties
     * to <tt>Hello World</tt>, <tt>new Color(0,0,0)</tt>, and
     * <tt>new Dimension(256,256)</tt> respectively.
     * <p>
     * This method calls {@link #getObject} to look up resources
     * and it uses {@link Introspector#getBeanInfo} to find
     * the target component's properties.
     * <p>
     * If target is null an IllegalArgumentException is thrown.  If a
     * resource is found that matches the target component's name but
     * the corresponding property can't be set, an (unchecked) {@link
     * PropertyInjectionException} is thrown.
     *
     *
     *
     * @param target the Component to inject
     * @see #injectComponents
     * @see #getObject
     * @see ResourceConverter#forType
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws PropertyInjectionException if a property specified by a resource can't be set
     * @throws IllegalArgumentException if target is null
     */
    public void injectComponent(Component target) {
        if (target == null) {
            throw new IllegalArgumentException("null target");
        }
        injectComponentProperties(target.getName(), target);
    }

    /**
     * Applies {@link #injectComponent} to each Component in the
     * hierarchy with root <tt>root</tt>.
     *
     * @param root the root of the component hierarchy
     * @throws PropertyInjectionException if a property specified by a resource can't be set
     * @throws IllegalArgumentException if target is null
     * @see #injectComponent
     */
    public void injectComponents(Component root) {
        injectComponent(root);
        if (root instanceof JMenu) {
            /* Warning: we're bypassing the popupMenu here because
             * JMenu#getPopupMenu creates it; doesn't seem right
             * to do so at injection time.  Unfortunately, this
             * means that attempts to inject the popup menu's
             * "label" property will fail.
             */
            JMenu menu = (JMenu) root;
            for (Component child : menu.getMenuComponents()) {
                injectComponents(child);
            }
        } else if (root instanceof Container) {
            Container container = (Container) root;
            for (Component child : container.getComponents()) {
                injectComponents(child);
            }
        }
    }

    /**
     * Unchecked exception thrown by {@link #injectFields} when
     * an error occurs while attempting to set a field (a field that
     * had been marked with <tt>&#064;Resource</tt>).
     *
     * @see #injectFields
     */
    @SuppressWarnings("serial")
    public static class InjectFieldException extends RuntimeException {

        private final Field field;
        private final Object target;
        private final String key;

        /**
         * Constructs an instance of this class with some useful information
         * about the failure.
         *
         * @param msg the detail message
         * @param field the Field we were attempting to set
         * @param target the object whose field we were attempting to set
         * @param key the name of the resource
         * @param cause the exception that triggered this exception.
         */
        public InjectFieldException(String msg, Field field, Object target, String key, Throwable cause) {
            super(String.format("%s: resource %s, field %s, target %s", msg, key, field, target), cause);
            this.field = field;
            this.target = target;
            this.key = key;
        }

        /**
         * Return the Field whose value couldn't be set.
         * @return the field whose value couldn't be set
         */
        public Field getField() {
            return field;
        }

        /**
         * Return the Object whose Field we were attempting to set
         * @return the Object whose Field we were attempting to set
         */
        public Object getTarget() {
            return target;
        }

        /**
         * Returns the type of the name of resource for which lookup failed.
         * @return the resource name
         */
        public String getKey() {
            return key;
        }
    }

    /**
     * Inject a single field.
     *
     * @param field The field to inject.
     * @param target The target object instance.
     * @param key The resource key.
     */
    private void injectField( Field field, Object target, String key )
    {
        if (!field.isAccessible())
            field.setAccessible(true);

        Class<?> type = field.getType();

        if ( Component.class.isAssignableFrom( type ) )
        {
            Component fieldValue = null;
            try
            {
                fieldValue = (Component)field.get( target );
            }
            catch ( Exception e )
            {
                throw new InjectFieldException("unable to get field's value", field, target, key, e);
            }

            if ( fieldValue == null )
                throw new InjectFieldException( "null component field marked with @Resource", field, target, key, null );
            // TODO if null try to create instance using deflt ctor?

            injectComponentProperties( key, fieldValue );
        }
        else
        {
            Object value = getObject(key, type);

            if ( value == null )
            {
                LOG.warning( "No value for @Resource(" + key + ")" );
                return;
            }

            try {
                field.set(target, value);
            }
            catch (Exception e) {
                throw new InjectFieldException("unable to set field's value", field, target, key, e);
            }
        }
    }

    /**
     * Set each field with a <tt>&#064;Resource</tt> annotation in the target object,
     * to the value of a resource whose name is the simple name of the target
     * class followed by "." followed by the name of the field.  If the
     * key <tt>&#064;Resource</tt> parameter is specified, then a resource with that name
     * is used instead.  Array valued fields can also be initialized.
     * For example:
     * <pre>
     * class MyClass {
     *   &#064;Resource String sOne;
     *   &#064;Resource(key="sTwo") String s2;
     *   &#064;Resource int[] numbers;
     * }
     * </pre>
     * Given the previous class and the following resource file:
     * <pre>
     * MyClass.sOne = One
     * sTwo = Two
     * MyClass.numbers = 10 11
     * </pre>
     * Then <tt>injectFields(new MyClass())</tt> would initialize the MyClass
     * <tt>sOne</tt> field to "One", the <tt>s2</tt> field to "Two", and the
     * two elements of the numbers array to 10 and 11.
     * <p>
     * If <tt>target</tt> is null an IllegalArgumentException is
     * thrown.  If an error occurs during resource lookup, then an
     * unchecked LookupException is thrown.  If a target field marked
     * with <tt>&#064;Resource</tt> can't be set, then an unchecked
     * InjectFieldException is thrown.
     *
     * @param target the object whose fields will be initialized
     * @param targetType The type of the target object to inject.
     * This is used to explicitly inject super-class resources.
     * @throws LookupException if an error occurs during lookup or string conversion
     * @throws InjectFieldException if a field can't be set
     * @throws IllegalArgumentException if target is null
     * @see #getObject
     */
    void injectFields(Object target, Class<?> targetType) {
        if (target==null)
            throw new IllegalArgumentException("null target");
        if (targetType.isPrimitive())
            throw new IllegalArgumentException("primitive target");
        if (targetType.isArray())
            throw new IllegalArgumentException("array target");

        String keyPrefix = targetType.getSimpleName() + ".";

        for (Field field : targetType.getDeclaredFields()) {
            Resource resource = field.getAnnotation(Resource.class);
            if (resource != null) {
                String key = resource.key();

                if ( ! StringUtils.hasContent( key, true ) )
                    key = keyPrefix + field.getName();

                injectField(field, target, key);
            }
        }
    }

    /**
     * Inject the passed bean's properties from this map. The prefix is
     * used to find the configuration keys in the map. Keys in the
     * map have to look like prefix.propertyName. The dot is added to
     * the prefix.
     *
     * @param bean The bean whose properties are injected.
     * @param prefix The prefix used to filter the map's keys.
     */
    public void injectProperties( Object bean, String prefix )
    {
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(
                    bean.getClass() );
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException( "Introspection failed.", e );
        }

        // Add the dot.
        prefix += ".";

        Set<String> definedKeys = new HashSet<String>();
        for ( String c : keySet() )
            if ( c.startsWith( prefix ) )
                definedKeys.add( c );

        if ( definedKeys.size() == 0 )
            return;

        for ( PropertyDescriptor c : beanInfo.getPropertyDescriptors() )
        {
            Method setter = c.getWriteMethod();

            // Skip read-only properties.
            if ( setter == null )
                continue;

            String currentKey = prefix + c.getName();
            if ( ! definedKeys.contains( currentKey ) )
                continue;

            definedKeys.remove( currentKey );

            try
            {
                // This implicitly transforms the key's value.
                setter.invoke( bean, get( currentKey, c.getPropertyType() ) );
            }
            catch ( IllegalAccessException e )
            {
                throw new RuntimeException( e );
            }
            catch ( InvocationTargetException e )
            {
                throw new RuntimeException( e.getCause() );
            }

            if ( definedKeys.size() == 0 )
                return;
        }

        for ( String c : definedKeys )
            LOG.warning( String.format( "Key '%s' defined in map does not match property.", c ) );
    }

    @Override
    public String toString()
    {
        if ( _bundleNames == null )
            return "null";

        return StringUtils.concatenate( " ", _bundleNames );
    }

    /* Register ResourceConverters that are defined in this class
     * and documented here.
     */
    static {
        ResourceConverter[] stringConverters = {
            new ColorStringConverter(),
            new IconStringConverter(),
            new ImageStringConverter(),
            new FontStringConverter(),
            new KeyStrokeStringConverter(),
            new DimensionStringConverter(),
            new PointStringConverter(),
            new Point2dStringConverter(),
            new RectangleStringConverter(),
            new InsetsStringConverter(),
            new EmptyBorderStringConverter()
        };
        for (ResourceConverter sc : stringConverters) {
            ResourceConverter.register(sc);
        }
    }

    /**
     * If path doesn't have a leading "/" then the resourcesDir
     * is prepended, otherwise the leading "/" is removed.
     */
    private static String resourcePath(final String path, ResourceMap resourceMap) {
        if (path == null) {
            return null;
        } else if (path.startsWith("/")) {
            return (path.length() > 1) ? path.substring(1) : null;
        } else {
            return resourceMap.getResourcesDir() + path;
        }
    }

    /**
     *
     * @param s
     * @param resourceMap
     * @return
     * @throws ResourceConverterException
     */
    private static ImageIcon loadImageIcon(String s, ResourceMap resourceMap)
            throws ResourceConverterException {
        String rPath = resourcePath(s, resourceMap);
        if (rPath == null) {
            String msg = String.format("invalid image/icon path \"%s\"", s);
            throw new ResourceConverterException(msg, s);
        }
        URL url = resourceMap.getClassLoader().getResource(rPath);
        if (url != null) {
            return new ImageIcon(url);
        } else {
            String msg = String.format("couldn't find Icon resource \"%s\"", s);
            throw new ResourceConverterException(msg, s);
        }
    }

    private static class FontStringConverter extends ResourceConverter {

        FontStringConverter() {
            super(Font.class);
        }
        /* Just delegates to Font.decode.
         * Typical string is: face-STYLE-size, for example "Arial-PLAIN-12"
         */

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            return Font.decode(s);
        }
    }

    private static class ColorStringConverter extends ResourceConverter {

        ColorStringConverter() {
            super(Color.class);
        }

        /**
         * Parses colors with an alpha channel and comma separated RGB[A] values.
         * Legal formats for color resources are:
         * "#RRGGBB",  "#AARRGGBB", "R, G, B", "R, G, B, A"
         * or the color plain names defined on {@link Color}.
         * @author Romain Guy
         */
        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {

            // Implanted michab.
            {
                Color result = checkPlainColorName( s );
                if ( result != null )
                    return result;
            }
            // TODO michab -- check code below for simplification.
            final Color color;

            if (s.startsWith("#")) {
                switch (s.length()) {
                    // RGB/hex color
                    case 7:
                        color = Color.decode(s);
                        break;
                    // ARGB/hex color
                    case 9:
                        int alpha = Integer.decode(s.substring(0, 3));
                        int rgb = Integer.decode("#" + s.substring(3));
                        color = new Color(alpha << 24 | rgb, true);
                        break;
                    default:
                        throw new ResourceConverterException("invalid #RRGGBB or #AARRGGBB color string", s);
                }
            } else {
                String[] parts = s.split(",");
                if (parts.length < 3 || parts.length > 4) {
                    throw new ResourceConverterException("invalid R, G, B[, A] color string", s);
                }
                try {
                    // with alpha component
                    if (parts.length == 4) {
                        int r = Integer.parseInt(parts[0].trim());
                        int g = Integer.parseInt(parts[1].trim());
                        int b = Integer.parseInt(parts[2].trim());
                        int a = Integer.parseInt(parts[3].trim());
                        color = new Color(r, g, b, a);
                    } else {
                        int r = Integer.parseInt(parts[0].trim());
                        int g = Integer.parseInt(parts[1].trim());
                        int b = Integer.parseInt(parts[2].trim());
                        color = new Color(r, g, b);
                    }
                } catch (NumberFormatException e) {
                    throw new ResourceConverterException("invalid R, G, B[, A] color string", s, e);
                }
            }
            return color;
        }

        private Color checkPlainColorName( String name )
        {
            try
            {
                Field f = Color.class.getField( name );
                if ( ! Color.class.equals( f.getType() ) )
                    return null;
                if ( ! Modifier.isStatic( f.getModifiers() ) )
                    return null;
                return (Color) f.get( null );
            }
            catch ( Exception e )
            {
                return null;
            }
        }
    }

    private static class IconStringConverter extends ResourceConverter {

        IconStringConverter() {
            super(Icon.class);
        }

        @Override
        public Object parseString(String s, ResourceMap resourceMap) throws ResourceConverterException {
            return loadImageIcon(s, resourceMap);
        }

        @Override
        public boolean supportsType(Class<?> testType) {
            return testType.equals(Icon.class) || testType.equals(ImageIcon.class);
        }
    }

    private static class ImageStringConverter extends ResourceConverter {

        ImageStringConverter() {
            super(Image.class);
        }

        @Override
        public Object parseString(String s, ResourceMap resourceMap) throws ResourceConverterException {
            return loadImageIcon(s, resourceMap).getImage();
        }
    }

    private static class KeyStrokeStringConverter extends ResourceConverter {
        private static final String KEYWORD_SHORTCUT = "shortcut";
        private static final String KEYWORD_META = "meta";
        private static final String KEYWORD_CONTROL = "control";

        private static final String REPLACE = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ==
            Event.META_MASK ? KEYWORD_META : KEYWORD_CONTROL;
        private static final Pattern PATTERN = Pattern.compile(KEYWORD_SHORTCUT);

        KeyStrokeStringConverter() {
            super(KeyStroke.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) {
            if (s.contains(KEYWORD_SHORTCUT)) {
//                int k = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                s = PATTERN.matcher(s).replaceFirst(REPLACE);
            }
            return KeyStroke.getKeyStroke(s);
        }
    }

    /* String s is assumed to contain n number substrings separated by
     * commas.  Return a list of those integers or null if there are too
     * many, too few, or if a substring can't be parsed.  The format
     * of the numbers is specified by Double.valueOf().
     */
    private static List<Double> parseDoubles(String s, int n, String errorMsg) throws ResourceConverterException {
        String[] doubleStrings = s.split(",", n + 1);
        if (doubleStrings.length != n) {
            throw new ResourceConverterException(errorMsg, s);
        } else {
            List<Double> doubles = new ArrayList<Double>(n);
            for (String doubleString : doubleStrings) {
                try {
                    doubles.add(Double.valueOf(doubleString));
                } catch (NumberFormatException e) {
                    throw new ResourceConverterException(errorMsg, s, e);
                }
            }
            return doubles;
        }
    }

    private static class DimensionStringConverter extends ResourceConverter {

        DimensionStringConverter() {
            super(Dimension.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            List<Double> xy = parseDoubles(s, 2, "invalid x,y Dimension string");
            Dimension d = new Dimension();
            d.setSize(xy.get(0), xy.get(1));
            return d;
        }
    }

    private static class PointStringConverter extends ResourceConverter {

        PointStringConverter() {
            super(Point.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            List<Double> xy = parseDoubles(s, 2, "invalid x,y Point string");
            Point p = new Point();
            p.setLocation(xy.get(0), xy.get(1));
            return p;
        }
    }

    private static class Point2dStringConverter extends ResourceConverter {

        Point2dStringConverter() {
            super(Point2D.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            List<Double> xy = parseDoubles(s, 2, "invalid x,y Point string");
            return new Point2D.Double(xy.get(0), xy.get(1));
        }
    }

    private static class RectangleStringConverter extends ResourceConverter {

        RectangleStringConverter() {
            super(Rectangle.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            List<Double> xywh = parseDoubles(s, 4, "invalid x,y,width,height Rectangle string");
            Rectangle r = new Rectangle();
            r.setFrame(xywh.get(0), xywh.get(1), xywh.get(2), xywh.get(3));
            return r;
        }
    }

    private static class InsetsStringConverter extends ResourceConverter {

        InsetsStringConverter() {
            super(Insets.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            List<Double> tlbr = parseDoubles(s, 4, "invalid top,left,bottom,right Insets string");
            return new Insets(tlbr.get(0).intValue(), tlbr.get(1).intValue(), tlbr.get(2).intValue(), tlbr.get(3).intValue());
        }
    }

    private static class EmptyBorderStringConverter extends ResourceConverter {

        EmptyBorderStringConverter() {
            super(EmptyBorder.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            List<Double> tlbr = parseDoubles(s, 4, "invalid top,left,bottom,right EmptyBorder string");
            return new EmptyBorder(tlbr.get(0).intValue(), tlbr.get(1).intValue(), tlbr.get(2).intValue(), tlbr.get(3).intValue());
        }
    }
}
