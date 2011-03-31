package com.tomtessier.scrollabledesktop;

import javax.swing.*;
import java.awt.*;

import java.beans.PropertyVetoException;

/**
 * This class provides a custom desktop manager for 
 * {@link com.tomtessier.scrollabledesktop.BaseDesktopPane BaseDesktopPane}.
 *
 * @author <a href="mailto:tessier@gabinternet.com">Tom Tessier</a>
 * @version 1.0  9-Aug-2001
 */

public class BaseDesktopManager extends DefaultDesktopManager {

      private BaseDesktopPane desktopPane;

     /** 
       *  creates the BaseDesktopManager
       *
       * @param desktopPane a reference to BaseDesktopPane
       */
      public BaseDesktopManager(BaseDesktopPane desktopPane) {
            this.desktopPane = desktopPane;
      }

     /** 
       * maximizes the internal frame to the viewport bounds rather 
       * than the desktop bounds 
       *
       * @param f the internal frame being maximized
       */
      public void maximizeFrame(JInternalFrame f) {

            Rectangle p = desktopPane.getScrollPaneRectangle();
            f.setNormalBounds(f.getBounds());
            setBoundsForFrame(f, p.x, p.y, p.width, p.height);
            try { 
                  f.setSelected(true); 
            } catch (PropertyVetoException pve) {
                  System.out.println(pve.getMessage());
            }

            removeIconFor(f);

     }

      /**
        * insures that the associated toolbar and menu buttons of 
        * the internal frame are activated as well
        *
        * @param f the internal frame being activated
        */
      public void activateFrame(JInternalFrame f) {

            super.activateFrame(f);
            ((BaseInternalFrame)f).selectFrameAndAssociatedButtons();

      }


      /**
        * closes the internal frame and removes any associated button 
        * and menu components
        *
        * @param f the internal frame being closed
        */
      public void closeFrame(JInternalFrame f) {

            super.closeFrame(f);

      // possible to retrieve the associated buttons right here via 
      // f.getAssociatedButton(), and then with a call to getParent() the item 
      // can be directly removed from its parent container, but I find the 
      // below message propogation to DesktopPane a cleaner implementation...

            desktopPane.removeAssociatedComponents((BaseInternalFrame)f);
            desktopPane.resizeDesktop();        

      }


    /* could override iconifyFrame here as well, but much simpler
       to define an EmptyDesktopIconUI look and feel class in BaseDesktopPane */


}
