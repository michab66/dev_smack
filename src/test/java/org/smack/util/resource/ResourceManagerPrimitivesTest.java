package org.smack.util.resource;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.smack.util.ServiceManager;
import org.smack.util.resource.ResourceManager.Resource;

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
    private byte[] byteResourceArray;

    @Resource
    private short shortResource;
    @Resource
    private short[] shortResourceArray;

    @Resource
    private int intResource;
    @Resource
    private long longResource;

    @Resource
    private float floatResource;
    @Resource
    private float floatResourceArray[];

    @Resource
    private double doubleResource;
    @Resource
    private double doubleResourceArray[];

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

        byte[] values = {
                -128, 8, 127
        };

        assertArrayEquals( values, byteResourceArray );
    }

    @Test
    public void testShortPrimitive()
    {
        assertEquals( 16, shortResource );

        short[] values = {
                0, 1, 15
        };

        assertArrayEquals( values, shortResourceArray );
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

        assertEquals( -2.71f, floatResourceArray[0], 0.0f );
        assertEquals( -3.14f, floatResourceArray[1], 0.0f );
    }

    @Test
    public void testDoublePrimitive()
    {
        assertEquals( 3.14159265, doubleResource, 0.0f );

        assertEquals( 1e2, doubleResourceArray[0], 0.0f );
        assertEquals( 1e3, doubleResourceArray[1], 0.0f );
    }
}
