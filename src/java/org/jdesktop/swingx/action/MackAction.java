/* $Id$
 *
 * Mack
 *
 * Released under Gnu Public License
 * Copyright © 2009-2012 Michael G. Binz
 */
package org.jdesktop.swingx.action;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;
import org.jdesktop.smack.util.StringUtils;




/**
 * A fully resource configurable Action.  The action is keyed by the
 * actionCommand.  An action can be defined to be part of the application
 * toolbar, the menubar or the popup menu.
 *
 * The complete set of MackAction resources is:
 * <pre>
 * Action.icon
 * Action.text
 * Action.shortDescription
 * Action.longDescription
 * Action.smallIcon
 * Action.largeIcon
 * Action.command
 * Action.accelerator
 * Action.mnemonic
 * Action.displayedMnemonicIndex
 *
 * groupSortId
 *
 * TODO michab implement Action.selected
 * </pre>
 * <p/>
 * <p/>
 * A few of the resources are handled specially:
 * <ul>
 * <li><tt>Action.text</tt><br>
 * Used to initialize the Action properties with keys
 * <tt>Action.NAME</tt>, <tt>Action.MNEMONIC_KEY</tt> and
 * <tt>Action.DISPLAYED_MNEMONIC_INDEX</tt>.
 * If the resources's value contains an "&" or an "_" it's
 * assumed to mark the following character as the mnemonic.
 * If Action.mnemonic/Action.displayedMnemonic resources are
 * also defined (an odd case), they'll override the mnemonic
 * specified with the Action.text marker character.
 * <p/>
 * <li><tt>Action.icon</tt><br>
 * Used to initialize both ACTION.SMALL_ICON,LARGE_ICON.  If
 * Action.smallIcon or Action.largeIcon resources are also defined
 * they'll override the value defined for Action.icon.
 * <p/>
 * <li><tt>Action.displayedMnemonicIndexKey</tt><br>
 * The corresponding javax.swing.Action constant is only defined in Java SE 6.
 * We'll set the Action property in Java SE 5 too.
 * </ul>
 *
 * @version $Rev$
 * @author Michael Binz
 */
public abstract class MackAction extends AbstractActionExt
{
    private static final long serialVersionUID = -2511845641813776124L;


    private final ResourceMap _resourceMap;

    /**
     * Create an instance using the simple classname as key.
     */
    protected MackAction()
    {
        this( null );
    }
    /**
     * Create a mack action with the given key.  This is to be used
     * when manually implementing action classes.
     *
     * @param key The unique action key.
     */
    protected MackAction( String key )
    {
        if ( ! StringUtils.hasContent( key, true ) )
            key = getClass().getSimpleName();

        _resourceMap = resourceMap( getClass() );
        init( key, _resourceMap );

        // Place us into the application action map.
        Application.getInstance().getContext().getActionMap().put( key, this );
    }


    /**
     * The constructor used by the action annotation.
     *
     * @param key
     * @param resourceMap
     */
    protected MackAction( String key, ResourceMap resourceMap )
    {
        if ( ! StringUtils.hasContent( key, true ) )
            throw new IllegalArgumentException( "key == null" );

        _resourceMap = resourceMap;
        init( key, _resourceMap );
    }

    /**
     * Initializes the action.
     *
     * @param key The action key.
     */
    private void init( String key, ResourceMap rm )
    {
        Application.getResourceManager().injectResources( this );

        setActionCommand( key );

        // Mark the action as MackAction.
        putValue( "" + serialVersionUID, Boolean.TRUE );

        initActionProperties( rm );
    }



    /**
     * Get this Action's key.
     *
     * @return This Action's key.
     */
    public String getKey()
    {
        return getActionCommand();
    }

    /**
     * Get the Action's user displayable text.
     * <p>Note that this is in raw Swing called the Action's
     * name, which is pretty confusing.</p>
     *
     * @return The Action's user display text.
     * @see #setText(String)
     */
    @Override
    public String getText()
    {
        return getName();
    }

