/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
//
// DesktopWindow is an AWT Canvas representing a VNC desktop.
//
// Methods on DesktopWindow are called from both the GUI thread and the thread
// which processes incoming RFB messages ("the RFB thread").  This means we
// need to be careful with synchronization here.
//

package vncviewer;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

class DesktopWindow extends Canvas implements Runnable {

  ////////////////////////////////////////////////////////////////////
  // The following methods are all called from the RFB thread

  public DesktopWindow(rfb.PixelFormat serverPF, CConn cc_) {
    cc = cc_;
    setSize(cc.cp.width, cc.cp.height);
    im = new PixelBufferImage(cc.cp.width, cc.cp.height, this);

    cursor = new rfb.Cursor();
    cursorBacking = new rfb.ManagedPixelBuffer();
  }

  // initGraphics() is needed because for some reason you can't call
  // getGraphics() on a newly-created awt Component.  It is called when the
  // DesktopWindow has actually been made visible so that getGraphics() ought
  // to work.

  public void initGraphics() { graphics = getGraphics(); }

  final public rfb.PixelFormat getPF() { return im.getPF(); }

  // Methods called from the RFB thread - these need to be synchronized
  // wherever they access data shared with the GUI thread.

  synchronized public void setCursor(int hotspotX, int hotspotY, int w, int h,
                                     byte[] data, byte[] mask) {
    // strictly we should use a mutex around this test since useLocalCursor
    // might be being altered by the GUI thread.  However it's only a single
    // boolean and it doesn't matter if we get the wrong value anyway.
    if (!cc.viewer.useLocalCursor.getValue()) return;
    if (!cursorAvailable) {
      //XDefineCursor(dpy, win(), noCursor);
      cursorAvailable = true;
    }

    hideLocalCursor();

    cursor.hotspotX = hotspotX;
    cursor.hotspotY = hotspotY;

    cursor.setSize(w, h);
    cursor.setPF(getPF());
    System.arraycopy(data, 0, cursor.data, 0, cursor.dataLen());
    System.arraycopy(mask, 0, cursor.mask, 0, cursor.maskLen());

    cursorBacking.setSize(w, h);
    cursorBacking.setPF(getPF());

    showLocalCursor();
  }

  // setColourMapEntries() changes some of the entries in the colourmap.
  // Unfortunately these messages are often sent one at a time, so we delay the
  // settings taking effect unless the whole colourmap has changed.  This is
  // because getting java to recalculate its internal translation table and
  // redraw the screen is expensive.

  synchronized public void setColourMapEntries(int firstColour, int nColours,
                                               int[] rgbs) {
    im.setColourMapEntries(firstColour, nColours, rgbs);
    if (nColours == 256) {
      im.updateColourMap();
      im.put(0, 0, im.width(), im.height(), graphics);
    } else {
      if (setColourMapEntriesTimerThread == null) {
        setColourMapEntriesTimerThread = new Thread(this);
        setColourMapEntriesTimerThread.start();
      }
    }
  }

  // resize() is called when the desktop has changed size
  synchronized public void resize() {
    vlog.debug("DesktopWindow.resize() called");
    int w = cc.cp.width;
    int h = cc.cp.height;
    hideLocalCursor();
    setSize(w, h);
    im.resize(w, h, this);
  }

  final void drawInvalidRect() {
    if (!invalidRect) return;
    int x = invalidLeft;
    int w = invalidRight - x;
    int y = invalidTop;
    int h = invalidBottom - y;
    invalidRect = false;

    synchronized (this) {
      im.put(x, y, w, h, graphics);
    }
  }

  final void invalidate(int x, int y, int w, int h) {
    if (invalidRect) {
      if (x < invalidLeft) invalidLeft = x;
      if (x + w > invalidRight) invalidRight = x + w;
      if (y < invalidTop) invalidTop = y;
      if (y + h > invalidBottom) invalidBottom = y + h;
    } else {
      invalidLeft = x;
      invalidRight = x + w;
      invalidTop = y;
      invalidBottom = y + h;
      invalidRect = true;
    }

    if ((invalidRight - invalidLeft) * (invalidBottom - invalidTop) > 100000)
      drawInvalidRect();
  }

