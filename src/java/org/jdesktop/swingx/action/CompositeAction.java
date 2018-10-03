/*
 * $Id$
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.action;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.jdesktop.util.ServiceManager;

/**
 * A class that represents an action which will fire a sequence of actions.
 * The actions are added to the internal list. When this action is invoked,
 * the event will be dispatched to the actions in the internal list.
 *
 * @version $Rev$
 * @author Mark Davidson
 */
@SuppressWarnings("serial")
public class CompositeAction extends AbstractActionExt {

    /**
     * The composite actions.
     */
    List<Action> _actions = new ArrayList<Action>();

    /**
     * Create an instance with a default name.
     */
    public CompositeAction() {
        this("CompositeAction");
    }

    /**
     * Create an instance.
     *
     * @param name display name of the action
     */
    public CompositeAction(String name) {
        super(name);
    }

    /**
     * Create an instance.
     *
     * @param name display name of the action
     * @param command the value of the action command key
     */
    public CompositeAction(String name, String command) {
        super(name, command);
    }

    /**
     * Create an instance.
     *
     * @param name display name of the action
     * @param icon icon to display
     */
    public CompositeAction(String name, Icon icon) {
        super(name, icon);
    }

    /**
     * Create an instance.
     *
     * @param name display name of the action
     * @param command the value of the action command key
     * @param icon icon to display
     */
    public CompositeAction(String name, String command, Icon icon) {
        super(name, command, icon);
    }

    /**
     * Add an action id to the action list. This action will be invoked
     * when this composite action is invoked.
     * The id must refer to an action in the application's
     * {@link ActionManager}.
     *
     * @throws IllegalArgumentException If the id is not found in the
     * ActionManager.
     */
    public void addAction(String id) {
        ActionManager manager =
                ServiceManager.getApplicationService( ActionManager.class );

        Action action = manager.getAction(id);

        if ( action == null )
            throw new IllegalArgumentException( id );

        addAction( action );
    }

    /**
     * Add an action id to the action list. This action will be invoked
     * when this composite action is invoked.
     */
    public void addAction( Action action ) {
        if ( _actions.contains( action ) )
            throw new IllegalArgumentException(
                    "Duplicate action: " +
                    AbstractActionExt.getActionCommand( action ) );

        _actions.add( action );
    }

    /**
     * Returns a list of action ids which indicates that this is a composite
     * action.
     * @return a valid list of action ids or null
     */
    public List<String> getActionIDs() {
        List<String> result = new ArrayList<String>();

        for ( Action c : _actions )
            result.add( AbstractActionExt.getActionCommand( c ) );

        return result;
    }

    /**
     * Get the list of the target actions.
     *
     * @return The list of target actions.
     */
    public List<Action> getActions()
    {
        return Collections.unmodifiableList( _actions );
    }

    /**
     * Callback for composite actions. This method will redispatch the
     * ActionEvent to all the actions held in the list.
     */
    @Override
    public void actionPerformed(ActionEvent evt) {

        for ( Action c : _actions )
        {
            if ( c.isEnabled() )
                c.actionPerformed( evt );
        }
    }

    /**
     * Callback for toggle actions.
     */
    @Override
    public void itemStateChanged(ItemEvent evt) {

        for ( Action c : _actions ) {
            if ( c instanceof AbstractActionExt && c.isEnabled() )
            {
                ((AbstractActionExt)c).itemStateChanged(evt);
            }
        }
    }
}
