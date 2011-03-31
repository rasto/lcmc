package com.tomtessier.scrollabledesktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class coordinates state changes between other classes in the system.
 * Based upon the "mediator" design pattern.
 *
 * @author <a href="mailto:tessier@gabinternet.com">Tom Tessier</a>
 * @version 1.0  11-Aug-2001
 */


public class DesktopMediator implements DesktopConstants {

      private DesktopScrollPane desktopScrollpane;
      private DesktopResizableToolBar desktopResizableToolbar;
      private DesktopListener dListener;
      private DesktopMenu dMenu;

    /**
     * creates the DesktopMediator object.
     *
     * @param mainPane a reference to the JScrollableDesktopPane that this
     *      object is to mediate.
     */
      public DesktopMediator(JScrollableDesktopPane mainPane) {

            desktopScrollpane = new DesktopScrollPane(this);
            desktopResizableToolbar = new DesktopResizableToolBar(this);
            dListener = new DesktopListener(this);

            mainPane.add(desktopResizableToolbar, BorderLayout.NORTH);
            mainPane.add(desktopScrollpane, BorderLayout.CENTER);
            mainPane.addComponentListener(dListener);

      }


     /** 
       * registers a menubar with the mediator, applying the "Window" menu items
       * to that menubar in the process.
       *
       * @param mb the menubar to register
       */
      public void registerMenuBar(JMenuBar mb) {
            dMenu = new DesktopMenu(this);
            mb.add(dMenu);
            mb.setBorder(null);     // turn off the menubar border (looks better)
      }


     /** 
       * adds an internal frame to the scrollable desktop pane
       *
       * @param title the title displayed in the title bar of the internal frame
       * @param icon the icon displayed in the title bar of the internal frame
       * @param frameContents the contents of the internal frame
       * @param isClosable <code>boolean</code> indicating whether internal frame
       *          is closable
       * @param x x coordinates of internal frame within the scrollable desktop
       *    <code>-1</code> indicates the virtual desktop is to determine the position
       * @param y y coordinates of internal frame within the scrollable desktop
       *    <code>-1</code> indicates the virtual desktop is to determine the position
       *
       * @return the internal frame that was created
       */
      public JInternalFrame add(String title, ImageIcon icon, 
                        JPanel frameContents, 
                        boolean isClosable, int x, int y) {

            BaseInternalFrame frame = null;

            if (desktopScrollpane.getNumberOfFrames() < MAX_FRAMES) {

                  frame = desktopScrollpane.add(
                          dListener, title, icon,
                          frameContents, isClosable, x, y);

                  createFrameAssociates(frame);

            }

            return frame;
      }

     /** 
       * adds an internal frame to the scrollable desktop pane
       *
       * @param f the internal frame of class BaseInternalFrame to add
       * @param x x coordinates of internal frame within the scrollable desktop
       *    <code>-1</code> indicates the virtual desktop is to determine the position
       * @param y y coordinates of internal frame within the scrollable desktop
       *    <code>-1</code> indicates the virtual desktop is to determine the position
       */
      public void add(JInternalFrame frame, int x, int y) {

            if (desktopScrollpane.getNumberOfFrames() < MAX_FRAMES) {
                  desktopScrollpane.add(dListener, frame, x, y);
                  createFrameAssociates((BaseInternalFrame)frame);
            }

      }

      /**
        * creates the associated frame components (ie: toggle and menu items)
        */
      private void createFrameAssociates(BaseInternalFrame frame) {

                  BaseToggleButton button = null;
                  BaseRadioButtonMenuItem menuButton = null;

                  button = desktopResizableToolbar.add(frame.getTitle());

                  button.setAssociatedFrame(frame);
                  frame.setAssociatedButton(button);

                  if (dMenu != null) {
                        dMenu.add(frame);
                  }

                  if (desktopScrollpane.getAutoTile()) {
                        desktopScrollpane.tileInternalFrames();
                  }

      }


      /**
        * removes the secondary components associated with an internal frame,
        * such as toggle and menu buttons, and selects the next available frame
        *
        * @param f the internal frame whose associated components are 
        *   to be removed
        */
      public void removeAssociatedComponents(BaseInternalFrame f) {

            desktopResizableToolbar.remove(f.getAssociatedButton());
            if (dMenu != null) {
                  dMenu.remove(f.getAssociatedMenuButton());
            }
            // and select the next available frame...
            desktopScrollpane.selectNextFrame();

      }


      /** 
        * propogates getSelectedFrame to DesktopScrollPane
        *
        * @return the currently selected internal frame
        */
      public JInternalFrame getSelectedFrame() {
            return desktopScrollpane.getSelectedFrame();
      }
      /** 
        * propogates setSelectedFrame to DesktopScrollPane
        *
        * @param f the internal frame to set as selected
        */
      public void setSelectedFrame(JInternalFrame f) {
            desktopScrollpane.setSelectedFrame(f);
      }
      /** 
        * propogates flagContentsChanged to DesktopScrollPane
        *
        * @param f the internal frame to flag as "contents changed"
        */
      public void flagContentsChanged(JInternalFrame f) {
            desktopScrollpane.flagContentsChanged(f);
      }


      /** 
        * propogates resizeDesktop to DesktopScrollPane
        */
      public void resizeDesktop() {
            desktopScrollpane.resizeDesktop();
      }
      /** 
        * propogates revalidateViewport to DesktopScrollPane
        */
      public void revalidateViewport() {
            desktopScrollpane.revalidate();
      }
      /** 
        * propogates centerView to DesktopScrollPane
        *
        * @param f the internal frame to center the view about
        */
      public void centerView(BaseInternalFrame f) {
            desktopScrollpane.centerView(f);
      }
      /** 
        * propogates closeSelectedFrame to DesktopScrollPane
        */
      public void closeSelectedFrame() {
            desktopScrollpane.closeSelectedFrame();
      }

      /** 
        * propogates tileInternalFrames to DesktopScrollPane
        */
      public void tileInternalFrames() {
            desktopScrollpane.tileInternalFrames();
      }
      /** 
        * propogates cascadeInternalFrames to DesktopScrollPane
        */
      public void cascadeInternalFrames() {
            desktopScrollpane.cascadeInternalFrames();
      }
      /** 
        * propogates setAutoTile to DesktopScrollPane
        *
        * @param tileMode <code>true</code> indicates tile internal frames, 
        *         <code>false</code> indicates cascade internal frames
        */
      public void setAutoTile(boolean tileMode) {
            desktopScrollpane.setAutoTile(tileMode);
      }


      /**
        * propogates actionPerformed event to DesktopListener
        *
        * @param e the ActionEvent to propogate
        */
      public void actionPerformed(ActionEvent e) {
            dListener.actionPerformed(e);
      }


}