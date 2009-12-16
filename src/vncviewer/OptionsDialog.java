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

class OptionsDialog extends vncviewer.Dialog {

  public OptionsDialog(OptionsDialogCallback cb_) { 
    super(false);
    cb = cb_;
    setTitle("VNC Viewer: Connection Options");

	setLayout(new BorderLayout());
	
	Panel tabPanel = new Panel();
	tabPanel.add(encodingSel=new Button("Encoding"));
	tabPanel.add(inputSel=new Button("Inputs"));
	tabPanel.add(miscSel=new Button("Misc"));
	add(tabPanel, BorderLayout.NORTH);
	
	cardPanel = new Panel();
	cardPanel.setLayout(new CardLayout());
	add(cardPanel, BorderLayout.CENTER);
	
    mainGBC = new GridBagConstraints();
    mainGBC.gridwidth = GridBagConstraints.REMAINDER;
    mainGBC.anchor = GridBagConstraints.WEST;
    mainGBC.ipadx = 2;
    mainGBC.ipady = 2;

    startPanel("Encoding and Colour Level:");
    panelGBC.gridwidth = 1;
    autoSelect = addCheckbox("Auto select");
    encodingGroup = new CheckboxGroup();
    colourGroup = new CheckboxGroup();
    panelGBC.gridwidth = GridBagConstraints.REMAINDER;
    fullColour = addRadioCheckbox("Full (all available colours)", colourGroup);
    panelGBC.gridwidth = 1;
    zrle = addRadioCheckbox("ZRLE", encodingGroup);
    panelGBC.gridwidth = GridBagConstraints.REMAINDER;
    mediumColour = addRadioCheckbox("Medium (256 colours)", colourGroup);
    panelGBC.gridwidth = 1;
    hextile = addRadioCheckbox("Hextile", encodingGroup);
    panelGBC.gridwidth = GridBagConstraints.REMAINDER;
    lowColour = addRadioCheckbox("Low (64 colours)", colourGroup);
    panelGBC.gridwidth = 1;
    raw = addRadioCheckbox("Raw", encodingGroup);
    panelGBC.gridwidth = GridBagConstraints.REMAINDER;
    veryLowColour = addRadioCheckbox("Very low (8 colours)", colourGroup);

    startPanel("Inputs:");
    viewOnly = addCheckbox("View only (ignore mouse & keyboard)");
    acceptClipboard = addCheckbox("Accept clipboard from server");
    sendClipboard = addCheckbox("Send clipboard to server");

    startPanel("Misc:");
    shared = addCheckbox("Shared (don't disconnect other viewers)");
    useLocalCursor = addCheckbox("Render cursor locally");
    fastCopyRect = addCheckbox("Fast CopyRect");

    Panel pb = new Panel();
    okButton = new Button("OK");
    pb.add(okButton);
    cancelButton = new Button("Cancel");
    pb.add(cancelButton);
    add(pb, BorderLayout.SOUTH);

    pack();
	makeBold(encodingSel);
  }

  public void initDialog() {
    if (cb != null) cb.setOptions();
    zrle.setEnabled(!autoSelect.getState());
    hextile.setEnabled(!autoSelect.getState());
    raw.setEnabled(!autoSelect.getState());
  }

  void startPanel(String title) {
    panelGB = new GridBagLayout();
    panel = new Panel(panelGB);
    cardPanel.add(panel, title);

    panelGBC = new GridBagConstraints();
    panelGBC.gridwidth = GridBagConstraints.REMAINDER;
    panelGBC.anchor = GridBagConstraints.WEST;
    panelGBC.ipadx = 2;
    panelGBC.ipady = 2;
    panelGBC.insets = new Insets(0,4,0,0);
    Label l = new Label(title);
    panelGB.setConstraints(l, panelGBC);
    panel.add(l);
  }

  Checkbox addCheckbox(String str) {
    Checkbox c = new Checkbox(str);
    panelGB.setConstraints(c, panelGBC);
    panel.add(c);
    return c;
  }

  Checkbox addRadioCheckbox(String str, CheckboxGroup group) {
    Checkbox c = new Checkbox(str, group, false);
    panelGB.setConstraints(c, panelGBC);
    panel.add(c);
    return c;
  }

  public boolean action(Event event, Object arg) {
    if (event.target == okButton) {
      ok = true;
      if (cb != null) cb.getOptions();
      endDialog();
    } else if (event.target == cancelButton) {
      ok = false;
      endDialog();
    } else if (event.target == autoSelect) {
      zrle.setEnabled(!autoSelect.getState());
      hextile.setEnabled(!autoSelect.getState());
      raw.setEnabled(!autoSelect.getState());
    } else if (event.target == encodingSel) {
	  ((CardLayout)cardPanel.getLayout()).show(cardPanel, "Encoding and Colour Level:");
	  makeBold(encodingSel);
    } else if (event.target == inputSel) {
      ((CardLayout)cardPanel.getLayout()).show(cardPanel, "Inputs:");
      makeBold(inputSel);
	} else if (event.target == miscSel) {
      ((CardLayout)cardPanel.getLayout()).show(cardPanel, "Misc:");
      makeBold(miscSel);
    }
    return true;
  }
  
  private void makeBold ( Button b )
  {
    int size = b.getFont().getSize(); // Don't want to rely on getFontSize()
    String name = b.getFont().getName(); // Can't rely on getFontName()
    encodingSel.setFont(new Font(name,(b==encodingSel) ? Font.BOLD : Font.PLAIN,size));
    inputSel.setFont(new Font(name,(b==inputSel) ? Font.BOLD : Font.PLAIN,size));
    miscSel.setFont(new Font(name,(b==miscSel) ? Font.BOLD : Font.PLAIN,size));
  }

  OptionsDialogCallback cb;
  Button encodingSel, inputSel, miscSel;
  //GridBagLayout mainGB;
  GridBagConstraints mainGBC;
  GridBagLayout panelGB;
  GridBagConstraints panelGBC;
  Panel cardPanel;
  Panel panel;
  Checkbox autoSelect;
  CheckboxGroup encodingGroup, colourGroup;
  Checkbox zrle, hextile, raw;
  Checkbox fullColour, mediumColour, lowColour, veryLowColour;
  Checkbox viewOnly, acceptClipboard, sendClipboard;
  Checkbox shared, useLocalCursor, fastCopyRect;
  Button okButton, cancelButton;
}
