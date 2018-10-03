/* $Id$
 *
 * http://sourceforge.net/projects/smackfw/
 *
 * Copyright © 2005-2012 Michael G. Binz
 */
package org.jdesktop.swingx;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * Tools.
 *
 * @version $Revision$
 * @deprecated Use {@link JXTools}
 */
@Deprecated
public class GTools
{
    /**
     * Icon for information messages. This is the default icon.
     */
    public static final Icon ICON_INFO = UIManager.getIcon( "OptionPane.informationIcon" );

    /**
     * Icon for warning messages.
     */
    public static final Icon ICON_WARN = UIManager.getIcon("OptionPane.warningIcon");

    /**
     * Icon for error messages.
     */
    public static final Icon ICON_ERROR = UIManager.getIcon("OptionPane.errorIcon");

    /**
     * Icon for information messages. This is the default icon.
     */
    public static final Icon ICON_QUESTION = UIManager.getIcon( "OptionPane.questionIcon" );

    /**
     * @param c
     *            the component to add the double click action
     * @param a
     *            the action to add
     */
    static void registerDoubleClick(Component c, final Action a) {
        c.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                if (ev.getClickCount() == 2
                        && SwingUtilities.isLeftMouseButton(ev)
                        && a.isEnabled()) {
                    a.actionPerformed(new ActionEvent(ev.getSource(),
                            ActionEvent.ACTION_PERFORMED, (String) a
                                    .getValue(Action.ACTION_COMMAND_KEY)));
                }
            }
        });
    }

    /**
     * Shows a modal dialog for asking the user about the given message.
     *
     * Provides YES and NO options. The dialog has the title "Confirm".
     *
     * @param pParent
     *            the parent component relative to which the displayed dialog
     *            will be modal.
     * @param pMessage
     *            the message to be displayed in the dialog.
     * @return whether the user confirmed with YES.
     */
    public static boolean confirm(Component pParent, String pMessage)
    {
        int option = JOptionPane.showConfirmDialog(
                    pParent,
                    pMessage,
                    getText( GTools.class, "confirmDialogTitle" ),
                    JOptionPane.YES_NO_OPTION);
        return JOptionPane.YES_OPTION == option;
    }

    /**
     * Registers the given action to be fired when the ESC key is pressed in
     * the given dialog.
     *
     * @param pDialog
     * @param pEscAction
     */
    static void registerEsc(
            final JDialog pDialog,
            final Action pEscAction)
    {
        JLayeredPane layeredPane = pDialog.getLayeredPane();
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-it");
        layeredPane.getActionMap().put("close-it", pEscAction);
    }

    /**
     * Registers the given action to be fired when the given window is closed.
     *
     * @param pDialog
     *            the dialog on which closing to listen.
     * @param pCancelAction
     *            the action to fire when the dialog closes. This is usualiy the
     *            same action as in the {@link #registerEsc(JDialog, Action)}.
     */
    static void registerWindowClosing(
            final JDialog pDialog,
            final Action pCancelAction)
    {
        pDialog.addWindowListener(
                new WindowAdapter()
                {
                    @Override
                    public void windowClosing(WindowEvent pE)
                    {
                        pCancelAction.actionPerformed( new ActionEvent(
                                pE.getSource(),
                                pE.getID(),
                                null ) );
                    }
                } );
    }

    /**
     * Registers the given action to be fired when the Enter key is pressed in
     * the given dialog.
     *
     * @param dlg
     * @param pEnterAction
     */
    public static void registerEnter(final JDialog dlg, Action pEnterAction) {
        JLayeredPane layeredPane = dlg.getLayeredPane();
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "doenter-it");
        layeredPane.getActionMap().put("doenter-it", pEnterAction);
    }

    /**
     * Adds the key binding to the given action.
     *
     * @param pComp
     *            the component on which to register the key stroke.
     * @param pKeyStroke
     *            e.g. KeyStroke.getKeyStroke( KeyEvent.VK_ENTER,
     *            InputEvent.ALT_DOWN_MASK )
     * @param pCondition
     *            e.g. {@link JComponent#WHEN_FOCUSED}
     * @param pAction
     *            the action to execute when the key stroke is entered.
     */
    public static void addKeyBinding(
            JComponent pComp,
            KeyStroke pKeyStroke,
            int pCondition,
            Action pAction)
    {
        Object key = pAction.getValue(Action.ACTION_COMMAND_KEY);
        if (key == null) {
            key = pAction.getValue(Action.NAME);
        }
        if (key == null) {
            key = "Action" + new Random().nextInt();
        }
        pComp.getInputMap( pCondition ).put( pKeyStroke, key );
        pComp.getActionMap().put( key, pAction );
        pAction.putValue(Action.ACCELERATOR_KEY, pKeyStroke );
    }

    /**
     * Returns the application's main frame.
     *
     * @return The application's main frame.
     */
    public static JFrame getFrame() {
        return Application.getInstance( SingleFrameApplication.class ).getMainFrame();
    }

    /**
     * Returns the localized text for the given key and class.
     *
     * @param pClass
     *            the class which properties to use as resource.
     * @param pKey
     *            the resource key.
     * @param pArgs
     *            optional arguments for the message. If this array is not null
     *            or empty then the arguments in it will be replaced in the
     *            localised resource text as by the method
     *            {@link String#format(String, Object...)}.
     */
    private static String getText(Class<?> pClass, String pKey, Object... pArgs) {
        if (pArgs == null)
            pArgs = new Object[0];

        return Application.getResourceManager().getResourceMap(pClass).getString( pKey, pArgs );
    }

    /**
     * Returns the localized text for the given key and class.
     */
    public static String getText(Class<?> pClass, String pKey) {
        return getText( pClass, pKey, new Object[0] );
    }

    /**
     * Looks up the action for the given name in the action map associated
     * with the passed object.
     *
     * @param pObj The object that carries the action.
     * @param pName The name of the action.
     */
    public static Action getAction(Object pObj, String pName) {
        return getAction( pObj.getClass(), pObj, pName );
    }

    /**
     * Looks up the action for the given name in the action map associated
     * with the passed object.
     *
     * @param pClass The target class.
     * @param pObject The target instance.
     * @return The Action reference.
     * @throws IllegalArgumentException If the Action was not found.
     */
    private static Action getAction(
            Class<?> pClass,
            Object pObject,
            String pName)
    {
        Action a = Application.getInstance().getContext().
                    getActionMap(pClass, pObject).get(pName);

        if ( a == null )
        {
            throw new IllegalArgumentException( String.format(
                "Action '%s' not found on class '%s'.",
                    pName,
                    pClass.getName() ) );
        }

        if (a.getValue( Action.ACTION_COMMAND_KEY ) == null)
            a.putValue( Action.ACTION_COMMAND_KEY, pName );
        return a;
    }

    /**
     * Recursively localizes the given component and all its child components
     * using the resource map of the component's class.
     *
     * @param pComponent
     *            the component to localize.
     * @see ResourceMap#injectComponents(Component)
     */
