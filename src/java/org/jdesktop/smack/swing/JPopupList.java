/* $Id$
 *
 * UI general
 *
 * Released under Gnu Public License
 * Copyright © 2002-2011 Michael G. Binz
 */
package org.jdesktop.smack.swing;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;



/**
 * <p>A <code>JPopupList</code> is very similar to a <code>JPopupMenu</code>,
 * instead of menu elements this popup component contains a <code>JList</code>
 * that is used to find a selection.</p>
 * <p>For getting selection notification an <code>ActionListener</code> can be
 * registered with the component that is called when the user selected a value.
 * The <code>ActionEvent</code> holds a reference to the embedded
 * <code>JList</code>, the selected index, and the selected value in
 * stringified representation in its respective <code>source</code>,
 * <code>id</code>, and <code>command</code> arguments.</p>
 *
 * @version $Rev$
 * @author Michael G. Binz
 */
@SuppressWarnings("serial")
public final class JPopupList
  extends
    JPopupMenu
{
  /**
   * This component's <code>JList</code>.
   */
  private final JList _list;



  /**
   * The scrollpane controlling this component's <code>JList</code>.
   */
  private final JScrollPane _scrollPane;



  /**
   * The listener that gets called in case the user makes a selection.
   */
  private ActionListener _listener = null;



  /**
   * Creates a <code>JPopupList</code> with the passed choices.
   *
   * @param choices The elements to display in the list.
   */
  public JPopupList( String[] choices )
  {
    _list = new JList( choices );
    // If we have the focus we never release it.
    _list.setNextFocusableComponent( _list );

    _scrollPane = new JScrollPane( _list );

    _list.addKeyListener( _keyListener );
    _list.addMouseListener( _mouseListener );

    // Select the first list item.
    _list.setSelectedIndex( 0 );

    add( _scrollPane );
    pack();
  }



  /**
   * Add the <code>ActionListener</code> that gets notified in case of a user
   * selection.
   *
   * @param a The listener to add.
   */
  public void addActionListener( ActionListener a )
  {
    // Currently only a single listener is supported.
    _listener = a;
  }



  /**
   * Shows the popup list.  The position for the actual popup is tried to be
   * kept inside the invoker's boundaries.  So either the top left position of
   * the popup is equal to the top left position of the passed rectangle or the
   * bottom left position of the popup is equal to the bottom left position of
   * the passed rectangle.
   *
   * @param invoker The component the popup is placed on.
   * @param r The position for the popup to display.
   */
  public void show( Component invoker, Rectangle r )
  {
    // TODO implement the wonderful algorithm described in the comment.
    show( invoker, r.x, r.y );
  }



  /**
   * This is a direct replacement for the <code>JPopupMenu</code> method.
   *
   * @param invoker The component this should display on.
   * @param x The x position for this.
   * @param y The y position for this.
   */
  public void show( Component invoker, int x, int y )
  {
    // Ensure that the list uses the same font as the owner component.
    _list.setFont( invoker.getFont() );

    // Show the popup.
    super.show( invoker, x ,y );

    Window sw = SwingUtilities.getWindowAncestor( this );
    Window cw = SwingUtilities.getWindowAncestor( invoker );

    // The following code block is needed only on jdk 1.4.  It allows the
    // JPopupList's window to take the focus.  This is only needed in case the
    // JPopupList is heavyweight but does no harm otherwise.
    sw.setFocusableWindowState( true );

    // If the popup is heavyweight--has a different peer window than the
    // owner--Swing does not activate the popup's window.  This in turn results
    // in problems getting the focus into the popup.
    // So we check if owner and popup are in different windows and in case
    // they are, we do a toFront() on the popup, which activates it.
    // No way to actually say 'activate'.  And don't ask me what
    // Component.isLightweight computes, at least the result is *not* what we
    // expect here.
    if ( cw != sw )
    {
      sw.toFront();
    }

    // Set the focus on the list.
    _list.requestFocus();
  }



  /**
   * Forwards the focus request to the embedded <code>JList</code>.
   */
  public void requestFocus()
  {
    _list.requestFocus();
  }



  /**
   * The mouse handler used by this component.
   */
  private final MouseListener _mouseListener = new MouseAdapter()
  {
    /**
     * Implements the <code>MouseListener</code> interface.
     *
     * @param me A mouse event.
     */
    public void mouseClicked( MouseEvent me )
    {
      if ( SwingUtilities.isLeftMouseButton( me ) &&
           me.getClickCount() == 2 )
      {
        accept();
      }
    }
  };



  /**
   * The key handler used by this component.
   */
  private final KeyListener _keyListener = new KeyAdapter()
  {
    /**
     * Implements the <code>KeyListener</code> interface.
     *
     * @param e A key event.
     */
    public void keyTyped(KeyEvent e)
    {
      if ( e.getKeyChar() == KeyEvent.VK_ENTER )
        accept();
      else if ( e.getKeyChar() == KeyEvent.VK_ESCAPE )
        cancel();
    }
  };



  /**
   * Handle a user selection.  Fires the action listener.  The fired action
   * event's source refers to the embedded <code>JList</code> instance.  The
   * action event's integer <code>id</code> holds the lists's selected index
   * and finally the action event's command holds the stringified selected
   * value from the <code>JList</code>.
   */
  private void accept()
  {
    // Send out the accept notification...
    if ( _listener != null )
    {
      _listener.actionPerformed(
        new ActionEvent( _list,
                         _list.getSelectedIndex(),
                         _list.getSelectedValue().toString() ) );
    }
    // ...and cancel the dialog.
    cancel();
  }



  /**
   * Pops down the <code>JPopupList</code>.  Equivalent to a call to
   * <code>setVisible( false )</code>.  Additionally,
   * <code>cancel()</code> places the focus to the invoker of the popup that
   * has been passed to the <code>show()</code> method.
   *
   * @see org.jdesktop.smack.swing.JPopupList#show
   */
  public void cancel()
  {
    // Pop ourselves down.
    setVisible( false );
    // Give the focus back to the invoker.
    Component invoker = getInvoker();
    if ( invoker != null )
      invoker.requestFocus();
  }
}
