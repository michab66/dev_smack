/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2003-15 Michael G. Binz
 */
package org.jdesktop.swingx;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.smack.FontResizer;
import org.jdesktop.smack.util.FileUtils;
import org.jdesktop.smack.util.OutputStreamForwarder;

/**
 * A console ui component. Connects stream oriented in- and output to a text component.
 *
 * @version $Rev$
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public final class JXConsole extends JPanel implements KeyListener {

    private final Logger LOG = Logger.getLogger( getClass().getName() );

    // TODO(micbinz) History prefix search.

    /**
     * The maximum text length a console is allowed to handle.  Adding more text results in
     * the console discarding the oldest text.
     */
    private final static int MAX_TEXT_LENGTH = 250000;

    /**
     * The stream that receives input from the console. This is passed in by the client.
     */
    private OutputStream _outPipe  = null;

    /**
     * The console's output stream that is connected to the text area.
     */
    private final OutputStream _out = new OutputStream()
    {

        @Override
        public void write( int b ) throws IOException
        {
            write( new byte[] { (byte)(b & 0xff) } );
        }

        @Override
        public void write( byte b[] ) throws IOException
        {
            threadSavePrint( new String( b ) );
        }

        @Override
        public void write( byte[] b, int off, int len ) throws IOException
        {
            threadSavePrint( new String( b, off, len  ) );
        }
    };

    /**
     * Get a reference to the output stream.
     *
     * @return A reference to the output stream.
     */
    public OutputStream getOut() {
        return _out;
    }

    /**
     * The document position where the editable content starts.
     */
    private int _cmdStart = 0;

    private static final int  maximumHistoryLength = 100;

    /**
     * The command line history.  The last line is the newest line in the history.
     */
    private final Vector<String> _history = new Vector<String>();

    /**
     *
     */
    private String _startedLine;

    /**
     *
     */
    private int _histLine = 0;

    /**
     * The context popup.
     */
    private final JPopupMenu _menu = new JPopupMenu();

    /**
     * The text component used.
     */
    private final JTextArea _text;

    /**
     * The window toolbar.
     */
    private final JToolBar _toolbar = new JToolBar();

    /**
     * True if a console should display an entered CR, false
     * otherwise.  Default is false.
     */
    private final boolean _showCr;

    /**
     * Creates a console.
     */
    public JXConsole() {
        this( false );
    }

    /**
     * Creates a console. Allows to select whether carriage
     * returns are displayed in the console window as they are entered.
     *
     * @param showCr If true then entered carriage returns are shown in the
     * console window. Otherwise carriage returns are not displayed.
     */
    public JXConsole( boolean showCr ) {

        super( new BorderLayout() );

        _showCr = showCr;

        Application.getResourceManager().injectResources( this );

        _text = new JTextArea() {

            @Override
            public void cut() {
                // If not in the edit area map the cut action to copy.
                if ( _text.getCaretPosition() < _cmdStart) {
                    super.copy();
                }
                else {
                    super.cut();
                }
            }

            @Override
            public void paste() {
                forceCaretIntoEditArea();
                super.paste();
            }
        };

        new FontResizer( _text );

        _text.setLineWrap( true );
        _text.setBackground( _toolbar.getBackground() );
        _text.setEditable( false );

        setFont(new Font("Monospaced", Font.PLAIN, 12));

        _text.addKeyListener(this);

        JScrollPane scrollPane = new JScrollPane(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setViewportView( _text );

        // create popup menu
        _menu.add(new JMenuItem( getNamedAction( "actCopy" ) ) );
        _menu.add(new JMenuItem( getNamedAction( "actPaste" ) ) );
        _menu.add(new JMenuItem( getNamedAction( "actSave" ) ) );
        _menu.add(new JMenuItem( getNamedAction( "actFont" ) ) );
        _menu.add(new JMenuItem( getNamedAction( "actClear" ) ) );

        _text.setComponentPopupMenu( _menu );

        _toolbar.setFloatable( false );

        add( scrollPane, BorderLayout.CENTER );

        addToolbarAction( getNamedAction( "actClear" ), false );
        addToolbarAction( getNamedAction( "actScrollLock" ), true );

        add( _toolbar, BorderLayout.PAGE_START );
    }

    /**
     *
     * @param a
     */
    public void addToolbarAction( javax.swing.Action a, boolean isSelectable )
    {
        if ( isSelectable )
        {
            _toolbar.add( new JToggleButton( a ) );
        }
        else
        {
            _toolbar.add(  a );
        }
    }

    /**
     * Access the console's toolbar.
     *
     * @return The console's toolbar.
     */
    public JToolBar getToolbar()
    {
        return _toolbar;
    }

    /**
     *
     */
    @Override
    public void keyPressed(KeyEvent e) {
        type(e);
    }

    /**
     *
     */
    @Override
    public void keyTyped(KeyEvent e) {}

    /**
     *
     */
    @Override
    public void keyReleased(KeyEvent e) {
        type(e);
    }

    /**
     *
     */
    private synchronized void type(KeyEvent e) {

        if (e.getKeyChar() == '\b') {
            e.setKeyCode(e.getKeyChar());
        }

        // TODO(micbinz) This is a quick hack. Because of the special
        // key handling it's not so easy to add a normal action for
        // handling control keys. Research.  The whole key handling
        // needs a rework.
        if ( e.isControlDown() && e.getKeyCode() == 'A' )
        {
            _text.selectAll();
            e.consume();
            return;
        }

        switch (e.getKeyCode()) {
        case (KeyEvent.VK_ENTER):
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                enter();
                resetEditArea();
                this._text.setCaretPosition(this._cmdStart);
            }
            e.consume();
            this._text.repaint();
            break;

        case (KeyEvent.VK_UP):
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                historyUp();
            }
            e.consume();
            break;

        case (KeyEvent.VK_DOWN):
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                historyDown();
            }
            e.consume();
            break;

        case (KeyEvent.VK_DELETE):
            if (this._text.getCaretPosition() < this._cmdStart) {
                e.consume();
            }
            break;

        case (KeyEvent.VK_LEFT):
        case (KeyEvent.VK_BACK_SPACE):
            if ((this._text.getCaretPosition() <= this._cmdStart)) {
                e.consume();
            }
            break;
        /*
         * case ( KeyEvent.VK_RIGHT ): forceCaretMoveToStart(); break;
         */

        case (KeyEvent.VK_HOME):
            this._text.setCaretPosition(this._cmdStart);
            e.consume();
            break;

        case (KeyEvent.VK_U): // clear line
            if ((e.getModifiers() & InputEvent.CTRL_MASK) > 0) {
                replaceEditArea("");
                this._histLine = 0;
                e.consume();
            }
            break;

        case (KeyEvent.VK_ALT):
        case (KeyEvent.VK_CAPS_LOCK):
        case (KeyEvent.VK_CONTROL):
        case (KeyEvent.VK_META):
        case (KeyEvent.VK_SHIFT):
        case (KeyEvent.VK_PRINTSCREEN):
        case (KeyEvent.VK_SCROLL_LOCK):
        case (KeyEvent.VK_PAUSE):
        case (KeyEvent.VK_INSERT):
        case (KeyEvent.VK_F1):
        case (KeyEvent.VK_F2):
        case (KeyEvent.VK_F3):
        case (KeyEvent.VK_F4):
        case (KeyEvent.VK_F5):
        case (KeyEvent.VK_F6):
        case (KeyEvent.VK_F7):
        case (KeyEvent.VK_F8):
        case (KeyEvent.VK_F9):
        case (KeyEvent.VK_F10):
        case (KeyEvent.VK_F11):
        case (KeyEvent.VK_F12):
        case (KeyEvent.VK_ESCAPE):
            // only modifier pressed
            break;

        // / case (KeyEvent.VK_TAB):
        // if (e.getID() == KeyEvent.KEY_RELEASED) {
        // doCommandCompletion(getEditAreaContents());
        // }
        // e.consume();
        // break;

        // This takes care that in case a character is entered the cursor gets
        // placed into the command area before the character is actually entered.
        default:
            if ((e.getModifiers() & (InputEvent.CTRL_MASK | InputEvent.ALT_MASK | InputEvent.META_MASK)) == 0) {
                // plain character
                forceCaretIntoEditArea();
            }
            break;
        }
    }

    /**
     * Forwards the focus to the embedded text component.
     */
    @Override
    public void requestFocus() {
        this._text.requestFocus();
    }

    /**
     * Creates a new edit area.
     */
    private void resetEditArea() {
        this._cmdStart = this._text.getDocument().getLength();
    }

    /**
     * Appends the passed text to the console display.
     */
    private void append(String string) {
        Document d = this._text.getDocument();

        int documentLength = d.getLength();

        // Check if we reached our maximum buffer size and release data if this is the case.
        if (documentLength + string.length() > JXConsole.MAX_TEXT_LENGTH) {
            documentRemove(
                    d,
                    0,
                    // Under extreme circumstances it may be the case that the passed strings
                    // are larger than the entire buffer, for example if the buffer is defined
                    // relatively small and a lot of data is sent.
                    Math.min( string.length(), documentLength )
            );
        }

        documentInsert(d, d.getLength(), string);
    }

    /**
     *
     */
    private void replaceEditArea(String s) {
        this._text.select(this._cmdStart, this._text.getDocument().getLength());
        this._text.replaceSelection(s);
        this._text.setCaretPosition(this._text.getDocument().getLength());
    }

    /**
     * In case the caret is to the left of the current command area moves the caret to the end of
     * the command area. If the caret is inside the current command area its position is not
     * changed.
     */
    private void forceCaretIntoEditArea() {
        if (this._text.getCaretPosition() < this._cmdStart) {
            this._text.setCaretPosition(this._text.getDocument().getLength());
        }
    }

    /**
     * Add a command line to the history.  The strategy is to not keep any
     * duplicate lines in the history and to add recent lines always to the
     * recent history.  This means that rarely used entries move towards
     * the old end of the history and are thrown out first.
     *
     * @param commandLine The command line to put into the history.
     */
    private void addToHistory(String commandLine) {

        // We do not keep empty lines in our history.
        if (commandLine.isEmpty()) {
            return;
        }

        _history.remove( commandLine );

        if ( _history.size() >= JXConsole.maximumHistoryLength ) {
            _history.remove(0);
        }

        _history.addElement(commandLine);
    }

    /**
     * The user pressed enter after editing.
     */
    private void enter() {

        String confirmedLine = getEditAreaContents().trim();

        addToHistory(confirmedLine);

        this._histLine = 0;

        postLine(confirmedLine + "\n");

        if ( _showCr )
            try
            {
                getOut().write( '\n' );
            }
            catch ( IOException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    /**
     * Returns the edited line.
     *
     * @return The line edited, if this is empty then the empty string is returned. {@code null} is
     *         never returned.
     */
    private String getEditAreaContents() {
        try {
            return _text.getText(this._cmdStart, _text.getDocument().getLength() - _cmdStart);
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes a cursor up key.
     */
    private void historyUp()
    {
        // TODO Implementation is a mess. Cleanup.

        if (_history.size() == 0) {
            return;
        }

        if (_histLine == 0) {
            _startedLine = getEditAreaContents();
        }

        int historyLine = _histLine;

        while (historyLine < _history.size()) {
            historyLine++;
            if ( _history.get( _history.size() - historyLine ).startsWith( _startedLine ) )
            {
                _histLine = historyLine;
                showHistoryLine();
                break;
            }
        }
    }

    /**
     * Processes a cursor down key.
     */
    private void historyDown()
    {
        // TODO Implementation is a mess. Cleanup.

        if (_histLine == 0)
            return;

        int historyLine = _histLine;

        while ( historyLine > 0 ) {
            historyLine--;
            if ( historyLine == 0 ||
                 _history.get( _history.size() - historyLine ).startsWith( _startedLine ) )
            {
                _histLine = historyLine;
                showHistoryLine();
                break;
            }
        }
    }

    /**
     *
     */
    private void showHistoryLine()
    {
        // TODO Implementation is a mess. Cleanup.

        String showline;
        if (_histLine == 0) {
            showline = _startedLine;
        }
        else {
            showline = _history.elementAt(_history.size() - _histLine);
        }

        replaceEditArea(showline);
        forceCaretIntoEditArea();
        _text.setCaretPosition(_text.getDocument().getLength());
    }

    /**
     * Write the passed line to the console's output listener.
     */
    private void postLine(String line) {

        if (this._outPipe == null) {
            return;
        }

        try {
            _outPipe.write(line.getBytes());
        }
        catch (IOException e) {
            // Signal to the user that we cannot accept further input.
            _text.setBackground( Color.LIGHT_GRAY );
            _text.setEditable( false );
            _outPipe = null;
            LOG.log( Level.WARNING, "Console pipe broken...", e );
        }
    }

    /**
     * Set an OutputStream that receives the lines entered by the user.
     *
     * @param os
     *            The output stream receiving lines entered by the user.
     */
    public void setInputReceiver(OutputStream os) {
        _outPipe = os == null ?
            null :
            new OutputStreamForwarder( os, 10 );

        // Set back to default background color.
        if ( _outPipe != null )
        {
            _text.setBackground( new JTextArea().getBackground() );
            _text.setEditable( true );
        }
        else
        {
            _text.setBackground( _toolbar.getBackground() );
            _text.setEditable( false );
        }
    }

    /**
     * A buffer used to carry the bytes across the border between non-edt and edt threads.
     * Since the edt event queue is processed very slow this buffer is used to collect incoming
     * data while the event queue has not processed the insert operation.
     * Used only in {@link #threadSavePrint(String)}.
     */
    private final StringBuffer _crossEdtBuffer = new StringBuffer();

    /**
     * Output the passed string to the console.
     *
     * @param string The string to print. The passed string buffer is locked and modified.
     */
    private void printTakeover( StringBuffer string ) {

        if ( ! SwingUtilities.isEventDispatchThread() )
            throw new InternalError( "Not on EDT." );

        int caretPosition = _text.getSelectionStart() - _text.getSelectionEnd();

        if ( caretPosition != 0 )
            caretPosition = -1;
        else
            caretPosition = _text.getCaretPosition();

        // Atomically read and reset the data to display.
        synchronized ( string )
        {
            append(string.toString());
            string.setLength( 0 );
        }

        resetEditArea();

        if ( caretPosition < 0 )
            ;
        else if ( isLocked() )
            _text.setCaretPosition( caretPosition );
        else
            _text.setCaretPosition( _cmdStart );
    }

    /**
     * Append to the console from a thread different from the EDT.
     *
     * @param string The text to append.
     */
    private synchronized void threadSavePrint( String string ) {

        boolean edtNotifyNeeded = true;

        synchronized ( _crossEdtBuffer )
        {
            // If our edt buffer was empty, we have to send a notification to Swing.
            // If it was not empty, Swing is already notified.
            edtNotifyNeeded = _crossEdtBuffer.length() == 0;
            _crossEdtBuffer.append( string );
        }

        // If no Swing notification is needed...
        if ( ! edtNotifyNeeded )
        {
            // ...we're done.
            return;
        }

        // Notify Swing of the new data.
        SwingUtilities.invokeLater( new Runnable()
        {
            @Override
            public void run()
            {
                printTakeover( _crossEdtBuffer );
            }
        } );
    }

    /**
     * Set the font on this component.
     *
     * @param font
     *            The font to set.
     */
    @Override
    public void setFont(Font font) {
        super.setFont(font);

        if (_text != null) {
            _text.setFont(font);
        }
    }

    /**
     * Returns the font set for this component.
     *
     * @return This component's font.
     */
    @Override
    public Font getFont() {
        Font result;

        if (this._text == null) {
            result = super.getFont();
        }
        else {
            result = _text.getFont();
        }

        return result;
    }


    @Action
    public void actCopy( ActionEvent ae )
    {
        _text.copy();
    }

    @Action
    public void actPaste( ActionEvent ae )
    {
        _text.paste();
    }

    @Action
    public void actFont( ActionEvent ae )
    {
        JXFontChooser fc = new JXFontChooser();
        fc.setSelectedFont(getFont());
        if (fc.showDialog(this) == JXFontChooser.OK_OPTION) {
            setFont(fc.getSelectedFont());
        }
    }

    @Resource
    private String FILE_EXISTS_MESSAGE;

    @Action
    public void actSave( ActionEvent ae ) {
        if (JXConsole.saveChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = JXConsole.saveChooser.getSelectedFile();
        if (f.exists()) {
            int result = JOptionPane.showOptionDialog(
                // Parent.
                this,
                // Message.
                String.format( FILE_EXISTS_MESSAGE, f.getName() ),
                // Title.
                Application.getInstance().getContext().getResourceMap().getString( Application.KEY_APPLICATION_TITLE ),
                // Options
                JOptionPane.YES_NO_OPTION,
                //
                JOptionPane.QUESTION_MESSAGE, null, null, null);

            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        writeFile(f, _text.getText());
    }

    @Action
    public void actClear( ActionEvent ae )
    {
        Document d = _text.getDocument();

        try {
            d.remove(0, d.getLength());
            resetEditArea();
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The scroll lock action.
     *
     * @param ae
     */
    @Action
    public void actScrollLock( ActionEvent ae )
    {
        boolean isSelected =
                ae.getSource() instanceof JToggleButton &&
                ((JToggleButton)ae.getSource()).isSelected();

        setLocked( isSelected );
    }

    private static JFileChooser saveChooser = new JFileChooser();

    /**
     * Writes the passed content into a file.
     *
     * @param pFile
     *            The target file.
     * @param pContent
     *            The content to write.
     * @throws IOException
     *             In case of an error.
     */
    private static void writeFile(File pFile, String pContent) {

        if (pFile == null) {
            throw new NullPointerException("pFile");
        }

        FileWriter fw = null;

        try {
            fw = new FileWriter(pFile);
            fw.write(pContent);
            fw.flush();
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(
                null, e.getLocalizedMessage(), "Writing failed.", JOptionPane.ERROR_MESSAGE, null);
        }
        finally {
            FileUtils.forceClose(fw);
        }
    }

    /**
     * Wraps document access to prevent checked exceptions.
     *
     * @param d
     *            The target document.
     * @param offset
     *            The insert offset into the document.
     * @param s
     *            The string to insert.
     */
    private static void documentInsert(Document d, int offset, String s) {
        try {
            d.insertString(offset, s, null);
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wraps document access to prevent checked exceptions.
     *
     * @param d
     *            The target document.
     * @param offset
     *            The offset into the document.
     * @param length
     *            The number of bytes to remove.
     */
    private static void documentRemove(Document d, int offset, int length) {
        try {
            d.remove(offset, length);
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the action corresponding to the passed name. The action is entered in the component's
     * action map.
     */
    private javax.swing.Action getNamedAction( String actionName )
    {
        javax.swing.Action result = Application.getInstance().getContext().getActionMap(this).get( actionName );

        if ( result != null && getActionMap().get( actionName ) == null )
            getActionMap().put( actionName, result );

        return result;
    }

    /**
     * If true then scroll lock is active.
     */
    private boolean _isLocked = false;

    /**
     * Get the scroll lock status.
     *
     * @return {code true} if scroll lock is active.
     */
    public boolean isLocked()
    {
        return _isLocked;
    }

    /**
     * Sets the scroll lock status.
     *
     * @param what The scroll lock status. {@code true} is scroll lock active.
     */
    public void setLocked( boolean what )
    {
        if ( what == _isLocked )
            return;

        _isLocked = what;

        firePropertyChange( "locked", !_isLocked, _isLocked );

        // Clear the selection if the lock status changed.
        _text.setSelectionEnd( _text.getSelectionStart() );

        // If not longer locked navigate to the end of the text.
        if ( !_isLocked )
            _text.setCaretPosition( _cmdStart );
    }
}
