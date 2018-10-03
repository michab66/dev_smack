/* $Id$
 *
 * Mp3 Tagger Hood
 *
 * Released under Gnu Public License
 * Copyright (c) 2009 Michael G. Binz
 */
package org.jdesktop.smack;

import java.awt.event.KeyEvent;
import java.util.EventObject;

import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.Document;

import org.jdesktop.swingx.JXTable.GenericEditor;



/**
 * A special cell editor for text components that by default overwrites
 * text when input begins.  Used for excel-like input behaviour.
 *
 * @author Michael Binz
 * @version $Rev$
 */
public class TextCellEditor extends GenericEditor
{
    private static final long serialVersionUID = -2717959287341829880L;

    public enum Alignment { Left, Right, Center };

    /**
     * TextField specialized as editingComponent which tries its best
     * to start with content selected.
     *
     * The implication is that
     * the text is replaced by the first keystroke. This might be
     * a usability issue if the table's autoStartEditOnKeyStroke is
     * true (default): users might be surprised that the content is
     * deleted without warning. Might smoothened by a visual clue that the
     * focused cell is editable.<p>
     *
     * PENDING: support mode which unselects on
     * very first char received from the table. This will make it
     * backward compatible.
     */
    @SuppressWarnings("serial")
    public static class EditorTextField extends JTextField {

        private boolean appendFirstKey;
        private boolean firstHandled;

        /**
         * Overridden to select all.
         */
        @Override
        public void addNotify() {
            super.addNotify();
            selectAll();
            firstHandled = false;
        }

        /**
         * Sets a flag indicating whether the first key passed-in from the
         * table should be appended. This is for backward compatibility, as
         * it behaves in the same manner as the default editors. <p>
         *
         * The default value of this flag is false.
         *
         * @param appendFirst
         */
        public void setAppendFirstKey(boolean appendFirst) {
            this.appendFirstKey = appendFirst;
        }

        /**
         * {@inheritDoc} <p>
         *
         * Overridden to handle backward compatible append key.
         */
        @Override
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                int condition, boolean pressed) {
            checkSelection(e);
            return super.processKeyBinding(ks, e, condition, pressed);
        }

        /**
         * Clears the selection if necessary for backward compatible append
         * first key mode.
         *
         * @param e the event received in processKeyBinding
         *
         * @see #processKeyBinding(KeyStroke, KeyEvent, int, boolean)
         */
        private void checkSelection(KeyEvent e) {
            if (!appendFirstKey || firstHandled) return;
            firstHandled = true;
            if ((e == null) || (e.getSource() != this)) {
                clearSelection();
            }
        }

        /**
         * Clears the selection and moves the caret to the end of the document.
         */
        private void clearSelection() {
            Document doc = getDocument();
            select(doc.getLength(), doc.getLength());

        }

    }

    /**
     * CellEditor specialized on selecting content after starting
     * an edit.
     */

        public TextCellEditor() {
            super(new EditorTextField());
        }

        public TextCellEditor( Alignment alignment )
        {
            super(new EditorTextField());

            int swingAlignment;

            switch ( alignment )
            {
               case Left:
                   swingAlignment = JTextField.LEFT;
                   break;
               case Right:
                   swingAlignment = JTextField.RIGHT;
                   break;
               case Center:
                   swingAlignment = JTextField.CENTER;
                   break;
               default:
                   throw new InternalError( "Unknown alignment." );
            }
            getComponent().setHorizontalAlignment( swingAlignment );
        }

        /**
         * Overridden to select all before calling super. This
         * is for selecting the text after starting edits with
         * the mouse. <p>
         *
         * NOTE: ui-delegates are not guaranteed to call this after
         * starting the edit triggered with the mouse. On the other
         * hand, not all are expected to dispatch the starting mouse
         * event to the editing component (which is the event which
         * destroys the selection done in addNotify).
         */
        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            getComponent().selectAll();
            return super.shouldSelectCell(anEvent);
        }

        @Override
        public JTextField getComponent() {
            return (JTextField) super.getComponent();
        }

//        table.setDefaultEditor(Object.class, new CellEditor());
}

