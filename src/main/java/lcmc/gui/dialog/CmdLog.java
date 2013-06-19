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

package lcmc.gui.dialog;

import lcmc.data.Host;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import lcmc.data.AccessMode;
import lcmc.data.ConfigData;
import lcmc.gui.widget.TextfieldWithUnit;
import lcmc.gui.widget.Widget;
import lcmc.utilities.Unit;
import java.util.Map;
import java.util.HashMap;

/**
 * An implementation of an dialog with log files.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class CmdLog extends HostLogs {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Command to retrieve the logs. */
    private String command = "CmdLog.Processed";
    /** Time in minutes of the logs. */
    private TextfieldWithUnit timeTF;
    private static final String DEFAULT_TIME = "5m";

    /** Prepares a new <code>CmdLog</code> object. */
    public CmdLog(final Host host) {
        super(host);
    }

    @Override
    protected String logFileCommand() {
        return command;
    }

    /** Return buttons for extra functionality. */
    @Override
    protected JComponent[] getAdditionalComponents() {
        final MyButton processed =
                         new MyButton(Tools.getString("CmdLog.Processed.Btn"));
        processed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                command = "CmdLog.Processed";
                refreshLogsThread();
            }
        });

        final MyButton raw = new MyButton(Tools.getString("CmdLog.Raw.Btn"));
        raw.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                command = "CmdLog.Raw";
                refreshLogsThread();
            }
        });

        final MyButton clear =
                             new MyButton(Tools.getString("CmdLog.Clear.Btn"));
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                command = "CmdLog.Clear";
                refreshLogsThread();
            }
        });
        timeTF =
             new TextfieldWithUnit(DEFAULT_TIME,
                                   getUnits(),
                                   Widget.NO_REGEXP,
                                   150,
                                   Widget.NO_ABBRV,
                                   new AccessMode(ConfigData.AccessType.ADMIN,
                                                  !AccessMode.ADVANCED),
                                   Widget.NO_BUTTON);
        return new JComponent[]{processed,
                                raw,
                                clear,
                                new JLabel(
                                        Tools.getString("CmdLog.Last.Label")),
                                timeTF,
                                getRefreshBtn()};
    }

    /** Time units. */
    protected static Unit[] getUnits() {
        return new Unit[]{
                   new Unit("s", "s", "second", "seconds"),
                   new Unit("m", "m", "minute", "minutes"),
                   new Unit("h", "h", "hour",   "hours"),
                   new Unit("d", "d", "day",    "days")
       };
    }

    /** Options for the log command. */
    @Override
    protected Map<String, String> getOptionsHash() {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@OPTIONS@", "--log-time=" + timeTF.getStringValue());
        return replaceHash;
    }

    @Override
    protected final String getDescription() {
        return Tools.getString("CmdLog.Description");
    }
}
