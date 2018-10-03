package org.jdesktop.swingx;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

/**
 * UI component that displays an information string.
 *
 * @author Tihomir Daskalov
 * @version $Revision$
 * @deprecated Use JXHeader instead.
 */
@Deprecated
class InfoComponent extends JPanel
{
    // TODO (daskalot) The org.jdesktop.swingx.JXHeader does the same, but is
    // supposedly better. Consider using it instead of this custom component.

    private static final long serialVersionUID = 1L;

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
     * The label that display the icon.
     */
    private final JLabel _iconLabel;

    /**
     * The text area that displays the message.
     */
    private final JTextArea _message = new JTextArea();

    /**
     * Constructs new info component with empty message, 3 message lines and
     * a message icon that is right aligned.
     */
    public InfoComponent() {
        this(3, null);
    }

    /**
     * Constructs new info component with the info icon and the given message.
     * The message will have 3 lines.
     *
     * @param pMessage
     *            The message to display in the component.
     */
    public InfoComponent(
        String pMessage)
    {
        this();
        setInfoMessage(pMessage);
    }

    /**
     * Constructs new info component with the given icon and message.
     * The message will have 3 lines.
     *
     * @param pIcon
     *            The message icon to display in the component.
     * @param pMessage
     *            The message to display in the component.
     */
    public InfoComponent(
        Icon pIcon,
        String pMessage)
    {
        this();
        setIcon(pIcon);
        setMessage(pMessage);
    }

    /**
     * Constructs new info component with the given message and the given number
     * of initial lines. The info icon will be used.
     *
     * @param pMessageLineCount
     *            The number of lines to display in the component.
     * @param pInitialMessage
     *            The initial message to display in the component.
     */
    public InfoComponent(
        int pMessageLineCount,
        String pInitialMessage)
    {
        // TODO (daskalot) Remove the pIconAlignment => always left.

        this.setLayout(new BorderLayout(10, 10));
        this.setBorder( BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black),
                BorderFactory.createEmptyBorder(10, 10, 10, 10) ) );

        // new Color( 200, 221, 242 )
        this.setBackground( Color.WHITE  );
        _iconLabel = new JLabel();

        _message.setFont(_iconLabel.getFont());
        setLineCount(pMessageLineCount);
        JScrollPane scroller = new JScrollPane( _message );
        scroller.setBorder( BorderFactory.createEmptyBorder() );

        _message.setText( "Messages getInstance() getText(Messages INFOCOMPONENT_MSG_HOWTODEFAULT)" );
        _message.setLineWrap(true);
        _message.setEditable(false);

        this.add(scroller, BorderLayout.CENTER);
        this.add(_iconLabel, BorderLayout.WEST);

        setInfoMessage(pInitialMessage);
    }

    /**
     * Sets the number of lines displayed by the info component. This affects
     * the height of the component.
     *
     * @param pLineCount
     */
    public void setLineCount(int pLineCount)
    {
        _message.setRows(pLineCount);
    }

    /**
     * Shows the given message in this info component. The icon remains
     * unchanged.
     *
     * @param pMessage
     */
    public void setMessage(String pMessage) {
        if (pMessage == null) {
            pMessage = "";
        }
        _message.setText(pMessage);
        _message.setCaretPosition(0);
    }

    /**
     * Sets the given icon to the info message.
     *
     * @param pIcon
     */
    public void setIcon(Icon pIcon) {
        _iconLabel.setIcon(pIcon);
    }

    /**
     * Shows the given message and set the info icon.
     *
     * @param pMessage
     */
    public void setInfoMessage(String pMessage)
    {
        setIcon(ICON_INFO);
        setMessage(pMessage);
    }

    /**
     * Shows the given message and sets the warning icon.
     *
     * @param pMessage
     */
    public void setWarnMessage(String pMessage)
    {
        setIcon(ICON_WARN);
        setMessage(pMessage);
    }

    /**
     * Shows the given message and sets the error icon.
     *
     * @param pMessage
     */
    public void setErrorMessage(String pMessage)
    {
        setIcon(ICON_ERROR);
        setMessage(pMessage);
    }

}
