/* $Id$
 *
 * MACK
 *
 * Released under Gnu Public License
 * Copyright © 2003-2010 Michael G. Binz
 */
package org.jdesktop.smack.actions;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.smack.MackApplication;




/**
 * <p>
 * An about box. The action id is <code>appAbout</code>. Allows to display a
 * message. For debug support it also supports a display
 * of the Java system properties.
 * </p>
 *
 * @version $Revision$
 * @author Michael G. Binz
 */
public final class AppAbout
    extends
      MackApplicationAction
{
    private static final long serialVersionUID = 3366361594001680742L;

    /**
     * The text shown in the about box. Can contain format placeholders for
     * the application name, version and the current year
     * respectively. Referred to by the resource key "message". The initial
     * value is the default text that will be replaced by text from the resource
     * bundle.
     */
    private final String _aboutText;

    /**
     * Create an about action.
     */
    public AppAbout()
    {
        super( AppActionKey.appAbout );
        setEnabled( true );

        Application a = Application.getInstance();

        setName(
            String.format(
                getName(),
                a.getTitle() ) );

        ResourceMap rm = getResourceMap();

        _aboutText = rm.getString(
            makeResourceKeyName( "message" ),
            a.getTitle(),
            a.getVersion(),
            new Date(),
            a.getVendor() );
    }

    @Resource
    private String _aboutTabTitle;
    @Resource
    private String _infoTabTitle;

    /**
     * Brings up the actual about dialog.
     *
     * @param ae The event that triggered execution of this action.
     */
    public void actionPerformed( ActionEvent ae )
    {
        // Create the actual about box text.
        JComponent optionPaneComponent = new JLabel( _aboutText );
        optionPaneComponent.setBorder(
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

        if ( accessToSystemPropertiesAllowed() )
        {
            // Second tab, displaying the system properties table.
            JComponent tabTwo = new JScrollPane(
                new JTable(
                    new SystemPropertiesTable() ) );

            Dimension tableSize =
                new JTable( 16, 2 ).getPreferredSize();

            tabTwo.setPreferredSize( tableSize );

            JTabbedPane tabbedPane =
                new JTabbedPane();
            tabbedPane.addTab( _aboutTabTitle, optionPaneComponent );
            tabbedPane.addTab( _infoTabTitle, tabTwo );

            optionPaneComponent = tabbedPane;
        }

        MackApplication<?> self =
            Application.getInstance(MackApplication.class);

        Component dlgParent =
            self.getMainView().getFrame();

        JOptionPane.showMessageDialog(
            dlgParent,
            optionPaneComponent,
            self.getTitle(),
            JOptionPane.INFORMATION_MESSAGE,
            self.getIcon() );
    }

    /**
     * Checks whether we are allowed to access the system properties.
     *
     * @return <code>true</code> if system properties access is allowed,
     *         <code>false</code> otherwise.
     */
    private boolean accessToSystemPropertiesAllowed()
    {
        try
        {
            SecurityManager sm = System.getSecurityManager();

            if ( sm != null )
                sm.checkPropertiesAccess();
        }
        catch ( SecurityException e )
        {
            return false;
        }
        return true;
    }
}
