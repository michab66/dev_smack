/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package org.jdesktop.application;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;

import org.jdesktop.application.util.AppHelper;
import org.jdesktop.beans.AbstractBeanEdt;
import org.jdesktop.util.ServiceManager;

/**
 * A singleton that manages shared objects, like actions, resources, and tasks,
 * for {@code Applications}.
 * <p>
 * {@link Application Applications} use {@code ApplicationContext},
 * via {@link Application#getContext}, to access global values and services.
 * The majority of the Swing Application Framework API can be accessed through {@code
 * ApplicationContext}.
 *
 * @see Application
 * @author Hans Muller (Hans.Muller@Sun.COM)
 */
public final class ApplicationContext extends AbstractBeanEdt {

    private final List<TaskService> taskServices;
    private final List<TaskService> taskServicesReadOnly;
    private final Application application;
    private JComponent focusOwner = null;
    private Clipboard clipboard = null;

    protected ApplicationContext( Application a ) {
        application = a;
        taskServices = new CopyOnWriteArrayList<TaskService>();
        taskServices.add(new TaskService(TaskService.DEFAULT_NAME));
        taskServicesReadOnly = Collections.unmodifiableList(taskServices);
    }

    /**
     * The {@code Application} singleton, or null if {@code launch} hasn't
     * been called yet.
     *
     * @return the launched Application singleton.
     * @see Application#launch
     */
    public final synchronized Application getApplication() {

        if ( application.isLaunched() )
            return application;
        return null;
    }

    /**
     * The application's {@code ResourceManager} provides
     * read-only cached access to resources in ResourceBundles via the
     * {@link ResourceMap ResourceMap} class.
     *
     * @return this application's ResourceManager.
     * @deprecated Use Application.getResourceManager
     */
    @Deprecated
    private final ResourceManager getResourceManager() {
        return AppHelper.getResourceManager( application );
    }

    /**
     * Returns the {@link ResourceMap#getParent chain} of ResourceMaps
     * that's shared by the entire application, beginning with the one
     * defined for the Application class, i.e. the value of the
     * {@code applicationClass} property.
     * <p>
     * This is just a convenience method that calls
     * {@link ResourceManager#getApplicationResourceMap()
     * ResourceManager.getResourceMap()}.  It's defined as:
     * <pre>
     * return getResourceManager().getResourceMap();
     * </pre>
     *
     * @return the Application's ResourceMap
     * @see ResourceManager#getApplicationResourceMap()
     */
    public final ResourceMap getResourceMap() {
        return getResourceManager().getApplicationResourceMap();
    }

    /**
     * Return this application's ActionManager.
     * @return this application's ActionManager.
     * @see #getActionMap(Object)
     */
    public final ActionManager getActionManager() {
        return ServiceManager.getApplicationService( ActionManager.class );
    }

    /**
     * Returns the shared {@code ActionMap} chain for the entire {@code Application}.
     * <p>
     *  This is just a convenience method that calls
     * {@link ActionManager#getActionMap()
     * ActionManager.getActionMap()}.  It's defined as:
     * <pre>
     * return getActionManager().getActionMap()
     * </pre>
     *
     * @return the {@code ActionMap} chain for the entire {@code Application}.
     * @see ActionManager#getActionMap()
     */
    public final ApplicationActionMap getActionMap() {
        return getActionManager().getActionMap();
    }

    /**
     * Returns the {@code ApplicationActionMap} chain for the specified
     * actions class and target object.
     * <p>
     *  This is just a convenience method that calls
     * {@link ActionManager#getActionMap()
     * ActionManager.getActionMap(Class, Object)}.  It's defined as:
     * <pre>
     * return getActionManager().getActionMap(actionsClass, actionsObject)
     * </pre>
     *
     * @param actionsClass
     * @param actionsObject
     * @return the {@code ActionMap} chain for the entire {@code Application}.
     * @see ActionManager#getActionMap(Class, Object)
     */
    public final ApplicationActionMap getActionMap(Class<?> actionsClass, Object actionsObject) {
        return getActionManager().getActionMap(actionsClass, actionsObject);
    }

