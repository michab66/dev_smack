/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package org.jdesktop.application;

import java.awt.Component;
import java.awt.Container;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JMenu;

import org.jdesktop.application.ResourceMap.InjectFieldException;
import org.jdesktop.application.ResourceMap.LookupException;
import org.jdesktop.application.ResourceMap.PropertyInjectionException;
import org.jdesktop.util.ReflectionUtil;
import org.jdesktop.util.StringUtil;

import javafx.util.Pair;


/**
 * The application's {@code ResourceManager} provides
 * read-only cached access to resources in {@code ResourceBundles} via the
 * {@link ResourceMap ResourceMap} class.  {@code ResourceManager} is a
 * property of the {@code ApplicationContext} and most applications
 * look up resources relative to it, like this:
 * <pre>
 * ResourceManager appResourceManager = Application.getResourceManager();
 * ResourceMap resourceMap = appResourceManager.getResourceMap(MyClass.class);
 * String msg = resourceMap.getString("msg");
 * Icon icon = resourceMap.getIcon("icon");
 * Color color = resourceMap.getColor("color");
 * </pre>
 * The {@code ResourceMap}
 * in this example contains resources from the ResourceBundle named
 * {@code MyClass}, and the rest of the
 * chain contains resources shared by the entire application.
 * <p>
 * The {@link Application} class itself may also provide resources. A complete
 * description of the naming conventions for ResourceBundles is provided
 * by the {@link #getResourceMap(Class, Class) getResourceMap()} method.
 * </p>
 * <p>
 * A stand alone {@link ResourceManager} can be created by the public
 * constructors.
 * <P>
 * @see ApplicationContext#getResourceManager
 * @see ApplicationContext#getResourceMap
 * @see ResourceMap
 *
 * @version $Rev$
 * @author Michael Binz
 * @author Hans Muller (Hans.Muller@Sun.COM)
 */
public final class ResourceManager
{
    private final static Logger LOG =
            Logger.getLogger( ResourceManager.class.getName() );

    private final Map<String, ResourceMap> resourceMaps =
                new ConcurrentHashMap<String, ResourceMap>();

    private final List<String> _applicationBundleNames;

    private ResourceMap _appResourceMap = null;

    /**
     * The application class used to compute the
     * application-wide elements of the resource map.
     * Is never null.
     */
    private final Class<?> _applicationClass;

    /**
     * Creates an instance.
     */
    public ResourceManager()
    {
        this( ResourceManager.class );
    }

    /**
     * Construct a {@code ResourceManager}.  Typically applications
     * will not create a ResourceManager directly, they'll retrieve
     * the shared one from the {@code ApplicationContext} with:
     * <pre>
     * Application.getInstance().getContext().getResourceManager()
     * </pre>
     * Or just look up {@code ResourceMaps} with the ApplicationContext
     * convenience method:
     * <pre>
     * Application.getInstance().getContext().getResourceMap(MyClass.class)
     * </pre>
     * <p>This constructor is used if the resource system is to be used
     * independently from the rest of the jsp192 API, especially if no
     * {@link Application} class is created, for example in a command-line-
     * based application.
     *
     * @param applicationClass The application class.  Note that this is
     * not needed to inherit from {@link Application}.  It is used as the
     * parent of the returned resource maps, which allows to define
     * application wide resources in the application class' resources.
     *
     * @see ApplicationContext#getResourceManager
     * @see ApplicationContext#getResourceMap
     */
    public ResourceManager( Class<?> applicationClass ) {
        if ( applicationClass == null )
            throw new IllegalArgumentException( "null applicationClass" );
        _applicationClass = applicationClass;
        _applicationBundleNames = allBundleNames(
                _applicationClass,
                Object.class );
    }

