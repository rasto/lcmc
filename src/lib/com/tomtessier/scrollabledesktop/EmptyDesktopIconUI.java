package com.tomtessier.scrollabledesktop;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopIconUI;


/**
 * This class provides an empty DesktopIconUI for 
 * {@link com.tomtessier.scrollabledesktop.BaseDesktopPane BaseDesktopPane}.
 *
 * @author <a href="mailto:tessier@gabinternet.com">Tom Tessier</a>
 * @version 1.0  29-Jul-2001
 */

public class EmptyDesktopIconUI extends DesktopIconUI {

  /**
    * stores the instance of this class. Used by 
    * {@link com.tomtessier.scrollabledesktop.EmptyDesktopIconUI#createUI(JComponent)
    * createUI}.
    */
  protected static EmptyDesktopIconUI desktopIconUI;


  /**
    * creates the EmptyDesktopIconUI object
    *
    * @param c the reference to the JComponent object required by createUI
    */
  public static ComponentUI createUI (JComponent c) {
    if (desktopIconUI == null) {
      desktopIconUI = new EmptyDesktopIconUI();
    }
    return desktopIconUI;
  }


  /**
    * overrides the paint method with a blank routine so that no 
    * component is displayed when an internal frame is iconified
    *
    * @param g the reference to the Graphics object used to paint the desktop
    */
  protected void paint(Graphics g) {}

}
