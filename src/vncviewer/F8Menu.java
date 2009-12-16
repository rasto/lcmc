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
package vncviewer;

import java.awt.*;
import java.awt.event.*;

public class F8Menu extends PopupMenu implements ActionListener {
  public F8Menu(CConn cc_) {
    super("VNC Menu");
    cc = cc_;
    exit       = addMenuItem("Exit viewer");
    addSeparator();
    clipboard  = addMenuItem("Clipboard...");
    addSeparator();
    f8         = addMenuItem("Send F8");
    ctrlAltDel = addMenuItem("Send Ctrl-Alt-Del");
    addSeparator();
    refresh    = addMenuItem("Refresh screen");
    addSeparator();
    newConn    = addMenuItem("New connection...");
    options    = addMenuItem("Options...");
    info       = addMenuItem("Connection info...");
    about      = addMenuItem("About VNCviewer...");
    addSeparator();
    dismiss    = addMenuItem("Dismiss menu");
  }

  MenuItem addMenuItem(String str) {
    MenuItem item = new MenuItem(str);
    item.addActionListener(this);
    add(item);
    return item;
  }

  boolean actionMatch(ActionEvent ev, MenuItem item) {
    return ev.getActionCommand().equals(item.getActionCommand());
  }

  public void actionPerformed(ActionEvent ev) {
    if (actionMatch(ev, exit)) {
      cc.close();
    } else if (actionMatch(ev, clipboard)) {
      cc.clipboardDialog.showDialog();
    } else if (actionMatch(ev, f8)) {
      cc.writeKeyEvent(rfb.Keysyms.F8, true);
      cc.writeKeyEvent(rfb.Keysyms.F8, false);
    } else if (actionMatch(ev, ctrlAltDel)) {
      cc.writeKeyEvent(rfb.Keysyms.Control_L, true);
      cc.writeKeyEvent(rfb.Keysyms.Alt_L, true);
      cc.writeKeyEvent(rfb.Keysyms.Delete, true);
      cc.writeKeyEvent(rfb.Keysyms.Delete, false);
      cc.writeKeyEvent(rfb.Keysyms.Alt_L, false);
      cc.writeKeyEvent(rfb.Keysyms.Control_L, false);
    } else if (actionMatch(ev, refresh)) {
      cc.refresh();
    } else if (actionMatch(ev, newConn)) {
      VNCViewer.newViewer(cc.viewer);
    } else if (actionMatch(ev, options)) {
      cc.options.showDialog();
    } else if (actionMatch(ev, info)) {
      cc.showInfo();
    } else if (actionMatch(ev, about)) {
      cc.about.showDialog();
    } else if (actionMatch(ev, dismiss)) {
    }
  }

  CConn cc;
  MenuItem exit, clipboard, f8, ctrlAltDel, refresh;
  MenuItem newConn, options, info, about, dismiss;
}