    /**
     * Defined as {@code getActionMap(actionsObject.getClass(), actionsObject)}.
     *
     * @param actionsObject
     * @return the {@code ActionMap} for the specified object
     * @see #getActionMap(Class, Object)
     */
    public final ApplicationActionMap getActionMap(Object actionsObject) {
        if (actionsObject == null) {
            throw new IllegalArgumentException("null actionsObject");
        }
        return getActionManager().getActionMap(actionsObject.getClass(), actionsObject);
    }

    /**
     * The shared {@link SessionStorage SessionStorage} object.
     *
     * @return the shared {@link SessionStorage SessionStorage} object.
     */
    public final SessionStorage getSessionStorage() {
        return ServiceManager.getApplicationService( SessionStorage.class );
    }

    /**
     * Return a shared {@code Clipboard}.
     * @return A shared {@code Clipboard}.
     */
    public Clipboard getClipboard() {
        if (clipboard == null) {
            try {
                clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (SecurityException e) {
                clipboard = new Clipboard("sandbox");
            }
        }
        return clipboard;
    }

    /**
     * Returns the application's focus owner.
     * @return  The application's focus owner.
     */
    public JComponent getFocusOwner() {
        return focusOwner;
    }

    /**
     * Changes the application's focus owner.
     * @param focusOwner new focus owner
     */
    void setFocusOwner(JComponent focusOwner) {
        Object oldValue = this.focusOwner;
        this.focusOwner = focusOwner;
        firePropertyChange("focusOwner", oldValue, this.focusOwner);
    }

    private List<TaskService> copyTaskServices() {
        return new ArrayList<TaskService>(taskServices);
    }

    /**
     * Register a new TaskService with the application. The task service
     * then be retrieved by name via {@link ApplicationContext#getTaskService(String)}.
     *
     * @param taskService Task service to register
     */
    public void addTaskService(TaskService taskService) {
        if (taskService == null) {
            throw new IllegalArgumentException("null taskService");
        }
        List<TaskService> oldValue = null, newValue = null;
        boolean changed = false;
        synchronized (taskServices) {
            if (!taskServices.contains(taskService)) {
                oldValue = copyTaskServices();
                taskServices.add(taskService);
                newValue = copyTaskServices();
                changed = true;
            }
        }
        if (changed) {
            firePropertyChange("taskServices", oldValue, newValue);
        }
    }

    /**
     * Unregister a previously registered TaskService. The task service
     * is not shut down.
     *
     * @param taskService TaskService to unregister
     */
    public void removeTaskService(TaskService taskService) {
        if (taskService == null) {
            throw new IllegalArgumentException("null taskService");
        }
        List<TaskService> oldValue = null, newValue = null;
        boolean changed = false;
        synchronized (taskServices) {
            if (taskServices.contains(taskService)) {
                oldValue = copyTaskServices();
                taskServices.remove(taskService);
                newValue = copyTaskServices();
                changed = true;
            }
        }
        if (changed) {
            firePropertyChange("taskServices", oldValue, newValue);
        }
    }

    /**
     * Look up a task service by name.
     *
     * @param name Name of the task service to retrieve.
     * @return Task service found, or null if no service of that name found
     */
    public TaskService getTaskService(String name) {
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }
        for (TaskService taskService : taskServices) {
            if (name.equals(taskService.getName())) {
                return taskService;
            }
        }
        return null;
    }

    /**
     * Returns the default TaskService, i.e. the one named "default":
     * <code>return getTaskService("default")</code>.  The
     * {@link ApplicationAction#actionPerformed ApplicationAction actionPerformed}
     * method executes background <code>Tasks</code> on the default
     * TaskService.  Application's can launch Tasks in the same way, e.g.
     * <pre>
     * Application.getInstance().getContext().getTaskService().execute(myTask);
     * </pre>
     *
     * @return the default TaskService.
     * @see #getTaskService(String)
     *
     */
    public final TaskService getTaskService() {
        return getTaskService(TaskService.DEFAULT_NAME);
    }

    /**
     * Returns a read-only view of the complete list of TaskServices.
     *
     * @return a list of all of the TaskServices.
     * @see #addTaskService
     * @see #removeTaskService
     */
    public List<TaskService> getTaskServices() {
        return taskServicesReadOnly;
    }
}
