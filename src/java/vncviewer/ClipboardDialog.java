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
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

class ClipboardDialog extends vncviewer.Dialog {

  public ClipboardDialog(CConn cc_) {
    super(false);
    cc = cc_;
    setTitle("VNC clipboard");
    textArea = new TextArea(5,50);
    add("Center", textArea);

    Panel pb = new Panel();
    clearButton = new Button("Clear");
    pb.add(clearButton);
    sendButton = new Button("Send to VNC server");
    pb.add(sendButton);
    cancelButton = new Button("Cancel");
    pb.add(cancelButton);
    add("South", pb);

    pack();
  }

  static Clipboard systemClipboard;
  static {
    try {
      systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    } catch (Exception e) { }
  }



  public void initDialog() {
    textArea.setText(current);
    textArea.selectAll();
  }
  
  public void setContents(String str) {
    current = str;
    textArea.setText(str);
    textArea.selectAll();
  }

  public void serverCutText(String str) {
    setContents(str);    
    if (systemClipboard != null) {
      StringSelection ss = new StringSelection(str);
      systemClipboard.setContents(ss, ss);
    }
  }

  public void setSendingEnabled(boolean b) {
    sendButton.setEnabled(b);
  }

  public boolean action(Event event, Object arg) {
    if (event.target == clearButton) {
      current = "";
      textArea.setText(current);
    } else if (event.target == sendButton) {
      ok = true;
      current = textArea.getText();
      cc.writeClientCutText(current);
      endDialog();
    } else if (event.target == cancelButton) {
      ok = false;
      endDialog();
    }
    return true;
  }

  CConn cc;
  String current;
  TextArea textArea;
  Button clearButton, sendButton, cancelButton;
}
