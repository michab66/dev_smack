/* $Id$
 *
 * Michael's Application Construction Kit (MACK)
 *
 * Released under Gnu Public License
 * Copyright © 2003-2004 Michael G. Binz
 */
package org.jdesktop.smack;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;




/**
 * A set of utility classes.  Basically I hate that, but some things <i>are</i>
 * just functions.
 * @deprecated Do not use.
 */
@Deprecated
public final class Utilities
{
    private final static Logger _log =
        Logger.getLogger( Utilities.class.getName() );



    /**
     * No construction allowed.
     */
    private Utilities()
    {
        throw new AssertionError();
    }



    /**
     * Sets the System Look and Feel for the calling application.
     *
     * @return {@code true} if the Look and Feel could be set successfully.
     */
    public static boolean setSystemLookAndFeel()
    {
        try
        {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName() );

            return true;
        }
        catch ( Exception e )
        {
            _log.log(
                Level.WARNING,
                "Couldn't set system l&f.",
                e );
        }

        return false;
    }



  /**
   * This is equivalent to the JOptionPane.showOptionDialog() call with the
   * same parameters, but solves a problem in jdk 1.4 with the passed
   * <code>initialValue</code> argument.  This is not properly handled in the
   * JDK.<p>
   * The code below is from the version
   * <code>@(#)JOptionPane.java 1.81 03/01/23</code> with a slight
   * modification.  Instead of trying to set the initial value nothing is done
   * which results in the same behavior as 1.3.  Setting the initial value is
   * not possible how it is done in the original code, would require a major
   * rework.
   *
   * @deprecated  This is not cool, the description above wrong.  Do not use.
   */
  @Deprecated
public static int showOptionDialog(
    java.awt.Component parentComponent,
    Object message,
    String title,
    int optionType,
    int messageType,
    Icon icon,
    Object[] options,
    Object initialValue )
  {
    final JOptionPane pane = new JOptionPane(message, messageType,
                                                   optionType, icon,
                                                   options, initialValue);

    pane.setInitialValue(initialValue);
    pane.setComponentOrientation(((parentComponent == null) ?
        JOptionPane.getRootFrame() : parentComponent).getComponentOrientation());

    JDialog dialog = pane.createDialog(parentComponent, title );

    dialog.show();
    dialog.dispose();

    Object        selectedValue = pane.getValue();

    if(selectedValue == null)
        return JOptionPane.CLOSED_OPTION;
    if(options == null) {
        if(selectedValue instanceof Integer)
            return ((Integer)selectedValue).intValue();
        return JOptionPane.CLOSED_OPTION;
    }
    for(int counter = 0, maxCounter = options.length;
        counter < maxCounter; counter++) {
        if(options[counter].equals(selectedValue))
            return counter;
    }
    return JOptionPane.CLOSED_OPTION;
  }



  /**
   * Dynamically sets the mnemonics on the JLabel instances in the passed set
   * of JComponents.
   *
   * @param comps The set of components to process.
   */
  public static void computeMnemonicsFor( JComponent[] comps )
  {
    Hashtable<Character, JComponent> mnemonicsMap =
        new Hashtable<Character, JComponent>();

    for ( int i = 0 ; i < comps.length ; i++ )
    {
      // Check if the component is able to accept a mnemonic...
      String text = lovesMnemonics( comps[i] );
      // ...and skip if not.
      if ( text == null )
        continue;

      // Now check the returned string character by character whether it has
      // been used yet as a mnemonic...
      for ( int j = 0 ; j < text.length() ; j++ )
      {
        Character c = new Character( text.charAt( j ) );
        if ( null == mnemonicsMap.get( c ) )
        {
          // ...and if it hasn't mark it as used...
          mnemonicsMap.put( c, comps[i] );
          // ...and set it on the component.
          eatMnemonic( comps[i], c.charValue() );
          break;
        }
      }
    }

    // TODO This algorithm has a small flaw:  If component 1 carries the text
    // "ab" and component 2 the text "a", then there will be no mnemonic set on
    // component 2, since the character a has been used as a mnemonic for
    // component 1 and there's no alternative.  This could be used through a
    // backtracking based algorithm, but currently I think that this is 'too
    // much of the good' as we ol' Germans say.
  }



  /**
   * Checks whether the passed component knows the mnemonic concept.  Make
   * polymorphic what <i>is</i> not polymorphic.
   *
   * @param comp The component to check.
   * @return A non-null text string if the component knows mnemonics, else
   *         <code>null</code>.
   */
  private static String lovesMnemonics( JComponent comp )
  {
    String result = null;

    if ( comp instanceof JButton )
      result = ((JButton)comp).getText();
    else if ( comp instanceof JLabel )
      result = ((JLabel)comp).getText();

    if ( result != null && result.length() == 0 )
      result = null;

    return result;
  }



  /**
   * Sets the mnemonic on the passed component.  Make polymorphic what is not
   * polymorphic.  A precondition for calling this method is a valid result
   * from <code>lovesMnemonics()</code>.
   *
   * @param comp The component we set the mnemonic on.
   * @param mnemonic The mnemonic character to set.
   * @throws InternalError If the component does not accept a mnemonic.
   * @see Utilities#lovesMnemonics( JComponent )
   */
  private static void eatMnemonic( JComponent comp, char mnemonic )
  {
    if ( comp instanceof JButton )
      ((JButton)comp).setMnemonic( mnemonic );
    else if ( comp instanceof JLabel )
      ((JLabel)comp).setDisplayedMnemonic( mnemonic );
    else
      throw new InternalError( "eatMnemonic not applicable." );
  }
}
