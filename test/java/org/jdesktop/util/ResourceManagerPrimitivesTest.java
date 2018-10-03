package org.jdesktop.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Michael
 */
public class ResourceManagerPrimitivesTest
{
    private final ResourceManager _rm =
            ServiceManager.getApplicationService( ResourceManager.class );


    @Resource
    private boolean booleanResource;
    @Resource
    private boolean[] booleanResourceArray;

    @Resource
    private byte byteResource;
    @Resource
    private short shortResource;
    @Resource
    private int intResource;
    @Resource
    private long longResource;

    @Resource
    private float floatResource;
    @Resource
    private double doubleResource;

    @Before
    public void testInit()
    {
        _rm.injectResources( this );
    }

    @Test
    public void testBooleanPrimitive()
    {
        assertEquals( true, booleanResource );

        boolean[] values =
            { true, false, true };

        assertArrayEquals( values, booleanResourceArray );
    }

    @Test
    public void testBytePrimitive()
    {
        assertEquals( 8, byteResource );
    }
    @Test
    public void testShortPrimitive()
    {
        assertEquals( 16, shortResource );
    }
    @Test
    public void testIntegerPrimitive()
    {
        assertEquals( 32, intResource );
    }
    @Test
    public void testLongPrimitive()
    {
        assertEquals( 64, longResource );
    }

    @Test
    public void testFloatPrimitive()
    {
        assertEquals( 2.71f, floatResource, 0.0f );
    }

    @Test
    public void testDoublePrimitive()
    {
        assertEquals( 3.14159265, doubleResource, 0.0f );
    }
}
