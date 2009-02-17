package com.tomtessier.scrollabledesktop;

import javax.swing.*;
import java.awt.*;

/**
 * This interface provides a set of reusable constants for use by 
 * other classes in the system.
 *
 * @author <a href="mailto:tessier@gabinternet.com">Tom Tessier</a>
 * @version 1.0  29-Jul-2001
 */
public interface DesktopConstants {

      // all variables declared here are automatically public static final

      /** maximum number of internal frames allowed */
      int MAX_FRAMES = 20;

      /** default x offset of first frame in cascade mode, relative to desktop */
      int X_OFFSET = 30;

      /** default y offset of first frame in cascade mode, relative to desktop */
      int Y_OFFSET = 30;

      /** minimum width of frame toolbar buttons */
      int MINIMUM_BUTTON_WIDTH = 30;

      /** maximum width of frame toolbar buttons */
      int MAXIMUM_BUTTON_WIDTH = 80;

      /** the foreground color of inactive buttons whose associated frame 
            contents have changed */
      Color CONTENTS_CHANGED_COLOR = Color.red;

}