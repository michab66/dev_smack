package org.smack.application;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;


public class CliApplicationPropertyTest
{
    @Test
    public void testBooleanSet() throws IOException
    {
        final var err =
                new ArrayList<String>();
        final var out =
                new ArrayList<String>();

        CliApplicationTest.execCli(
                out,
                err,
                ApplicationUnderTestProperties::main,
                "c_booleanProperty",
                "-booleanProperty=true");

        assertEquals( 0, err.size() );
        assertEquals( 1, out.size() );
        assertEquals( "true", out.get( 0 ) );
    }

    @Test
    public void testBooleanUnset() throws IOException
    {
        final var err =
                new ArrayList<String>();
        final var out =
                new ArrayList<String>();

        CliApplicationTest.execCli(
                out,
                err,
                ApplicationUnderTestProperties::main,
                "c_booleanProperty" );

        assertEquals( 0, err.size() );
        assertEquals( 1, out.size() );
        assertEquals( "false", out.get( 0 ) );
    }
    @Test
    public void testLevelSet() throws IOException
    {
        final var err =
                new ArrayList<String>();
        final var out =
                new ArrayList<String>();

        CliApplicationTest.execCli(
                out,
                err,
                ApplicationUnderTestProperties::main,
                "c_levelProperty",
                "-level=WARNING");

        assertEquals( 0, err.size() );
        assertEquals( 1, out.size() );
        assertEquals( "WARNING", out.get( 0 ) );
    }

    @Test
    public void testLevelUnset() throws IOException
    {
        final var err =
                new ArrayList<String>();
        final var out =
                new ArrayList<String>();

        CliApplicationTest.execCli(
                out,
                err,
                ApplicationUnderTestProperties::main,
                "c_levelProperty" );

        assertEquals( 0, err.size() );
        assertEquals( 1, out.size() );
        assertEquals( "ALL", out.get( 0 ) );
    }
}
