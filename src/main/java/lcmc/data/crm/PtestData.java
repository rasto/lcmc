/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * by Rasto Levrinc.
 *
 * Copyright (C) 2009, Rastislav Levrinc
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

package lcmc.data.crm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lcmc.utilities.CRM;
import lcmc.utilities.Tools;

/**
 * This class holds data that were retrieved from ptest command.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class PtestData {
    /** Pattern for LogActions. e.g. "Start res_IPaddr2_1     (hardy-a)" */
    private static final Pattern PTEST_ACTIONS_PATTERN =
          Pattern.compile(".*LogActions:\\s+(\\S+)\\s*(?:resource)?\\s+(\\S+)"
                          + "\\s+\\(([^)]*)\\).*");
    /** Pattern for pending actions. */
    private static final Pattern PTEST_ERROR_PATTERN = Pattern.compile(
                           "(?i).*ERROR: print_elem:\\s+\\[Action.*?: Pending "
                           + "\\(id: (\\S+)_(\\S+)_.*?, loc: ([^,]+).*");
           //".*native_color:\\s+Resource\\s+(\\S+)\\s+cannot run anywhere.*");
    /** Pattern that gets cloned resource id. */
    private static final Pattern PTEST_CLONE_PATTERN =
                                                 Pattern.compile("(.*):\\d+");
    /** Tool tip. */
    private final String toolTip;
    /** Shadow cib. */
    private final String shadowCib;
    /** Running on nodes. */
    private final Map<String, List<String>> runningOnNodes =
                                     new LinkedHashMap<String, List<String>>();
    /** Slave on nodes. */
    private final Map<String, List<String>> slaveOnNodes =
                                     new LinkedHashMap<String, List<String>>();
    /** Master on nodes. */
    private final Map<String, List<String>> masterOnNodes =
                                     new LinkedHashMap<String, List<String>>();
    /** Which resources are managed. */
    private final Map<String, Boolean> managedHash =
                                     new LinkedHashMap<String, Boolean>();

    /** Prepares a new {@code PtestData} object. */
    public PtestData(final String raw) {
        if (raw == null) {
            toolTip = null;
            shadowCib = null;
            return;
        }
        final StringBuilder sb = new StringBuilder(300);
        sb.append("<html><b>");
        sb.append(Tools.getString("PtestData.ToolTip"));
        sb.append("</b><br>");
        final String[] queries = raw.split(CRM.PTEST_END_DELIM);
        if (queries.length != 2) {
            shadowCib = null;
            toolTip = null;
            return;
        }
        boolean isToolTip = false;
        for (final String line : queries[0].split("\\r?\\n")) {
            final Matcher m = PTEST_ACTIONS_PATTERN.matcher(line);
            final Matcher mError = PTEST_ERROR_PATTERN.matcher(line);
            if (m.matches()) {
                final String action = m.group(1);
                String res = m.group(2);
                /* Clone */
                boolean clone = false;
                final Matcher cm = PTEST_CLONE_PATTERN.matcher(res);
                if (cm.matches()) {
                    res = cm.group(1);
                    clone = true;
                }

                final String state = m.group(3);

                List<String> nodes = runningOnNodes.get(res);
                if (nodes == null) {
                    nodes = new ArrayList<String>();
                }
                List<String> slaveNodes = null;
                if (clone) {
                    slaveNodes = slaveOnNodes.get(res);
                    if (slaveNodes == null) {
                        slaveNodes = new ArrayList<String>();
                    }
                }
                List<String> masterNodes = null;
                if (clone) {
                    masterNodes = masterOnNodes.get(res);
                    if (masterNodes == null) {
                        masterNodes = new ArrayList<String>();
                    }
                }
                if ("Start".equals(action)) {
                    nodes.add(state);
                    if (clone) {
                        slaveNodes.add(state);
                    }
                } else if ("Leave".equals(action)) {
                    if (state.indexOf(' ') >= 0) {
                        final String[] parts = state.split(" ");
                        if (parts.length == 2) {
                            final String what = parts[0];
                            final String node = parts[1];
                            if ("unmanaged".equals(node)) {
                                managedHash.put(res, false);
                                continue;
                            }
                            if ("Started".equals(what)) {
                                nodes.add(node);
                            } else if ("Slave".equals(what)) {
                                nodes.add(node);
                                if (clone) {
                                    slaveNodes.add(node);
                                }
                            } else if ("Master".equals(what)) {
                                nodes.add(node);
                                if (clone) {
                                    masterNodes.add(node);
                                }
                            }
                        }
                    }
                } else if ("Stop".equals(action)) {
                    /* is handled by next regexp, if stop fails */
                    nodes.remove(state);
                    if (clone) {
                        slaveNodes.remove(state);
                        masterNodes.remove(state);
                    }
                } else if ("Move".equals(action)) {
                    if (state.contains(" -> ")) {
                        final String[] parts = state.split(" -> ");
                        nodes.remove(parts[0]);
                        nodes.add(parts[parts.length - 1]);
                    }
                } else if ("Promote".equals(action)) {
                    if (state.contains(" -> Master ")) {
                        final String[] parts = state.split(" -> Master ");
                        if (parts.length > 0) {
                            nodes.add(parts[parts.length - 1]);
                            if (clone) {
                                slaveNodes.remove(parts[parts.length - 1]);
                                masterNodes.add(parts[parts.length - 1]);
                            }
                        }
                    }
                } else if ("Demote".equals(action)) {
                    if (state.contains(" -> Slave ")) {
                        final String[] parts = state.split(" -> Slave ");
                        if (parts.length > 0 && clone) {
                                masterNodes.remove(parts[parts.length - 1]);
                                slaveNodes.add(parts[parts.length - 1]);
                        }
                    }
                } else {
                    continue;
                }
                runningOnNodes.put(res, nodes);
                if (clone) {
                    slaveOnNodes.put(res, slaveNodes);
                    masterOnNodes.put(res, masterNodes);
                }
                if ("Leave".equals(action)) {
                    /* don't show it in tooltip */
                    continue;
                }
            } else if (mError.matches()) {
                String res = mError.group(1);
                final String action = mError.group(2);
                final String node = mError.group(3);
                /* Clone */
                boolean clone = false;
                final Matcher cm = PTEST_CLONE_PATTERN.matcher(res);
                if (cm.matches()) {
                    res = cm.group(1);
                    clone = true;
                }
                List<String> nodes = runningOnNodes.get(res);
                if (nodes == null) {
                    nodes = new ArrayList<String>();
                }
                List<String> slaveNodes = null;
                if (clone) {
                    slaveNodes = slaveOnNodes.get(res);
                    if (slaveNodes == null) {
                        slaveNodes = new ArrayList<String>();
                    }
                }
                List<String> masterNodes = null;
                if (clone) {
                    masterNodes = masterOnNodes.get(res);
                    if (masterNodes == null) {
                        masterNodes = new ArrayList<String>();
                    }
                }
                if ("stop".equals(action)) {
                    nodes.add(node); /* stop failed */
                    if (clone) {
                        slaveNodes.add(node);
                    }
                } else if ("start".equals(action)) {
                    nodes.remove(node); /* start failed */
                    if (clone) {
                        slaveNodes.remove(node);
                        masterNodes.remove(node);
                    }
                } else if ("promote".equals(action)) {
                    if (clone) {
                        masterNodes.remove(node); /* promote failed */
                        slaveNodes.add(node);
                    }
                } else if ("demote".equals(action)) {
                    if (clone) {
                        slaveNodes.remove(node); /* demote failed */
                        masterNodes.add(node);
                    }
                }
                runningOnNodes.put(res, nodes);
                if (clone) {
                    slaveOnNodes.put(res, slaveNodes);
                    masterOnNodes.put(res, masterNodes);
                }
            } else {
                continue;
            }
            if (line.contains("_post_notify_")
                || line.contains("_pre_notify_")
                || line.contains("_monitor_")) {
                continue;
            }
            final String[] prefixes = {"LogActions: ",
                    "ERROR: print_elem: "};
            for (final String prefix : prefixes) {
                final int index = line.indexOf(prefix);
                if (index >= 0) {
                    sb.append(line.substring(index + prefix.length()));
                    sb.append("<br>");
                    isToolTip = true;
                }
            }
        }
        if (!isToolTip) {
            sb.append(Tools.getString("PtestData.NoToolTip"));
        }
        sb.append("</html>");
        toolTip = sb.toString();
        shadowCib = queries[1];
    }

    /** Returns tooltip. */
    public String getToolTip() {
        return toolTip;
    }

    /** Returns shadow cib. */
    String getShadowCib() {
        return shadowCib;
    }

    /** Returns on which nodes is service running. */
    List<String> getRunningOnNodes(final String pmId) {
        return runningOnNodes.get(pmId);
    }

    /** Returns on which nodes is service master. */
    List<String> getMasterOnNodes(final String pmId) {
        return masterOnNodes.get(pmId);
    }

    /** Returns on which nodes is service slave. */
    List<String> getSlaveOnNodes(final String pmId) {
        return slaveOnNodes.get(pmId);
    }

    /** Returns if service is managed. */
    boolean isManaged(final String pmId) {
        final Boolean m = managedHash.get(pmId);
        if (m == null) {
            return true;
        }
        return m;
    }
}
