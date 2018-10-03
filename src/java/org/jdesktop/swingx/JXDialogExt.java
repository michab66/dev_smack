/* $Id$
 *
 * GNU LGPL
 *
 * Copyright � 2011 Michael G. Binz
 */
package org.jdesktop.swingx;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jdesktop.smack.util.StringUtils;

/**
 * Generic modal dialog that can display a user-defined UI component.
 *
 * The component can be specified with the method
 * {@link #setUserComponent(Component)}.
 *
 * One of the OK/Cancel or Close button combinations can be displayed.
 *
 * A message can be optionally placed in the top area of the dialog.
 * <p>
 * A note on setting the size of the dialog: Either use
 * {@link JXDialogExt#setSize(Size)} or set the preferred size on the
 * user component that is installed with {@link #setUserComponent(Component)}.
 * The preferred size has to be set before the component is installed.
 * Do not use explicit calls to {@link #pack()}.
 * </p>
 *
 * @version $Revision$
 * @author Michael Binz
 * @author Tihomir Daskalov
 */
@SuppressWarnings("serial")
public class JXDialogExt extends JDialog
{
    /**
     * The info component is lazy constructed and added to the view.
     */
    private final JXHeader _info =
        new JXHeader(
            StringUtils.EMPTY_STRING,
            StringUtils.EMPTY_STRING,
            GTools.ICON_INFO );

    /**
     * Panel containing the user component and the button box.
     */
    private final JPanel _mainPanel =
        new JPanel(
                new BorderLayout(
                        GTools.GAP,
                        GTools.GAP));

    private final HBox _btnBox = new HBox();

    /**
     * The action fired when the user click on the Close button, ESC key or the
     * window system close button.
     */
    public final Action ACT_CLOSE =
        GTools.getAction(
                this,
                "closeAction" );

    /**
     * The action fired when the user clicks the Cancel button.
     */
    public final Action ACT_CANCEL =
        GTools.getAction(
                this,
                "cancelAction" );

    /**
     * The action fired when the user clicks the OK button or the Enter key.
     */
    public final Action ACT_OK =
        GTools.getAction(
                this,
                "okAction" );

    /**
     * The combination of buttons to show.
     */
    private final Buttons _dialogButtons;

    /**
     * The dialog's size specified by the user.
     */
    private Size _size;

    /**
     * The component receiving the focus on each opening of the dialog.
     */
    private Component _focusComponent;

    /**
     * The current user component of this dialog.
     */
    private Component _userComponent;

    /**
     * Whether the dialog was submitted or canceled.
     */
    private boolean _submitted = false;

    /**
     * Whether the content of the user component has been modified.
     */
    private boolean _modified = false;

    /**
     * The possible combinations of buttons displayed in the dialog.
     */
    public enum Buttons
    {
        /**
         * The dialog will have OK and Cancel buttons.
         */
        OK_CANCEL,
        /**
         * The dialog will have Close button.
         */
        CLOSE
    }

    /**
     * Predefined dialog sizes.
     */
    public enum Size
    {
        SMALL(600, 400),
        MEDIUM(800, 600),
        BIG(1024, 800);

        final int _width;
        final int _height;

        private Size(int pWidth, int pHeight)
        {
            _width = pWidth;
            _height = pHeight;
        }
    }

    /**
     * Creates a dialog that is modal to the given pParent.
     *
     * The title and the user component should be configured later. The info
     * component is not displayed until {@link #setMessage} is invoked.
     *
     * @param pParent
     *            the parent component for this dialog.
     * @param pDialogButtons
     *            the combination of buttons to display.
     */
    public JXDialogExt(
            Component pParent,
            Buttons pDialogButtons)
    {
        super(
            windowForComponent(pParent),
            ModalityType.APPLICATION_MODAL);

        if (pDialogButtons == null)
            throw new IllegalArgumentException("pDialogButtons == null");

        setName( "dialog" );
        setLayout(new BorderLayout());
        _info.setVisible(false);

        add(_info, BorderLayout.PAGE_START);

        _mainPanel.setBorder(GTools.GAP_BORDER);
        _mainPanel.add(_btnBox, BorderLayout.PAGE_END);
        add(_mainPanel, BorderLayout.CENTER);

        _dialogButtons = pDialogButtons;
        showButtons();

        // We take over the closing of the dialog.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        GTools.registerEsc(this, ACT_CLOSE);
        GTools.registerWindowClosing(this, ACT_CLOSE);

        GTools.injectComponents( this );
    }

