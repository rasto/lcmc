/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
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

package lcmc.common.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JLabel;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.host.domain.Host;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.cluster.ui.widget.Widget;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.util.Tools;
import lcmc.common.domain.Unit;

/**
 * An implementation of an dialog with log files.
 */
@Named
public final class CmdLog extends HostLogs {
    private static final Value DEFAULT_TIME = new StringValue("5m");
    @Inject
    private WidgetFactory widgetFactory;

    protected static Unit[] getUnits() {
        return new Unit[]{
                   new Unit("s", "s", "second", "seconds"),
                   new Unit("m", "m", "minute", "minutes"),
                   new Unit("h", "h", "hour",   "hours"),
                   new Unit("d", "d", "day",    "days")
       };
    }
    private String getLogsCommand = "CmdLog.Processed";
    /** Time in minutes of the logs. */
    private Widget logTimeInMinutes;

    public void init(final Host host) {
        super.init(host);
    }

    @Override
    protected String logFileCommand() {
        return getLogsCommand;
    }

    @Override
    protected JComponent[] getAdditionalComponents() {
        final MyButton processed = widgetFactory.createButton(Tools.getString("CmdLog.Processed.Btn"));
        processed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                getLogsCommand = "CmdLog.Processed";
                refreshLogsThread();
            }
        });

        final MyButton raw = widgetFactory.createButton(Tools.getString("CmdLog.Raw.Btn"));
        raw.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                getLogsCommand = "CmdLog.Raw";
                refreshLogsThread();
            }
        });

        final MyButton clear = widgetFactory.createButton(Tools.getString("CmdLog.Clear.Btn"));
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                getLogsCommand = "CmdLog.Clear";
                refreshLogsThread();
            }
        });
        logTimeInMinutes = widgetFactory.createInstance(
                                   Widget.Type.TEXTFIELDWITHUNIT,
                                   DEFAULT_TIME,
                                   Widget.NO_ITEMS,
                                   getUnits(),
                                   Widget.NO_REGEXP,
                                   150,
                                   Widget.NO_ABBRV,
                                   new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                   Widget.NO_BUTTON);
        return new JComponent[]{processed,
                                raw,
                                clear,
                                new JLabel(Tools.getString("CmdLog.Last.Label")),
                                (JComponent) logTimeInMinutes,
                                getRefreshBtn()};
    }

    @Override
    protected Map<String, String> getOptionsHash() {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@OPTIONS@", "--log-time=" + logTimeInMinutes.getStringValue());
        return replaceHash;
    }

    @Override
    protected String getDescription() {
        return Tools.getString("CmdLog.Description");
    }
}
