package com.tomtessier.scrollabledesktop;

import javax.swing.*;
import java.awt.*;


/**
 * This class provides internal frame positioning methods for use by
 * {@link com.tomtessier.scrollabledesktop.DesktopScrollPane DesktopScrollPane}.
 *
 * @author <a href="mailto:tessier@gabinternet.com">Tom Tessier</a>
 * @version 1.0  11-Aug-2001
 */


public class FramePositioning implements DesktopConstants {

      private DesktopScrollPane desktopScrollpane;

      private boolean autoTile;  // determines whether to cascade or tile windows


    /**
     * creates the FramePositioning object
     *
     * @param desktopScrollpane a reference to the DesktopScrollpane object
     */
      public FramePositioning(DesktopScrollPane desktopScrollpane) {
            this.desktopScrollpane = desktopScrollpane;
      }


    /**
     * turns autoTile on or off
     *
     * @param autoTile <code>boolean</code> representing autoTile mode. 
     *      If <code>true</code>, then all new frames are tiled automatically.
     *      If <code>false</code>, then all new frames are cascaded automatically.
     */
      public void setAutoTile(boolean autoTile) {
            this.autoTile = autoTile;
            if (autoTile) {
                  tileInternalFrames();
            }
            else {
                  cascadeInternalFrames();
            }
      }
    /**
     * returns the autoTile mode
     *
     * @return <code>boolean</code> representing current autoTile mode
     */
      public boolean getAutoTile() {
            return autoTile;
      }

      /**
        * cycles through and cascades all internal frames
        */
      public void cascadeInternalFrames() {

            JInternalFrame[] frames = desktopScrollpane.getAllFrames();
            BaseInternalFrame f;

            int frameCounter=0;

            for (int i = frames.length - 1; i >=0; i--) {
                  f = (BaseInternalFrame)frames[i];

                  // don't include iconified frames in the cascade
                  if (!f.isIcon()) {      
                        f.setSize(f.getInitialDimensions());
                        f.setLocation(cascadeInternalFrame(f, frameCounter++));
                  }
            }

      }

    /** 
      * cascades the given internal frame based upon the current number 
      *     of internal frames
      *
      * @param f the internal frame to cascade
      *
      * @return a Point object representing the location 
      *     assigned to the internal frame upon the virtual desktop
      */
    public Point cascadeInternalFrame(JInternalFrame f) {
            return cascadeInternalFrame(f, desktopScrollpane.getNumberOfFrames());
    }

    /**
      * cascades the given internal frame based upon supplied count
      *
      * @param f the internal frame to cascade
      * @count the count to use in cascading the internal frame
      *
      * @return a Point object representing the location 
      *     assigned to the internal frame upon the virtual desktop
      */
    private Point cascadeInternalFrame(JInternalFrame f, int count) {

          int windowWidth = f.getWidth();
          int windowHeight = f.getHeight();

          Rectangle viewP = desktopScrollpane.getViewport().getViewRect();

          // get # of windows that fit horizontally
          int numFramesWide = (viewP.width-windowWidth)/X_OFFSET;
          if (numFramesWide < 1) { numFramesWide = 1; }
          // get # of windows that fit vertically
          int numFramesHigh = (viewP.height-windowHeight)/Y_OFFSET; 
          if (numFramesHigh < 1) { numFramesHigh = 1; }

          // position relative to the current viewport (viewP.x/viewP.y)
          // (so new windows appear onscreen)
          int xLoc = viewP.x + X_OFFSET*((count+1) - 
                  (numFramesWide-1)*(int)(count/numFramesWide));
          int yLoc = viewP.y + Y_OFFSET*((count+1) - 
                  numFramesHigh*(int)(count/numFramesHigh));

          return new Point(xLoc, yLoc);

    } 


    /** 
      * tiles internal frames upon the desktop. 
      * <BR><BR>
      * Based upon the following tiling algorithm:
      * <BR><BR>
      * - take the sqroot of the total frames rounded down, that gives 
      *  the number of columns.
      * <BR><BR>
      * - divide the total frames by the # of columns to get the # 
      *  of rows in each column, and any remainder is distributed 
      *  amongst the remaining rows from right to left)
      * <BR><BR>
      * eg) <BR>
      *     1 frame,  remainder 0, 1 row<BR>
      *     2 frames, remainder 0, 2 rows<BR>
      *     3 frames, remainder 0, 3 rows<BR>
      *     4 frames, remainder 0, 2 rows x 2 columns <BR>
      *     5 frames, remainder 1, 2 rows in column I, 3 rows in column II<BR>
      *     10 frames, remainder 1, 3 rows in column I, 3 rows in column II, 
      *                 4 rows in column III <BR>
      *     16 frames, 4 rows x 4 columns <BR>
      * <BR><BR>
      * Pseudocode:
      * <BR><BR><code>
      *     while (frames) { <BR>
      *           numCols = (int)sqrt(totalFrames); <BR>
      *           numRows = totalFrames / numCols; <BR>
      *           remainder = totalFrames % numCols <BR>
      *           if ((numCols-curCol) <= remainder) { <BR>
      *                 numRows++; // add an extra row for this column <BR>
      *           } <BR>
      *     } </code><BR>
      */
      public void tileInternalFrames() {

            Rectangle viewP = desktopScrollpane.getViewport().getViewRect();

            int totalNonIconFrames=0;

            JInternalFrame[] frames = desktopScrollpane.getAllFrames();

            for (int i=0; i < frames.length; i++) {
                  if (!frames[i].isIcon()) {    // don't include iconified frames...
                        totalNonIconFrames++;
                  }
            }

            int curCol = 0;
            int curRow = 0;
            int i=0;

            if (totalNonIconFrames > 0) {

                  // compute number of columns and rows then tile the frames
                  int numCols = (int)Math.sqrt(totalNonIconFrames);

                  int frameWidth = viewP.width/numCols;

                  for (curCol=0; curCol < numCols; curCol++) {

                        int numRows = totalNonIconFrames / numCols;
                        int remainder = totalNonIconFrames % numCols;

                        if ((numCols-curCol) <= remainder) {
                              numRows++; // add an extra row for this guy
                        }

                        int frameHeight = viewP.height/numRows;

                        for (curRow=0; curRow < numRows; curRow++) {
                              while (frames[i].isIcon()) { // find the next visible frame
                                    i++;
                              }

                              frames[i].setBounds(curCol*frameWidth,curRow*frameHeight,
                                    frameWidth,frameHeight);

                              i++;
                        }

                  }

            }

      }


}