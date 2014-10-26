/* $Id$
 *
 * Mack II
 *
 * Released under Gnu Public License
 * Copyright © 2002-2009 Michael G. Binz
 */
package de.michab.mack.actions;

import java.awt.event.ActionEvent;
import org.bsaf192.application.Application;



/**
 * The MACK standard application exit action.
 *
 * @version $Rev$
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class AppExit
    extends
        MackApplicationAction
{
    /**
     * Creates an instance.
     */
    public AppExit()
    {
        super( AppActionKey.appExit );
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {
        Application.getInstance().exit( e );
    }
}
