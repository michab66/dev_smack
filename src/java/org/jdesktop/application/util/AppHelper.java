/* $Id$
 *
 * http://sourceforge.net/projects/smackfw/
 *
 * Copyright © 2005-2012 Michael G. Binz
 */
package org.jdesktop.application.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.Action;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceManager;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.swingx.GTools;
import org.jdesktop.util.ServiceManager;

/**
 * Help methods on application level.
 *
 * @author Michael Binz
 * @author Vity
 */
public final class AppHelper {

    private static PlatformType activePlatformType = null;

    private AppHelper() {
        throw new AssertionError();
    }

    /**
     * Get the resource manager for the passed application.
     *
     * @param application The application.
     * @return The associated resource manager.
     * @deprecated
     */
    @Deprecated
    public static ResourceManager getResourceManager( Application application )
    {
        return ServiceManager.getApplicationService( ResourceManager.class );
    }

    /**
     * Returns the application resource map.
     *
     * @param application The application instance.
     * @return The application resource map.
     */
    public static ResourceMap getResourceMap( Application application )
    {
        return getResourceManager( application ).getApplicationResourceMap();
    }

    /**
     * Returns the resource map for the given class.
     *
     * @param app The application instance.
     * @param pClass The target class.
     * @return The associated resource map.
     */
    public static ResourceMap getResourceMap( Application app, Class<?> pClass )
    {
        return getResourceManager( app ).getResourceMap(pClass);
    }

    /**
     * Looks up the Action for the given name in the ActionMap associated
     * with the passed object.
     *
     * @param pObj The object that carries the action.
     * @param pName The name of the action.
     * @throws IllegalArgumentException If the Action was not found.
     */
    public static Action getAction( Object pObj, String pName )
    {
        return GTools.getAction( pObj, pName );
    }

    /**
     * Determines a platform type the application is running on.
	 * @return current platform type
     */
    public static PlatformType getPlatform() {
        if (activePlatformType != null)
            return activePlatformType;
        activePlatformType = PlatformType.DEFAULT;
        PrivilegedAction<String> doGetOSName = new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty("os.name");
            }
        };

        String osName = AccessController.doPrivileged(doGetOSName);
        if (osName != null) {
            osName = osName.toLowerCase();
            for (PlatformType platformType : PlatformType.values()) {
                for (String pattern : platformType.getPatterns()) {
                    if (osName.startsWith(pattern)) {
                        return activePlatformType = platformType;
                    }
                }
            }
        }
        return activePlatformType = PlatformType.DEFAULT;
    }
}
