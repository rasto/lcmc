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
// CConn
//
// Methods on CConn are called from both the GUI thread and the thread which
// processes incoming RFB messages ("the RFB thread").  This means we need to
// be careful with synchronization here.
//
// Any access to writer() must not only be synchronized, but we must also make
// sure that the connection is in RFBSTATE_NORMAL.  We are guaranteed this for
// any code called after serverInit() has been called.  Since the DesktopWindow
// isn't created until then, any methods called only from DesktopWindow can
// assume that we are in RFBSTATE_NORMAL.


package vncviewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Frame;
import java.awt.ScrollPane;

import rfb.SecTypes;

class ViewportFrame extends Frame {
  public ViewportFrame(String name, CConn cc_) {
    super(name);
    cc = cc_;
    sp = new ScrollPane();
    add(sp);
  }

  public void addChild(Component child) {
    sp.add(child);
  }

  public void setGeometry(int x, int y, int w, int h) {
    sp.setSize(w, h);
    pack();
    setLocation(x, y);
  }

  public boolean handleEvent(Event event) { 
    if (event.id == Event.WINDOW_DESTROY) {
      cc.close();
    }   
    return super.handleEvent(event);
  }

  CConn cc;
  ScrollPane sp;
}

public class CConn extends rfb.CConnection
  implements rfb.UserPasswdGetter, OptionsDialogCallback
{
  ////////////////////////////////////////////////////////////////////
  // The following methods are all called from the RFB thread

  public CConn(VNCViewer viewer_) {
    viewer = viewer_;
    // Set the rest of the defaults
    currentEncoding = rfb.Encodings.ZRLE;
    lastUsedEncoding = rfb.Encodings.max;
    fullColour = viewer.fullColour.getValue();
    autoSelect = viewer.autoSelect.getValue();
    shared = viewer.shared.getValue();
    options = new OptionsDialog(this);
    about = new AboutDialog();
    info = new InfoDialog();
    clipboardDialog = new ClipboardDialog(this);

    setShared(shared);
    addSecType(rfb.SecTypes.none);
    addSecType(rfb.SecTypes.vncAuth);
    String encStr = viewer.preferredEncoding.getValue();
    if (encStr != null) {
      int encNum = rfb.Encodings.num(encStr);
      if (encNum != -1) {
        currentEncoding = encNum;
        autoSelect = false;
      }
    }
    cp.supportsDesktopResize = true;
    cp.supportsLocalCursor = viewer.useLocalCursor.getValue();
    menu = new F8Menu(this);
  }

  // init() gets the name of the VNC server (if necessary), connects to it and
  // initiates the RFB protocol.  It returns false if no server was entered in
  // the dialog box.

  public boolean init(java.net.Socket sock_, String vncServerName,
                      boolean alwaysShowServerDialog)
    throws java.io.IOException
  {
    sock = sock_;

    if (sock != null) {
      String name = sock.getInetAddress().getHostAddress()+"::"+sock.getPort();
      vlog.info("Accepted connection from "+name);
    } else {
      if (alwaysShowServerDialog || vncServerName == null) {
        ServerDialog dlg = new ServerDialog(options, about, vncServerName);
        if (!dlg.showDialog() || dlg.server.getText().equals(""))
          return false;
        vncServerName = dlg.server.getText();
      }
      serverHost = rfb.Hostname.getHost(vncServerName);
      serverPort = rfb.Hostname.getPort(vncServerName);

      sock = new java.net.Socket(serverHost, serverPort);
      vlog.info("connected to host "+serverHost+" port "+serverPort);
    }

    setServerName(sock.getInetAddress().getHostAddress()+"::"+sock.getPort());
    jis = new rdr.JavaInStream(sock.getInputStream());
    jos = new rdr.JavaOutStream(sock.getOutputStream());
    setStreams(jis, jos);
    initialiseProtocol();
    return true;
  }

  // removeWindow() destroys the viewport and desktop windows.

  void removeWindow() {
    if (viewport != null)
      viewport.dispose();
    viewport = null;
  } 

  // getUserPasswd() is called by the CSecurity object when it needs us to read
  // a password from the user.

  public boolean getUserPasswd(StringBuffer user, StringBuffer passwd) {
    String title = ("VNC Authentication ["
                    + getCurrentCSecurity().description() + "]");
    PasswdDialog dlg = new PasswdDialog(title, (user == null), (passwd == null));
    if (!dlg.showDialog()) return false;
    if (user != null)
      user.append(dlg.userEntry.getText());
    if (passwd != null)
      passwd.append(dlg.passwdEntry.getText());
    return true;
  }

  // CConnection callback methods

  // getCSecurity() gets the appropriate CSecurity object for the security
  // types which we support.

  public rfb.CSecurity getCSecurity(int secType) {
    switch (secType) {
    case rfb.SecTypes.none:
      return new rfb.CSecurityNone();
    case rfb.SecTypes.vncAuth:
      return new rfb.CSecurityVncAuth(this);
    default:
      throw new rfb.Exception("Unsupported secType?");
    }
  }

  // serverInit() is called when the serverInit message has been received.  At
  // this point we create the desktop window and display it.  We also tell the
  // server the pixel format and encodings to use and request the first update.
  public void serverInit() {
    super.serverInit();
    serverPF = cp.pf();
    desktop = new DesktopWindow(serverPF, this);
    desktop.add(menu);
    fullColourPF = desktop.getPF();
    if (!serverPF.trueColour)
      fullColour = true;
    recreateViewport();
    formatChange = encodingChange = true;
    requestNewUpdate();
  }

  // setDesktopSize() is called when the desktop size changes (including when
  // it is set initially).
  public void setDesktopSize(int w, int h) {
    super.setDesktopSize(w,h);
    if (desktop != null) {
      desktop.resize();
      recreateViewport();
    }
  }

  // framebufferUpdateStart() and framebufferUpdateEnd() are called at the
  // beginning and end of an update.  We use the speed of the connection,
  // computed within beginRect() and endRect() to select the format and
  // encoding appropriately, and then request another incremental update.
  public void framebufferUpdateStart() {
  }
  public void framebufferUpdateEnd() {
    if (autoSelect)
      autoSelectFormatAndEncoding();
    requestNewUpdate();
  }

  // The rest of the callbacks are fairly self-explanatory...

  public void setColourMapEntries(int firstColour, int nColours, int[] rgbs) {
    desktop.setColourMapEntries(firstColour, nColours, rgbs);
  }

  public void bell() { desktop.getToolkit().beep(); }

  public void serverCutText(String str) {
    if (viewer.acceptClipboard.getValue())
      clipboardDialog.serverCutText(str);
  }

  public void beginRect(int x, int y, int w, int h, int encoding) {
    jis.startTiming();
    desktop.beginRect(x, y, w, h, encoding);
  }

  public void endRect(int x, int y, int w, int h, int encoding) {
    desktop.endRect(x, y, w, h, encoding);
    jis.stopTiming();
    if ( encoding <= rfb.Encodings.max )
      lastUsedEncoding = encoding;
  }

  public void fillRect(int x, int y, int w, int h, int p) {
    desktop.fillRect(x, y, w, h, p);
  }
  public void imageRect(int x, int y, int w, int h, byte[] p, int offset) {
    desktop.imageRect(x, y, w, h, p, offset);
  }
  public void copyRect(int x, int y, int w, int h, int sx, int sy) {
    desktop.copyRect(x, y, w, h, sx, sy);
  }

  public void setCursor(int hotspotX, int hotspotY, int w, int h,
                        byte[] data, byte[] mask) {
    desktop.setCursor(hotspotX, hotspotY, w, h, data, mask);
  }


  // recreateViewport() recreates our top-level window.  This seems to be
  // better than attempting to resize the existing window, at least with
  // various X window managers.

  void recreateViewport()
  {
    if (viewport != null) viewport.dispose();
    viewport = new ViewportFrame("VNC: "+cp.name, this);
    viewport.addChild(desktop);
    reconfigureViewport();
    viewport.show();
    desktop.initGraphics();
  }

  void reconfigureViewport()
  {
    //viewport->setMaxSize(cp.width, cp.height);
    int w = cp.width + 4; // 4 is due to bizarre ScrollPane border
    int h = cp.height + 4;
    Dimension dpySize = viewport.getToolkit().getScreenSize();
    int wmDecorationWidth = 6;
    int wmDecorationHeight = 24;
    if (w + wmDecorationWidth >= dpySize.width)
      w = dpySize.width - wmDecorationWidth;
    if (h + wmDecorationHeight >= dpySize.height)
      h = dpySize.height - wmDecorationHeight;

    int x = (dpySize.width - w - wmDecorationWidth) / 2;
    int y = (dpySize.height - h - wmDecorationHeight)/2;

    viewport.setGeometry(x, y, w, h);
  }

  // autoSelectFormatAndEncoding() chooses the format and encoding appropriate
  // to the connection speed:
  //   Above 3Mbps, switch to hextile
  //   Below 1.5Mbps, switch to ZRLE
  //   Above 1Mbps, switch to full colour mode
  void autoSelectFormatAndEncoding() {
    long kbitsPerSecond = jis.kbitsPerSecond();
    int newEncoding = currentEncoding;

    if (kbitsPerSecond > 3000) {
      newEncoding = rfb.Encodings.hextile;
    } else if (kbitsPerSecond < 1500) {
      newEncoding = rfb.Encodings.ZRLE;
    }

    if (newEncoding != currentEncoding) {
      vlog.info("Throughput "+kbitsPerSecond+" kbit/s - changing to "+
                rfb.Encodings.name(newEncoding)+" encoding");
      currentEncoding = newEncoding;
      encodingChange = true;
    }

//      if (kbitsPerSecond > 1000) {
//        if (!fullColour) {
//          vlog.info("Throughput "+kbitsPerSecond+
//                    " kbit/s - changing to full colour");
//          fullColour = true;
//          formatChange = true;
//        }
//      }
  }

  // requestNewUpdate() requests an update from the server, having set the
  // format and encoding appropriately.
  void requestNewUpdate()
  {
    if (formatChange) {
      if (fullColour) {
        //desktop.setPF(fullColourPF);
      } else {
        //desktop.setPF(rfb.PixelFormat(8,6,0,1,3,3,3,4,2,0));
      }
      String str = desktop.getPF().print();
      vlog.info("Using pixel format "+str);
      cp.setPF(desktop.getPF());
      synchronized (this) {
        writer().writeSetPixelFormat(cp.pf());
      }
    }
    checkEncodings();
    synchronized (this) {
      writer().writeFramebufferUpdateRequest(0, 0, cp.width, cp.height,
                                             !formatChange);
    }
    formatChange = false;
  }


  ////////////////////////////////////////////////////////////////////
  // The following methods are all called from the GUI thread

  // close() closes the socket, thus waking up the RFB thread.
  public void close() {
    try {
      shuttingDown = true;
      sock.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Menu callbacks.  These are guaranteed only to be called after serverInit()
  // has been called, since the menu is only accessible from the DesktopWindow

  public void showMenu(int x, int y) {
    menu.show(desktop, x, y);
  }

  public void showInfo() {
  	info.setDetails(cp.name, serverHost+":"+serverPort, cp.width+"x"+cp.height,
  	                cp.pf().print(), serverPF.print(),
		            rfb.Encodings.name(currentEncoding),
		            rfb.Encodings.name(lastUsedEncoding),
		            jis.kbitsPerSecond()+" kbit/s",		            cp.majorVersion+"."+cp.minorVersion,
                    SecTypes.name(getCurrentCSecurity().getType()),
                    getCurrentCSecurity().description());
    info.showDialog();
  }

  synchronized public void refresh() {
    writer().writeFramebufferUpdateRequest(0, 0, cp.width, cp.height, false);
  }


  // OptionsDialogCallback.  setOptions() sets the options dialog's checkboxes
  // etc to reflect our flags.  getOptions() sets our flags according to the
  // options dialog's checkboxes.  They are both called from the GUI thread.
  // Some of the flags are also accessed by the RFB thread.  I believe that
  // reading and writing boolean and int values in java is atomic, so there is
  // no need for synchronization.

  public void setOptions() {
    options.autoSelect.setState(autoSelect);
    options.fullColour.setState(false/*fullColour*/);
    options.veryLowColour.setState(false/*!fullColour && lowColourLevel==0*/);
    options.lowColour.setState(false/*!fullColour && lowColourLevel == 1*/);
    options.mediumColour.setState(true/*!fullColour && lowColourLevel == 2*/);
    options.fullColour.setEnabled(false);
    options.veryLowColour.setEnabled(false);
    options.lowColour.setEnabled(false);

    options.zrle.setState(currentEncoding == rfb.Encodings.ZRLE);
    options.hextile.setState(currentEncoding == rfb.Encodings.hextile);
    options.raw.setState(currentEncoding == rfb.Encodings.raw);
    options.viewOnly.setState(viewer.viewOnly.getValue());
    options.acceptClipboard.setState(viewer.acceptClipboard.getValue());
    options.sendClipboard.setState(viewer.sendClipboard.getValue());
    if (state() == RFBSTATE_NORMAL)
      options.shared.setEnabled(false);
    else
      options.shared.setState(shared);
    options.useLocalCursor.setState(viewer.useLocalCursor.getValue());
    options.fastCopyRect.setState(viewer.fastCopyRect.getValue());
  }

  public void getOptions() {
    autoSelect = options.autoSelect.getState();
//      if (fullColour != options.fullColour.getState())
//        formatChange = true;
    fullColour = options.fullColour.getState();
    int newEncoding = (options.zrle.getState() ? rfb.Encodings.ZRLE :
                       options.hextile.getState() ? rfb.Encodings.hextile :
                       rfb.Encodings.raw);
    if (newEncoding != currentEncoding) {
      currentEncoding = newEncoding;
      encodingChange = true;
    }
    viewer.viewOnly.setParam(options.viewOnly.getState());
    viewer.acceptClipboard.setParam(options.acceptClipboard.getState());
    viewer.sendClipboard.setParam(options.sendClipboard.getState());
    clipboardDialog.setSendingEnabled(viewer.sendClipboard.getValue());
    shared = options.shared.getState();
    setShared(shared);
    viewer.useLocalCursor.setParam(options.useLocalCursor.getState());
    if (cp.supportsLocalCursor != viewer.useLocalCursor.getValue()) {
      cp.supportsLocalCursor = viewer.useLocalCursor.getValue();
      encodingChange = true;
      if (desktop != null)
        desktop.resetLocalCursor();
    }
    viewer.fastCopyRect.setParam(options.fastCopyRect.getState());

    checkEncodings();
  }


  // writeClientCutText() is called from the clipboard dialog
  synchronized public void writeClientCutText(String str) {
    if (state() != RFBSTATE_NORMAL) return;
    writer().writeClientCutText(str);
  }

  synchronized public void writeKeyEvent(int keysym, boolean down) {
    if (state() != RFBSTATE_NORMAL) return;
    writer().writeKeyEvent(keysym, down);
  }

  synchronized public void writeKeyEvent(Event ev) {
    if (ev.id != Event.KEY_PRESS && ev.id != Event.KEY_ACTION)
      return;

    int keysym;

    if (ev.id == Event.KEY_PRESS) {
      vlog.debug("key press "+ev.key);
      if (ev.key < 32) {
        // if the ctrl modifier key is down, send the equivalent ASCII since we
        // will send the ctrl modifier anyway

        if ((ev.modifiers & Event.CTRL_MASK) != 0) {
          keysym = ev.key + 96;
          if (keysym == 127) keysym = 95;
        } else {
          switch (ev.key) {
          case Event.BACK_SPACE: keysym = rfb.Keysyms.BackSpace; break;
          case Event.TAB:        keysym = rfb.Keysyms.Tab; break;
          case Event.ENTER:      keysym = rfb.Keysyms.Return; break;
          case Event.ESCAPE:     keysym = rfb.Keysyms.Escape; break;
          default: return;
          }
        }

      } else if (ev.key == 127) {
        keysym = rfb.Keysyms.Delete;

      } else {
        keysym = rfb.UnicodeToKeysym.translate(ev.key);
        if (keysym == -1)
          return;
      }

    } else {
      // KEY_ACTION
      vlog.debug("key action "+ev.key);
      switch (ev.key) {
      case Event.HOME:         keysym = rfb.Keysyms.Home; break;
      case Event.END:          keysym = rfb.Keysyms.End; break;
      case Event.PGUP:         keysym = rfb.Keysyms.Page_Up; break;
      case Event.PGDN:         keysym = rfb.Keysyms.Page_Down; break;
      case Event.UP:           keysym = rfb.Keysyms.Up; break;
      case Event.DOWN:         keysym = rfb.Keysyms.Down; break;
      case Event.LEFT:         keysym = rfb.Keysyms.Left; break;
      case Event.RIGHT:        keysym = rfb.Keysyms.Right; break;
      case Event.F1:           keysym = rfb.Keysyms.F1; break;
      case Event.F2:           keysym = rfb.Keysyms.F2; break;
      case Event.F3:           keysym = rfb.Keysyms.F3; break;
      case Event.F4:           keysym = rfb.Keysyms.F4; break;
      case Event.F5:           keysym = rfb.Keysyms.F5; break;
      case Event.F6:           keysym = rfb.Keysyms.F6; break;
      case Event.F7:           keysym = rfb.Keysyms.F7; break;
      case Event.F8:           keysym = rfb.Keysyms.F8; break;
      case Event.F9:           keysym = rfb.Keysyms.F9; break;
      case Event.F10:          keysym = rfb.Keysyms.F10; break;
      case Event.F11:          keysym = rfb.Keysyms.F11; break;
      case Event.F12:          keysym = rfb.Keysyms.F12; break;
      case Event.PRINT_SCREEN: keysym = rfb.Keysyms.Print; break;
      case Event.PAUSE:        keysym = rfb.Keysyms.Pause; break;
      case Event.INSERT:       keysym = rfb.Keysyms.Insert; break;
      default: return;
      }
    }

    writeModifiers(ev.modifiers);
    writeKeyEvent(keysym, true);
    writeKeyEvent(keysym, false);
    writeModifiers(0);
  }


  synchronized public void writePointerEvent(Event ev) {
    if (state() != RFBSTATE_NORMAL) return;

    switch (ev.id) {
    case Event.MOUSE_DOWN:
      buttonMask = 1;
      if ((ev.modifiers & Event.ALT_MASK) != 0) buttonMask = 2;
      if ((ev.modifiers & Event.META_MASK) != 0) buttonMask = 4;
      break;
    case Event.MOUSE_UP:
      buttonMask = 0;
      break;
    }

    writeModifiers(ev.modifiers & ~Event.ALT_MASK & ~Event.META_MASK);

    if (ev.x < 0) ev.x = 0;
    if (ev.x > cp.width-1) ev.x = cp.width-1;
    if (ev.y < 0) ev.y = 0;
    if (ev.y > cp.height-1) ev.y = cp.height-1;

    writer().writePointerEvent(ev.x, ev.y, buttonMask);

    if (buttonMask == 0) writeModifiers(0);
  }


  void writeModifiers(int m) {
    if ((m & Event.SHIFT_MASK) != (pressedModifiers & Event.SHIFT_MASK))
      writeKeyEvent(rfb.Keysyms.Shift_L, (m & Event.SHIFT_MASK) != 0);
    if ((m & Event.CTRL_MASK) != (pressedModifiers & Event.CTRL_MASK))
      writeKeyEvent(rfb.Keysyms.Control_L, (m & Event.CTRL_MASK) != 0);
    if ((m & Event.ALT_MASK) != (pressedModifiers & Event.ALT_MASK))
      writeKeyEvent(rfb.Keysyms.Alt_L, (m & Event.ALT_MASK) != 0);
    if ((m & Event.META_MASK) != (pressedModifiers & Event.META_MASK))
      writeKeyEvent(rfb.Keysyms.Meta_L, (m & Event.META_MASK) != 0);
    pressedModifiers = m;
  }


  ////////////////////////////////////////////////////////////////////
  // The following methods are called from both RFB and GUI threads

  // checkEncodings() sends a setEncodings message if one is needed.
  synchronized private void checkEncodings() {
    if (encodingChange && state() == RFBSTATE_NORMAL) {
      vlog.info("Using "+rfb.Encodings.name(currentEncoding)+" encoding");
      writer().writeSetEncodings(currentEncoding, true);
      encodingChange = false;
    }
  }

  // the following never change so need no synchronization:
  String serverHost;
  int serverPort;
  java.net.Socket sock;
  rdr.JavaInStream jis;
  rdr.JavaOutStream jos;


  // viewer object is only ever accessed by the GUI thread so needs no
  // synchronization (except for one test in DesktopWindow - see comment
  // there).
  VNCViewer viewer;

  // access to desktop by different threads is specified in DesktopWindow
  DesktopWindow desktop;

  // the following need no synchronization:

  rfb.PixelFormat serverPF;
  ViewportFrame viewport;
  rfb.PixelFormat fullColourPF;

  // shuttingDown is set by the GUI thread and only ever tested by the RFB
  // thread after the window has been destroyed.
  boolean shuttingDown;

  // reading and writing int and boolean is atomic in java, so no
  // synchronization of the following flags is needed:
  int currentEncoding, lastUsedEncoding;
  
  boolean fullColour;
  boolean autoSelect;
  boolean shared;
  boolean formatChange;
  boolean encodingChange;
  boolean sameMachine;

  // All menu, options, about and info stuff is done in the GUI thread (apart
  // from when constructed).
  F8Menu menu;
  OptionsDialog options;
  AboutDialog about;
  InfoDialog info;

  // clipboard sync issues?
  ClipboardDialog clipboardDialog;

  // the following are only ever accessed by the GUI thread:
  int buttonMask;
  int pressedModifiers;
  
  static rfb.LogWriter vlog = new rfb.LogWriter("CConn");
}
