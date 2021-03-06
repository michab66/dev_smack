/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2016 Michael G. Binz
 */
package org.smack.application;

import java.awt.Image;
import java.util.Objects;

import org.smack.util.ServiceManager;
import org.smack.util.resource.ResourceManager;

/**
 * Application information.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class ApplicationInfo
{
    private final Class<?> _applicationClass;

    /**
     * Catches automatic instantiation in ServiceManager, being not
     * allowed for this class.  Instead in {@code main} explicitly initialize
     * the ServiceMenager with an instance of this class.
     */
    public ApplicationInfo()
    {
        throw new IllegalStateException( "Init ServiceManager in main." );
    }

    public ApplicationInfo( Class<?> applicationClass )
    {
        _applicationClass =
               Objects.requireNonNull( applicationClass );
        ResourceManager rm =
                ServiceManager.getApplicationService(
                        ResourceManager.class );
        var arm =
                rm.getResourceMap( _applicationClass );

        id = arm.get(
                "Application.id" );
        title = arm.get(
                "Application.title" );
        version = arm.get(
                "Application.version" );
        try
        {
            icon = arm.getAs(
                    "Application.icon", Image.class );
        }
        catch ( Exception e )
        {
            icon = null;
        }
        vendor = arm.get(
                "Application.vendor" );
        vendorId = arm.get(
                "Application.vendorId" );
    }

    public Class<?> getApplicationClass()
    {
        return _applicationClass;
    }

    private final String id;

    /**
     * Return the application's id as defined in the resources.
     * @return The application's id.
     */
    public String getId()
    {
        return id;
    }

    private final String title;

    /**
     * Return the application's title as defined in the resources.
     * @return The application's title.
     */
    public String getTitle()
    {
        return title;
    }

    private final String version;

    /**
     * Return the application's version as defined in the resources.
     * @return The application's version.
     */
    public String getVersion()
    {
        return version;
    }

    private Image icon;

    /**
     * Return the application's icon as defined in the resources.
     * @return The application icon.
     */
    public Image getIcon()
    {
        return icon;
    }

    private final String vendor;

    /**
     * Return the application's vendor as defined in the resources.
     * @return The vendor name.
     */
    public String getVendor()
    {
        return vendor;
    }

    private final String vendorId;

    /**
     * Return the application's vendor as defined in the resources.
     * @return The vendor name.
     */
    public String getVendorId()
    {
        return vendorId;
    }
}
