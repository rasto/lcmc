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

class ServerDialog extends vncviewer.Dialog {

  public ServerDialog(OptionsDialog options_,
                      AboutDialog about_, String defaultServerName) {
    super(true);
    setTitle("VNC Viewer : Connection Details");
    options = options_;
    about = about_;
    GridBagLayout gbl = new GridBagLayout();
    setLayout(gbl);
    // Add components
    addComponent(new Label("Server:", Label.RIGHT),
                 0, 0, 1, 1, 0, new Insets(4, 0, 0, 0));
    addComponent(server=new TextField(15),
                 1, 0, 2, 1, 0, new Insets(4, 0, 0, 0));
    addComponent(new Label("Encryption:", Label.RIGHT),
                 0, 1, 1, 1, 0, new Insets(2, 0, 0, 0));
    addComponent(encryption = new Choice(),
                 1, 1, 2, 1, 0, new Insets(2, 0, 0, 0));
    addComponent(optionsButton = new Button("Options..."),
		       	 1, 2, 1, 1, 0, new Insets(8, 4, 4, 4));
	  addComponent(aboutButton = new Button("About..."),
	             0, 2, 1, 1, 35, new Insets(8, 4, 4, 4));
    addComponent(okButton = new Button("OK"),
            	 2, 2, 1, 1, 40, new Insets(8, 4, 4, 4));
    addComponent(cancelButton = new Button("Cancel"),
                 3, 2, 1, 1, 30, new Insets(8, 4, 4, 4));
    // Set default values
    if (defaultServerName != null) server.setText(defaultServerName);
	  encryption.add("Not supported");
	  encryption.select(0);
    encryption.setEnabled(false);
    pack();
  }

 
  protected void addComponent(Component comp, int x, int y, int w, int h, int extra, Insets padding )
  {
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = x;
      gbc.gridy = y;
      gbc.gridwidth = w;
      gbc.gridheight = h;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.insets = padding;
      gbc.ipadx = extra;
      gbc.weightx = 1;
      gbc.weighty = 1;
      add(comp, gbc);
  }
  
  synchronized public boolean action(Event event, Object arg) {
    if (event.target == okButton || event.target == server) {
      ok = true;
      endDialog();
    } else if (event.target == cancelButton) {
      ok = false;
      endDialog();
    } else if (event.target == optionsButton) {
      options.showDialog();
    } else if (event.target == aboutButton) {
      about.showDialog();
    }
    return true;
  }

  TextField server;
  Choice encryption;
  Button aboutButton, optionsButton, okButton, cancelButton;
  OptionsDialog options;
  AboutDialog about;
}
