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
package lcmc.cluster.ui.resource;

import javax.inject.Named;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;

import lcmc.cluster.infrastructure.ssh.ExecCommandConfig;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.Info;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.host.ui.HostBrowser;

/**
 * This class holds info data for a filesystem.
 */
@Named
public final class FSInfo extends Info {
    private static final ImageIcon FS_ICON = Tools.createImageIcon(Tools.getDefault("HostBrowser.FileSystemIcon"));
    private String cachedModinfoOutput = null;

    public FSInfo(Application application, SwingUtils swingUtils, Access access, MainData mainData) {
        super(application, swingUtils, access, mainData);
    }

    @Override
    public HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return FS_ICON;
    }

    @Override
    protected String getInfoMimeType() {
        return MainData.MIME_TYPE_TEXT_HTML;
    }

    /** Returns info, before it is updated. */
    @Override
    public String getInfo() {
        return "<html><pre>" + getName() + "</html></pre>";
    }

    /** Updates info of the file system. */
    @Override
    public void updateInfo(final JEditorPane ep) {
        final Runnable runnable = () -> {
            if (cachedModinfoOutput == null) {
                final Host host = getBrowser().getHost();
                final var ret = host.captureCommand(new ExecCommandConfig().command("/sbin/modinfo " + getName()));
                cachedModinfoOutput = ret.getOutput();
            }
            ep.setText("<html><pre>" + cachedModinfoOutput + "</html></pre>");
        };
        final Thread thread = new Thread(runnable);
        thread.start();
    }
}
