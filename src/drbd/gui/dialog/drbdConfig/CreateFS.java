/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */


package drbd.gui.dialog.drbdConfig;

import drbd.utilities.Tools;
import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.gui.SpringUtilities;
import drbd.gui.resources.BlockDevInfo;
import drbd.gui.resources.DrbdResourceInfo;
import drbd.gui.resources.StringInfo;
import drbd.gui.GuiComboBox;
import drbd.gui.dialog.WizardDialog;
import drbd.utilities.MyButton;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * An implementation of a dialog where drbd block devices are initialized.
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class CreateFS extends DrbdConfig {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Pull down menu with hosts (or no host). */
    private GuiComboBox hostCB;
    /** Pull down menu with file systems. */
    private GuiComboBox filesystemCB;
    /** Make file system button. */
    private final MyButton makeFsButton = new MyButton(
                Tools.getString("Dialog.DrbdConfig.CreateFS.CreateFsButton"));
    /** No host string. (none) */
    private static final String NO_HOST_STRING =
                    Tools.getString("Dialog.DrbdConfig.CreateFS.NoHostString");
    /** No file system (use existing data). */
    private static final String NO_FILESYSTEM_STRING =
                Tools.getString("Dialog.DrbdConfig.CreateFS.SelectFilesystem");
    /** Width of the combo boxes. */
    private static final int COMBOBOX_WIDTH = 250;

    /**
     * Prepares a new <code>CreateFS</code> object.
     */
    public CreateFS(final WizardDialog previousDialog,
                    final DrbdResourceInfo dri) {
        super(previousDialog, dri);
    }

    /**
     * Finishes the dialog. If primary bd was choosen it is forced to be a
     * primary.
     */
    protected final void finishDialog() {
        final BlockDevInfo bdiPri = getPrimaryBD();
        if (bdiPri != null) {
            final boolean testOnly = false;
            bdiPri.forcePrimary(testOnly);
        }
    }

    /**
     * Returns the primary block device.
     */
    protected final BlockDevInfo getPrimaryBD() {
        final BlockDevInfo bdi1 = getDrbdResourceInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdResourceInfo().getSecondBlockDevInfo();
        final String h = hostCB.getStringValue();
        if (h.equals(bdi1.getHost().getName())) {
            return bdi1;
        } else if (h.equals(bdi2.getHost().getName())) {
            return bdi2;
        } else {
            return null;
        }
    }

    /**
     * Returns the secondary block device.
     */
    protected final BlockDevInfo getSecondaryBD() {
        final BlockDevInfo bdi1 = getDrbdResourceInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdResourceInfo().getSecondBlockDevInfo();
        final String h = hostCB.getStringValue();
        if (h.equals(bdi1.getHost().getName())) {
            return bdi2;
        } else if (h.equals(bdi2.getHost().getName())) {
            return bdi1;
        } else {
            Tools.appError("unknown host: " + h);
            return null;
        }
    }

    /**
     * Creates the file system.
     */
    protected final void createFilesystem() {
        final Runnable runnable = new Runnable() {
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        buttonClass(finishButton()).setEnabled(false);
                        makeFsButton.setEnabled(false);
                    }
                });
                BlockDevInfo bdiPri = getPrimaryBD();
                BlockDevInfo bdiSec = getSecondaryBD();
                final boolean testOnly = false;
                bdiPri.forcePrimary(testOnly);
                final String fs = filesystemCB.getStringValue();
                bdiPri.makeFilesystem(fs, testOnly);
                getDrbdResourceInfo().setCreatedFs(fs);
                bdiPri.setSecondary(testOnly);
                hostCB.setValue(NO_HOST_STRING);
                filesystemCB.setValue(NO_FILESYSTEM_STRING);
            }
        };
        final Thread thread = new Thread(runnable);
        //thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * Returns the next dialog, null in this dialog.
     */
    public final WizardDialog nextDialog() {
        return null;
    }

    /**
     * Returns title of the dialog.
     * It is defined in TextResources as "Dialog.DrbdConfig.CreateFS.Title"
     */
    protected final String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.CreateFS.Title");
    }

    /**
     * Returns description of the dialog.
     * It is defined in TextResources as
     * "Dialog.DrbdConfig.CreateFS.Description"
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.CreateFS.Description");
    }

    /**
     * Inits dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(finishButton())});
        enableComponents();
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    makeFsButton.pressButton();
                }
            });
        }
    }

    /**
     * Enables and disables the make fs and finish buttons depending on what
     * was chosen by user.
     */
    protected final void checkButtons() {
        final boolean noHost = hostCB.getStringValue().equals(NO_HOST_STRING);
        final boolean noFileSystem = filesystemCB.getStringValue().equals(
                                                        NO_FILESYSTEM_STRING);
        if (noFileSystem) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ((MyButton) buttonClass(finishButton())).setEnabled(true);
                    makeFsButton.setEnabled(false);
                }
            });
        } else if (noHost) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ((MyButton) buttonClass(finishButton())).setEnabled(false);
                }
            });
            makeFsButton.setEnabled(false);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ((MyButton) buttonClass(finishButton())).setEnabled(false);
                    makeFsButton.setEnabled(true);
                }
            });
        }
    }

    /**
     * Returns input pane, where file system can be created on the selected
     * host.
     */
    protected final JComponent getInputPane() {
        makeFsButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());

        /* host */
        final String[] hostNames = new String[3];
        hostNames[0] = NO_HOST_STRING;
        final Host[] hosts = getDrbdResourceInfo().getHosts();
        int i = 1;
        for (Host host : hosts) {
            hostNames[i] = host.getName();
            i++;
        }
        final JLabel hostLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.CreateFS.ChooseHost"));
        String defaultHost = NO_HOST_STRING;
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            defaultHost = hostNames[1];
        }
        hostCB = new GuiComboBox(defaultHost,
                                 hostNames,
                                 null, /* units */
                                 GuiComboBox.Type.COMBOBOX,
                                 null, /* regexp */
                                 COMBOBOX_WIDTH,
                                 null, /* abbrv */
                                 ConfigData.AccessType.RO);
        hostCB.addListeners(
            new  ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                checkButtons();
                            }
                        });
                        thread.start();
                    }
                }
            },
            null);
        inputPane.add(hostLabel);
        inputPane.add(hostCB);
        inputPane.add(new JLabel(""));

        /* Filesystem */
        final JLabel filesystemLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.CreateFS.Filesystem"));
        String defaultValue = NO_FILESYSTEM_STRING;
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            defaultValue = "ext3";
        }
        final StringInfo[] filesystems =
                    getDrbdResourceInfo().getCommonFileSystems(defaultValue);

        filesystemCB = new GuiComboBox(defaultValue,
                                       filesystems,
                                       null, /* units */
                                       GuiComboBox.Type.COMBOBOX,
                                       null, /* regexp */
                                       COMBOBOX_WIDTH,
                                       null, /* abbrv */
                                       ConfigData.AccessType.RO);
        inputPane.add(filesystemLabel);
        inputPane.add(filesystemCB);
        filesystemCB.addListeners(
            new  ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                if (NO_HOST_STRING.equals(
                                                hostCB.getStringValue())
                                    && !NO_FILESYSTEM_STRING.equals(
                                            filesystemCB.getStringValue())) {
                                    hostCB.setValue(hostNames[1]);
                                } else {
                                    checkButtons();
                                }
                            }
                        });
                        thread.start();
                    }
                }
            },
            null);

        makeFsButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                createFilesystem();
            }
        });
        inputPane.add(makeFsButton);

        SpringUtilities.makeCompactGrid(inputPane, 2, 3,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad

        pane.add(inputPane);
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad

        return pane;
    }
}
