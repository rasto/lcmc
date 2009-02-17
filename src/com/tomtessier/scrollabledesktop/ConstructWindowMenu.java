package com.tomtessier.scrollabledesktop;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * This class constructs the "Window" menu items for use by
 * {@link com.tomtessier.scrollabledesktop.DesktopMenu DesktopMenu}.
 *
 * @author <a href="mailto:tessier@gabinternet.com">Tom Tessier</a>
 * @version 1.0  11-Aug-2001
 */


public class ConstructWindowMenu implements ActionListener {

      private DesktopMediator desktopMediator;


    /**
     * creates the ConstructWindowMenu object.
     *
     * @param sourceMenu the source menu to apply the menu items
     * @param desktopMediator a reference to the DesktopMediator
     * @param tileMode the current tile mode (tile or cascade)
     */
      public ConstructWindowMenu(JMenu sourceMenu, 
                               DesktopMediator desktopMediator,
                               boolean tileMode) {
            this.desktopMediator = desktopMediator;
            constructMenuItems(sourceMenu, tileMode);
      }

    /**
     * constructs the actual menu items.
     *
     * @param sourceMenu the source menu to apply the menu items
     * @param tileMode the current tile mode
     */
      private void constructMenuItems(JMenu sourceMenu, boolean tileMode) {

            sourceMenu.add(new BaseMenuItem(this, "Tile", KeyEvent.VK_T, -1));
            sourceMenu.add(new BaseMenuItem(this, "Cascade", KeyEvent.VK_C, -1));
            sourceMenu.addSeparator();

            JMenu autoMenu = new JMenu("Auto");
            autoMenu.setMnemonic(KeyEvent.VK_U);
            ButtonGroup autoMenuGroup = new ButtonGroup();
            JRadioButtonMenuItem radioItem = 
                  new BaseRadioButtonMenuItem(this, 
                        "Tile", KeyEvent.VK_T, -1, tileMode);
            autoMenu.add(radioItem);
            autoMenuGroup.add(radioItem);

            radioItem = 
                  new BaseRadioButtonMenuItem(this, 
                        "Cascade", KeyEvent.VK_C, -1, !tileMode);
            autoMenu.add(radioItem);
            autoMenuGroup.add(radioItem);

            sourceMenu.add(autoMenu);
            sourceMenu.addSeparator();

            sourceMenu.add(new BaseMenuItem(this, 
                  "Close", KeyEvent.VK_S, KeyEvent.VK_Z));
            sourceMenu.addSeparator();

      }


      /**
        * propogates actionPerformed menu event to the DesktopMediator reference
        *
        * @param e the ActionEvent to propogate
        */
      public void actionPerformed(ActionEvent e) {
            desktopMediator.actionPerformed(e);
      }

}