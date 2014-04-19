/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package lcmc.gui.resources;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import lcmc.data.Application;
import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.utilities.SSH;
import lcmc.utilities.Tools;

/**
 * This class holds info data for a filesystem.
 */
public final class FSInfo extends Info {
    /** File system icon. */
    private static final ImageIcon FS_ICON = Tools.createImageIcon(
                               Tools.getDefault("HostBrowser.FileSystemIcon"));
    /** cached output from the modinfo command for the info panel. */
    private String modinfo = null;
    /** Prepares a new {@code FSInfo} object. */
    public FSInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /** Returns browser object of this info. */
    @Override
    public HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /** Returns file system icon for the menu. */
    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return FS_ICON;
    }

    /** Returns type of the info text. text/plain or text/html. */
    @Override
    protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /** Returns info, before it is updated. */
    @Override
    public String getInfo() {
        return "<html><pre>" + getName() + "</html></pre>";
    }

    /** Updates info of the file system. */
    @Override
    public void updateInfo(final JEditorPane ep) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (modinfo == null) {
                    final SSH.SSHOutput ret =
                              Tools.execCommand(getBrowser().getHost(),
                                                "/sbin/modinfo "
                                                + getName(),
                                                null,   /* ExecCallback */
                                                false,  /* outputVisible */
                                                SSH.DEFAULT_COMMAND_TIMEOUT);
                    modinfo = ret.getOutput();
                }
                ep.setText("<html><pre>" + modinfo + "</html></pre>");
            }
        };
        final Thread thread = new Thread(runnable);
        thread.start();
    }
}
