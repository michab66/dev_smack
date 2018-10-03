package org.jdesktop.swingx;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;

/**
 * UI component that layouts horizontally the components added to it.
 *
 * @author Michael Binz
 * @author daskalot
 * @version $Revision$
 */
class HBox extends Box
{
    private static final long serialVersionUID = 1L;

    public HBox()
    {
        super( BoxLayout.X_AXIS );
    }

    /**
     * Adds empty space that extends itself to fill the gap between the
     * components.
     */
    public void addGlue()
    {
        add( Box.createHorizontalGlue() );
    }

    /**
     * Adds empty space with fixed width.
     */
    public void addGap()
    {
        add( Box.createHorizontalStrut( GTools.GAP ) );
    }

    /**
     * Adds button for the given action.
     *
     * @param pAction
     * @return
     */
    public JButton addButton( Action pAction )
    {
        JButton b = new JButton( pAction );
        add( b );
        return b;
    }
}
