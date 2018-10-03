/* $Id$
 *
 * Released under Gnu Public License
 * Copyright © 2003-2015 Michael G. Binz
 */
package org.jdesktop.swingx.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;

import org.jdesktop.application.Application;
import org.jdesktop.smack.util.MultiMap;
import org.jdesktop.smack.util.StringUtils;
import org.jdesktop.swingx.JXToolbar;


/**
 * Manages an application's set of actions. The current main responsibility is
 * to keep references to all existing application actions and to compute a popup
 * menu and a menu bar.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public final class MackActionManager
{
    private final static MackActionSorter _sorter =
        new MackActionSorter();

    private final ActionContainerFactory _acf =
        new ActionContainerFactory();

    private final MultiMap<Object, String, MackAction> _toolbarMap =
        new MultiMap<Object, String, MackAction>();
    private final MultiMap<Object, String, MackAction> _menuMap =
        new MultiMap<Object, String, MackAction>();
    private final MultiMap<Object, String, MackAction> _popupMap =
        new MultiMap<Object, String, MackAction>();

    /**
     * Constants representing the default application menus.
     */
    public enum ApplicationMenu {
        MEN_FILE, MEN_EDIT, MEN_TOOLS, MEN_HELP
    }

    /**
     * Create an instance.
     */
    public MackActionManager()
    {
    }

    /**
     * Get the ordered menu ids.
     *
     * @return The ordered menu ids.
     */
    private static List<String> getOrderedMenuIds()
    {
        ArrayList<String> result = new ArrayList<String>();

        String userConfiguredMenuOrder = Application.getInstance().getContext()
                .getResourceMap().getString("MACK_MENU");

        if (userConfiguredMenuOrder != null)
        {
            String[] menuIds = userConfiguredMenuOrder.split(" ");
            result.addAll(Arrays.asList(menuIds));
        }
        else
        {
            for (ApplicationMenu c : ApplicationMenu.values())
                result.add(c.toString());
        }

        result.trimToSize();

        return result;
    }

    /**
     * Add an action to the <code>ActionManager</code>'s set of managed
     * actions.
     *
     * @param mackAction The action to add.
     */
    public void addAction(MackAction mackAction)
    {
        assert mackAction != null;
        Object key = mackAction.getKey();
        assert key != null;

        ActionManager am = ActionManager.getInstance();

        assert null == am.getAction(key);

        // First add the action to our embedded action manager.
        am.addAction(mackAction);

        // One key, many values.
        if (mackAction.isToolbar()) {
            _toolbarMap.put(
                    mackAction.getCategory(),
                    mackAction.getKey(),
                    mackAction);
        }

        if (mackAction.isMenubar()) {
            _menuMap.put(
                    mackAction.getCategory(),
                    mackAction.getKey(),
                    mackAction);
        }

        if (mackAction.isPopup()) {
            _popupMap.put(
                    mackAction.getCategory(),
                    mackAction.getKey(),
                    mackAction);
        }
    }

    /**
     * Creates a menu bar from the currently registered set of actions.
     *
     * @return A reference to a menu bar holding the configured actions.
     */
    public JMenuBar getMenuBar()
    {
        ActionManager am = ActionManager.getInstance();

        List<Object> menu = new ArrayList<Object>();

        for (String c : getOrderedMenuIds()) {
            Map<String, MackAction> currentMenu = _menuMap.getAll(c);

            // If we got no entries for the current menu we skip it.
            if (currentMenu.size() == 0)
                continue;

            // Create the list for the current menu actions.
            List<Object> current = new ArrayList<Object>();
            // Add the menu head action.
            current.add(new DummyAction(c, am)
                    .getActionCommand());

            for (MackAction cAction : _sorter
                    .sortCategory(currentMenu.values()))
                current.add(cAction.getKey());

            menu.add(current);
        }

        return _acf.createMenuBar(menu,am);
    }

    /**
     * Creates a tool bar from the currently registered set of actions.
     *
     * @return A reference to the tool bar holding the configured actions.
     */
    public JXToolbar getToolbar()
    {
        List<Action> actions = getActionBar(_toolbarMap);

        JXToolbar result = new JXToolbar();

        for (Action c : actions)
        {
            if (c != null)
                result.add(c);
            else
                result.addSeparator();
        }

        return result;
    }

    /**
     * Make a list of actions that is suitable to create the toolbar and the
     * popup menu.
     *
     * @return A list of actions, null entries represent separators.
     */
    private static List<Action> getActionBar(
            MultiMap<Object, String, MackAction> actionMap)
    {
        boolean firstSeparator = false;

        List<String> orderedGroupNames = getOrderedMenuIds();

        for (Object groupName : actionMap.getPrimaryKeys()) {
            if (!orderedGroupNames.contains(groupName))
                orderedGroupNames.add(groupName.toString());
        }

        Vector<Action> result = new Vector<Action>();

        for (Object cGroup : orderedGroupNames) {
            Map<String, MackAction> actions = actionMap.getAll(cGroup);

            if (actions.size() == 0)
                continue;

            // Add a separator always but before the first group.
            if (firstSeparator == false)
                firstSeparator = true;
            else
                result.add(null);

            for (Action cAction : _sorter.sortCategory(actions.values()))
                result.add(cAction);
        }

        return result;
    }

    /**
     * Creates a popup menu from the registered actions. The menu
     * contains the enabled actions from the different action
     * groups separated by separator bars.
     *
     * @return A newly created popup menu.
     */
    public JPopupMenu getPopup()
    {
        JPopupMenu result = new JPopupMenu();

        // Add only enabled actions and ensure that
        // no duplicate separators are added.
        boolean separatorAdded = false;
        for (Action c : getActionBar(_popupMap))
        {
            if ( c == null && !separatorAdded )
            {
                result.addSeparator();
                separatorAdded = true;
            }
            else if ( c.isEnabled() )
            {
                result.add(c);
                separatorAdded = false;
            }
        }

        return result;
    }

    /**
     * Used for swingX menu headers
     */
    @SuppressWarnings("serial")
    private static class DummyAction
        extends AbstractActionExt
    {
        public DummyAction(
            String name,
            org.jdesktop.swingx.action.ActionManager am)
        {
            setActionCommand(name.toString());

            // Localize the menu name.
            String itemName =
                Application.getResourceManager().getResourceMap(getClass()).getString(name.toString());
            if (!StringUtils.hasContent(itemName))
                itemName = name.toString();

            setName( itemName );

            am.addAction( this );
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        @Override
        public void actionPerformed(ActionEvent e)
        {
            throw new InternalError();
        }
    }
}
