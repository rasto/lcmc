package com.tomtessier.scrollabledesktop;

import java.awt.event.*;
import javax.swing.*;

/**
 * This class creates a base radio button menu item. ActionListener, mnemonic, 
 * keyboard shortcut, and title are set via the constructor.
 * <BR><BR>
 * A {@link com.tomtessier.scrollabledesktop.BaseInternalFrame BaseInternalFrame}
 * object may optionally be associated with an instance of this class.
 *
 * @author <a href="mailto:tessier@gabinternet.com">Tom Tessier</a>
 * @version 1.0  11-Aug-2001
 */

public class BaseRadioButtonMenuItem extends JRadioButtonMenuItem 
            implements FrameAccessorInterface {

      private BaseInternalFrame associatedFrame;


    /**
     * creates the BaseRadioButtonMenuItem with an associated frame. Used for
     * radio menu items that are associated with an internal frame.
     *
     * @param listener the action listener to assign
     * @param itemTitle the title of the item
     * @param mnemonic the mnemonic used to access the menu
     * @param shortcut the keyboard shortcut used to access the menu. 
     *      -1 indicates no shortcut.
     * @param selected <code>boolean</code> that indicates whether 
     *      the menu item is selected or not
     * @param associatedFrame the BaseInternalFrame associated with the menu item
     */
      public BaseRadioButtonMenuItem(ActionListener listener, 
                             String itemTitle, 
                             int mnemonic, 
                             int shortcut, 
                             boolean selected,
                             BaseInternalFrame associatedFrame) {

            this(listener, itemTitle, mnemonic, shortcut, selected);
            this.associatedFrame = associatedFrame;

      }

    /**
     * creates the BaseRadioButtonMenuItem without an associated frame. Used
     * for generic radio button menu items.
     *
     * @param listener the action listener to assign
     * @param itemTitle the title of the item
     * @param mnemonic the mnemonic used to access the menu
     * @param shortcut the keyboard shortcut used to access the menu. 
     *      -1 indicates no shortcut.
     * @param selected <code>boolean</code> that indicates whether 
     *      the menu item is selected or not
     */
      public BaseRadioButtonMenuItem(ActionListener listener, 
                             String itemTitle, 
                             int mnemonic, 
                             int shortcut, 
                             boolean selected) {

            super(itemTitle, selected);
            setMnemonic(mnemonic);


            // set the alt-Shortcut accelerator
            if (shortcut != -1) {
                  setAccelerator(
                        KeyStroke.getKeyStroke(
                                shortcut, ActionEvent.ALT_MASK)); 
            }

            setActionCommand(itemTitle + "Radio");
            addActionListener(listener);

      }


     /** 
       *  sets the associated frame
       *
       * @param associatedFrame the BaseInternalFrame object to associate with 
       * the menu item
       */
      public void setAssociatedFrame(BaseInternalFrame associatedFrame) {
            this.associatedFrame = associatedFrame;
      }

     /** 
       *  returns the associated frame
       *
       * @return the BaseInternalFrame object associated with this menu item
       */
      public BaseInternalFrame getAssociatedFrame() {
            return associatedFrame;
      }


}