    /**
     * Get the parent window of the passed component.  Note that
     * {@link JOptionPane#getFrameForComponent(Component)} cannot be used
     * since this operation does not detect the case that the passed component
     * is a java.awt.Dialog and continues the traversal of the parent chain.
     *
     * @param pComponent The component we want to receive the parent component.
     * In case this is {@code null} a default parent is returned.
     * @return The parent window.
     */
    private static Window windowForComponent(Component pComponent)
    {
        if ( pComponent == null )
        {
            if ( GTools.getFrame() != null )
                return GTools.getFrame();

            return JOptionPane.getRootFrame();
        }

        if (pComponent instanceof Window)
            return (Window) pComponent;

        return SwingUtilities.windowForComponent(pComponent);
    }

    /**
     * Sets the given message of the info component. The icon remains unchanged.
     * <p>
     * If the given message is not empty then it will be displayed in the info
     * component. If the info component is not already displayed then it will be
     * displayed.
     * <p>
     * If the given message is null then the info component will be hidden.
     *
     * @param pMessage
     *            the message to display in the info component.
     */
    public void setMessage(String pMessage)
    {
        if ( ! StringUtils.hasContent(pMessage)) {
            _info.setDescription( StringUtils.EMPTY_STRING );
            _info.setVisible( false );
            return;
        }

//        if (pMessage.split("\n").length > 1)
//            _info.setLineCount(3);

        _info.setDescription( pMessage );
        _info.setVisible( true );
    }

    /**
     * Sets the given icon and message of the info component.
     * <p>
     * If the given message is not empty then it will be displayed in the info
     * component. If the info component is not already displayed then it will be
     * displayed.
     * <p>
     * If the given message is null then the info component will be hidden.
     *
     * @param pIcon
     *            the icon to set on the info component.
     * @param pMessage
     *            the message to display in the info component.
     */
    public void setMessage(Icon pIcon, String pMessage)
    {
        _info.setIcon(pIcon);
        setMessage(pMessage);
    }

    private void showButtons(Action... pUserActions)
    {
        _btnBox.removeAll();

        // Add the user actions to the left.
        if (pUserActions != null) {
            for (Action a : pUserActions) {
                _btnBox.addButton(a);
                _btnBox.addGap();
            }
        }

        _btnBox.addGlue();

        // Add the system actions to the right.
        if (_dialogButtons == Buttons.OK_CANCEL) {
            JButton okBtn = _btnBox.addButton(ACT_OK);
            _btnBox.addGap();
            _btnBox.addButton(ACT_CANCEL);
            getRootPane().setDefaultButton(okBtn);
            GTools.registerEnter(this, ACT_OK);
        } else if (_dialogButtons == Buttons.CLOSE) {
            _btnBox.addButton(ACT_CLOSE);
        }
    }

    /**
     * Sets additional user specific actions to this dialog. They
     * will be displayed as buttons on the left side of the button bar.
     *
     * @param pUserActions
     *            the user actions to display. If empty then no user actions
     *            will be displayed.
     */
    public void setUserActions(Action... pUserActions)
    {
        showButtons(pUserActions);
    }

    /**
     * Sets the dialog size. If not set or set to <code>null</code> then the
     * dialog size will be determined by the preferred size of the user
     * component.
     *
     * @param pSize
     */
    public void setSize(Size pSize)
    {
        _size = pSize;
        resize();
    }

    /**
     * Sets the main component to display in the dialog. If a user component was
     * already set, then it will be replaced. The dialog will be resized to fit
     * the preferred size of the given component and will be centered on the
     * screen.
     *
     * @param pComponent
     *            the component to display in the main area of the dialog.
     */
    public void setUserComponent(Component pComponent)
    {
        Component oldUserComponent = _userComponent;
        if (_userComponent != null) {
            _mainPanel.remove(_userComponent);
            _userComponent = null;
        }
        _mainPanel.add(pComponent, BorderLayout.CENTER);
        _userComponent = pComponent;

        // Resize the dialog if this is the first time its user component
        // is set.
        if (oldUserComponent == null)
            resize();
    }

