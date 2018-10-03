/* $Id$
 *
 * Application framework
 *
 * Unpublished work.
 * Copyright (c) 1999 Michael G. Binz
 */
package org.jdesktop.swingx;

import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * This dialog gets displayed after more than a defined number of child windows is displayed. This
 * allows to activate any available window though no menu representation exists.
 *
 * @see org.jdesktop.smack.swing.MdiDesktopPane
 * @see de.michab.swingx.MdiFrame
 */
class WindowsDialog {
    public static void showFor(JXDesktop desktop) {
        JInternalFrame[] frames = desktop.getAllFrames();

        String[] titles = new String[frames.length];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = frames[i].getTitle();
        }

        JList titleList = new JList(titles);
        titleList.setSelectedIndex(0);

        JScrollPane scrollPane = new JScrollPane(titleList);

        int result = JOptionPane.showOptionDialog(
            desktop, scrollPane, "Select a window", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
            null, null);

        if (result == JOptionPane.OK_OPTION) {
            JInternalFrame child = frames[titleList.getSelectedIndex()];
            // TODO this duplicates code in MdiFrame. Find a way to share this...
            try {
                if (child.isIcon()) {
                    child.setIcon(false);
                }

                child.setSelected(true);
            }
            catch (Throwable e) {
                System.err.println("setSelected failed.");
            }
        }
    }
}