    /**
     * Returns a list of the ResourceBundle names for all of
     * the classes from startClass to (including) stopClass.  The
     * bundle names for each class are #getClassBundleNames(Class).
     * The list is in priority order: resources defined in bundles
     * earlier in the list shadow resources with the same name that
     * appear bundles that come later.
     */
    static private List<String> allBundleNames(Class<?> startClass, Class<?> stopClass)
    {
        List<String> result = new ArrayList<String>();

        Class<?> limitClass = stopClass.getSuperclass(); // could be null

        for (Class<?> c = startClass; c != limitClass; c = c.getSuperclass())
            result.addAll(getClassBundleNames(c));

        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the package name of a resource bundle.
     * "org.bsaf192.Lumumba" -> "org.bsaf192".
     *
     * @param bundleName A resource bundle name.
     * @return The corresponding package name.
     */
    private static String bundlePackageName( String bundleName )
    {
        int idx = bundleName.lastIndexOf( "." );

        return ( idx == -1 ) ?
                StringUtil.EMPTY_STRING :
                bundleName.substring( 0, idx );
    }

    /**
     * Creates a parent chain of ResourceMaps for the specified
     * ResourceBundle names.  One ResourceMap is created for each
     * subsequence of ResourceBundle names with a common bundle
     * package name, i.e. with a common resourcesDir.  The parent
     * of the final ResourceMap in the chain is root.
     */
    private ResourceMap createResourceMapChain(
            Locale locale,
            ClassLoader cl,
            ResourceMap root,
            ListIterator<String> names)
    {
        if ( !names.hasNext() )
            return root;

        String bundleName0 = names.next();
        String rmBundlePackage = bundlePackageName(bundleName0);

        List<String> rmNames = new ArrayList<String>();
        rmNames.add(bundleName0);

        while ( names.hasNext())
        {
            String bundleName = names.next();
            if (rmBundlePackage.equals(bundlePackageName(bundleName))) {
                rmNames.add(bundleName);
            } else {
                names.previous();
                break;
            }
        }

        // Process the tail of the iterator.  A bit lispy.
        ResourceMap parent = createResourceMapChain(
                locale,
                cl,
                root,
                names);

        return new ResourceMap(locale, parent, cl, rmNames);
    }

    /**
     * Returns a {@link ResourceMap#getParent chain} of {@code ResourceMaps}
     * that encapsulate the {@code ResourceBundles} for each class
     * from {@code startClass} to (including) {@code stopClass}.  The
     * final link in the chain is Application ResourceMap chain, i.e.
     * the value of {@link #getApplicationResourceMap() getResourceMap()}.
     * <p>
     * The ResourceBundle names for the chain of ResourceMaps
     * are defined by  {@link #getClassBundleNames} and
     * {@link #getApplicationBundleNames}.  Collectively they define the
     * standard location for {@code ResourceBundles} for a particular
     * class as the {@code resources} subpackage.  For example, the
     * ResourceBundle for the single class {@code com.myco.MyScreen}, would
     * be named {@code com.myco.resources.MyScreen}.  Typical
     * ResourceBundles are ".properties" files, so: {@code
     * com/foo/bar/resources/MyScreen.properties}.  The following table
     * is a list of the ResourceMaps and their constituent
     * ResourceBundles for the same example:
     * <p>
     * <table border="1" cellpadding="4%">
     *   <caption><em>ResourceMap chain for class MyScreen in MyApp</em></caption>
     *     <tr>
     *       <th></th>
     *       <th>ResourceMap</th>
     *       <th>ResourceBundle names</th>
     *       <th>Typical ResourceBundle files</th>
     *     </tr>
     *     <tr>
     *       <td>1</td>
     *       <td>class: com.myco.MyScreen</td>
     *       <td>com.myco.resources.MyScreen</td>
     *       <td>com/myco/resources/MyScreen.properties</td>
     *     </tr>
     *     <tr>
     *       <td>2</td>
     *       <td>application: com.myco.MyApp</td>
     *       <td>com.myco.resources.MyApp</td>
     *       <td>com/myco/resources/MyApp.properties</td>
     *     </tr>
     *     <tr>
     *       <td>3</td>
     *       <td>application: javax.swing.application.Application</td>
     *       <td>javax.swing.application.resources.Application</td>
     *       <td>javax.swing.application.resources.Application.properties</td>
     *     </tr>
     * </table>
     *
     * <p>Note that inner classes are searched for by "simple" name - eg,
     * for a class MyApp$InnerClass, the resource bundle must be named
     * InnerClass.properties. See the notes on {@link #classBundleBaseName classBundleBaseName} </p>
     *
     * <p>
     * None of the ResourceBundles are required to exist.  If more than one
     * ResourceBundle contains a resource with the same name then
     * the one earlier in the list has precedence
     * <p>
     * ResourceMaps are constructed lazily and cached.  One ResourceMap
     * is constructed for each sequence of classes in the same package.
     *
     * @param startClass the first class whose ResourceBundles will be included
     * @param stopClass the last class whose ResourceBundles will be included
     * @return a {@code ResourceMap} chain that contains resources loaded from
     *   {@code ResourceBundles}  found in the resources subpackage for
     *   each class.
     * @see #getClassBundleNames
     * @see #getApplicationBundleNames
     * @see ResourceMap#getParent
     * @see ResourceMap#getBundleNames
     */
    private ResourceMap getResourceMap(Locale locale, Class<?> startClass, Class<?> stopClass)
    {
        if (startClass == null)
            throw new IllegalArgumentException("null startClass");
        if (stopClass == null)
            throw new IllegalArgumentException("null stopClass");
        if (!stopClass.isAssignableFrom(startClass))
            throw new IllegalArgumentException("startClass is not a subclass, or the same as, stopClass");

        String classResourceMapKey = startClass.getName() + stopClass.getName();

        ResourceMap result = resourceMaps.get(classResourceMapKey);

        if ( result != null )
            return result;

        // Get the bundle names for the whole chain of classes.
        // We put the application bundle names in front of the list to
        // allow overriding of all resources in the application resources.
        List<String> classBundleNames =
                new ArrayList<String>( _applicationBundleNames );
        classBundleNames.addAll( allBundleNames( startClass, stopClass ) );

        ClassLoader classLoader =
                startClass.getClassLoader();

        result = createResourceMapChain(
                locale,
                classLoader,
                null,
                classBundleNames.listIterator() );

        resourceMaps.put(classResourceMapKey, result);

        return result;
    }

    public ResourceMap getResourceMap( Class<?> startClass, Class<?> stopClass )
    {
        return getResourceMap( Locale.getDefault(), startClass, stopClass );
    }

    /**
     * Return the ResourceMap chain for the specified class. This is
     * just a convenience method, it's the same as:
     * <code>getResourceMap(cls, cls)</code>.
     *
     * @param cls the class that defines the location of ResourceBundles
     * @return a {@code ResourceMap} that contains resources loaded from
     *   {@code ResourceBundles}  found in the resources subpackage of the
     *   specified class's package.
     * @see #getResourceMap(Class, Class)
     */
    public final ResourceMap getResourceMap( Class<?> cls )
    {
        return getResourceMap( Locale.getDefault(), cls, cls );
    }

    private ResourceMap getApplicationResourceMap( Locale locale )
    {
        if (_appResourceMap == null)
        {
            ClassLoader classLoader =
                    _applicationClass.getClassLoader();

            _appResourceMap = createResourceMapChain(
                    locale,
                    classLoader,
                    null,
                    _applicationBundleNames.listIterator());
        }

        return _appResourceMap;
    }

    /**
     * Returns the chain of ResourceMaps that's shared by the entire application,
     * beginning with the resources defined for the application's class.
     * If the {@code applicationClass} property has not been set, e.g. because
     * the application has not been {@link Application#launch launched} yet,
     * then a ResourceMap for just {@code Application.class} is returned.
     *
     * @return the Application's ResourceMap.
     */
    public ResourceMap getApplicationResourceMap()
    {
        return getApplicationResourceMap( Locale.getDefault() );
    }

    /**
     * Performs injection of attributes marked with the resource annotation.
     *
     * @param o The object whose resources should be injected. Null is not
     * allowed, array instances are not allowed, primitive classes are not
     * allowed.
     * @throws IllegalArgumentException In case a bad object was passed.
     */
    public void injectResources( Object o )
    {
        Class<?> clazz;

        // If we already received a class instance, use it. Otherwise
        // get the objects class.  This allows injection of static
        // attribute values on library classes.
        if ( o instanceof Class )
            clazz = (Class<?>)o;
        else
            clazz = o.getClass();

        ResourceMap resourceMap = getResourceMap(
                clazz, Object.class );

        // Perform the injection for the object's class and all its
        // super classes.
        // TODO michab -- Ensure quick return if already injected.
        for ( Class<?> c : ReflectionUtil.getInheritanceList( clazz ) )
        {
            if ( c.getClassLoader() == null )
                break;

            injectFields( o, c, resourceMap );
        }
    }

    /**
     * Map from a class to a list of the names of the
     * {@code ResourceBundles} specific to the class.
     * The list is in priority order: resources defined
     * by the first ResourceBundle shadow resources with the
     * the same name that come later.
     * <p>
     * By default this method returns one ResourceBundle
     * whose name is the same as the class's name, but in the
     * {@code "resources"} subpackage.
     * <p>
     * For example, given a class named
     * {@code com.foo.bar.MyClass}, the ResourceBundle name would
     * be {@code "com.foo.bar.resources.MyClass"}. If MyClass is
     * an inner class, only its "simple name" is used.  For example,
     * given an inner class named {@code com.foo.bar.OuterClass$InnerClass},
     * the ResourceBundle name would be
     * {@code "com.foo.bar.resources.InnerClass"}.
     * Although this could result in a collision, creating more
     * complex rules for inner classes would be a burden for
     * developers.
     * <p>
     * This method is used by the {@code getResourceMap} methods
     * to compute the list of ResourceBundle names
     * for a new {@code ResourceMap}.
     *
     * @param cls the named ResourceBundles are specific to {@code cls}.
     * @return the names of the ResourceBundles to be loaded for {@code cls}
     * @see #getResourceMap
     * @see #getApplicationBundleNames
     */
    private static List<String> getClassBundleNames(Class<?> cls)
    {
        Package packge = cls.getPackage();

        String resourcePackage = packge != null ?
                packge.getName() + "." :
                StringUtil.EMPTY_STRING;

        resourcePackage += "resources.";

        String classBundle =
            resourcePackage + cls.getSimpleName();
        String packageBundle =
            resourcePackage + "package";

        return Arrays.asList( classBundle, packageBundle );
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
    public void injectFields(Object target, Class<?> targetType, ResourceMap map ) {
        if (target==null)
            throw new IllegalArgumentException("null target");
        if (targetType.isPrimitive())
            throw new IllegalArgumentException("primitive target");
        if (targetType.isArray())
            throw new IllegalArgumentException("array target");

        String keyPrefix = targetType.getSimpleName() + ".";

        for ( Pair<Field,Resource> field :
            ReflectionUtil.getAnnotatedFields(
                    targetType,
                    Resource.class ) )
        {
            String key = field.getValue().mappedName();

            if ( ! StringUtil.hasContent( key ) )
                key = keyPrefix + field.getKey().getName();

            injectField( field.getKey(), target, key, map );
        }
    }

    /**
     * Inject a single field.
     *
     * @param field The field to inject.
     * @param target The target object instance.
     * @param key The resource key.
     */
    private void injectField( Field field, Object target, String key, ResourceMap map )
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

            injectComponentProperties( key, fieldValue, map );
        }
        else
        {
            Object value = map.getObject(key, type);

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
     *
     * @param component
     * @param pd
     * @param key
     */
    private void injectComponentProperty(Component component, PropertyDescriptor pd, String key, ResourceMap map ) {
        Method setter = pd.getWriteMethod();
        Class<?> type = pd.getPropertyType();
        if ((setter != null) && (type != null) && map.containsKey(key)) {
            Object value = map.getObject(key, type);
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
    private void injectComponentProperties(String componentName, Component component, ResourceMap map) {
        if ( componentName == null )
            return;

        /* Optimization: punt early if componentName doesn't
         * appear in any componentName.propertyName resource keys
         */
        boolean matchingResourceFound = false;
        for (String key : map.keySet()) {
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
            for (String key : map.keySet()) {
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
                            injectComponentProperty(component, pd, key,map);
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

    /* Applies {@link #injectComponent} to each Component in the
     * hierarchy with root <tt>root</tt>.
     *
     * @param root the root of the component hierarchy
     * @throws PropertyInjectionException if a property specified by a resource can't be set
     * @throws IllegalArgumentException if target is null
     * @see #injectComponent
     */
    public void injectComponents(Component root,ResourceMap map) {
        injectComponent(root,map);
        if (root instanceof JMenu) {
            /* Warning: we're bypassing the popupMenu here because
             * JMenu#getPopupMenu creates it; doesn't seem right
             * to do so at injection time.  Unfortunately, this
             * means that attempts to inject the popup menu's
             * "label" property will fail.
             */
            JMenu menu = (JMenu) root;
            for (Component child : menu.getMenuComponents()) {
                injectComponents(child,map);
            }
        } else if (root instanceof Container) {
            Container container = (Container) root;
            for (Component child : container.getComponents()) {
                injectComponents(child,map);
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
    public void injectComponent(Component target,ResourceMap map) {
        if (target == null) {
            throw new IllegalArgumentException("null target");
        }
        injectComponentProperties(target.getName(), target,map);
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
    public void injectProperties( Object bean, String prefix, ResourceMap map )
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
        for ( String c : map.keySet() )
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
                setter.invoke( bean, map.get( currentKey, c.getPropertyType() ) );
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
}
