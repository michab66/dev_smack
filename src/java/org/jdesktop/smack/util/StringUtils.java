/* $Id$
 *
 * Utilities
 *
 * Released under Gnu Public License
 * Copyright © 2009-2011 Michael G. Binz
 */
package org.jdesktop.smack.util;

import java.util.ArrayList;
import java.util.Arrays;



/**
 * Helper operations for strings.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class StringUtils
{
    /**
     * Instantiation forbidden.
     */
    private StringUtils()
    {
        throw new AssertionError();
    }



    /**
     * Checks whether a string has content.
     *
     * @param string The string to test.
     * @return {@code False} if the string is either {@code null} or has
     * a trimmed length of zero.  Otherwise {@code true}.
     * @see #hasContent(String, boolean)
     */
    public static boolean hasContent( String string )
    {
        return hasContent( string, true );
    }



    /**
     * Checks whether a string has content.
     *
     * @param string The string to test.
     * @param trim If {@code true}, then the string is trimmed before the test.
     * @return {@code False} if the string is either {@code null} or has
     * a length of zero.  Otherwise {@code true}.
     * @see #hasContent(String)
     * @see String#trim()
     */
    public static boolean hasContent( String string, boolean trim )
    {
        if ( string != null && trim )
            string = string.trim();

        return string != null && string.length() > 0;
    }



    /**
     * Checks whether a string has content.
     *
     * @param string The string to test.
     * @return {@code true} if the string is either {@code null} or has
     * a trimmed length of zero.  Otherwise {@code false}.
     * @see #isEmpty(String, boolean)
     */
    public static boolean isEmpty( String string )
    {
        return isEmpty( string, true );
    }



    /**
     * Checks whether a string has content.
     *
     * @param string The string to test.
     * @param trim If {@code true}, then the string is trimmed before the test.
     * @return {@code true} if the string is either {@code null} or has
     * a length of zero.  Otherwise {@code false}.
     * @see #isEmpty(String)
     * @see String#trim()
     */
    public static boolean isEmpty( String string, boolean trim )
    {
        if ( string != null && trim )
            string = string.trim();

        return string == null || string.isEmpty();
    }



    /**
     * Trims the characters passed in {@code charactersToTrim} from
     * the beginning and the end of {@code stringToTrim}.
     *
     * @param stringToTrim
     * @param charactersToTrim
     * @return The trimmed string.
     */
    public static String trim( String stringToTrim, String charactersToTrim )
    {
        if ( stringToTrim == null )
            throw new NullPointerException( "stringToTrim" );
        if ( charactersToTrim == null )
            throw new NullPointerException( "charactersToTrim" );

        int idx1 = 0;
        while ( -1 != charactersToTrim.indexOf( stringToTrim.charAt( idx1 ) ) )
            idx1++;

        int idx2 = stringToTrim.length() -1;
        while ( -1 != charactersToTrim.indexOf( stringToTrim.charAt( idx2 ) ) )
            idx2--;

        if ( idx2 <= idx1 )
            return EMPTY_STRING;

        return stringToTrim.substring( idx1, idx2 +1 );
    }



    /**
     * Create a string consisting of n occurrences of a single character.
     *
     * @param filler The character used to fill the string.
     * @param count The required length of the string.
     * @return A string containing count filler characters.
     */
    public static String createFilledString( char filler, int count )
    {
        if ( count <= 0 )
            throw new IllegalArgumentException( "count <= 0" );

        StringBuilder result = new StringBuilder( count );

        while ( count-- > 0 )
            result.append( filler );

        return result.toString();
    }



    /**
     * The canonical empty string.  Can be used to keep the number of
     * allocated empty string objects in an application small.
     */
    public static final String EMPTY_STRING = "";



    /**
     * Creates a delimited string from the elements in the passed container.
     * Each element is converted to a string using the
     * {@link String#valueOf(Object)} operation and the resulting strings are
     * concatenated using the passed delimiter.  For a container holding the
     * elements 'Donald', 'Daisy' and 'Scrooge' and a delimiter of '+' this
     * will result in "Donald+Daisy+Scrooge".
     *
     * @param delimiter The delimiter to use.  {@code null} is allowed and
     * results in a concatenation without delimiters.
     * @param iterable The container used for building the delimited list.
     * {@code null} is allowed, resulting in an empty string.
     * @return The result string.
     */
    public static String concatenate( String delimiter, Iterable<?> iterable )
    {
        if ( delimiter == null )
            delimiter = EMPTY_STRING;

        if ( iterable == null )
            return EMPTY_STRING;

        StringBuilder result = null;

        for ( Object c : iterable )
        {
            if ( result == null )
                result = new StringBuilder();
            else
                result.append( delimiter );

            result.append( String.valueOf( c ) );
        }

        return result.toString();
    }

    /**
     * Creates a delimited string from the elements in the passed array.
     * Each element is converted to a string using the
     * {@link String#valueOf(Object)} operation and the resulting strings are
     * concatenated using the passed delimiter.  For an array holding the
     * elements 'Donald', 'Daisy' and 'Scrooge' and a delimiter of '+' this
     * will result in "Donald+Daisy+Scrooge".
     *
     * @param delimiter The delimiter to use.  Must not be {@code null}.
     * @param array The array used for building the delimited list.
     * {@code null} is allowed, resulting in an empty string.
     * @return The result string.
     */
    public static <T> String concatenate( String delimiter, T[] array )
    {
        if ( array == null )
            return EMPTY_STRING;

        return concatenate( delimiter, Arrays.asList( array ) );
    }

    /**
     * Ensures that a string that is to be displayed to a human starts with
     * an upper case character.  If the first character has no upper case
     * equivalent, then the string is not modified.
     *
     * @param sentence The sentence to be converted.
     * @return The converted sentence.
     */
    public static String ensureFirstCharacterUppercase( String sentence )
    {
        if ( Character.isUpperCase( sentence.charAt( 0 ) ) )
            return sentence;

        String firstChar =
            sentence.substring( 0, 1 );
        String remaining =
            sentence.substring( 1 );

        return firstChar.toUpperCase() + remaining;
    }



    /**
     * Makes a string from an object.  If the object is {@code null}
     * returns the empty string.
     *
     * @param object The object.
     * @return The resulting string, never {@code null}.
     */
    public static String toString( Object object )
    {
        if ( object == null )
            return EMPTY_STRING;

        return object.toString();
    }

    /**
     * The default quote character for the quote related operations.
     */
    private static final char QUOTE_CHAR = '\"';
    private static final String ESCAPE_CHAR = "\\";

    /**
     * Quotes a string.
     *
     * @param quoteChar The quote character to use.
     * @param string The string to quote.  A {@code null} is allowed, resulting
     * in a quoted and empty string.
     * @return The resulting string.
     * @see #splitQuoted(String)
     */
    public static String quote( char quoteChar, String string )
    {
        if ( ! StringUtils.hasContent( string ) )
            return EMPTY_STRING + QUOTE_CHAR + QUOTE_CHAR;

        StringBuilder result = new StringBuilder();

        result.append( quoteChar );

        for ( int i = 0 ; i < string.length() ; i++ )
        {
            char c = string.charAt( i );

            if ( c == QUOTE_CHAR )
                result.append( ESCAPE_CHAR );

            result.append( c );
        }

        result.append( quoteChar );

        return result.toString();
    }

    /**
     * Quotes the passed string.
     *
     * @param string The string to quote.  A {@code null} is allowed, resulting
     * in a quoted and empty string.
     * @return The quoted string.
     */
    public static String quote( String string )
    {
        return quote( QUOTE_CHAR, string );
    }

    /**
     * Splits a whitespace delimited and quoted string as used on command
     * lines into its elements.  For example 'Admiral "von Schneider"' is
     * split into 'Admiral' and 'von Schneider'.
     *
     * TODO(micbinz) does not support quoting of quote characters.
     * @param quoteChar The character used for quotes.
     * @param string The string to split.
     * @return The split strings.
     */
    public static String[] splitQuoted( char quoteChar, String string )
    {
        // See also http://stackoverflow.com/questions/10695143/split-a-quoted-string-with-a-delimiter
        // for a sketch of solving the same with regular expressions.  Can be made workable, but is even less
        // understandable.

        boolean inDoubleQuotes = false;

        ArrayList<String> result = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        for ( char c : string.toCharArray() )
        {
            boolean isDoubleQuote = c == quoteChar;

            if ( isDoubleQuote )
            {
                inDoubleQuotes = !inDoubleQuotes;

                if ( ! inDoubleQuotes )
                {
                    // End of the quoted sequence.
                    result.add( sb.toString() );
                    sb.setLength( 0 );
                }
                continue;
            }

            if ( inDoubleQuotes )
            {
                sb.append( c );
                continue;
            }
            else if ( Character.isWhitespace( c ) )
            {
                if ( sb.length() > 0 )
                {
                    result.add( sb.toString() );
                    sb.setLength( 0 );
                }
                continue;
            }

            sb.append( c );
        }

        if ( sb.length() > 0 || inDoubleQuotes )
            result.add( sb.toString() );

        return result.toArray( new String[ result.size()] );
    }
    /**
     * Splits a whitespace delimited and quoted string as used on command
     * lines into its elements.  For example 'Admiral "von Schneider"' is
     * split into 'Admiral' and 'von Schneider'.
     *
     * TODO(micbinz) does not support quoting of quote characters.
     * @param toSplit The string to split
     * @return The split strings.
     */
    public static String[] splitQuoted( String toSplit )
    {
        return splitQuoted( QUOTE_CHAR, toSplit );
    }


    static void testSplitQuote()
    {
        // Test split quote.
        String[] testCases = {
                // Plain
                "ab cd ef",
                // Whitespace is tab.
                "ab\tcd\tef",
                // Whitespace is mixed and at the end.
                "ab\tcd ef\t \t \t \t \tgh \t",
                // Quoted simple.
                "ab \"cd ef\" gh",
                // Quoted leading and trailing spaces.
                "ab \" cd ef \" gh",
                // Last quote not terminated, trailing space.
                "ab \" cd ef ",
                // Empty string.
                "ab \"\" cd",
                // Pathological: ab" cd ef" -> "ab cd ef"
                "ab\" cd ef ",
                // Empty string at eol.
                "michael \""
        };

        for ( String c : testCases )
        {
            System.err.println( "parseQuoted( '" + c + "' )" );
            for ( String c1 : splitQuoted( c ) )
                System.err.println( "'" + c1 + "'" );
        }
    }
}