    /**
     * Set the action's user displayable text.
     * <p>Note that this is in raw Swing called the Action's
     * name, which is pretty confusing.</p>
     *
     * @param text The new visible action text.
     * @see MackAction#getText()
     */
    @Override
    public void setText( String text )
    {
        setName( text );
    }

    private final static String CATEGORY =
        "MackCategory";
    private final static String CATEGORY_SORT_ID =
        "MackCategorySortId";
    private final static String TOOLBAR_FLAG =
        "MackToolbar";
    private final static String MENUBAR_FLAG =
        "MackMenubar";
    private final static String POPUP_FLAG =
        "MackPopup";


    /**
     * Set the action's category.
     */
    private void setCategory( String key )
    {
        putValue( CATEGORY, key );
    }

    /**
     * Get the actions category.  If the category was not set but a group has
     * been set, then the group is returned. If no group was set either, then
     * the empty string is returned.
     *
     * @return The actions's category. Never null.
     */
    public String getCategory()
    {
        Object result = getValue( CATEGORY );

        if ( result == null )
            result = getGroup();

        if ( result == null )
            result = StringUtils.EMPTY_STRING;

        return result.toString();
    }

    /**
     * Action should show up in the toolbar.
     *
     * @param visible True if it is to show up in the toolbar.
     */
    public MackAction setToolbar( boolean visible )
    {
        putValue(
                TOOLBAR_FLAG,
                Boolean.valueOf( visible ) );

        return this;
    }

    /**
     *
     * @return True if the action is placed into the application toolbar.
     */
    public boolean isToolbar()
    {
        Object result =
            getValue( TOOLBAR_FLAG );

        return result != null && result == Boolean.TRUE;
    }



    /**
     * Action should show up in the application popup.
     *
     * @param visible True if it is to show up in the application popup.
     */
    public MackAction setPopup( boolean visible )
    {
        putValue(
                POPUP_FLAG,
                Boolean.valueOf( visible ) );

        return this;
    }



    /**
     *
     * @return True if this action is part of the application popup.
     */
    public boolean isPopup()
    {
        Object result =
            getValue( POPUP_FLAG );

        return result != null && result == Boolean.TRUE;
    }



    /**
     * Check if the passed action is part of the application toolbar.
     *
     * @param action The action to check.
     * @return True if the action is in the application toolbar.
     */
    public static boolean inToolbar( Action action )
    {
        Object result = action.getValue( TOOLBAR_FLAG );

        return
            result != null &&
            result == Boolean.TRUE;
    }



    /**
     * The action should show up in the menu.
     *
     * @param visible True if part of menu.
     * @return The reference to the action (this).
     */
    public MackAction setMenubar( boolean visible )
    {
        putValue(
                MENUBAR_FLAG,
                Boolean.valueOf( visible ) );

        return this;
    }

    /**
     *
     * @return True if the action is part of the application menubar.
     */
    public boolean isMenubar()
    {
        Object result =
            getValue( MENUBAR_FLAG );

        return result != null && result == Boolean.TRUE;
    }

    /**
     * Check if the passed action is in the application menubar.
     *
     * @param action The action to check.
     * @return True if part of the application menubar.
     */
    public static boolean inMenubar( Action action )
    {
        Object result = action.getValue( MENUBAR_FLAG );

        return
            result != null &&
            result == Boolean.TRUE;
    }



    /**
     * Set the sort id.
     * @param categorySortId
     */
    private void setCategorySortId( String categorySortId )
    {
        putValue(
                CATEGORY_SORT_ID,
                categorySortId );
    }



    /**
     * Get the category sort id.
     *
     * @return The category sort id.
     */
    public String getCategorySortId()
    {
        String result =
            (String)getValue( CATEGORY_SORT_ID );

        if ( result == null )
            return StringUtils.EMPTY_STRING;

        return result;
    }



