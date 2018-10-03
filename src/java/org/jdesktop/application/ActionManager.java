
/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package org.jdesktop.application;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.ActionMap;

import org.jdesktop.application.util.AppHelper;
import org.jdesktop.beans.AbstractBeanEdt;

/**
 * The application's {@code ActionManager} provides read-only cached
 * access to {@code ActionMaps} that contain one entry for each method
 * marked with the {@code @Action} annotation in a class.
 *
 *
 * @see ApplicationContext#getActionMap(Object)
 * @see ApplicationActionMap
 * @see ApplicationAction
 * @author Hans Muller (Hans.Muller@Sun.COM)
 */
@Deprecated
public final class ActionManager extends AbstractBeanEdt
{
    private final WeakHashMap<Object, WeakReference<ApplicationActionMap>> actionMaps =
            new WeakHashMap<Object, WeakReference<ApplicationActionMap>>();
    private ApplicationActionMap globalActionMap = null;

    public ActionManager()
    {
    }

    private ApplicationActionMap createActionMapChain(
            Class<?> startClass, Class<?> stopClass, Object actionsObject, ResourceMap resourceMap) {
        // All of the classes from stopClass to startClass, inclusive.
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (Class<?> c = startClass;; c = c.getSuperclass()) {
            classes.add(c);
            if (c.equals(stopClass)) {
                break;
            }
        }
        Collections.reverse(classes);
        // Create the ActionMap chain, one per class
//        ApplicationContext ctx = context;
        ApplicationActionMap parent = null;
        for (Class<?> cls : classes) {
            ApplicationActionMap appAM = new ApplicationActionMap( cls, actionsObject, resourceMap);
            appAM.setParent(parent);
            parent = appAM;
        }
        return parent;
    }

    /**
     * The {@code ActionMap} chain for the entire {@code Application}.
     * <p>
     * Returns an {@code ActionMap} with the {@code @Actions} defined
     * in the application's {@code Application} subclass, i.e. the
     * the value of:
     * <pre>
     * ApplicationContext.getInstance().getApplicationClass()
     * </pre>
     * The remainder of the chain contains one {@code ActionMap}
     * for each superclass, up to {@code Application.class}.  The
     * {@code ActionMap.get()} method searches the entire chain, so
     * logically, the {@code ActionMap} that this method returns contains
     * all of the application-global actions.
     * <p>
     * The value returned by this method is cached.
     *
     * @return the {@code ActionMap} chain for the entire {@code Application}.
     * @see #getActionMap(Class, Object)
     * @see ApplicationContext#getActionMap()
     * @see ApplicationContext#getActionMap(Class, Object)
     * @see ApplicationContext#getActionMap(Object)
     */
    public ApplicationActionMap getActionMap() {
        if (globalActionMap == null) {
            Application appObject = Application.getInstance();
            Class<?> appClass = appObject.getClass();
            ResourceMap resourceMap = AppHelper.getResourceMap( appObject );
            globalActionMap = createActionMapChain(appClass, Application.class, appObject, resourceMap);
        }
        return globalActionMap;
    }

    /**
     * Returns the {@code ApplicationActionMap} chain for the specified
     * actions class and target object.
     * <p>
     * The specified class can contain methods marked with
     * the {@code @Action} annotation.  Each one will be turned
     * into an {@link ApplicationAction ApplicationAction} object
     * and all of them will be added to a single
     * {@link ApplicationActionMap ApplicationActionMap}.  All of the
     * {@code ApplicationActions} invoke their {@code actionPerformed}
     * method on the specified {@code actionsObject}.
     * The parent of the returned {@code ActionMap} is the global
     * {@code ActionMap} that contains the {@code @Actions} defined
     * in this application's {@code Application} subclass.
     *
     * <p>
     * To bind an {@code @Action} to a Swing component, one specifies
     * the {@code @Action's} name in an expression like this:
     * <pre>
     * ApplicationContext ctx = Application.getInstance(MyApplication.class).getContext();
     * MyActions myActions = new MyActions();
     * myComponent.setAction(ac.getActionMap(myActions).get("myAction"));
     * </pre>
     *
     * <p>
     * The value returned by this method is cached.  The lifetime of
     * the cached entry will be the same as the lifetime of the {@code
     * actionsObject} and the {@code ApplicationActionMap} and {@code
     * ApplicationActions} that refer to it.  In other words, if you
     * drop all references to the {@code actionsObject}, including
     * its {@code ApplicationActions} and their {@code
     * ApplicationActionMaps}, then the cached {@code ActionMap} entry
     * will be cleared.
     *
     * @param actionsClass
     * @param actionsObject
     * @see #getActionMap()
     * @return the {@code ApplicationActionMap} for {@code actionsClass} and {@code actionsObject}
     */
    public ApplicationActionMap getActionMap(Class<?> actionsClass, Object actionsObject) {
        if (actionsClass == null) {
            throw new IllegalArgumentException("null actionsClass");
        }
        if (actionsObject == null) {
            throw new IllegalArgumentException("null actionsObject");
        }
        if (!actionsClass.isAssignableFrom(actionsObject.getClass())) {
            throw new IllegalArgumentException("actionsObject not instanceof actionsClass");
        }
        synchronized (actionMaps) {
            WeakReference<ApplicationActionMap> ref = actionMaps.get(actionsObject);
            ApplicationActionMap classActionMap = (ref != null) ? ref.get() : null;
            if ((classActionMap == null) || (classActionMap.getActionsClass() != actionsClass)) {
                Class<?> actionsObjectClass = actionsObject.getClass();
                ResourceMap resourceMap =
                    Application.getResourceManager().getResourceMap(actionsObjectClass, actionsClass);
                classActionMap = createActionMapChain(actionsObjectClass, actionsClass, actionsObject, resourceMap);
                ActionMap lastActionMap = classActionMap;
                while (lastActionMap.getParent() != null) {
                    lastActionMap = lastActionMap.getParent();
                }
                lastActionMap.setParent(getActionMap());
                actionMaps.put(actionsObject, new WeakReference<ApplicationActionMap>(classActionMap));
            }
            return classActionMap;
        }
    }



}
