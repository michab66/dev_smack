/* $Id$
 *
 * Mack
 *
 * Released under Gnu Public License
 * Copyright © 2010 Michael G. Binz
 */
package org.jdesktop.smack;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComponent;

import org.jdesktop.application.Application;

/**
 * A component that offers typed model access.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public abstract class MackComponent<MT>
    extends JComponent
{
    private static final long serialVersionUID = -3353625264373850389L;


    /**
     * This component's model.
     */
    private MT _model;



    /**
     * Create an instance.
     */
    public MackComponent()
    {
    }


    /**
     * Create an instance using the passed model.
     *
     * @param model The model to use.
     */
    public MackComponent( MT model )
    {
        _model = model;
    }



    /**
     * Get the component's model.
     *
     * @return The component's model.
     */
    public MT getModel()
    {
        return _model;
    }

    /**
     * Set the component's model.
     *
     * @param model The new model.
     */
    public void setModel( MT model )
    {
        _model = model;
    }

    /**
     * Get a named action resolved from the components Application action map.
     *
     * @param name The named action.
     * @return The named action or null.
     */
    final protected Action getComponentAction( String name )
    {
        ActionMap am =
            Application.getInstance().getContext().getActionMap( this );
        return am.get( name );
    }
}
