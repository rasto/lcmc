package com.tomtessier.scrollabledesktop;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;


/**
 * This class provides the optional "Window" menu for the scrollable desktop.
 *
 * @author <a href="mailto:tessier@gabinternet.com">Tom Tessier</a>
 * @version 1.0  11-Aug-2001
 */


public class DesktopMenu extends JMenu implements ActionListener {

      private DesktopMediator desktopMediator;

      private boolean tileMode;
      private int baseItemsEndIndex;
      private ButtonGroup frameRadioButtonMenuItemGroup;


    /**
     * creates the DesktopMenu object
     *
     * @param desktopMediator a reference to the DesktopMediator object
     */
      public DesktopMenu(DesktopMediator desktopMediator) {
            this(desktopMediator, false);
      }

    /**
     * creates the DesktopMenu object with the specified tileMode
     *
     * @param desktopMediator a reference to the DesktopMediator object
     * @param tileMode the tile mode to use (<code>true</code> = tile 
     *      internal frames, <code>false</code> = cascade internal frames)
     */
      public DesktopMenu(DesktopMediator desktopMediator, boolean tileMode) {

            super("Window");
            setMnemonic(KeyEvent.VK_W);

            this.desktopMediator = desktopMediator;
            this.tileMode = tileMode;

            frameRadioButtonMenuItemGroup = new ButtonGroup();

            new ConstructWindowMenu(this, desktopMediator, tileMode);

            // set the default item count (ie: number of items comprising
            // current menu contents)
            baseItemsEndIndex = getItemCount();

      }


     /** 
       * adds a 
       * {@link com.tomtessier.scrollabledesktop.BaseRadioButtonMenuItem
       * BaseRadioButtonMenuItem} to the menu and associates it with an internal frame
       *
       * @param associatedFrame the internal frame to associate with the menu item
       */
      public void add (BaseInternalFrame associatedFrame) {

            int displayedCount = getItemCount() - baseItemsEndIndex + 1;
            int currentMenuCount = displayedCount;

            // compute the key mnemonic based upon the currentMenuCount
            if (currentMenuCount > 9) { 
                  currentMenuCount/=10; 
            } 

            BaseRadioButtonMenuItem menuButton = 
                  new BaseRadioButtonMenuItem(this,
                    displayedCount + " " + associatedFrame.getTitle(), 
                      KeyEvent.VK_0 + currentMenuCount, -1, true, associatedFrame);

            associatedFrame.setAssociatedMenuButton(menuButton);

            add(menuButton);
            frameRadioButtonMenuItemGroup.add(menuButton);

            menuButton.setSelected(true); // and reselect here, so that the 
                                          // buttongroup recognizes the change

      }

      /**
        * removes the specified radio menu button from the menu
        *
        * @param menuButton the JRadioButtonMenuItem to remove
        */
      public void remove(JRadioButtonMenuItem menuButton) {
            frameRadioButtonMenuItemGroup.remove(menuButton);
            super.remove(menuButton); 

        // cannot simply remove the radio menu button, as need to renumber the
        // keyboard shortcut keys as well. Hence, a call to refreshMenu is in order...

            refreshMenu(); // refresh the mnemonics associated with the other items
      }

      
      private void refreshMenu() {

            // refresh the associated mnemonics, so that the keyboard shortcut 
            // keys are properly renumbered...

            // get an enumeration to the elements of the current button group
            Enumeration e = frameRadioButtonMenuItemGroup.getElements();

            int displayedCount = 1;
            int currentMenuCount = 0;

            while (e.hasMoreElements()) {
                  BaseRadioButtonMenuItem b = 
                        (BaseRadioButtonMenuItem)e.nextElement();

                  // compute the key mnemonic based upon the currentMenuCount
                  currentMenuCount = displayedCount;
                  if (currentMenuCount > 9) { 
                        currentMenuCount/=10; 
                  } 
                  b.setMnemonic(KeyEvent.VK_0 + currentMenuCount);
                  b.setText(displayedCount + 
                        " " + b.getAssociatedFrame().getTitle());
                  displayedCount++;
            }

      }

      /**
        * propogates the actionPerformed menu event to DesktopMediator
        *
        * @param e the ActionEvent to propogate
        */
      public void actionPerformed(ActionEvent e) {
            desktopMediator.actionPerformed(e);
      }


}