    /**
     * Convert the passed action to a MackAction if it is a
     * MackAction.
     *
     * @param a The Action to convert.
     * @return A reference to a MackAction, or null if the
     * passed action was no MackAction.
     */
    public static MackAction toMackAction( Action a )
    {
        if ( a.getValue( "" + serialVersionUID ) != Boolean.TRUE )
            return null;

        assert a instanceof MackAction;

        return (MackAction)a;
    }



    /**
     * Get an optional component tied to the action.
     *
     * @param a The action to check.
     * @return An attached component or null.
     */
    public static Component getActionComponent( Action a )
    {
        MackAction ma = toMackAction( a );

        if ( ma == null )
            return null;

        return ma.getToolbarComponent();
    }



    /**
     * Creates a <code>JComponent</code> that is to be placed on a toolbar.
     * Override if other components need to be placed on the toolbar. Restrict
     * yourself to <i>simple</i> components, since the real estate on a toolbar
     * is rare.  Note that this operation is used by the MackToolbar.
     *
     * @return A component to be placed on the toolbar.
     */
    public JComponent getToolbarComponent()
    {
      return null;
    }



    /**
     * Generates a name that can be used to lookup a resource entry
     * for this action.  Typically if a name of 'msg' is passed the name
     * is extended to 'ActionName.Action.msg'.
     *
     * @param simpleName The base name.
     * @return The resource key name.
     */
    protected final String makeResourceKeyName( String simpleName )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( getKey() );
        sb.append( ".Action." );
        sb.append( simpleName );
        return sb.toString();
    }



    /**
     * Init all of the javax.swing.Action properties from the passed
     * resources.
     *
     * @param resourceMap The resources to use to initialize the properties.
     */
    private void initActionProperties(
            ResourceMap resourceMap )
    {
        // true if Action's icon/name properties set.
        boolean iconOrNameSpecified = false;

        // Action.text => Action.NAME,MNEMONIC_KEY,DISPLAYED_MNEMONIC_INDEX_KEY
        String text = resourceMap.getString(
                makeResourceKeyName( "text" ) );
        if ( text != null )
        {
            MnemonicText.configure( this, text );
            iconOrNameSpecified = true;
        }
        // Action.mnemonic => Action.MNEMONIC_KEY
        Integer mnemonic = resourceMap.getKeyCode(
                makeResourceKeyName( "mnemonic" ) );
        if ( mnemonic != null )
        {
            setMnemonic( mnemonic );
        }
        // Action.mnemonic => Action.DISPLAYED_MNEMONIC_INDEX_KEY
        Integer index = resourceMap.getInteger(
                makeResourceKeyName( "displayedMnemonicIndex" ) );
        if ( index != null )
            putValue( DISPLAYED_MNEMONIC_INDEX_KEY, index );

        // Action.accelerator => Action.ACCELERATOR_KEY
        KeyStroke key = resourceMap.getKeyStroke(
                makeResourceKeyName( "accelerator" ) );
        if ( key != null )
            setAccelerator( key );

        // Action.icon => Action.SMALL_ICON,LARGE_ICON_KEY
        Icon icon = resourceMap.getIcon(
                makeResourceKeyName( "icon" ) );
        if ( icon != null )
        {
            setSmallIcon( icon );
            setLargeIcon( icon );
            iconOrNameSpecified = true;
        }
        // Action.smallIcon => Action.SMALL_ICON
        Icon smallIcon = resourceMap.getIcon(
                makeResourceKeyName( "smallIcon" ) );
        if ( smallIcon != null )
        {
            setSmallIcon( smallIcon );
            iconOrNameSpecified = true;
        }
        // Action.largeIcon => Action.LARGE_ICON
        Icon largeIcon = resourceMap.getIcon(
                makeResourceKeyName( "largeIcon" ) );
        if ( largeIcon != null )
        {
            setLargeIcon( largeIcon );
            iconOrNameSpecified = true;
        }
        // Action.shortDescription => Action.SHORT_DESCRIPTION
        setShortDescription( resourceMap.getString(
                makeResourceKeyName( "shortDescription" ) ) );
        // Action.longDescription => Action.LONG_DESCRIPTION
        setLongDescription( resourceMap.getString(
                makeResourceKeyName( "longDescription" ) ) );

        // Action.command => Action.ACTION_COMMAND_KEY
        String actionCommand = resourceMap.getString(
                makeResourceKeyName( "command" ) );
        if ( StringUtils.hasContent( actionCommand ) )
            setActionCommand( actionCommand );

        // If no visual was defined for this Action, i.e. no text
        // and no icon, then we default to the action key.
        if ( !iconOrNameSpecified )
            setName( StringUtils.EMPTY_STRING + getKey() );

        // Check if this is a toolbar action.
        Boolean toolbarFlag = resourceMap.getBoolean(
                makeResourceKeyName( "toolbarFlag" ) );
        if ( toolbarFlag != null )
            setToolbar( toolbarFlag.booleanValue() );

        // Check if this is a menu bar action.
        {
            Boolean menuFlag = resourceMap.getBoolean(
                    makeResourceKeyName( "menuFlag" ) );
            if ( menuFlag != null )
                setMenubar( menuFlag.booleanValue() );
        }
        // Check if this is a popup menu action.
        {
            Boolean popupFlag = resourceMap.getBoolean(
                    makeResourceKeyName( "popupFlag" ) );
            if ( popupFlag != null )
                setPopup( popupFlag.booleanValue() );
        }
        // Get the action's group.
        {
            String group = resourceMap.getString(
                    makeResourceKeyName( "group" ) );
            if ( group != null )
                setGroup( group );
        }
        // Get the action's category.
        {
            String category = resourceMap.getString(
                    makeResourceKeyName( "category" ) );
            if ( category != null )
                setCategory( category );
        }
        // Get the category sort id.
        {
            String categorySortId = resourceMap.getString(
                    makeResourceKeyName( "categorySortId" ) );
            if ( categorySortId != null )
                setCategorySortId( categorySortId );
        }
        // Set the selected state.
        {
            String selected = resourceMap.getString(
                    makeResourceKeyName( "selected" ) );
            if ( selected != null )
            {
                setStateAction();
                setSelected( Boolean.TRUE.toString().equals(selected) );
            }
        }
    }



    /**
     *
     * @param task The task to execute.
     */
    protected final void execute( Task<?,?> task )
    {
        Application.getInstance().getContext().getTaskService().execute(
                task );
    }



    /**
     * Get the resource map for the passed class.
     *
     * @param classs The key class.
     * @return The resource map.
     */
    private static ResourceMap resourceMap( Class<?> classs )
    {
        return Application.getResourceManager().getResourceMap(
                classs,
                MackAction.class );
    }
    public final ResourceMap getResourceMap()
    {
        return _resourceMap;
    }



    /**
     * An internal helper class that configures the text and mnemonic
     * properties for instances of AbstractButton, JLabel, and
     * javax.swing.Action.  It's used like this:
     * <pre>
     * MnemonicText.configure(myButton, "Save &As")
     * </pre>
     * The configure method unconditionally sets three properties on the
     * target object:
     * <ul>
     * <li>the label text, "Save As"
     * <li>the mnemonic key code, VK_A
     * <li>the index of the mnemonic character, 5
     * </ul>
     * If the mnemonic marker character isn't present, then the second
     * two properties are cleared to VK_UNDEFINED (0) and -1 respectively.
     * <p/>
     */
    static class MnemonicText {
        private MnemonicText() {
        } // not used

        public static void configure(Object target, String markedText) {
            String text = markedText;
            int mnemonicIndex = -1;
            int mnemonicKey = KeyEvent.VK_UNDEFINED;
            // TBD: mnemonic marker char should be an application resource
            int markerIndex = mnemonicMarkerIndex(markedText, '&');
            if (markerIndex == -1) {
                markerIndex = mnemonicMarkerIndex(markedText, '_');
            }
            if (markerIndex != -1) {
                text = text.substring(0, markerIndex) + text.substring(markerIndex + 1);
                mnemonicIndex = markerIndex;
                CharacterIterator sci = new StringCharacterIterator(markedText, markerIndex);
                mnemonicKey = mnemonicKey(sci.next());
            }
            if (target instanceof javax.swing.Action) {
                configureAction((javax.swing.Action) target, text, mnemonicKey, mnemonicIndex);
            } else if (target instanceof AbstractButton) {
                configureButton((AbstractButton) target, text, mnemonicKey, mnemonicIndex);
            } else if (target instanceof JLabel) {
                configureLabel((JLabel) target, text, mnemonicKey, mnemonicIndex);
            } else {
                throw new IllegalArgumentException("unrecognized target type " + target);
            }
        }

        private static int mnemonicMarkerIndex(String s, char marker) {
            if ((s == null) || (s.length() < 2)) {
                return -1;
            }
            CharacterIterator sci = new StringCharacterIterator(s);
            int i = 0;
            while (i != -1) {
                i = s.indexOf(marker, i);
                if (i != -1) {
                    sci.setIndex(i);
                    char c1 = sci.previous();
                    sci.setIndex(i);
                    char c2 = sci.next();
                    boolean isQuote = (c1 == '\'') && (c2 == '\'');
                    boolean isSpace = Character.isWhitespace(c2);
                    if (!isQuote && !isSpace && (c2 != CharacterIterator.DONE)) {
                        return i;
                    }
                }
                if (i != -1) {
                    i += 1;
                }
            }
            return -1;
        }

        /* A general purpose way to map from a char to a KeyCode is needed.  An
         * AWT RFE has been filed:
         * http://bt2ws.central.sun.com/CrPrint?id=6559449
         * CR 6559449 java/classes_awt Support for converting from char to KeyEvent VK_ keycode
         */
        private static int mnemonicKey(char c) {
            int vk = c;
            if ((vk >= 'a') && (vk <= 'z')) {
                vk -= ('a' - 'A');
            }
            return vk;
        }


        private static void configureAction(javax.swing.Action target, String text, int key, int index) {
            target.putValue(javax.swing.Action.NAME, text);
            if (key != KeyEvent.VK_UNDEFINED) {
                target.putValue(javax.swing.Action.MNEMONIC_KEY, key);
            }
            if (index != -1) {
                target.putValue( Action.DISPLAYED_MNEMONIC_INDEX_KEY, index);
            }
        }

        private static void configureButton(AbstractButton target, String text, int key, int index) {
            target.setText(text);
            if (key != KeyEvent.VK_UNDEFINED) {
                target.setMnemonic(key);
            }
            if (index != -1) {
                target.setDisplayedMnemonicIndex(index);
            }
        }

        private static void configureLabel(JLabel target, String text, int key, int index) {
            target.setText(text);
            if (key != KeyEvent.VK_UNDEFINED) {
                target.setDisplayedMnemonic(key);
            }
            if (index != -1) {
                target.setDisplayedMnemonicIndex(index);
            }
        }
    }

    /**
     * Computes the default parent component for a dialog displayed by this
     * action.  The returned result can always directly passed into dialog
     * creation, even in case it is <code>null</code>.
     *
     * @param event The event that triggered the action.
     * @return Either a reference to a component or <code>null</code> in case no
     *         root component could be found.
     */
    protected final static Component getDialogRoot( java.util.EventObject event )
    {
      Component result;

      try
      {
        result = SwingUtilities.getRoot( (Component)event.getSource() );
      }
      catch ( ClassCastException e )
      {
        // We end up here in case the source was no Component.
        result = null;
      }

//      // Note that getRoot() above can return null without throwing an exception.
//      // As a consequence we need this special condition.
//      if ( result == null )
//         result = Application.getInstance( MackApplication.class ).getMainComponent();

      // We did our best.
      return result;
    }
}
