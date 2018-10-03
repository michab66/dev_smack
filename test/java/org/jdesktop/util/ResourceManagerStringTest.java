package org.jdesktop.util;

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 *
 * @version $Revision$
 * @author Michael Binz
 */
public class ResourceManagerStringTest
{
    private final ResourceManager _rm =
            ServiceManager.getApplicationService( ResourceManager.class );

    @Resource
    private String stringResource;
    @Resource
    private String[] stringArrayResource;

    @Before
    public void testInit()
    {
        _rm.injectResources( this );
    }

    @Test
    public void testString()
    {
        assertEquals( "Michael Binz", stringResource );
    }
    @Test
    public void testStringArray()
    {
        String[] values = {
                "Januar",
                "Feb uar",
                "März",
                "April",
                "Mai",
                "Juni",
                "Juli",
                "August",
                "September",
                "Oktober",
                "November",
                "Dezember"
        };

        Assert.assertArrayEquals( values, stringArrayResource );
    }
}
