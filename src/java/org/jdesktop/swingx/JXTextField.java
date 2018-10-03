/* $Id$
 *
 * See http://sourceforge.net/projects/smackfw/
 */
package org.jdesktop.swingx;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.util.List;

import javax.swing.JTextField;

import org.jdesktop.swingx.prompt.BuddySupport;
import org.jdesktop.swingx.prompt.BuddySupport.Position;
import org.jdesktop.swingx.prompt.PromptSupport;
import org.jdesktop.swingx.prompt.PromptSupport.FocusBehavior;

/**
 * A {@link JTextField} with integrated support for prompts and buddies.
 *
 * A prompt is text content that is displayed in the textfield before the
 * user entered data.  It can be used to give information on the data
 * expected to be entered. Use {@link #setFocusBehavior(FocusBehavior)} to
 * adjust how prompts are displayed.
 *
 * @see PromptSupport
 * @see BuddySupport
 * @author Peter Weishapl <petw@gmx.net>
 *
 */
public class JXTextField extends JTextField {
    public JXTextField() {
        this(null);
    }

    public JXTextField(String promptText) {
        this(promptText, null);
    }

    public JXTextField(String promptText, Color promptForeground) {
        this(promptText, promptForeground, null);
    }

    public JXTextField(String promptText, Color promptForeground,
            Color promptBackground) {
        PromptSupport.init(promptText, promptForeground, promptBackground,
                this);
    }

    /**
     * @see PromptSupport#getFocusBehavior(javax.swing.text.JTextComponent)
     */
    public FocusBehavior getFocusBehavior() {
        return PromptSupport.getFocusBehavior(this);
    }

    /**
     * @see PromptSupport#getPrompt(javax.swing.text.JTextComponent)
     */
    public String getPrompt() {
        return PromptSupport.getPrompt(this);
    }

    /**
     * @see PromptSupport#getForeground(javax.swing.text.JTextComponent)
     */
    public Color getPromptForeground() {
        return PromptSupport.getForeground(this);
    }

    /**
     * @see PromptSupport#getForeground(javax.swing.text.JTextComponent)
     */
    public Color getPromptBackground() {
        return PromptSupport.getBackground(this);
    }

    /**
     * @see PromptSupport#getFontStyle(javax.swing.text.JTextComponent)
     */
    public Integer getPromptFontStyle() {
        return PromptSupport.getFontStyle(this);
    }

    /**
     * @see PromptSupport#getFocusBehavior(javax.swing.text.JTextComponent)
     */
    public void setFocusBehavior(FocusBehavior focusBehavior) {
        PromptSupport.setFocusBehavior(focusBehavior, this);
    }

    /**
     * @see PromptSupport#setPrompt(String, javax.swing.text.JTextComponent)
     */
    public void setPrompt(String labelText) {
        PromptSupport.setPrompt(labelText, this);
    }

    /**
     * @see PromptSupport#setForeground(Color, javax.swing.text.JTextComponent)
     */
    public void setPromptForeground(Color promptTextColor) {
        PromptSupport.setForeground(promptTextColor, this);
    }

    /**
     * @see PromptSupport#setBackground(Color, javax.swing.text.JTextComponent)
     */
    public void setPromptBackground(Color promptTextColor) {
        PromptSupport.setBackground(promptTextColor, this);
    }

    /**
     * @see PromptSupport#setFontStyle(Integer, javax.swing.text.JTextComponent)
     */
    public void setPromptFontStyle(Integer fontStyle) {
        PromptSupport.setFontStyle(fontStyle, this);
    }

    /**
     * @see BuddySupport#setOuterMargin(JTextField, Insets)
     */
    public void setOuterMargin(Insets margin) {
        BuddySupport.setOuterMargin(this, margin);
    }

    /**
     * @see BuddySupport#getOuterMargin(JTextField)
     */
    public Insets getOuterMargin() {
        return BuddySupport.getOuterMargin(this);
    }

    /**
     * @see BuddySupport#add(Component, Position, JTextField)
     */
    public void addBuddy(Component buddy, Position pos) {
        BuddySupport.add(buddy, pos, this);
    }

    /**
     * @see BuddySupport#addGap(int, Position, JTextField)
     */
    public void addGap(int width, Position pos) {
        BuddySupport.addGap(width, pos, this);
    }

    /**
     * @see BuddySupport#getBuddies(Position, JTextField)
     */
    public List<Component> getBuddies(Position pos) {
        return BuddySupport.getBuddies(pos, this);
    }

    /**
     * @see BuddySupport#removeAll(JTextField)
     */
    public void removeAllBuddies() {
        BuddySupport.removeAll(this);
    }

    private static final long serialVersionUID = -4010058317845466382L;
}