  public void beginRect(int x, int y, int w, int h, int encoding) {
    invalidRect = false;
  }

  public void endRect(int x, int y, int w, int h, int encoding) {
    drawInvalidRect();
  }

  synchronized final public void fillRect(int x, int y, int w, int h, int pix)
  {
    if (overlapsCursor(x, y, w, h)) hideLocalCursor();
    im.fillRect(x, y, w, h, pix);
    invalidate(x, y, w, h);
    showLocalCursor();
  }

  synchronized final public void imageRect(int x, int y, int w, int h,
                                           byte[] pix, int offset) {
    if (overlapsCursor(x, y, w, h)) hideLocalCursor();
    im.imageRect(x, y, w, h, pix, offset);
    invalidate(x, y, w, h);
    showLocalCursor();
  }

  synchronized final public void copyRect(int x, int y, int w, int h,
                                          int srcX, int srcY) {
    if (overlapsCursor(x, y, w, h) || overlapsCursor(srcX, srcY, w, h))
      hideLocalCursor();
    im.copyRect(x, y, w, h, srcX, srcY);
    if (cc.viewer.fastCopyRect.getValue()) {
      graphics.setClip(0, 0, im.width(), im.height());
      graphics.copyArea(srcX, srcY, w, h, x-srcX, y-srcY);
    } else {
      invalidate(x, y, w, h);
    }
  }


  // mutex MUST be held when overlapsCursor() is called
  final boolean overlapsCursor(int x, int y, int w, int h) {
    return (x < cursorBackingX + cursorBacking.width() &&
            y < cursorBackingY + cursorBacking.height() &&
            x+w > cursorBackingX && y+h > cursorBackingY);
  }


  ////////////////////////////////////////////////////////////////////
  // The following methods are all called from the GUI thread

  synchronized void resetLocalCursor() {
    hideLocalCursor();
    //XDefineCursor(dpy, win(), dotCursor);
    cursorAvailable = false;
  }

  synchronized public Dimension getPreferredSize() {
    return new Dimension(im.width(), im.height());
  }

  synchronized public Dimension getMinimumSize() {
    return new Dimension(im.width(), im.height());
  }

  public void update(Graphics g) {
    System.err.println("update called");
  }

  synchronized public void paint(Graphics g) {
    g.drawImage(im.image, 0, 0, null);
  }


  String oldContents = "";
  
  synchronized public void checkClipboard() {
    if (ClipboardDialog.systemClipboard != null &&
        cc.viewer.sendClipboard.getValue()) {
      Transferable t = ClipboardDialog.systemClipboard.getContents(this);
      if ((t != null) && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        try {
          String newContents = (String) t.getTransferData(DataFlavor.stringFlavor);
          if (newContents != null && !newContents.equals(oldContents)) {
            cc.writeClientCutText(newContents);
            oldContents = newContents;
            cc.clipboardDialog.setContents(newContents);
          }
        } catch (Exception e) {
          System.out.println("Exception getting clipboard data: " + e.getMessage());
        }
      }
    }
  }

  // handleEvent().  Called by the GUI thread and calls on to CConn as
  // appropriate.  CConn is responsible for synchronizing the writing of key
  // and pointer events with other protocol messages.