    /**
     * Invoked when the dialog's size must be changed to correspond the current
     * dialog's state.
     */
    private void resize()
    {
        // Fit to the user component.
        pack();

        // Apply the user size if specified.
        if (_size != null)
            setSize(_size._width, _size._height);
    }

    /**
     * Sets the component that will receive the focus on each opening of the
     * dialog. If the given component is <code>null</code> then the focus won't
     * be manipulated.
     *
     * @param pFocusComponent
     */
    public void setFocusComponent(Component pFocusComponent)
    {
        _focusComponent = pFocusComponent;
    }

    /**
     * Invoked when the submit action is fired.
     *
     * The default implementation returns <code>true</code> leading to hiding
     * the dialog.
     *
     * Can be overwritten to validate the input and to veto the submission.
     *
     * @return whether the submit is confirmed. If true then the dialog will be
     *         hidden otherwise will be left open.
     *
     */
    protected boolean onSubmit()
    {
        return true;
    }

    /**
     * Invoked when the Cancel action is fired.
     *
     * The default implementation returns <code>true</code> leading to hiding
     * the dialog.
     *
     * Can be overridden to customize the behavior and to veto the cancel.
     *
     * @return whether the operation is confirmed. If true then the dialog will
     *         be hidden otherwise will be left open.
     */
    protected boolean onCancel()
    {
        return true;
    }

    /**
     * Invoked when the close action is fired - i.e. when the user clicks ESC or
     * the close button or the window system close button.
     *
     * The default implementation checks for modifications. In case of
     * modifications asks confirmation for throwing the changes away. In case of
     * confirmation delegates to the {@link #onCancel()} method.
     *
     * Can be overwritten to customize the behaviour and to veto the closing.
     *
     * @return whether the operation is confirmed. If true then the dialog will
     *         be hidden otherwise will be left open.
     */
    protected boolean onClose()
    {
        if (isModified()) {
            String msg = "msg.confirmThrowChanges";
            if (!GTools.confirm(this, msg))
                return false;
        }
        return onCancel();
    }

    /**
     * Returns whether the content of the user component has been modified.
     *
     * @return {@code true} if the content of the user component has been
     * modified.
     */
    public boolean isModified()
    {
        return _modified;
    }

    /**
     * Sets whether the content of the user component has been modified.
     *
     * @param pModified The new status of the modified flag.
     */
    public void setModified(boolean pModified)
    {
        _modified = pModified;
    }

    /**
     * When the given component is double clicked then the dialog will be
     * submitted in the same way as if the OK button was clicked. Use this if
     * you implement e.g. a select dialog.
     *
     * @param pComponent
     */
    public void submitOnDoubleClickOn(Component pComponent)
    {
        GTools.registerDoubleClick(pComponent, ACT_OK);
    }

    /**
     * Whether the dialog was submitted after the last time it was displayed.
     * This method makes only sense when the dialog is not visible.
     *
     * @return <code>true</code> if the dialog was submitted. <code>false</code>
     *         if the dialog was canceled.
     */
    public boolean isSubmitted()
    {
        return _submitted;
    }

    /**
     * Closes the dialog in the same way as if the Close button was clicked.
     */
    @org.jdesktop.application.Action(name="closeAction")
    public final void close()
    {
        boolean closeConfirmed = onClose();
        if (closeConfirmed) {
            _submitted = false;
            setVisible(false);
        }
    }

    /**
     * Cancels the dialog in the same way as if the Cancel button was clicked.
     */
    @org.jdesktop.application.Action(name="cancelAction")
    public final void cancel()
    {
        boolean cancelConfirmed = onCancel();
        if (cancelConfirmed) {
            _submitted = false;
            setVisible(false);
        }
    }

    /**
     * Submits the dialog in the same way as if the OK button was clicked.
     */
    @org.jdesktop.application.Action(name="okAction")
    public final void ok()
    {
        _submitted = onSubmit();
        if (_submitted)
            setVisible(false);
    }

    @Override
    public void setVisible(boolean pVisible)
    {
        if (pVisible) {
            // Make sure the dialog follows its parent.
            setLocationRelativeTo(getParent());
            // Reset the submitted flag before displaying the dialog.
            _submitted = false;
            _modified = false;
        }

        // Focus on the configured component.
        if (pVisible && _focusComponent != null)
            _focusComponent.requestFocusInWindow();

        super.setVisible(pVisible);
    }
}
