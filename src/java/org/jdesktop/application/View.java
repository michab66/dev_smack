
/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package org.jdesktop.application;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JToolBar;

import org.jdesktop.beans.AbstractBeanEdt;

/**
 * A View encapsulates a top-level Application GUI component, like a JFrame
 * or an Applet, and its main GUI elements: a menu bar, tool bar, component,
 * and a status bar.  All of the elements are optional (although a View without
 * a main component would be unusual).  Views have a {@code JRootPane}, which
 * is the root component for all of the Swing Window types as well as JApplet.
 * Setting a View property, like {@code menuBar} or {@code toolBar}, just
 * adds a component to the rootPane in a way that's defined by the View subclass.
 * By default the View elements are arranged in a conventional way:
 * <ul>
 * <li> {@code menuBar} - becomes the rootPane's JMenuBar
 * <li> {@code toolBar} - added to {@code BorderLayout.NORTH} of the rootPane's contentPane
 * <li> {@code component} - added to {@code BorderLayout.CENTER} of the rootPane's contentPane
 * <li> {@code statusBar} - added to {@code BorderLayout.SOUTH} of the rootPane's contentPane
 * </ul>
 * <p>
 * To show or hide a View you call the corresponding Application methods.  Here's a simple
 * example:
 * <pre>
 * class MyApplication extends SingleFrameApplication {
 *     &#064;ppOverride protected void startup() {
 *         View view = getMainView();
 *         view.setComponent(createMainComponent());
 *         view.setMenuBar(createMenuBar());
 *         show(view);
 *     }
 * }
 * </pre>
 * <p>
 * The advantage of Views over just configuring a JFrame or JApplet
 * directly, is that a View is more easily moved to an alternative
 * top level container, like a docking framework.
 *
 * @see JRootPane
 * @see Application#show(View)
 * @see Application#hide(View)
 * @version $Rev$
 */
public class View extends AbstractBeanEdt
{
    private final Application application;
    private JRootPane rootPane = null;
    private Component component = null;
    private JMenuBar _menuBar = null;
    private List<JToolBar> toolBars = Collections.emptyList();
    private final JComponent toolBarsPanel = null;
    private JComponent statusBar = null;

