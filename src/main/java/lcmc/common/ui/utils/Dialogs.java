/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2015, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.common.ui.utils;

import java.io.File;
import java.util.Optional;

import javax.inject.Named;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.main.MainData;

@Named
public class Dialogs {
    private final MainData mainData;
    private final Application application;

    public Dialogs(MainData mainData, Application application) {
        this.mainData = mainData;
        this.application = application;
    }

    public Optional<String> getFileName(final String filePrefix) {
        final File defaultFile = getNextAvailableFile(filePrefix);
        JFileChooser chooser = new JFileChooser() {
            @Override
            public void approveSelection() {
                File f = getSelectedFile();
                if (f.exists() && getDialogType() == SAVE_DIALOG) {
                    int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file",
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    switch (result) {
                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.NO_OPTION:
                            return;
                        case JOptionPane.CLOSED_OPTION:
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                    }
                }
                super.approveSelection();
            }
        };
        chooser.setSelectedFile(defaultFile);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setCurrentDirectory(new File("."));
        chooser.addChoosableFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "PNG Images (*.png)";
            }

            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    return f.getName().toLowerCase().endsWith(".png");
                }
            }
        });

        int r = chooser.showSaveDialog(mainData.getClustersPanel());
        if (r != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        return Optional.of(chooser.getSelectedFile().getAbsolutePath());
    }

    public Optional<String> getLcmcConfFilename() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(application.getDefaultSaveFile()));
        final FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File f) {
                if (f.isDirectory()) {
                    return true;
                }
                final String name = f.getName();
                final int i = name.lastIndexOf('.');
                if (i > 0 && i < name.length() - 1) {
                    final String ext = name.substring(i + 1);
                    return ext.equals(Tools.getDefault("MainMenu.DrbdGuiFiles.Extension"));
                }
                return false;
            }

            @Override
            public String getDescription() {
                return Tools.getString("MainMenu.DrbdGuiFiles");
            }
        };
        chooser.setFileFilter(filter);
        final int ret = chooser.showOpenDialog(mainData.getMainFrame());
        Optional<String> name = Optional.empty();
        if (ret == JFileChooser.APPROVE_OPTION) {
            name = Optional.of(chooser.getSelectedFile().getAbsolutePath());
        }
        return name;
    }

    private File getNextAvailableFile(String filePrefix) {
        File defaultFile = new File(filePrefix + ".png");
        int index = 1;
        while (defaultFile.exists()) {
            defaultFile = new File(filePrefix + "-" + index + ".png");
            index++;
        }
        return defaultFile;
    }
}
