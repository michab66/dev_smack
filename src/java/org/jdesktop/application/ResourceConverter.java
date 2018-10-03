
/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package org.jdesktop.application;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdesktop.smack.util.StringUtils;

/**
 * A base class for converting arbitrary types to and from Strings, as well as
 * a registry of ResourceConverter implementations.
 * <p>
 * The <tt>supportsType</tt> method defines what types a ResourceConverter supports.
 * By default it returns true for classes that are equal to the constructor's
 * <tt>type</tt> argument.  The <tt>parseType</tt> methods converts a string
 * the ResourceConverter's supported type, and the <tt>toString</tt> does the
 * inverse, it converts a supported type to a String.  Concrete ResourceConverter
 * subclasses must override <tt>parseType()</tt> and, in most cases, the
 * <tt>toString</tt> method as well.
 * <p>
 * This class maintains a registry of ResourceConverters.
 * The <tt>forType</tt> method returns the first ResourceConverter that
 * supports a particular type, new ResourceConverters can be added with
 * <tt>register()</tt>.  A small set of generic ResourceConverters are
 * registered by default.  They support the following types:
 * <ul>
 * <li><tt>Boolean</tt></li>
 * <li><tt>Integer</tt></li>
 * <li><tt>Float</tt></li>
 * <li><tt>Double</tt></li>
 * <li><tt>Long</tt></li>
 * <li><tt>Short</tt></li>
 * <li><tt>Byte</tt></li>
 * <li><tt>MessageFormat</tt></li>
 * <li><tt>URL</tt></li>
 * <li><tt>URI</tt></li>
 * </ul>
 * <p>
 * The Boolean ResourceConverter returns true for "true", "on", "yes",
 * false otherwise.  The other primitive type ResourceConverters rely on
 * the corresponding static parse<i>Type</i> method,
 * e.g. <tt>Integer.parseInt()</tt>.  The MessageFormat
 * ResourceConverter just creates MessageFormat object with the string
 * as its constructor argument.  The URL/URI converters just apply
 * the corresponding constructor to the resource string.
 *
 * @author Hans Muller (Hans.Muller@Sun.COM)
 * @see ResourceMap
 */
public abstract class ResourceConverter {

    protected final Class<?> type;

    /**
     * Convert string to object
     * @param s the string to be parsed
     * @param r the {@code ResourceMap}
     * @return the object which was created from the string
     * @throws org.jdesktop.application.ResourceConverter.ResourceConverterException
     */
    public abstract Object parseString(String s, ResourceMap r)
            throws ResourceConverterException;

//    /** TODO this is the future
//     * Convert string to object
//     * @param s the string to be parsed
//     * @param r the {@code ResourceMap}
//     * @return the object which was created from the string
//     * @throws org.jdesktop.application.ResourceConverter.ResourceConverterException
//     */
//    public abstract Object parseString(String s)
//            throws ResourceConverterException;