  public boolean handleEvent(Event event) {
    switch (event.id) {
    case Event.GOT_FOCUS:
      checkClipboard();
      break;
    case Event.MOUSE_MOVE:
    case Event.MOUSE_DRAG:
      if (!cc.viewer.viewOnly.getValue())
        cc.writePointerEvent(event);
      // - If local cursor rendering is enabled then use it
      synchronized (this) {
        if (cursorAvailable) {
          // - Render the cursor!
          if (event.x != cursorPosX || event.y != cursorPosY) {
            hideLocalCursor();
            if (event.x >= 0 && event.x < im.width() &&
                event.y >= 0 && event.y < im.height()) {
              cursorPosX = event.x;
              cursorPosY = event.y;
              showLocalCursor();
            }
          }
        }
      }
      lastX = event.x;
      lastY = event.y;
      break;

    case Event.MOUSE_DOWN:
    case Event.MOUSE_UP:
      if (!cc.viewer.viewOnly.getValue())
        cc.writePointerEvent(event);
      lastX = event.x;
      lastY = event.y;
      break;

    case Event.KEY_ACTION:
      if (event.key == Event.F8) {
        cc.showMenu(lastX, lastY);
        break;
      }
      // drop through

    case Event.KEY_PRESS:
      // The AWT's release events are not useful since they don't correspond to
      // the press [ Try pressing shift, pressing another key, releasing shift,
      // then releasing the other key - you'll find that you get an AWT press
      // event and a release event, but the key fields will be different.
      // Without intimate knowledge of the keyboard layout being used, there's
      // no way you can correlate the two events. ]
      if (!cc.viewer.viewOnly.getValue())
        cc.writeKeyEvent(event);
        break;
    }

    return super.handleEvent(event);
  }


  ////////////////////////////////////////////////////////////////////
  // The following methods are called from both RFB and GUI threads

  // Note that mutex MUST be held when hideLocalCursor() and showLocalCursor()
  // are called.

  private void hideLocalCursor() {
    // - Blit the cursor backing store over the cursor
    if (cursorVisible) {
      cursorVisible = false;
      im.imageRect(cursorBackingX, cursorBackingY, cursorBacking.width(),
                   cursorBacking.height(), cursorBacking.data, 0);
      im.put(cursorBackingX, cursorBackingY, cursorBacking.width(),
             cursorBacking.height(), graphics);
    }
  }

  private void showLocalCursor() {
    if (cursorAvailable && !cursorVisible) {
      if (!im.getPF().equal(cursor.getPF()) ||
          cursor.width() == 0 || cursor.height() == 0) {
        vlog.debug("attempting to render invalid local cursor");
        cursorAvailable = false;
        return;
      }
      cursorVisible = true;

      int cursorLeft = cursorPosX - cursor.hotspotX;
      int cursorTop = cursorPosY - cursor.hotspotY;
      int cursorRight = cursorLeft + cursor.width();
      int cursorBottom = cursorTop + cursor.height();

      int x = (cursorLeft >= 0 ? cursorLeft : 0);
      int y = (cursorTop >= 0 ? cursorTop : 0);
      int w = ((cursorRight < im.width() ? cursorRight : im.width()) - x);
      int h = ((cursorBottom < im.height() ? cursorBottom : im.height()) - y);

      cursorBackingX = x;
      cursorBackingY = y;
      cursorBacking.setSize(w, h);
      
      for (int j = 0; j < h; j++)
        System.arraycopy(im.data, (y+j) * im.width() + x,
                         cursorBacking.data, j*w, w);

      im.maskRect(cursorLeft, cursorTop, cursor.width(), cursor.height(),
                  cursor.data, cursor.mask);
      im.put(x, y, w, h, graphics);
    }
  }


  // run() is executed by the setColourMapEntriesTimerThread - it sleeps for
  // 100ms before actually updating the colourmap.
  public void run() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {}
    synchronized (this) {
      im.updateColourMap();
      im.put(0, 0, im.width(), im.height(), graphics);
      setColourMapEntriesTimerThread = null;
    }
  }

  // access to cc by different threads is specified in CConn
  CConn cc;

  // access to the following must be synchronized:
  PixelBufferImage im;
  Graphics graphics;
  Thread setColourMapEntriesTimerThread;

  rfb.Cursor cursor;
  boolean cursorVisible;     // Is cursor currently rendered?
  boolean cursorAvailable;   // Is cursor available for rendering?
  int cursorPosX, cursorPosY;
  rfb.ManagedPixelBuffer cursorBacking;
  int cursorBackingX, cursorBackingY;

  // the following are only ever accessed by the RFB thread:
  boolean invalidRect;
  int invalidLeft, invalidRight, invalidTop, invalidBottom;

  // the following are only ever accessed by the GUI thread:
  int lastX, lastY;

  static rfb.LogWriter vlog = new rfb.LogWriter("DesktopWindow");
}
