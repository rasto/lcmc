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

class InfoDialog extends vncviewer.Dialog {

  public InfoDialog() { 
    super(false);
    setTitle("VNC connection info");
    Panel p1 = new Panel();
    p1.setLayout(new GridBagLayout());
    desktopName   = addItem(p1, "Desktop Name:");
    serverHost    = addItem(p1, "Host:");
    desktopSize   = addItem(p1, "Size:");
    pixelFormat   = addItem(p1, "Pixel Format:");
    serverDefault = addItem(p1, "Server Default:");
    reqEncoding   = addItem(p1, "Requested Encoding:");
    lastEncoding  = addItem(p1, "Last Used Encoding:");
    lineSpeed     = addItem(p1, "Line Speed Estimate:");
    protocol      = addItem(p1, "Protocol Version:");
    security      = addItem(p1, "Security Method:");
    encryption    = addItem(p1, "Encryption:");
    add("Center", p1);

    Panel p2 = new Panel();
    okButton = new Button("OK");
    p2.add(okButton);
    add("South", p2);

    pack();
  }
  private int nextRow = 0;
  protected Label addItem(Panel p, String tag)
  {
  	GridBagConstraints gbc = new GridBagConstraints();
  	gbc.gridy = nextRow++;
  	gbc.gridx = 0;
  	gbc.anchor = GridBagConstraints.WEST;
  	Label result;
  	p.add(new Label(tag), gbc);
	gbc.gridx = 1;
  	p.add(result=new Label(""),gbc);
  	return result;
  }
  
  public void setDetails(String desktopName_, String serverHost_, String desktopSize_,
                         String pixelFormat_, String serverDefault_,
                         String reqEncoding_, String lastEncoding_,
                         String lineSpeed_,   String protocol_,
                         String security_,    String encryption_)
  {
  	desktopName.setText(desktopName_);
  	serverHost.setText(serverHost_);
  	desktopSize.setText(desktopSize_);
  	pixelFormat.setText(pixelFormat_);
  	serverDefault.setText(serverDefault_);
  	reqEncoding.setText(reqEncoding_);
  	lastEncoding.setText(lastEncoding_);
  	lineSpeed.setText(lineSpeed_);
  	protocol.setText(protocol_);
  	security.setText(security_);
  	encryption.setText(encryption_);
  	pack();
  }

  public boolean action(Event event, Object arg) {
    if (event.target == okButton) {
      ok = true;
      endDialog();
    }
    return true;
  }

//  TextArea infoLabel;
  Label desktopName, serverHost, desktopSize,
        pixelFormat, serverDefault,
        reqEncoding, lastEncoding,
        lineSpeed,   protocol,
        security,    encryption; 
  Button okButton;
}
