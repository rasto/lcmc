package com.tomtessier.scrollabledesktop;

import java.awt.event.*;
import javax.swing.*;

/**
 * This class creates a generic base menu item. ActionListener, mnemonic, 
 * keyboard shortcut, and title are set via the constructor.
 *
 * @author <a href="mailto:tessier@gabinternet.com">Tom Tessier</a>
 * @version 1.0  29-Jan-2001
 */

public class BaseMenuItem extends JMenuItem  {


    /**
     * creates the BaseMenuItem
     *
     * @param listener the action listener to assign
     * @param itemTitle the title of the item
     * @param mnemonic the mnemonic used to access the menu
     * @param shortcut the keyboard shortcut used to access the menu. 
     *      -1 indicates no shortcut.
     */
      public BaseMenuItem(ActionListener listener, 
                             String itemTitle, 
                             int mnemonic, 
                             int shortcut) {

            super(itemTitle, mnemonic);


            // set the alt-Shortcut accelerator
            if (shortcut != -1) {
                  setAccelerator(
                        KeyStroke.getKeyStroke(
                                shortcut, ActionEvent.ALT_MASK)); 
            }

            setActionCommand(itemTitle);
            addActionListener(listener);

      }

}