//    public static void injectComponents(Component pComponent)
//    {
//        injectComponents(pComponent.getClass(), pComponent);
//    }

    /**
     * Recursively localizes the given component using the resource map of the
     * given class.
     *
     * @param pResourceClass
     *            the resource map of the class will be used for the
     *            localization.
     * @param pComponent
     *            the component to localize.
     * @see ResourceMap#injectComponents(Component)
     */
//    private static void injectComponents(
//        Class<?> pResourceClass,
//        Component pComponent)
//    {
//        getResourceMap(pResourceClass).injectComponents(pComponent);
//    }

    /**
     * The standard gap between the components.
     */
    static final int GAP = 10;

    /**
     * Empty border with GAP space on all sides.
     */
    static final Border GAP_BORDER = BorderFactory.createEmptyBorder(
            GAP, GAP, GAP, GAP);

//    /**
//     * Returns the resource map for the given class.
//     *
//     * @param pClass The target class.
//     * @return The associated ResourceMap
//     * @deprecated Use {@link AppHelper#getResourceMap(Application, Class)}
//     */
//    @Deprecated
//    private static ResourceMap getResourceMap(Class<?> pClass)
//    {
//        return Application.getResourceManager().getResourceMap(pClass);
//    }

    /**
     * This is a library and cannot be instantiated.
     */
    private GTools()
    {
        throw new AssertionError();
    }
}