    /**
     *
     * @param type
     */
    protected ResourceConverter(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("null type");
        }
        this.type = type;
    }

    protected Class<?> getType() {
        return type;
    }

    /**
     * Checks whether {@code testType} can be converted with this converter.
     * @param testType
     * @return {@code true} if {@code testType} can be converted with this converter.
     */
    public boolean supportsType(Class<?> testType) {
        return type.equals(testType);
    }

    @SuppressWarnings("serial")
    public static class ResourceConverterException extends Exception {

        private final String badString;

        private String maybeShorten(String s) {
            int n = s.length();
            return (n < 128) ? s : s.substring(0, 128) + "...[" + (n - 128) + " more characters]";
        }

        public ResourceConverterException(String message, String badString, Throwable cause) {
            super(message, cause);
            this.badString = maybeShorten(badString);
        }

        public ResourceConverterException(String message, String badString) {
            super(message);
            this.badString = maybeShorten(badString);
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer(super.toString());
            sb.append(" string: \"");
            sb.append(badString);
            sb.append("\"");
            return sb.toString();
        }
    }

    /**
     * Registers a {@code ResourceConverter}
     * @param resourceConverter the resource converter to be registered
     */
    public static void register(ResourceConverter resourceConverter) {
        if (resourceConverter == null) {
            throw new IllegalArgumentException("null resourceConverter");
        }
        resourceConverters.add(resourceConverter);
    }

    /**
     * Returns {@code ResourceConverter} for the specified type.
     * @param type the type a converter must be found for.
     * @return the converter for specified type or {@code null} if no converter
     * is found for the {@code type}.
     */
    public static ResourceConverter forType(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("null type");
        }

        // Check for a direct match.
        for (ResourceConverter sc : resourceConverters) {
            if (sc.supportsType(type)) {
                return sc;
            }
        }

        // We did not find a resource converter so far, so we perform
        // a fallback strategy:  If the sought type is not an array...
        if ( ! type.isArray() )
            // ... we try to interpret the string from the resources
            // as a class name.
            return new ClassResourceConverter( type );

        // If the sought type is an array, we try to resolve this via
        // its component type using the normal resource converter lookup
        // strategy.
        ResourceConverter rc = forType( type.getComponentType() );
        if ( rc == null )
            return null;

        return new ArrayResourceConverter( rc, type );
    }

    private static ResourceConverter[] resourceConvertersArray = {
        new StringResourceConverter(),
        new BooleanResourceConverter("true", "on", "yes"),
        new IntegerResourceConverter(),
        new MessageFormatResourceConverter(),
        new FloatResourceConverter(),
        new DoubleResourceConverter(),
        new LongResourceConverter(),
        new ShortResourceConverter(),
        new ByteResourceConverter(),
        new URLResourceConverter(),
        new URIResourceConverter()
    };

    /**
     * The resource converter registry.
     */
    private static List<ResourceConverter> resourceConverters =
            new ArrayList<ResourceConverter>(Arrays.asList(resourceConvertersArray));

    /**
     * A ResourceConverter that takes the passed string as a classname.
     * Checks whether the class can be loaded and is assignable to
     * the target type.  If this is the case creates and returns an instance
     * of the configured class using the default constructor.
     */
    private static class ClassResourceConverter extends ResourceConverter
    {
        protected ClassResourceConverter( Class<?> type )
        {
            super( type );
        }

        @Override
        public Object parseString( String s, ResourceMap r )
                throws ResourceConverterException
        {
            try
            {
                Class<?> resultClass = Class.forName( s );

                if ( ! getType().isAssignableFrom( resultClass ) )
                    throw new ClassCastException( getType().getName() + " is not assignable from " + resultClass.getName() );

                return resultClass.newInstance();
            }
            catch ( Exception e )
            {
                throw new ResourceConverterException( getType().getName(), s, e );
            }
        }
    }

    private static class StringResourceConverter extends ResourceConverter {

        StringResourceConverter() {
            super(String.class);
        }

        @Override
        public Object parseString( String s, ResourceMap r )
                throws ResourceConverterException
        {
            return s;
        }
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
                throws ResourceConverterException
        {
            String[] split = StringUtils.splitQuoted( s );

            Object result = Array.newInstance(
                    getType().getComponentType(), split.length );

            int idx = 0;
            for ( String c : split )
                Array.set( result, idx++, _delegate.parseString( c, r ) );

            return result;
        }
    }

    private static class BooleanResourceConverter extends ResourceConverter {

        private final String[] trueStrings;

        BooleanResourceConverter(String... trueStrings) {
            super(Boolean.class);
            this.trueStrings = trueStrings;
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) {
            s = s.trim();
            for (String trueString : trueStrings) {
                if (s.equalsIgnoreCase(trueString)) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public boolean supportsType(Class<?> testType) {
            return testType.equals(Boolean.class) || testType.equals(boolean.class);
        }
    }

    private static abstract class NumberResourceConverter extends ResourceConverter {

        private final Class<?> primitiveType;

        NumberResourceConverter(Class<?> type, Class<?> primitiveType) {
            super(type);
            this.primitiveType = primitiveType;
        }

        protected abstract Number parseString(String s) throws NumberFormatException;

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            try {
                return parseString(s);
            } catch (NumberFormatException e) {
                throw new ResourceConverterException("invalid " + type.getSimpleName(), s, e);
            }
        }

        @Override
        public boolean supportsType(Class<?> testType) {
            return testType.equals(type) || testType.equals(primitiveType);
        }
    }

    private static class FloatResourceConverter extends NumberResourceConverter {

        FloatResourceConverter() {
            super(Float.class, float.class);
        }

        @Override
        protected Number parseString(String s) throws NumberFormatException {
            return Float.parseFloat(s);
        }
    }

    private static class DoubleResourceConverter extends NumberResourceConverter {

        DoubleResourceConverter() {
            super(Double.class, double.class);
        }

        @Override
        protected Number parseString(String s) throws NumberFormatException {
            return Double.parseDouble(s);
        }
    }

    private static abstract class INumberResourceConverter extends ResourceConverter {

        private final Class<?> primitiveType;

        INumberResourceConverter(Class<?> type, Class<?> primitiveType) {
            super(type);
            this.primitiveType = primitiveType;
        }

        protected abstract Number parseString(String s, int radix) throws NumberFormatException;

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            try {
                String[] nar = s.split("&"); // number ampersand radix
                int radix = (nar.length == 2) ? Integer.parseInt(nar[1]) : -1;
                return parseString(nar[0], radix);
            } catch (NumberFormatException e) {
                throw new ResourceConverterException("invalid " + type.getSimpleName(), s, e);
            }
        }

        @Override
        public boolean supportsType(Class<?> testType) {
            return testType.equals(type) || testType.equals(primitiveType);
        }
    }

    private static class ByteResourceConverter extends INumberResourceConverter {

        ByteResourceConverter() {
            super(Byte.class, byte.class);
        }

        @Override
        protected Number parseString(String s, int radix) throws NumberFormatException {
            return (radix == -1) ? Byte.decode(s) : Byte.parseByte(s, radix);
        }
    }

    private static class IntegerResourceConverter extends INumberResourceConverter {

        IntegerResourceConverter() {
            super(Integer.class, int.class);
        }

        @Override
        protected Number parseString(String s, int radix) throws NumberFormatException {
            return (radix == -1) ? Integer.decode(s) : Integer.parseInt(s, radix);
        }
    }

    private static class LongResourceConverter extends INumberResourceConverter {

        LongResourceConverter() {
            super(Long.class, long.class);
        }

        @Override
        protected Number parseString(String s, int radix) throws NumberFormatException {
            return (radix == -1) ? Long.decode(s) : Long.parseLong(s, radix);
        }
    }

    private static class ShortResourceConverter extends INumberResourceConverter {

        ShortResourceConverter() {
            super(Short.class, short.class);
        }

        @Override
        protected Number parseString(String s, int radix) throws NumberFormatException {
            return (radix == -1) ? Short.decode(s) : Short.parseShort(s, radix);
        }
    }

    private static class MessageFormatResourceConverter extends ResourceConverter {

        MessageFormatResourceConverter() {
            super(MessageFormat.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) {
            return new MessageFormat(s);
        }
    }

    private static class URLResourceConverter extends ResourceConverter {

        URLResourceConverter() {
            super(URL.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new ResourceConverterException("invalid URL", s, e);
            }
        }
    }

    private static class URIResourceConverter extends ResourceConverter {

        URIResourceConverter() {
            super(URI.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            try {
                return new URI(s);
            } catch (URISyntaxException e) {
                throw new ResourceConverterException("invalid URI", s, e);
            }
        }
    }
}