    /**
     * Construct an empty View object for the specified Application.
     *
     * @param application the Application responsible for showing/hiding this View
     * @see Application#show(View)
     * @see Application#hide(View)
     */
    public View(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("null application");
        }
        this.application = application;
    }

    /**
     * Returns the {@code Application} that's responsible for showing/hiding this View.
     *
     * @return the Application that owns this View
     * @see Application#show(View)
     * @see Application#hide(View)
     */
    public final Application getApplication() {
        return application;
    }

    /**
     * Gets the {@code JRootPane} for this View.  All of the components for this
     * View must be added to its rootPane.  Most applications will do so
     * by setting the View's {@code component}, {@code menuBar}, {@code toolBar},
     * and {@code statusBar} properties.
     *
     * @return The {@code rootPane} for this View
     * @see #setComponent
     * @see #setMenuBar
     * @see #setToolBar
     * @see #setStatusBar
     */
    public JRootPane getRootPane() {
        if (rootPane == null) {
            rootPane = new JRootPane();
            rootPane.setOpaque(true);
        }
        return rootPane;
    }

    private void replaceContentPaneChild(Component oldChild, Component newChild, String constraint) {
        Container contentPane = getRootPane().getContentPane();
        if (oldChild != null) {
            contentPane.remove(oldChild);
        }
        if (newChild != null) {
            contentPane.add(newChild, constraint);
        }

        getRootPane().revalidate();
    }

    /**
     * Returns the main {@link JComponent} for this View.
     *
     * @return The {@code component} for this View
     * @see #setComponent
     */
    public Component getComponent() {
        return component;
    }

    /**
     * Sets the single main Component for this View.  It's added to the
     * {@code BorderLayout.CENTER} of the rootPane's contentPane.  If
     * the component property was already set, the old component is removed
     * first.
     * <p>
     * This is a bound property.  The default value is null.
     *
     * @param component The {@code component} for this View
     * @see #getComponent
     */
    public void setComponent(Component component) {
        Component oldValue = this.component;
        this.component = component;
        replaceContentPaneChild(oldValue, this.component, BorderLayout.CENTER);
        firePropertyChange("component", oldValue, this.component);
    }

    /**
     * Returns the main {@link JMenuBar} for this View.
     *
     * @return The {@code menuBar} for this View
     * @see #setMenuBar
     */
    public JMenuBar getMenuBar() {
        return _menuBar;
    }

    /**
     * Sets the menu bar for this View.
     * <p>
     * This is a bound property.  The default value is null.
     *
     * @param menuBar The {@code menuBar} for this View.
     * @see #getMenuBar
     */
    public void setMenuBar(JMenuBar menuBar)
    {
        // Add a menu bar only in case we really have menu entries.
        // This is needed to smoothly cooperate with round windows.
        if ( menuBar != null && menuBar.getSubElements().length == 0 )
            menuBar = null;

        JMenuBar oldValue = getMenuBar();
        _menuBar = menuBar;

        getRootPane().setJMenuBar( menuBar );

        firePropertyChange("menuBar", oldValue, menuBar);
    }

    /**
     * Returns the list of tool bars for this View
     *
     * @return The list of tool bars
     */
    public List<JToolBar> getToolBars() {
        return toolBars;
    }

    /**
     * Sets the tool bars for this View
     * <p>
     * This is a bound property.  The default value is an empty list.
     *
     * @param toolBars
     * @see #setToolBar(JToolBar)
     * @see #getToolBars()
     */
    public void setToolBars(List<JToolBar> toolBars) {
        if (toolBars == null) {
            throw new IllegalArgumentException("null toolbars");
        }
        List<JToolBar> oldValue = getToolBars();
        this.toolBars = Collections.unmodifiableList(new ArrayList<JToolBar>(toolBars));
        JComponent oldToolBarsPanel = this.toolBarsPanel;
        JComponent newToolBarsPanel = null;
        if (this.toolBars.size() == 1) {
            newToolBarsPanel = toolBars.get(0);
        } else if (this.toolBars.size() > 1) {
            newToolBarsPanel = new JPanel();
            for (JComponent toolBar : this.toolBars) {
                newToolBarsPanel.add(toolBar);
            }
        }
        replaceContentPaneChild(oldToolBarsPanel, newToolBarsPanel, BorderLayout.NORTH);
        firePropertyChange("toolBars", oldValue, this.toolBars);
    }

    /**
     * Gets the first tool bar for this View
     *
     * @return The first {@link JToolBar} for this View
     * @see #setToolBars
     * @see #getToolBars
     * @see #setToolBar
     */
    public final JToolBar getToolBar() {
        List<JToolBar> toolBars = getToolBars();
        return (toolBars.size() == 0) ? null : toolBars.get(0);
    }

    /**
     * Sets the only tool bar for this View.
     * <p>
     * This is a bound property.
     *
     * @param toolBar The {@link JToolBar} for this view. If {@code null} resets the tool bar.
     * @see #getToolBar()
     * @see #setToolBars(List)
     * @see #getToolBars()
     */
    public final void setToolBar(JToolBar toolBar) {
        JToolBar oldValue = getToolBar();
        List<JToolBar> toolBars = Collections.emptyList();
        if (toolBar != null) {
            toolBars = Collections.singletonList(toolBar);
        }
        setToolBars(toolBars);
        firePropertyChange("toolBar", oldValue, toolBar);
    }

    /**
     * Returns the Status bar for this View.
     *
     * @return The status bar {@link JComponent} for this View
     */
    public JComponent getStatusBar() {
        return statusBar;
    }

    /**
     * Sets the status bar for this View. The status bar is a generic {@link JComponent}.
     *
     * @param statusBar The status bar {@link JComponent} for this View
     */
    public void setStatusBar(JComponent statusBar) {
        JComponent oldValue = this.statusBar;
        this.statusBar = statusBar;
        replaceContentPaneChild(oldValue, this.statusBar, BorderLayout.SOUTH);
        firePropertyChange("statusBar", oldValue, this.statusBar);
    }
}
