/* $Id$
 *
 * Utilities
 *
 * Released under Gnu Public License
 * Copyright © 2017 Michael G. Binz
 */
package org.jdesktop.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;

import javafx.util.Pair;

/**
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class ResourceManager
{
    private final static Logger LOG =
            Logger.getLogger( ResourceManager.class.getName() );

    private final ResourceConverterRegistry _converters =
            new ResourceConverterRegistry();

    private WeakHashMap<Class<?>, ResourceMap> staticInjectionDone =
            new WeakHashMap<>();

    private WeakMapWithProducer<Class<?>, ResourceMap> _resourceMapCache =
            new WeakMapWithProducer<>( ResourceMap::new );

    public ResourceManager()
    {
        for ( ResourceConverter c : ServiceLoader.load( ResourceConverter.class ) )
        {
            Class<?> type =  c.getType();

            if ( _converters.containsKey( type ) )
                LOG.warning( "Duplicate resource converter for: " + type );

            _converters.put( type, c );
        }

        for ( ResourceConverterExtension c : ServiceLoader.load( ResourceConverterExtension.class ) )
            c.extendTypeMap( _converters );
    }

    /**
     * @param converter A converter to add to the list of known converters.
     */
    public void addConverter( ResourceConverter converter )
    {
        _converters.put( converter.getType(), converter );
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
                setter.invoke(
                        bean,
                        convert(
                                c.getPropertyType(),
                                map.get( currentKey ),
                                map ) );
//                        map.get( currentKey, c.getPropertyType() ) );
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


    public void injectResources( Object o )
    {
        if ( o instanceof Class )
            injectResources( null, (Class<?>)o );
        else
            injectResources( o, o.getClass() );
    }

    /**
     * @return
     */
    public ResourceMap getResourceMap( Class<?> clazz )
    {
        return _resourceMapCache.get( clazz );
    }

    public void injectResources( Object instance, Class<?> cIass )
    {
        if ( instance == null && staticInjectionDone.containsKey( cIass ) )
            return;

        List<Pair<Field, Resource>> fields = ReflectionUtil.getAnnotatedFields(
                cIass,
                Resource.class,
                instance == null ? Modifier.STATIC : 0 );

        if ( fields.isEmpty() )
            return;

        ResourceMap rb =
                new ResourceMap( cIass );

        if ( rb.isEmpty() )
        {
            // @Resource annotations exist, but no property file.
            LOG.severe( "No resources found for class " + cIass.getName() );
            return;
        }

        for ( Pair<Field, Resource> c : fields )
        {
            Field f = c.getKey();

            if ( Modifier.isStatic( f.getModifiers() ) &&
                    staticInjectionDone.containsKey( cIass ) )
                continue;

            Resource r = c.getValue();

            String name = r.name();

            if ( StringUtil.isEmpty( name ) )
                name = f.getName();

            String value = rb.get( name );

            if ( value == null )
            {
                String message = String.format(
                        "No resource key found for field '%s#%s'.",
                        f.getDeclaringClass(),
                        f.getName() );
                LOG.severe(
                        message );
                continue;
            }

            try
            {
                performInjection( instance, f, value, rb );
            }
            catch ( Exception e )
            {
                LOG.log( Level.SEVERE, "Injection failed for field " + f.getName(), e );
            }
        }

        staticInjectionDone.put( cIass, rb );
    }

    /**
     *
     * @param instance
     * @param f
     * @param value
     * @param map
     * @throws Exception
     */
    private void performInjection(
            Object instance,
            Field f,
            String value,
            ResourceMap map ) throws Exception
    {
        boolean accessible = f.isAccessible();

        try
        {
            if ( ! accessible )
                f.setAccessible( true );

            performInjectionImpl( instance, f, value, map );
        }
        catch ( Exception e )
        {
            String msg =
                    String.format(
                            "Resource init for %s failed.",
                            f.getName() );

            LOG.log(
                    Level.WARNING, msg, e );
        }
        finally
        {
            if ( f.isAccessible() != accessible )
                f.setAccessible( accessible );
        }
    }

    /**
     *
     * @param instance
     * @param f
     * @param resource
     * @param map
     * @throws Exception
     */
    private void performInjectionImpl(
            Object instance,
            Field f,
            String resource,
            ResourceMap map ) throws Exception
    {
        Class<?> targetType = f.getType();

        ResourceConverter converter =
                _converters.get( targetType );

        if ( converter != null )
        {
            f.set(
                    instance,
                    converter.parseString( resource, map ) );

            return;
        }

        // Check if we can synthesize an array resource converter.
        if ( targetType.isArray() )
        {
            converter = _converters.get( targetType.getComponentType() );
            if ( converter != null )
            {
                converter = new ArrayResourceConverter( converter, targetType );
                f.set( instance, converter.parseString( resource, map ) );
                return;
            }
        }

        // Check if we can synthesize a constructor-based resource converter.
        if ( targetType.getConstructor( String.class ) != null )
        {
            converter = new ConstructorResourceConverter(
                    targetType.getConstructor( String.class ),
                    targetType );

            f.set( instance, converter.parseString( resource, map ) );

            return;
        }

        LOG.warning( "No resource converter found for type: " + targetType );
    }

    private static class ArrayResourceConverter extends ResourceConverter
    {
        private final ResourceConverter _delegate;

        ArrayResourceConverter( ResourceConverter delegate, Class<?> type )
        {
            super( type );

            if ( ! type.isArray() )
                throw new IllegalArgumentException();

            _delegate = delegate;
        }

        @Override
        public Object parseString( String s, ResourceMap r )
                throws Exception
        {
            String[] split = StringUtil.splitQuoted( s );

            Object result = Array.newInstance(
                    getType().getComponentType(), split.length );

            int idx = 0;
            for ( String c : split )
            {
                Array.set(
                        result,
                        idx++,
                        _delegate.parseString(
                                c,
                                r ) );
            }

            return result;
        }
    }

    private static class ConstructorResourceConverter extends ResourceConverter
    {
        private final Constructor<?> _ctor;

        ConstructorResourceConverter( Constructor<?> delegate, Class<?> type )
        {
            super( type );

            _ctor = delegate;
        }

        @Override
        public Object parseString( String s, ResourceMap r )
            throws Exception
        {
            return _ctor.newInstance( s );
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T convert( Class<T> targetType, String toConvert, ResourceMap map )
    {
        ResourceConverter converter =
                _converters.get( targetType );

        if ( converter == null )
            converter = synthArrayConverter( targetType );

        if ( converter == null )
            converter = synthConstructorConverter( targetType );

        if ( converter == null )
            throw new IllegalArgumentException( "No resource converter found for type: " + targetType );

        try
        {
            return (T)converter.parseString( toConvert, map );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "Conversion failed.", e );
        }
    }

    private ResourceConverter synthArrayConverter( Class<?> targetType )
    {
        if ( ! targetType.isArray() )
            return null;

        ResourceConverter rc = _converters.get(
                targetType.getComponentType() );

        if ( rc == null )
            return null;

        return new ArrayResourceConverter( rc, targetType );
    }

    private ResourceConverter synthConstructorConverter( Class<?> targetType )
    {
        Constructor<?> ctor =
                ReflectionUtil.getConstructor( targetType, String.class );

        if ( ctor == null )
            return null;

        return new ConstructorResourceConverter(
                ctor,
                targetType );
    }
}
