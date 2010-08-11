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
package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.gui.ClusterBrowser;
import drbd.data.Host;
import drbd.data.HostLocation;
import drbd.data.ResourceAgent;
import drbd.gui.GuiComboBox;

import java.awt.geom.Point2D;
import drbd.data.resources.Service;
import drbd.data.Subtext;
import drbd.data.CRMXML;
import drbd.data.ClusterStatus;
import drbd.data.ConfigData;
import drbd.data.PtestData;
import drbd.utilities.MyMenu;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Unit;
import drbd.utilities.Tools;
import drbd.utilities.CRM;
import drbd.utilities.ButtonCallback;
import drbd.utilities.MyMenuItem;
import drbd.utilities.MyList;
import drbd.gui.SpringUtilities;
import drbd.utilities.MyButton;
import drbd.gui.dialog.pacemaker.ServiceLogs;

import java.awt.Color;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.Locale;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;
import javax.swing.JRadioButton;
import javax.swing.SpringLayout;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * This class holds info data for one hearteat service and allows to enter
 * its arguments and execute operations on it.
 */
public class ServiceInfo extends EditableInfo {
    /** This is a map from host to the combobox with scores. */
    private final Map<HostInfo, GuiComboBox> scoreComboBoxHash =
                                          new HashMap<HostInfo, GuiComboBox>();
    /** A map from host to stored score. */
    private final Map<HostInfo, HostLocation> savedHostLocations =
                                         new HashMap<HostInfo, HostLocation>();
    /** Saved meta attrs id. */
    private String savedMetaAttrsId = null;
    /** Saved operations id. */
    private String savedOperationsId = null;
    /** A map from operation to the stored value. First key is
     * operation name like "start" and second key is parameter like
     * "timeout". */
    private final MultiKeyMap savedOperation = new MultiKeyMap();
    /** Whether id-ref for meta-attributes is used. */
    private ServiceInfo savedMetaAttrInfoRef = null;
    /** Combo box with same as operations option. */
    private GuiComboBox sameAsMetaAttrsCB = null;
    /** Whether id-ref for operations is used. */
    private ServiceInfo savedOperationIdRef = null;
    /** Combo box with same as operations option. */
    private GuiComboBox sameAsOperationsCB = null;
    /** Saved operations lock. */
    private final Mutex mSavedOperationsLock = new Mutex();
    /** A map from operation to its combo box. */
    private final MultiKeyMap operationsComboBoxHash = new MultiKeyMap();
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Group info object of the group this service is in or null, if it is
     * not in any group. */
    private GroupInfo groupInfo = null;
    /** Master/Slave info object, if is null, it is not master/slave
     * resource. */
    private CloneInfo cloneInfo = null;
    /** ResourceAgent object of the service, with name, ocf informations
     * etc. */
    private final ResourceAgent resourceAgent;
    /** Radio buttons for clone/master/slave primitive resources. */
    private GuiComboBox typeRadioGroup;
    /** Default values item in the "same as" scrolling list in meta
        attributes.*/
    private static final String META_ATTRS_DEFAULT_VALUES_TEXT =
                                                          "default values";
    /** Default values internal name. */
    private static final String META_ATTRS_DEFAULT_VALUES = "default";
    /** Default values item in the "same as" scrolling list in operations. */
    private static final String OPERATIONS_DEFAULT_VALUES_TEXT =
                                                          "advisory minimum";
    /** Default values internal name. */
    private static final String OPERATIONS_DEFAULT_VALUES = "default";

    /** Name of the heartbeat provider. */
    public static final String HB_HEARTBEAT_PROVIDER = "heartbeat";
    /** Check the cached fields. */
    private static final String CACHED_FIELD = "cached";
    /** Master / Slave type string. */
    public static final String MASTER_SLAVE_TYPE_STRING = "Master/Slave";
    /** Unmanage service icon. */
    private static final ImageIcon UNMANAGE_ICON = Tools.createImageIcon(
                      Tools.getDefault("HeartbeatGraph.ServiceUnmanagedIcon"));
    /** Started service icon. */
    private static final ImageIcon SERVICE_STARTED_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.ServiceStartedIcon"));
    /** Stopped service icon. */
    private static final ImageIcon SERVICE_STOPPED_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.ServiceStoppedIcon"));
    /** Running service icon. */
    private static final ImageIcon SERVICE_RUNNING_ICON =
        Tools.createImageIcon(
                Tools.getDefault("HeartbeatGraph.ServiceRunningIcon"));
    /** Not running service icon. */
    private static final ImageIcon SERVICE_NOT_RUNNING_ICON =
        Tools.createImageIcon(
                Tools.getDefault("HeartbeatGraph.ServiceNotRunningIcon"));
    /** Start service icon. */
    private static final ImageIcon START_ICON = SERVICE_RUNNING_ICON;
    /** Stop service icon. */
    private static final ImageIcon STOP_ICON  = SERVICE_NOT_RUNNING_ICON;
    /** Migrate icon. */
    private static final ImageIcon MIGRATE_ICON = Tools.createImageIcon(
                            Tools.getDefault("HeartbeatGraph.MigrateIcon"));
    /** Unmigrate icon. */
    private static final ImageIcon UNMIGRATE_ICON = Tools.createImageIcon(
                            Tools.getDefault("HeartbeatGraph.UnmigrateIcon"));
    /** Orphaned subtext. */
    private static final Subtext ORPHANED_SUBTEXT =
                                         new Subtext("(ORPHANED)", null);
    /** Orphaned with fail-count subtext. */
    private static final Subtext ORPHANED_FAILED_SUBTEXT =
                                         new Subtext("(ORPHANED)", Color.RED);
    /** Unmanaged subtext. */
    private static final Subtext UNMANAGED_SUBTEXT =
                                         new Subtext("(unmanaged)", Color.RED);
    /** Migrated subtext. */
    private static final Subtext MIGRATED_SUBTEXT =
                                         new Subtext("(migrated)", Color.RED);
    /** Clone type string. */
    protected static final String CLONE_TYPE_STRING = "Clone";
    /** Primitive type string. */
    private static final String PRIMITIVE_TYPE_STRING = "Primitive";
    /** Gui ID parameter. */
    public static final String GUI_ID = "__drbdmcid";
    /** PCMK ID parameter. */
    public static final String PCMK_ID = "__pckmkid";


    /**
     * Prepares a new <code>ServiceInfo</code> object and creates
     * new service object.
     */
    public ServiceInfo(final String name,
                       final ResourceAgent resourceAgent,
                       final Browser browser) {
        super(name, browser);
        this.resourceAgent = resourceAgent;
        if (resourceAgent != null && resourceAgent.isStonith()) {
            setResource(new Service(name.replaceAll("/", "_")));
            getService().setStonith(true);
        } else {
            setResource(new Service(name));
        }
        getService().setNew(true);
    }

    /**
     * Prepares a new <code>ServiceInfo</code> object and creates
     * new service object. It also initializes parameters along with
     * heartbeat id with values from xml stored in resourceNode.
     */
    public ServiceInfo(final String name,
                       final ResourceAgent ra,
                       final String heartbeatId,
                       final Map<String, String> resourceNode,
                       final Browser browser) {
        this(name, ra, browser);
        getService().setHeartbeatId(heartbeatId);
        /* TODO: cannot call setParameters here, only after it is
         * constructed. */
        setParameters(resourceNode);
    }

    /**
     * Returns id of the service, which is heartbeatId.
     * TODO: this id is used for stored position info, should be named
     * differently.
     */
    public String getId() {
        return getService().getHeartbeatId();
    }

    /**
     * Returns browser object of this info.
     */
    protected ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Sets info panel of the service.
     */
    public void setInfoPanel(final JPanel infoPanel) {
        this.infoPanel = infoPanel;
    }

    /**
     * Returns true if the node is active.
     */
    public boolean isOfflineNode(final String node) {
        return "no".equals(getBrowser().getClusterStatus().isOnlineNode(node));
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    public boolean checkResourceFieldsCorrect(final String param,
                                              final String[] params) {
        boolean ret = true;
        if (getService().isOrphaned()) {
            return false;
        }
        if (cloneInfo != null
            && !cloneInfo.checkResourceFieldsCorrect(
                                         param,
                                         cloneInfo.getParametersFromXML())) {
            /* the next super checkResourceFieldsCorrect must be run at
              least once. */
            ret = false;
        }
        if (!super.checkResourceFieldsCorrect(param, params)) {
            return false;
        }
        if (cloneInfo == null) {
            boolean on = false;
            for (Host host : getBrowser().getClusterHosts()) {
                final HostInfo hi = host.getBrowser().getHostInfo();
                /* at least one "eq" */
                final GuiComboBox cb = scoreComboBoxHash.get(hi);
                if (cb != null) {
                    final String op = getOpFromLabel(hi.getName(),
                                                     cb.getLabel().getText());
                    if (cb.getValue() == null || "eq".equals(op)) {
                        on = true;
                        break;
                    }
                }
            }
            if (!on) {
                return false;
            }
        }
        if (!ret) {
            return false;
        }
        return true;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params) {
        if (getService().isNew()) {
            return true;
        }
        boolean changed = false;
        if (super.checkResourceFieldsChanged(param, params)) {
            changed = true;
        }
        boolean allMetaAttrsAreDefaultValues = true;
        if (params != null) {
            for (String otherParam : params) {
                if (isMetaAttr(otherParam)) {
                    final GuiComboBox cb = paramComboBoxGet(otherParam, null);
                    if (cb == null) {
                        continue;
                    }
                    final Object newValue = cb.getValue();
                    final Object defaultValue = getParamDefault(otherParam);
                    if (!Tools.areEqual(newValue, defaultValue)) {
                        allMetaAttrsAreDefaultValues = false;
                    }
                }
            }
        }
        if (cloneInfo != null
                   && cloneInfo.checkResourceFieldsChanged(
                                           param,
                                           cloneInfo.getParametersFromXML())) {
            changed = true;
        }
        final String id = getComboBoxValue(GUI_ID);
        final String heartbeatId = getService().getHeartbeatId();
        if (ConfigData.PM_GROUP_NAME.equals(getName())) {
            if (heartbeatId == null) {
                changed = true;
            } else if (heartbeatId.equals(Service.GRP_ID_PREFIX + id)
                || heartbeatId.equals(id)) {
                if (checkHostLocationsFieldsChanged()
                    || checkOperationFieldsChanged()) {
                    changed = true;
                }
            } else {
                changed = true;
            }
        } else if (ConfigData.PM_CLONE_SET_NAME.equals(getName())
                   || ConfigData.PM_MASTER_SLAVE_SET_NAME.equals(getName())) {
            String prefix;
            if (getService().isMaster()) {
                prefix = Service.MS_ID_PREFIX;
            } else {
                prefix = Service.CL_ID_PREFIX;
            }
            if (heartbeatId.equals(prefix + id)
                || heartbeatId.equals(id)) {
                if (checkHostLocationsFieldsChanged()) {
                    changed = true;
                }
            } else {
                changed = true;
            }
        } else {
            if (heartbeatId == null) {
                changed = true;
            } else if (heartbeatId.equals(Service.RES_ID_PREFIX
                                          + getService().getName()
                                          + "_" + id)
                       || heartbeatId.equals(Service.STONITH_ID_PREFIX
                                          + getService().getName()
                                          + "_" + id)
                       || heartbeatId.equals(id)) {
                if (checkHostLocationsFieldsChanged()
                    || checkOperationFieldsChanged()) {
                    changed = true;
                }
            } else {
                changed = true;
            }
        }
        final String cl = getService().getResourceClass();
        if (cl != null && cl.equals(ClusterBrowser.HB_HEARTBEAT_CLASS)) {
            /* in old style resources don't show all the textfields */
            boolean visible = false;
            GuiComboBox cb = null;
            for (int i = params.length - 1; i >= 0; i--) {
                final GuiComboBox prevCb = paramComboBoxGet(params[i],
                                                            null);
                if (prevCb == null) {
                    continue;
                }
                if (!visible && !prevCb.getStringValue().equals("")) {
                    visible = true;
                }
                if (cb != null && cb.isVisible() != visible) {
                    final boolean v = visible;
                    final GuiComboBox c = cb;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            c.setVisible(v);
                            getLabel(c).setVisible(v);
                        }
                    });
                }
                cb = prevCb;
            }
        }

        /* id-refs */
        if (sameAsMetaAttrsCB != null) {
            final Info info = (Info) sameAsMetaAttrsCB.getValue();
            final boolean defaultValues =
                    info != null
                    && META_ATTRS_DEFAULT_VALUES_TEXT.equals(info.toString());
            final boolean nothingSelected =
                      info == null
                      || GuiComboBox.NOTHING_SELECTED.equals(info.toString());
            if (!nothingSelected
                && !defaultValues
                && info != savedMetaAttrInfoRef) {
                changed = true;
            } else {
                if ((nothingSelected || defaultValues)
                    && savedMetaAttrInfoRef != null) {
                    changed = true;
                }
                if (savedMetaAttrInfoRef == null
                    && defaultValues != allMetaAttrsAreDefaultValues) {
                    if (allMetaAttrsAreDefaultValues) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                sameAsMetaAttrsCB.setValue(
                                    META_ATTRS_DEFAULT_VALUES_TEXT);
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                sameAsMetaAttrsCB.setValue(
                                         GuiComboBox.NOTHING_SELECTED);
                            }
                        });
                    }
                }
            }
            sameAsMetaAttrsCB.processAccessType();
        }
        return changed;
    }

    /**
     * Returns operation default for parameter.
     */
    private String getOpDefaultsDefault(final String param) {
        /* if op_defaults is set... It cannot be set in the GUI  */
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (cs != null) {
            return cs.getOpDefaultsValuePairs().get(param);
        }
        return null;
    }

    /** Sets service parameters with values from resourceNode hash. */
    public void setParameters(final Map<String, String> resourceNode) {
        final boolean infoPanelOk = isInfoPanelOk();
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (crmXML == null) {
            Tools.appError("crmXML is null");
            return;
        }
        /* Attributes */
        final String[] params = crmXML.getParameters(resourceAgent,
                                                     getService().isMaster());
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (params != null) {
            boolean allMetaAttrsAreDefaultValues = true;
            boolean allSavedMetaAttrsAreDefaultValues = true;
            final String newMetaAttrsId = cs.getMetaAttrsId(
                                               getService().getHeartbeatId());
            if ((savedMetaAttrsId == null && newMetaAttrsId != null)
                || (savedMetaAttrsId != null
                    && !savedMetaAttrsId.equals(newMetaAttrsId))) {
                /* newly generated operations id, reload all other combo
                   boxes. */
                getBrowser().reloadAllComboBoxes(this);
            }
            savedMetaAttrsId = newMetaAttrsId;
            String refCRMId = cs.getMetaAttrsRef(getService().getHeartbeatId());
            final ServiceInfo metaAttrInfoRef =
                                getBrowser().getServiceInfoFromCRMId(refCRMId);
            if (refCRMId == null) {
                refCRMId = getService().getHeartbeatId();
            }
            resourceNode.put(PCMK_ID, getService().getHeartbeatId());
            resourceNode.put(GUI_ID, getService().getId());
            for (String param : params) {
                String value;
                if (isMetaAttr(param) && refCRMId != null) {
                    value = cs.getParameter(refCRMId, param, false);
                } else {
                    value = resourceNode.get(param);
                }
                final String defaultValue = getParamDefault(param);
                if (value == null) {
                    value = defaultValue;
                }
                if (value == null) {
                    value = "";
                }
                final String oldValue = getParamSaved(param);
                if (isMetaAttr(param)) {
                    if (!Tools.areEqual(defaultValue, value)) {
                        allMetaAttrsAreDefaultValues = false;
                    }
                    if (!Tools.areEqual(defaultValue, oldValue)) {
                        allSavedMetaAttrsAreDefaultValues = false;
                    }
                }
                if (infoPanelOk) {
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    final boolean haveChanged =
                       !Tools.areEqual(value, oldValue)
                       || !Tools.areEqual(defaultValue,
                                          getResource().getDefaultValue(param));
                    if (haveChanged
                        || (metaAttrInfoRef != null && isMetaAttr(param))) {
                        getResource().setValue(param, value);
                        getResource().setDefaultValue(param, defaultValue);
                        if (cb != null) {
                            cb.setValue(value);
                            if (haveChanged) {
                                cb.setEnabled(metaAttrInfoRef == null);
                            }
                        }
                    }
                }
            }
            if (!Tools.areEqual(metaAttrInfoRef, savedMetaAttrInfoRef)) {
                savedMetaAttrInfoRef = metaAttrInfoRef;
                if (sameAsMetaAttrsCB != null) {
                    if (metaAttrInfoRef == null) {
                        if (allMetaAttrsAreDefaultValues) {
                            if (!allSavedMetaAttrsAreDefaultValues) {
                                sameAsMetaAttrsCB.setValue(
                                            META_ATTRS_DEFAULT_VALUES_TEXT);
                            }
                        } else {
                            if (metaAttrInfoRef != null) {
                                sameAsMetaAttrsCB.setValue(
                                             GuiComboBox.NOTHING_SELECTED);
                            }
                        }
                    } else {
                        sameAsMetaAttrsCB.setValue(metaAttrInfoRef);
                    }
                }
            }
        }

        /* set scores */
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(
                                                getService().getHeartbeatId(),
                                                hi.getName(),
                                                false);
            final HostLocation savedLocation = savedHostLocations.get(hi);
            if (!Tools.areEqual(hostLocation, savedLocation)) {
                if (hostLocation == null) {
                    savedHostLocations.remove(hi);
                } else {
                    savedHostLocations.put(hi, hostLocation);
                }
                if (infoPanelOk) {
                    final GuiComboBox cb = scoreComboBoxHash.get(hi);
                    String score = null;
                    String op = null;
                    if (hostLocation != null) {
                        score = hostLocation.getScore();
                        op = hostLocation.getOperation();
                    }
                    cb.setValue(score);
                    final JLabel label = cb.getLabel();
                    final String text = getHostLocationLabel(hi.getName(), op);
                    label.setText(text);
                }
            }
        }

        boolean allAreDefaultValues = true;
        boolean allSavedAreDefaultValues = true;
        /* Operations */
        final String newOperationsId = cs.getOperationsId(
                                                getService().getHeartbeatId());
        if ((savedOperationsId == null && newOperationsId != null)
            || (savedOperationsId != null
                && !savedOperationsId.equals(newOperationsId))) {
            /* newly generated operations id, reload all other combo
               boxes. */
            getBrowser().reloadAllComboBoxes(this);
        }

        savedOperationsId = newOperationsId;
        String refCRMId = cs.getOperationsRef(getService().getHeartbeatId());
        final ServiceInfo operationIdRef =
                                getBrowser().getServiceInfoFromCRMId(refCRMId);
        if (refCRMId == null) {
            refCRMId = getService().getHeartbeatId();
        }
        try {
            mSavedOperationsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final String op : ClusterBrowser.HB_OPERATIONS) {
            for (final String param : getBrowser().getCRMOperationParams().get(
                                                                          op)) {
                String defaultValue =
                                   resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                    defaultValue = "";
                }
                String value = cs.getOperation(refCRMId, op, param);
                if (value == null || "".equals(value)) {
                    value = getOpDefaultsDefault(param);
                }
                if (value == null) {
                    value = "";
                }
                if (!defaultValue.equals(value)) {
                    allAreDefaultValues = false;
                }
                if (!defaultValue.equals(savedOperation.get(op, param))) {
                    allSavedAreDefaultValues = false;
                }
                if (!value.equals(savedOperation.get(op, param))) {
                    savedOperation.put(op, param, value);
                    if (infoPanelOk) {
                        final GuiComboBox cb =
                           (GuiComboBox) operationsComboBoxHash.get(op, param);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                cb.setEnabled(operationIdRef == null);
                            }
                        });
                        if (value != null) {
                            cb.setValue(value);
                        }
                    }
                }
            }
        }
        if (!Tools.areEqual(operationIdRef, savedOperationIdRef)) {
            savedOperationIdRef = operationIdRef;
            if (sameAsOperationsCB != null) {
                if (operationIdRef == null) {
                    if (allAreDefaultValues) {
                        if (!allSavedAreDefaultValues) {
                            sameAsOperationsCB.setValue(
                                       OPERATIONS_DEFAULT_VALUES_TEXT);
                        }
                    } else {
                        if (savedOperationIdRef != null) {
                            sameAsOperationsCB.setValue(
                                       GuiComboBox.NOTHING_SELECTED);
                        }
                    }
                } else {
                    sameAsOperationsCB.setValue(operationIdRef);
                }
            }
        }
        mSavedOperationsLock.release();
        getService().setAvailable();
        if (cs.isOrphaned(getHeartbeatId(false))) {
            getService().setOrphaned(true);
            getService().setNew(false);
            final CloneInfo ci = cloneInfo;
            if (ci != null) {
                ci.getService().setNew(false);
            }
        } else {
            getService().setOrphaned(false);
        }
    }

    /** Returns the main text that appears in the graph. */
    public String getMainTextForGraph() {
        return toString();
    }

    /** Returns name of this resource, that is used in logs. */
    public String getNameForLog() {
        return getName();
    }


    /**
     * Returns a name of the service with id in the parentheses.
     * It adds prefix 'new' if id is null.
     */
    public String toString() {
        final StringBuffer s = new StringBuffer(30);
        final String provider = resourceAgent.getProvider();
        if (!HB_HEARTBEAT_PROVIDER.equals(provider)
            && !"".equals(provider)) {
            s.append(provider);
            s.append(':');
        }
        s.append(getName());
        final String string = getService().getId();

        /* 'string' contains the last string if there are more dependent
         * resources, although there is usually only one. */
        if (string == null) {
            s.insert(0, "new ");
        } else {
            if (!"".equals(string)) {
                s.append(" (");
                s.append(string);
                s.append(')');
            }
        }
        return s.toString();
    }

    /**
     * Returns node name of the host where this service is running.
     */
    public List<String> getMasterOnNodes(final boolean testOnly) {
        return getBrowser().getClusterStatus().getMasterOnNodes(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
    }

    /**
     * Returns node name of the host where this service is running.
     */
    public List<String> getRunningOnNodes(final boolean testOnly) {
        return getBrowser().getClusterStatus().getRunningOnNodes(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
    }

   /**
    * Returns whether service is started.
    */
    public boolean isStarted(final boolean testOnly) {
        final Host dcHost = getBrowser().getDCHost();
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        String targetRoleString = "target-role";
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            targetRoleString = "target_role";
        }
        String crmId = getHeartbeatId(testOnly);
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String refCRMId = cs.getMetaAttrsRef(crmId);
        if (refCRMId != null) {
            crmId = refCRMId;
        }
        String targetRole = cs.getParameter(crmId, targetRoleString, testOnly);
        if (targetRole == null) {
            targetRole = getParamDefault(targetRoleString);
        }
        if (!CRMXML.TARGET_ROLE_STOPPED.equals(targetRole)) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether service is stopped.
     */
    public boolean isStopped(final boolean testOnly) {
        final Host dcHost = getBrowser().getDCHost();
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        String targetRoleString = "target-role";
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            targetRoleString = "target_role";
        }
        String crmId = getHeartbeatId(testOnly);
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String refCRMId = cs.getMetaAttrsRef(crmId);
        if (refCRMId != null) {
            crmId = refCRMId;
        }
        String targetRole = cs.getParameter(crmId, targetRoleString, testOnly);
        if (targetRole == null) {
            targetRole = getParamDefault(targetRoleString);
        }
        if (CRMXML.TARGET_ROLE_STOPPED.equals(targetRole)) {
            return true;
        }
        return false;
    }

    /** Returns whether the group is stopped. */
    public boolean isGroupStopped(final boolean testOnly) {
        return false;
    }

    /**
     * Returns whether service is managed.
     * TODO: "default" value
     */
    public boolean isManaged(final boolean testOnly) {
        return getBrowser().getClusterStatus().isManaged(
                                                    getHeartbeatId(testOnly),
                                                    testOnly);
    }

    /**
     * Returns whether the service where was migrated or null.
     */
    public List<Host> getMigratedTo(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (Host host : getBrowser().getClusterHosts()) {
            final String locationId = cs.getLocationId(getHeartbeatId(testOnly),
                                                 host.getName());
            if (locationId == null
                || (!locationId.startsWith("cli-prefer-")
                    && !locationId.startsWith("cli-standby-"))) {
                continue;
            }
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(
                                                      getHeartbeatId(testOnly),
                                                      hi.getName(),
                                                      testOnly);
            String score = null;
            String op = null;
            if (hostLocation != null) {
                score = hostLocation.getScore();
                op = hostLocation.getOperation();
            }
            if ((CRMXML.INFINITY_STRING.equals(score)
                 || CRMXML.PLUS_INFINITY_STRING.equals(score))
                && "eq".equals(op)) {
                final List<Host> hosts = new ArrayList<Host>();
                hosts.add(host);
                return hosts;
            }
        }
        return null;
    }

    /**
     * Returns whether the service where was migrated or null.
     */
    public List<Host> getMigratedFrom(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (Host host : getBrowser().getClusterHosts()) {
            final String locationId = cs.getLocationId(getHeartbeatId(testOnly),
                                                       host.getName());
            if (locationId == null
                || (!locationId.startsWith("cli-prefer-")
                    && !locationId.startsWith("cli-standby-"))) {
                continue;
            }
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(
                                                      getHeartbeatId(testOnly),
                                                      hi.getName(),
                                                      testOnly);
            String score = null;
            String op = null;
            if (hostLocation != null) {
                score = hostLocation.getScore();
                op = hostLocation.getOperation();
            }
            if (CRMXML.MINUS_INFINITY_STRING.equals(score)
                && "eq".equals(op)) {
                final List<Host> hosts = new ArrayList<Host>();
                hosts.add(host);
                return hosts;
            }
        }
        return null;
    }

    /**
     * Returns whether the service is running.
     */
    public boolean isRunning(final boolean testOnly) {
        final List<String> runningOnNodes = getRunningOnNodes(testOnly);
        return runningOnNodes != null && !runningOnNodes.isEmpty();
    }

    /**
     * Returns fail count string that appears in the graph.
     */
    private String getFailCountString(final String hostName,
                                      final boolean testOnly) {
        String fcString = "";
        final String failCount = getFailCount(hostName, testOnly);
        if (failCount != null) {
            if (CRMXML.INFINITY_STRING.equals(failCount)) {
                fcString = " failed";
            } else {
                fcString = " failed: " + failCount;
            }
        }
        return fcString;
    }


    /**
     * Returns fail count.
     */
    protected String getFailCount(final String hostName,
                                  final boolean testOnly) {

        final ClusterStatus cs = getBrowser().getClusterStatus();
        return cs.getFailCount(hostName, getHeartbeatId(testOnly), testOnly);
    }

    /**
     * Returns whether the resource failed on the specified host.
     */
    protected final boolean failedOnHost(final String hostName,
                                         final boolean testOnly) {
        final String failCount = getFailCount(hostName,
                                              testOnly);
        return failCount != null
               && CRMXML.INFINITY_STRING.equals(failCount);
    }

    /**
     * Returns whether the resource has failed to start.
     */
    public boolean isFailed(final boolean testOnly) {
        if (isRunning(testOnly)) {
            return false;
        }
        for (final Host host : getBrowser().getClusterHosts()) {
            if (host.isClStatus() && failedOnHost(host.getName(),
                                                  testOnly)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the resource has failed on one of the nodes.
     */
    public boolean isOneFailed(final boolean testOnly) {
        for (final Host host : getBrowser().getClusterHosts()) {
            if (failedOnHost(host.getName(), testOnly)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the resource has fail-count on one of the nodes.
     */
    public boolean isOneFailedCount(final boolean testOnly) {
        for (final Host host : getBrowser().getClusterHosts()) {
            if (getFailCount(host.getName(), testOnly) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets whether the service is managed.
     */
    public void setManaged(final boolean isManaged,
                           final Host dcHost,
                           final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.setManaged(dcHost, getHeartbeatId(testOnly), isManaged, testOnly);
    }

    /**
     * Returns color for the host vertex.
     */
    public List<Color> getHostColors(final boolean testOnly) {
        return getBrowser().getCluster().getHostColors(
                                                  getRunningOnNodes(testOnly));
    }

    /**
     * Returns service icon in the menu. It can be started or stopped.
     * TODO: broken icon, not managed icon.
     */
    public ImageIcon getMenuIcon(final boolean testOnly) {
        if (getBrowser().allHostsDown() || isStopped(testOnly)) {
            return SERVICE_STOPPED_ICON;
        }
        return SERVICE_STARTED_ICON;
    }

    /**
     * Gets saved host scores.
     */
    public Map<HostInfo, HostLocation> getSavedHostLocations() {
        return savedHostLocations;
    }

    /**
     * Returns list of all host names in this cluster.
     */
    public List<String> getHostNames() {
        final List<String> hostNames = new ArrayList<String>();
        final Enumeration e = getBrowser().getClusterHostsNode().children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                              (DefaultMutableTreeNode) e.nextElement();
            final String hostName = ((HostInfo) n.getUserObject()).getName();
            hostNames.add(hostName);
        }
        return hostNames;
    }

    /**
     * TODO: wrong doku
     * Converts enumeration to the info array, get objects from
     * hash if they exist.
     */
    protected Info[] enumToInfoArray(final Info defaultValue,
                                     final String serviceName,
                                     final Enumeration e) {
        final List<Info> list = new ArrayList<Info>();
        if (defaultValue != null) {
            list.add(defaultValue);
        }

        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                             (DefaultMutableTreeNode) e.nextElement();
            final Info i = (Info) n.getUserObject();
            final String name = i.getName();
            final ServiceInfo si = getBrowser().getServiceInfoFromId(
                                                                 serviceName,
                                                                 i.getName());

            if (si == null && !name.equals(defaultValue)) {
                list.add(i);
            }
        }
        return list.toArray(new Info[list.size()]);
    }

    /**
     * Stores scores for host.
     */
    private void storeHostLocations() {
        savedHostLocations.clear();
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final GuiComboBox cb = scoreComboBoxHash.get(hi);
            final String score = cb.getStringValue();
            final String op = getOpFromLabel(hi.getName(),
                                             cb.getLabel().getText());
            if (score == null || "".equals(score)) {
                savedHostLocations.remove(hi);
            } else {
                savedHostLocations.put(hi, new HostLocation(score, op));
            }
        }
    }

    /**
     * Returns thrue if an operation field changed.
     */
    private boolean checkOperationFieldsChanged() {
        boolean changed = false;
        boolean allAreDefaultValues = true;
        try {
            mSavedOperationsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final String op : ClusterBrowser.HB_OPERATIONS) {
            for (final String param : getBrowser().getCRMOperationParams().get(
                                                                          op)) {
                String defaultValue =
                                resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                    defaultValue = "";
                }
                final GuiComboBox cb =
                        (GuiComboBox) operationsComboBoxHash.get(op, param);
                if (cb == null) {
                    mSavedOperationsLock.release();
                    continue;
                }
                final Object[] defaultValueE = Tools.extractUnit(defaultValue);
                Object value = cb.getValue();
                if (Tools.areEqual(value, new Object[]{"", ""})) {
                    value = new Object[]{getOpDefaultsDefault(param), null};
                }
                if (!Tools.areEqual(value, defaultValueE)) {
                    allAreDefaultValues = false;
                }
                final String savedOp = (String) savedOperation.get(op, param);
                final Object[] savedOpE = Tools.extractUnit(savedOp);
                if (savedOp == null) {
                    if (!Tools.areEqual(value, defaultValueE)) {
                        changed = true;
                    }
                } else if (!Tools.areEqual(value, savedOpE)) {
                    changed = true;
                }
                cb.setBackground(defaultValueE, savedOpE, false);
            }
        }
        if (sameAsOperationsCB != null) {
            final Info info = (Info) sameAsOperationsCB.getValue();
            final boolean defaultValues =
                    info != null
                    && OPERATIONS_DEFAULT_VALUES_TEXT.equals(info.toString());
            final boolean nothingSelected =
                      info == null
                      || GuiComboBox.NOTHING_SELECTED.equals(info.toString());
            if (!nothingSelected
                && !defaultValues
                && info != savedOperationIdRef) {
                changed = true;
            } else {
                if ((nothingSelected || defaultValues)
                    && savedOperationIdRef != null) {
                    changed = true;
                }
                if (savedOperationIdRef == null
                    && defaultValues != allAreDefaultValues) {
                    if (allAreDefaultValues) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                sameAsOperationsCB.setValue(
                                       OPERATIONS_DEFAULT_VALUES_TEXT);
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                sameAsOperationsCB.setValue(
                                         GuiComboBox.NOTHING_SELECTED);
                            }
                        });
                    }
                }
            }
            sameAsOperationsCB.processAccessType();
        }
        mSavedOperationsLock.release();
        return changed;
    }

    /**
     * Returns true if some of the scores have changed.
     */
    private boolean checkHostLocationsFieldsChanged() {
        boolean changed = false;
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final GuiComboBox cb = scoreComboBoxHash.get(hi);
            final HostLocation hlSaved = savedHostLocations.get(hi);
            String hsSaved = null;
            String opSaved = null;
            if (hlSaved != null) {
                hsSaved = hlSaved.getScore();
                opSaved = hlSaved.getOperation();
            }
            final String opSavedLabel = getHostLocationLabel(host.getName(),
                                                             opSaved);
            if (cb == null) {
                continue;
            }
            if (!Tools.areEqual(hsSaved, cb.getStringValue())
                || (!Tools.areEqual(opSavedLabel, cb.getLabel().getText())
                    && (hsSaved != null  && !"".equals(hsSaved)))) {
                changed = true;
            }
            cb.setBackground(getHostLocationLabel(host.getName(), "eq"),
                             null,
                             opSavedLabel,
                             hsSaved,
                             false);
        }
        return changed;
    }

    /**
     * Returns the list of all services, that can be used in the 'add
     * service' action.
     */
    public List<ResourceAgent> getAddServiceList(final String cl) {
        return getBrowser().globalGetAddServiceList(cl);
    }

    /**
     * Returns info object of all block devices on all hosts that have the
     * same names and other attributes.
     */
    public Info[] getCommonBlockDevInfos(final Info defaultValue,
                                         final String serviceName) {
        final List<Info> list = new ArrayList<Info>();

        /* drbd resources */
        final Enumeration drbdResources = getBrowser().getDrbdNode().children();

        if (defaultValue != null) {
            list.add(defaultValue);
        }
        while (drbdResources.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                      (DefaultMutableTreeNode) drbdResources.nextElement();
            final CommonDeviceInterface drbdRes =
                                (CommonDeviceInterface) n.getUserObject();
            list.add((Info) drbdRes);
        }

        /* block devices that are the same on all hosts */
        final Enumeration cbds =
                        getBrowser().getCommonBlockDevicesNode().children();
        while (cbds.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                               (DefaultMutableTreeNode) cbds.nextElement();
            final CommonDeviceInterface cbd =
                                (CommonDeviceInterface) n.getUserObject();
            list.add((Info) cbd);
        }

        return list.toArray(new Info[list.size()]);
    }

    /**
     * Selects the node in the menu and reloads everything underneath.
     */
    public void selectMyself() {
        super.selectMyself();
        getBrowser().nodeChanged(getNode());
    }

    /**
     * Adds clone fields to the option pane.
     */
    protected void addCloneFields(final JPanel optionsPanel,
                                  final int leftWidth,
                                  final int rightWidth) {
        cloneInfo.paramComboBoxClear();

        final String[] params = cloneInfo.getParametersFromXML();
        final Info savedMAIdRef = cloneInfo.getSavedMetaAttrInfoRef();
        cloneInfo.getResource().setValue(GUI_ID,
                                         cloneInfo.getService().getId());
        cloneInfo.addParams(optionsPanel,
                            params,
                            ClusterBrowser.SERVICE_LABEL_WIDTH,
                            ClusterBrowser.SERVICE_FIELD_WIDTH,
                            cloneInfo.getSameAsFields(savedMAIdRef));
        if (!cloneInfo.getService().isNew()) {
            cloneInfo.paramComboBoxGet(GUI_ID, null).setEnabled(false);
        }
        for (final String param : params) {
            if (cloneInfo.isMetaAttr(param)) {
                final GuiComboBox cb = cloneInfo.paramComboBoxGet(param, null);
                cb.setEnabled(savedMAIdRef == null);
            }
        }

        cloneInfo.addHostLocations(optionsPanel,
                                   ClusterBrowser.SERVICE_LABEL_WIDTH,
                                   ClusterBrowser.SERVICE_FIELD_WIDTH);
    }

    /**
     * Returns label for host locations, which consist of host name and
     * operation.
     */
    private String getHostLocationLabel(final String hostName,
                                        final String op) {
        final StringBuffer sb = new StringBuffer(20);
        if (op == null || "eq".equals(op)) {
            sb.append("on ");
        } else if ("ne".equals(op)) {
            sb.append("NOT on ");
        } else {
            sb.append(op);
            sb.append(' ');
        }
        sb.append(hostName);
        return sb.toString();
    }

    /**
     * Creates host score combo boxes with labels, one per host.
     */
    protected void addHostLocations(final JPanel optionsPanel,
                                    final int leftWidth,
                                    final int rightWidth) {
        int rows = 0;
        scoreComboBoxHash.clear();

        final JPanel panel =
             getParamPanel(Tools.getString("ClusterBrowser.HostLocations"));
        panel.setLayout(new SpringLayout());

        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Map<String, String> abbreviations =
                                             new HashMap<String, String>();
            abbreviations.put("i", CRMXML.INFINITY_STRING);
            abbreviations.put("+", CRMXML.PLUS_INFINITY_STRING);
            abbreviations.put("I", CRMXML.INFINITY_STRING);
            abbreviations.put("a", "ALWAYS");
            abbreviations.put("n", "NEVER");
            final GuiComboBox cb =
                new GuiComboBox(null,
                                new String[]{null,
                                             "0",
                                             "2",
                                             "ALWAYS",
                                             "NEVER",
                                             CRMXML.INFINITY_STRING,
                                             CRMXML.MINUS_INFINITY_STRING,
                                             CRMXML.INFINITY_STRING},
                                null, /* units */
                                null, /* type */
                                "^((-?\\d*|(-|\\+)?" + CRMXML.INFINITY_STRING
                                + "))|ALWAYS|NEVER|@NOTHING_SELECTED@$",
                                rightWidth,
                                abbreviations,
                                ConfigData.AccessType.ADMIN);
            cb.setEditable(true);
            scoreComboBoxHash.put(hi, cb);

            /* set selected host scores in the combo box from
             * savedHostLocations */
            final HostLocation hl = savedHostLocations.get(hi);
            String hsSaved = null;
            if (hl != null) {
                hsSaved = hl.getScore();
            }
            cb.setValue(hsSaved);
        }

        /* host score combo boxes */
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final GuiComboBox cb = scoreComboBoxHash.get(hi);
            String op = null;
            final HostLocation hl = savedHostLocations.get(hi);
            if (hl != null) {
                op = hl.getOperation();
            }
            final String text = getHostLocationLabel(hi.getName(), op);
            final JLabel label = new JLabel(text);
            final String onText = getHostLocationLabel(hi.getName(), "eq");
            final String notOnText = getHostLocationLabel(hi.getName(), "ne");
            label.addMouseListener(new MouseListener() {
                public final void mouseClicked(final MouseEvent e) {
                    /* do nothing */
                }
                public final void mouseEntered(final MouseEvent e) {
                    /* do nothing */
                }
                public final void mouseExited(final MouseEvent e) {
                    /* do nothing */
                }
                public final void mousePressed(final MouseEvent e) {
                    final String currentText = label.getText();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (currentText.equals(onText)) {
                                label.setText(notOnText);
                            } else if (currentText.equals(notOnText)) {
                                label.setText(onText);
                            } else {
                                /* wierd things */
                                label.setText(onText);
                            }
                            final String[] params = getParametersFromXML();
                            applyButton.setEnabled(
                                checkResourceFields(CACHED_FIELD, params));
                        }
                    });
                }
                public final void mouseReleased(final MouseEvent e) {
                    /* do nothing */
                }
            });
            cb.setLabel(label);
            addField(panel,
                     label,
                     cb,
                     leftWidth,
                     rightWidth,
                     0);
            rows++;
        }

        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,           /* initX, initY */
                                        1, 1);          /* xPad, yPad */
        optionsPanel.add(panel);
    }

    /**
     * Returns whetrher this service's meta attributes are referenced by
     * some other service.
     */
    private boolean isMetaAttrReferenced() {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final Map<String, ServiceInfo> services =
                                    getBrowser().getHeartbeatIdToServiceInfo();
        for (final ServiceInfo si : services.values()) {
            final String refCRMId = cs.getMetaAttrsRef(
                                           si.getService().getHeartbeatId());
            if (refCRMId != null
                && refCRMId.equals(getService().getHeartbeatId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets meta attrs with same values as other service info, or default
     * values.
     */
    private void setMetaAttrsSameAs(final Info info) {
        if (sameAsMetaAttrsCB == null || info == null) {
            return;
        }
        boolean nothingSelected = false;
        if (GuiComboBox.NOTHING_SELECTED.equals(info.toString())) {
            nothingSelected = true;
        }
        boolean sameAs = true;
        if (META_ATTRS_DEFAULT_VALUES_TEXT.equals(info.toString())) {
            sameAs = false;
        }
        final String[] params = getParametersFromXML();
        if (params != null) {
            for (String param : params) {
                if (!isMetaAttr(param)) {
                    continue;
                }
                String defaultValue = getParamPreferred(param);
                if (defaultValue == null) {
                    defaultValue = getParamDefault(param);
                }
                final GuiComboBox cb = paramComboBoxGet(param, null);
                if (cb == null) {
                    continue;
                }
                Object oldValue = cb.getValue();
                if (oldValue == null) {
                    oldValue = defaultValue;
                }
                cb.setEnabled(!sameAs || nothingSelected);
                if (!nothingSelected) {
                    if (sameAs) {
                        /* same as some other service */
                        defaultValue =
                                 ((ServiceInfo) info).getParamSaved(param);
                    }
                    final String newValue = defaultValue;
                    if (!Tools.areEqual(oldValue, newValue)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (cb != null) {
                                    cb.setValue(newValue);
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * Returns whetrher this service's operations are referenced by some
     * other service.
     */
    private boolean isOperationReferenced() {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final Map<String, ServiceInfo> services =
                                    getBrowser().getHeartbeatIdToServiceInfo();
        for (final ServiceInfo si : services.values()) {
            final String refCRMId = cs.getOperationsRef(
                                        si.getService().getHeartbeatId());
            if (refCRMId != null
                && refCRMId.equals(getService().getHeartbeatId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns selected operations id reference.
     */
    private Info getSameServiceOpIdRef() {
        try {
            mSavedOperationsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final ServiceInfo savedOpIdRef = savedOperationIdRef;
        mSavedOperationsLock.release();
        return savedOpIdRef;
    }

    /**
     * Returns all services except this one, that are of the same type
     * for meta attributes.
     */
    private Info[] getSameServicesMetaAttrs() {
        final List<Info> sl = new ArrayList<Info>();
        sl.add(new StringInfo(GuiComboBox.NOTHING_SELECTED,
                              null,
                              getBrowser()));
        sl.add(new StringInfo(META_ATTRS_DEFAULT_VALUES_TEXT,
                              META_ATTRS_DEFAULT_VALUES,
                              getBrowser()));
        final Host dcHost = getBrowser().getDCHost();
        final String pmV = dcHost.getPacemakerVersion();
        final String hbV = dcHost.getHeartbeatVersion();

        if (isMetaAttrReferenced()
            || (pmV == null && Tools.compareVersions(hbV, "2.1.4") <= 0)) {
            return sl.toArray(new Info[sl.size()]);
        }
        getBrowser().lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash =
                        getBrowser().getNameToServiceInfoHash().get(getName());
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (idToInfoHash != null) {
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(
                                                  idToInfoHash.values())) {
                if (si != this
                    && cs.getMetaAttrsId(
                                si.getService().getHeartbeatId()) != null
                    && cs.getMetaAttrsRef(
                               si.getService().getHeartbeatId()) == null) {
                    sl.add(si);
                }
            }
        }
        final boolean clone = getResourceAgent().isClone();
        for (final String name
                          : getBrowser().getNameToServiceInfoHash().keySet()) {
            final Map<String, ServiceInfo> idToInfo =
                             getBrowser().getNameToServiceInfoHash().get(name);
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(
                                                  idToInfo.values())) {
                if (si != this
                    && !si.getName().equals(getName())
                    && si.getResourceAgent() != null
                    && si.getResourceAgent().isClone() == clone
                    && cs.getMetaAttrsId(
                               si.getService().getHeartbeatId()) != null
                    && cs.getMetaAttrsRef(
                               si.getService().getHeartbeatId()) == null) {
                    sl.add(si);
                }
            }
        }
        getBrowser().unlockNameToServiceInfo();
        return sl.toArray(new Info[sl.size()]);
    }

    /**
     * Returns all services except this one, that are of the same type
     * for operations.
     */
    private Info[] getSameServicesOperations() {
        final List<Info> sl = new ArrayList<Info>();
        sl.add(new StringInfo(GuiComboBox.NOTHING_SELECTED,
                              null,
                              getBrowser()));
        sl.add(new StringInfo(OPERATIONS_DEFAULT_VALUES_TEXT,
                              OPERATIONS_DEFAULT_VALUES,
                              getBrowser()));
        final Host dcHost = getBrowser().getDCHost();
        final String pmV = dcHost.getPacemakerVersion();
        final String hbV = dcHost.getHeartbeatVersion();
        if (isOperationReferenced()
            || (pmV == null && Tools.compareVersions(hbV, "2.1.4") <= 0)) {
            return sl.toArray(new Info[sl.size()]);
        }
        getBrowser().lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash =
                        getBrowser().getNameToServiceInfoHash().get(getName());
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (idToInfoHash != null) {
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(
                                                  idToInfoHash.values())) {
                if (si != this
                    && cs.getOperationsId(
                                si.getService().getHeartbeatId()) != null
                    && cs.getOperationsRef(
                               si.getService().getHeartbeatId()) == null) {
                    sl.add(si);
                }
            }
        }
        final boolean clone = getResourceAgent().isClone();
        for (final String name
                           : getBrowser().getNameToServiceInfoHash().keySet()) {
            final Map<String, ServiceInfo> idToInfo =
                             getBrowser().getNameToServiceInfoHash().get(name);
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(
                                                  idToInfo.values())) {
                if (si != this
                    && si.getResourceAgent() != null
                    && si.getResourceAgent().isClone() == clone
                    && !si.getName().equals(getName())
                    && cs.getOperationsId(
                               si.getService().getHeartbeatId()) != null
                    && cs.getOperationsRef(
                               si.getService().getHeartbeatId()) == null) {
                    sl.add(si);
                }
            }
        }
        getBrowser().unlockNameToServiceInfo();
        return sl.toArray(new Info[sl.size()]);
    }

    /**
     * Sets operations with same values as other service info, or default
     * values.
     */
    private void setOperationsSameAs(final Info info) {
        if (sameAsOperationsCB == null) {
            return;
        }
        boolean nothingSelected = false;
        if (info == null
            || GuiComboBox.NOTHING_SELECTED.equals(info.toString())) {
            nothingSelected = true;
        }
        boolean sameAs = true;
        if (info == null
            || OPERATIONS_DEFAULT_VALUES_TEXT.equals(info.toString())) {
            sameAs = false;
        }
        try {
            mSavedOperationsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final String op : ClusterBrowser.HB_OPERATIONS) {
            for (final String param : getBrowser().getCRMOperationParams().get(
                                                                         op)) {
                String defaultValue =
                                resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                    defaultValue = "";
                }
                final GuiComboBox cb =
                          (GuiComboBox) operationsComboBoxHash.get(op, param);
                final Object oldValue = cb.getValue();
                cb.setEnabled(!sameAs || nothingSelected);
                if (!nothingSelected) {
                    if (sameAs) {
                        /* same as some other service */
                        defaultValue = (String)
                          ((ServiceInfo) info).getSavedOperation().get(op,
                                                                       param);
                    }
                    final String newValue = defaultValue;
                    if (!Tools.areEqual(oldValue,
                                        Tools.extractUnit(newValue))) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (cb != null) {
                                    cb.setValue(newValue);
                                }
                            }
                        });
                    }
                }
            }
        }
        mSavedOperationsLock.release();
    }

    /**
     * Creates operations combo boxes with labels.
     */
    protected void addOperations(final JPanel optionsPanel,
                                 final int leftWidth,
                                 final int rightWidth) {
        int rows = 0;
        // TODO: need lock operationsComboBoxHash
        operationsComboBoxHash.clear();

        final JPanel sectionPanel = getParamPanel(
                                Tools.getString("ClusterBrowser.Operations"));
        //panel.setLayout(new SpringLayout());
        String defaultOpIdRef = null;
        final Info savedOpIdRef = getSameServiceOpIdRef();
        if (savedOpIdRef != null) {
            defaultOpIdRef = savedOpIdRef.toString();
        }
        sameAsOperationsCB = new GuiComboBox(defaultOpIdRef,
                                             getSameServicesOperations(),
                                             null, /* units */
                                             null, /* type */
                                             null, /* regexp */
                                             rightWidth,
                                             null, /* abbrv */
                                             ConfigData.AccessType.ADMIN);
        sameAsOperationsCB.setToolTipText(defaultOpIdRef);
        final JLabel label = new JLabel(Tools.getString(
                                           "ClusterBrowser.OperationsSameAs"));
        sameAsOperationsCB.setLabel(label);
        final JPanel saPanel = new JPanel(new SpringLayout());
        saPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        addField(saPanel,
                 label,
                 sameAsOperationsCB,
                 leftWidth,
                 rightWidth,
                 0);
        SpringUtilities.makeCompactGrid(saPanel, 1, 2,
                                        1, 1,  // initX, initY
                                        1, 1); // xPad, yPad
        sectionPanel.add(saPanel);
        boolean allAreDefaultValues = true;
        try {
            mSavedOperationsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final JPanel normalOpPanel = new JPanel(new SpringLayout());
        normalOpPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        int normalRows = 0;
        final JPanel advancedOpPanel = new JPanel(new SpringLayout());
        advancedOpPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        addToAdvancedList(advancedOpPanel);
        advancedOpPanel.setVisible(Tools.getConfigData().getExpertMode());
        int advancedRows = 0;
        for (final String op : ClusterBrowser.HB_OPERATIONS) {
            for (final String param : getBrowser().getCRMOperationParams().get(
                                                                          op)) {
                String defaultValue =
                                   resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                    defaultValue = "";
                }
                GuiComboBox.Type type;
                final String regexp = "^-?\\d*$";
                type = GuiComboBox.Type.TEXTFIELDWITHUNIT;
                // TODO: old style resources
                if (defaultValue == null) {
                    defaultValue = "0";
                }
                String savedValue = (String) savedOperation.get(op, param);
                if (!getService().isNew()
                    && (savedValue == null || "".equals(savedValue))) {
                    savedValue = getOpDefaultsDefault(param);
                    if (savedValue == null) {
                        savedValue = "";
                    }
                }
                if (!defaultValue.equals(savedValue)) {
                    allAreDefaultValues = false;
                }
                if (savedValue != null) {
                    defaultValue = savedValue;
                }
                final GuiComboBox cb = new GuiComboBox(
                                                defaultValue,
                                                 null, /* items */
                                                 getUnits(),
                                                 type,
                                                 regexp,
                                                 rightWidth,
                                                 null, /* abbrv */
                                                 ConfigData.AccessType.ADMIN);
                cb.setEnabled(savedOpIdRef == null);

                operationsComboBoxHash.put(op, param, cb);
                rows++;
                final JLabel cbLabel = new JLabel(Tools.ucfirst(op)
                                                  + " / "
                                                  + Tools.ucfirst(param));
                cb.setLabel(cbLabel);
                JPanel panel;
                if (ClusterBrowser.HB_OP_NOT_ADVANCED.containsKey(op, param)) {
                    panel = normalOpPanel;
                    normalRows++;
                } else {
                    panel = advancedOpPanel;
                    advancedRows++;
                }
                addField(panel,
                         cbLabel,
                         cb,
                         leftWidth,
                         rightWidth,
                         0);
            }
        }
        SpringUtilities.makeCompactGrid(normalOpPanel, normalRows, 2,
                                        1, 1,  // initX, initY
                                        1, 1); // xPad, yPad
        SpringUtilities.makeCompactGrid(advancedOpPanel, advancedRows, 2,
                                        1, 1,  // initX, initY
                                        1, 1); // xPad, yPad
        sectionPanel.add(normalOpPanel);
        sectionPanel.add(getMoreOptionsPanel(leftWidth + rightWidth + 4));
        sectionPanel.add(advancedOpPanel);
        mSavedOperationsLock.release();
        if (allAreDefaultValues && savedOpIdRef == null) {
            sameAsOperationsCB.setValue(OPERATIONS_DEFAULT_VALUES_TEXT);
        }
        sameAsOperationsCB.addListeners(
            new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                final Info info =
                                     (Info) sameAsOperationsCB.getValue();
                                setOperationsSameAs(info);
                                final String[] params = getParametersFromXML();
                                final boolean enable =
                                    checkResourceFields(CACHED_FIELD, params);
                                SwingUtilities.invokeLater(
                                new Runnable() {
                                    public void run() {
                                        applyButton.setEnabled(enable);
                                        if (info != null) {
                                            sameAsOperationsCB.setToolTipText(
                                                              info.toString());
                                        }
                                    }
                                });
                            }
                        });
                        thread.start();
                    }
                }
            },
            null
        );
        optionsPanel.add(sectionPanel);
    }

    /** Returns parameters. */
    public String[] getParametersFromXML() {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParameters(resourceAgent, getService().isMaster());
    }

    /** Returns the regexp of the parameter. */
    protected String getParamRegexp(String param) {
        if (isInteger(param)) {
            return "^((-?\\d*|(-|\\+)?" + CRMXML.INFINITY_STRING
                   + "|" + CRMXML.DISABLED_STRING
                   + "))|@NOTHING_SELECTED@$";
        }
        return null;
    }

    /**
     * Returns true if the value of the parameter is ok.
     */
    protected boolean checkParam(final String param,
                                 final String newValue) {
        if (param.equals("ip")
            && newValue != null
            && !Tools.isIp(newValue)) {
            return false;
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.checkParam(resourceAgent, param, newValue);
    }

    /**
     * Returns default value for specified parameter.
     */
    protected String getParamDefault(final String param) {
        if (isMetaAttr(param)) {
            final String paramDefault = getBrowser().getRscDefaultsInfo()
                                                 .getResource().getValue(param);
            if (paramDefault != null) {
                return paramDefault;
            }
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamDefault(resourceAgent, param);
    }

    /**
     * Returns saved value for specified parameter.
     */
    protected String getParamSaved(final String param) {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (isMetaAttr(param)) {
            final String crmId = getService().getHeartbeatId();
            final String refCRMId = clStatus.getMetaAttrsRef(crmId);
            if (refCRMId != null) {
                String value = clStatus.getParameter(refCRMId, param, false);
                if (value == null) {
                    value = getParamPreferred(param);
                    if (value == null) {
                        return getParamDefault(param);
                    }
                }
                return value;
            }
        }
        String value = super.getParamSaved(param);
        if (value == null) {
            value = clStatus.getParameter(getService().getHeartbeatId(),
                                    param,
                                    false);
            if (value == null) {
                value = getParamPreferred(param);
                if (value == null) {
                    return getParamDefault(param);
                }
            }
        }
        return value;
    }

    /**
     * Returns preferred value for specified parameter.
     */
    protected String getParamPreferred(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamPreferred(resourceAgent, param);
    }

    /**
     * Returns possible choices for drop down lists.
     */
    protected Object[] getParamPossibleChoices(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (isCheckBox(param)) {
            return crmXML.getCheckBoxChoices(resourceAgent, param);
        } else {
            final boolean ms = cloneInfo != null
                               && cloneInfo.getService().isMaster();
            return crmXML.getParamPossibleChoices(resourceAgent, param, ms);
        }
    }

    /**
     * Returns short description of the specified parameter.
     */
    protected String getParamShortDesc(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamShortDesc(resourceAgent, param);
    }

    /**
     * Returns long description of the specified parameter.
     */
    protected String getParamLongDesc(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamLongDesc(resourceAgent, param);
    }

    /**
     * Returns section to which the specified parameter belongs.
     */
    protected String getSection(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getSection(resourceAgent, param);
    }

    /** Returns true if the specified parameter is required. */
    protected boolean isRequired(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRequired(resourceAgent, param);
    }

    /** Returns whether this parameter is advanced. */
    protected boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param),
                            getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isAdvanced(resourceAgent, param);
    }

    /** Whether the parameter should be enabled. */
    protected final boolean isEnabled(final String param) {
        return true;
    }

    /** Returns access type of this parameter. */
    protected ConfigData.AccessType getAccessType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getAccessType(resourceAgent, param);
    }

    /**
     * Returns true if the specified parameter is meta attribute.
     */
    protected boolean isMetaAttr(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isMetaAttr(resourceAgent, param);
    }

    /** Returns true if the specified parameter is integer. */
    protected boolean isInteger(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isInteger(resourceAgent, param);
    }

    /** Returns true if the specified parameter is label. */
    protected boolean isLabel(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isLabel(resourceAgent, param);
    }

    /**
     * Returns true if the specified parameter is of time type.
     */
    protected boolean isTimeType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isTimeType(resourceAgent, param);
    }

    /**
     * Returns whether parameter is checkbox.
     */
    protected boolean isCheckBox(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isBoolean(resourceAgent, param);
    }

    /**
     * Returns the type of the parameter according to the OCF.
     */
    protected String getParamType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamType(resourceAgent, param);
    }

    /**
     * Returns the type of the parameter.
     */
    protected GuiComboBox.Type getFieldType(final String param) {
        return resourceAgent.getFieldType(param);
    }

    /**
     * Is called before the service is added. This is for example used by
     * FilesystemInfo so that it can add LinbitDrbdInfo or DrbddiskInfo
     * before it adds itself.
     */
    public void addResourceBefore(final Host dcHost, final boolean testOnly) {
        /* Override to add resource before this one. */
    }

    /** Change type to Master, Clone or Primitive. */
    protected final void changeType(final String value) {
        boolean masterSlave = false;
        boolean clone = false;
        if (MASTER_SLAVE_TYPE_STRING.equals(value)) {
            masterSlave = true;
            clone = true;
        } else if (CLONE_TYPE_STRING.equals(value)) {
            clone = true;
        }

        if (clone) {
            final CRMXML crmXML = getBrowser().getCRMXML();
            final CloneInfo oldCI = cloneInfo;
            String title = ConfigData.PM_CLONE_SET_NAME;
            if (masterSlave) {
                title = ConfigData.PM_MASTER_SLAVE_SET_NAME;
            }
            cloneInfo = new CloneInfo(crmXML.getHbClone(),
                                      title,
                                      masterSlave,
                                      getBrowser());
            if (oldCI == null) {
                getBrowser().getHeartbeatGraph().exchangeObjectInTheVertex(
                                                                     cloneInfo,
                                                                     this);
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getBrowser().getServicesNode().remove(oldCI.getNode());
                    }
                });
                getBrowser().getHeartbeatGraph().exchangeObjectInTheVertex(
                                                                     cloneInfo,
                                                                     oldCI);
            }
            cloneInfo.setCloneServicePanel(this);
            infoPanel = null;
            selectMyself();
        } else if (PRIMITIVE_TYPE_STRING.equals(value)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    cloneInfo.getNode().remove(getNode());
                    getBrowser().getServicesNode().remove(cloneInfo.getNode());
                }
            });
            getBrowser().getServicesNode().add(getNode());
            getBrowser().getHeartbeatGraph().exchangeObjectInTheVertex(
                                                                    this,
                                                                    cloneInfo);
            getBrowser().getHeartbeatIdToServiceInfo().remove(
                                    cloneInfo.getService().getHeartbeatId());
            getBrowser().removeFromServiceInfoHash(cloneInfo);
            cloneInfo = null;
            infoPanel = null;
            selectMyself();
        }
    }

    /** Adds host score listeners. */
    protected void addHostLocationsListeners() {
        final String[] params = getParametersFromXML();
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final GuiComboBox cb = scoreComboBoxHash.get(hi);
            cb.addListeners(
                new ItemListener() {
                    public void itemStateChanged(final ItemEvent e) {
                        if (cb.isCheckBox()
                            || e.getStateChange() == ItemEvent.SELECTED) {
                            final Thread thread = new Thread(new Runnable() {
                                public void run() {
                                    final boolean enable =
                                      checkResourceFields(CACHED_FIELD, params);
                                    SwingUtilities.invokeLater(
                                    new Runnable() {
                                        public void run() {
                                            cb.setEditable();
                                            applyButton.setEnabled(enable);
                                        }
                                    });
                                }
                            });
                            thread.start();
                        }
                    }
                },

                new DocumentListener() {
                    private void check() {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                final boolean enable =
                                    checkResourceFields(CACHED_FIELD, params);
                                SwingUtilities.invokeLater(
                                new Runnable() {
                                    public void run() {
                                        applyButton.setEnabled(enable);
                                    }
                                });
                            }
                        });
                        thread.start();
                    }

                    public void insertUpdate(final DocumentEvent e) {
                        check();
                    }

                    public void removeUpdate(final DocumentEvent e) {
                        check();
                    }

                    public void changedUpdate(final DocumentEvent e) {
                        check();
                    }
                }
            );
        }
    }

    /**
     * Adds listeners for operation and parameter.
     */
    private void addOperationListeners(final String op, final String param) {
        final String dv = resourceAgent.getOperationDefault(op, param);
        if (dv == null) {
            return;
        }
        final GuiComboBox cb =
                    (GuiComboBox) operationsComboBoxHash.get(op, param);
        final String[] params = getParametersFromXML();
        cb.addListeners(
            new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    if (cb.isCheckBox()
                        || e.getStateChange() == ItemEvent.SELECTED) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                final boolean enable = checkResourceFields(
                                                            CACHED_FIELD,
                                                            params);
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        applyButton.setEnabled(enable);
                                    }
                                });
                            }
                        });
                        thread.start();
                    }
                }
            },

            new DocumentListener() {
                private void check() {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            final boolean enable =
                                 checkResourceFields(CACHED_FIELD, params);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    applyButton.setEnabled(enable);
                                }
                            });
                        }
                    });
                    thread.start();
                }

                public void insertUpdate(final DocumentEvent e) {
                    check();
                }

                public void removeUpdate(final DocumentEvent e) {
                    check();
                }

                public void changedUpdate(final DocumentEvent e) {
                    check();
                }
            }
        );
    }

    /**
     * Returns "same as" fields for some sections. Currently only "meta
     * attributes".
     */
    protected final Map<String, GuiComboBox> getSameAsFields(
                                                final Info savedMAIdRef) {
        String defaultMAIdRef = null;
        if (savedMAIdRef != null) {
            defaultMAIdRef = savedMAIdRef.toString();
        }
        sameAsMetaAttrsCB = new GuiComboBox(defaultMAIdRef,
                                            getSameServicesMetaAttrs(),
                                            null, /* units */
                                            null, /* type */
                                            null, /* regexp */
                                            ClusterBrowser.SERVICE_FIELD_WIDTH,
                                            null, /* abbrv */
                                            ConfigData.AccessType.ADMIN);
        sameAsMetaAttrsCB.setToolTipText(defaultMAIdRef);
        final Map<String, GuiComboBox> sameAsFields =
                                            new HashMap<String, GuiComboBox>();
        sameAsFields.put("Meta Attributes", sameAsMetaAttrsCB);
        sameAsMetaAttrsCB.addListeners(
            new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                final Info info =
                                     (Info) sameAsMetaAttrsCB.getValue();
                                setMetaAttrsSameAs(info);
                                final String[] params =
                                                    getParametersFromXML();
                                final boolean enable =
                                  checkResourceFields(CACHED_FIELD, params);
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        applyButton.setEnabled(enable);
                                        if (info != null) {
                                            sameAsMetaAttrsCB.setToolTipText(
                                                              info.toString());
                                        }
                                    }
                                });
                            }
                        });
                        thread.start();
                    }
                }
            },
            null
        );
        return sameAsFields;
    }

    /**
     * Returns saved meta attributes reference to another service.
     */
    protected final Info getSavedMetaAttrInfoRef() {
        return savedMetaAttrInfoRef;
    }

    /**
     * Returns info panel with comboboxes for service parameters.
     */
    public JComponent getInfoPanel() {
        if (cloneInfo == null) {
            getBrowser().getHeartbeatGraph().pickInfo(this);
        } else {
            getBrowser().getHeartbeatGraph().pickInfo(cloneInfo);
        }
        if (infoPanel != null) {
            return infoPanel;
        }
        /* init save button */
        final boolean abExisted = applyButton != null;
        final ServiceInfo thisClass = this;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            /**
             * Whether the whole thing should be enabled.
             */
            public final boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                if (dcHost == null) {
                    return false;
                }
                final String pmV = dcHost.getPacemakerVersion();
                final String hbV = dcHost.getHeartbeatVersion();
                if (pmV == null
                    && hbV != null
                    && Tools.compareVersions(hbV, "2.1.4") <= 0) {
                    return false;
                }
                return true;
            }
            public final void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getHeartbeatGraph().stopTestAnimation(applyButton);
                applyButton.setToolTipText(null);
            }

            public final void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                applyButton.setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                applyButton.setToolTipBackground(Tools.getDefaultColor(
                                   "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getHeartbeatGraph().startTestAnimation(
                                                               applyButton,
                                                               startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                final ClusterStatus cs = getBrowser().getClusterStatus();
                cs.setPtestData(null);
                apply(dcHost, true);
                final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                applyButton.setToolTipText(ptestData.getToolTip());
                cs.setPtestData(ptestData);
                getBrowser().ptestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);
        if (cloneInfo != null) {
            cloneInfo.applyButton = applyButton;
        }
        /* add item listeners to the apply button. */
        if (!abExisted) {
            applyButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), false);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        JMenu serviceCombo;
        if (cloneInfo == null) {
            serviceCombo = getActionsMenu();
        } else {
            serviceCombo = cloneInfo.getActionsMenu();
        }
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);
        String defaultValue = PRIMITIVE_TYPE_STRING;
        if (cloneInfo != null) {
            if (cloneInfo.getService().isMaster()) {
                defaultValue = MASTER_SLAVE_TYPE_STRING;
            } else {
                defaultValue = CLONE_TYPE_STRING;
            }
        }
        if (!getResourceAgent().isClone() && getGroupInfo() == null) {
            typeRadioGroup = new GuiComboBox(
                                     defaultValue,
                                     new String[]{PRIMITIVE_TYPE_STRING,
                                                  CLONE_TYPE_STRING,
                                                  MASTER_SLAVE_TYPE_STRING},
                                     null, /* units */
                                     GuiComboBox.Type.RADIOGROUP,
                                     null, /* regexp */
                                     ClusterBrowser.SERVICE_LABEL_WIDTH
                                     + ClusterBrowser.SERVICE_FIELD_WIDTH,
                                     null, /* abbrv */
                                     ConfigData.AccessType.ADMIN);

            if (!getService().isNew()) {
                typeRadioGroup.setEnabled(false);
            }
            typeRadioGroup.addListeners(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            if (e.getStateChange() == ItemEvent.SELECTED) {
                                final String value =
                                    ((JRadioButton) e.getItem()).getText();
                                changeType(value);
                            }
                        }
                    });
                    thread.start();
                }
            }, null);
            final JPanel tp = new JPanel();
            tp.setBackground(ClusterBrowser.PANEL_BACKGROUND);
            tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
            tp.add(typeRadioGroup);
            typeRadioGroup.setBackgroundColor(ClusterBrowser.PANEL_BACKGROUND);
            optionsPanel.add(tp);
        }
        if (cloneInfo != null) {
            /* add clone fields */
            addCloneFields(optionsPanel,
                           ClusterBrowser.SERVICE_LABEL_WIDTH,
                           ClusterBrowser.SERVICE_FIELD_WIDTH);
        }
        getResource().setValue(GUI_ID, getService().getId());

        /* get dependent resources and create combo boxes for ones, that
         * need parameters */
        paramComboBoxClear();
        final String[] params = getParametersFromXML();
        final Info savedMAIdRef = savedMetaAttrInfoRef;
        addParams(optionsPanel,
                  params,
                  ClusterBrowser.SERVICE_LABEL_WIDTH,
                  ClusterBrowser.SERVICE_FIELD_WIDTH,
                  getSameAsFields(savedMAIdRef));
        if (cloneInfo == null) {
            /* score combo boxes */
            addHostLocations(optionsPanel,
                             ClusterBrowser.SERVICE_LABEL_WIDTH,
                             ClusterBrowser.SERVICE_FIELD_WIDTH);
        }

        for (final String param : params) {
            if (isMetaAttr(param)) {
                final GuiComboBox cb = paramComboBoxGet(param, null);
                cb.setEnabled(savedMAIdRef == null);
            }
        }
        if (!getService().isNew()) {
            paramComboBoxGet(GUI_ID, null).setEnabled(false);
        }
        if (!getResourceAgent().isGroup()
            && !getResourceAgent().isClone()) {
            /* Operations */
            addOperations(optionsPanel,
                          ClusterBrowser.SERVICE_LABEL_WIDTH,
                          ClusterBrowser.SERVICE_FIELD_WIDTH);
            /* add item listeners to the operations combos */
            for (final String op : ClusterBrowser.HB_OPERATIONS) {
                for (final String param
                              : getBrowser().getCRMOperationParams().get(op)) {
                    addOperationListeners(op, param);
                }
            }
        }
        /* add item listeners to the host scores combos */
        if (cloneInfo == null) {
            addHostLocationsListeners();
        } else {
            cloneInfo.addHostLocationsListeners();
        }
        /* apply button */
        addApplyButton(buttonPanel);
        applyButton.setEnabled(checkResourceFields(null, params));
        mainPanel.add(optionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        newPanel.add(new JScrollPane(mainPanel));
        /* if id textfield was changed and this id is not used,
         * enable apply button */
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /**
     * Clears the info panel cache, forcing it to reload.
     */
    public boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /**
     * Returns operation from host location label. "eq", "ne" etc.
     */
    private String getOpFromLabel(final String onHost,
                                  final String labelText) {
        final int l = labelText.length();
        final int k = onHost.length();
        String op = null;
        if (l > k) {
            final String labelPart = labelText.substring(0, l - k - 1);
            if ("on".equals(labelPart)) {
                op = "eq";
            } else if ("NOT on".equals(labelPart)) {
                op = "ne";
            } else {
                op = labelPart;
            }
        }
        return op;
    }

    /**
     * Goes through the scores and sets preferred locations.
     */
    protected void setLocations(final String heartbeatId,
                                final Host dcHost,
                                final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final GuiComboBox cb = scoreComboBoxHash.get(hi);
            String hs = cb.getStringValue();
            if ("ALWAYS".equals(hs)) {
                hs = CRMXML.INFINITY_STRING;
            } else if ("NEVER".equals(hs)) {
                hs = CRMXML.MINUS_INFINITY_STRING;
            }
            final HostLocation hlSaved = savedHostLocations.get(hi);
            String hsSaved = null;
            String opSaved = null;
            if (hlSaved != null) {
                hsSaved = hlSaved.getScore();
                opSaved = hlSaved.getOperation();
            }
            final String onHost = hi.getName();
            final String op = getOpFromLabel(onHost, cb.getLabel().getText());
            final HostLocation hostLoc = new HostLocation(hs, op);
            if (!hostLoc.equals(hlSaved)) {
                String locationId = cs.getLocationId(getHeartbeatId(testOnly),
                                                     onHost);
                if (((hs == null || "".equals(hs))
                    || !Tools.areEqual(op, opSaved))
                    && locationId != null) {
                    CRM.removeLocation(dcHost,
                                       locationId,
                                       getHeartbeatId(testOnly),
                                       hlSaved,
                                       testOnly);
                    locationId = null;
                }
                if (hs != null && !"".equals(hs)) {
                    CRM.setLocation(dcHost,
                                    getHeartbeatId(testOnly),
                                    onHost,
                                    hostLoc,
                                    locationId,
                                    testOnly);
                }
            }
        }
        if (!testOnly) {
            storeHostLocations();
        }
    }

    /**
     * Returns hash with changed operation ids and all name, value pairs.
     * This works for new heartbeats >= 2.99.0
     */
    private Map<String, Map<String, String>> getOperations(
                                                final String heartbeatId) {
        final Map<String, Map<String, String>> operations =
                              new LinkedHashMap<String, Map<String, String>>();

        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (final String op : ClusterBrowser.HB_OPERATIONS) {
            final Map<String, String> opHash =
                                           new LinkedHashMap<String, String>();
            String opId = cs.getOpId(heartbeatId, op);
            if (opId == null) {
                /* generate one */
                opId = "op-" + heartbeatId + "-" + op;
            }
            /* operations have different kind of default, that is
             * recommended, but not used by default. */
            boolean firstTime = true;
            for (final String param : ClusterBrowser.HB_OPERATION_PARAM_LIST) {
                if (getBrowser().getCRMOperationParams().get(op).contains(
                                                                       param)) {
                    if (cloneInfo == null
                        && (ClusterBrowser.HB_OP_DEMOTE.equals(op)
                            || ClusterBrowser.HB_OP_PROMOTE.equals(op))) {
                        continue;
                    }
                    final GuiComboBox cb =
                        (GuiComboBox) operationsComboBoxHash.get(op, param);
                    String value;
                    if (cb != null) {
                        value = cb.getStringValue();
                    } else {
                        value = "0";
                    }
                    if (value != null && !"".equals(value)) {
                        if (cb != null && firstTime) {
                            opHash.put("id", opId);
                            opHash.put("name", op);
                            firstTime = false;
                            operations.put(op, opHash);
                        }
                        opHash.put(param, value);
                    }
                }
            }
        }
        return operations;
    }

    /**
     * Returns id of the meta attrs to which meta attrs of this service are
     * referring to.
     */
    protected String getMetaAttrsRefId() {
        String metaAttrsRefId = null;
        if (sameAsMetaAttrsCB != null) {
            final Info i = (Info) sameAsMetaAttrsCB.getValue();
            if (!GuiComboBox.NOTHING_SELECTED.equals(i.toString())
                && !META_ATTRS_DEFAULT_VALUES_TEXT.equals(i.toString())) {
                final ServiceInfo si  = (ServiceInfo) i;
                final ClusterStatus cs = getBrowser().getClusterStatus();
                metaAttrsRefId = cs.getMetaAttrsId(
                                            si.getService().getHeartbeatId());
            }
        }
        return metaAttrsRefId;
    }

    /**
     * Returns id of the operations to which operations of this service are
     * referring to.
     */
    private String getOperationsRefId() {
        String operationsRefId = null;
        if (sameAsOperationsCB != null) {
            final Info i = (Info) sameAsOperationsCB.getValue();
            if (!GuiComboBox.NOTHING_SELECTED.equals(i.toString())
                && !OPERATIONS_DEFAULT_VALUES_TEXT.equals(i.toString())) {
                final ServiceInfo si  = (ServiceInfo) i;
                final ClusterStatus cs = getBrowser().getClusterStatus();
                operationsRefId = cs.getOperationsId(
                                            si.getService().getHeartbeatId());
            }
        }
        return operationsRefId;
    }

    /**
     * Applies the changes to the service parameters.
     */
    public void apply(final Host dcHost, final boolean testOnly) {
        /* TODO: make progress indicator per resource. */
        if (!testOnly) {
            setUpdated(true);
        }
        final String[] params = getParametersFromXML();
        String cloneId = null;
        String[] cloneParams = null;
        boolean master = false;
        final GroupInfo gInfo = groupInfo;
        CloneInfo ci = cloneInfo;
        String[] groupParams = null;
        if (gInfo != null) {
            ci = gInfo.getCloneInfo();
            groupParams = gInfo.getParametersFromXML();
        }
        final CloneInfo clInfo = ci;
        if (clInfo != null) {
            cloneId = clInfo.getHeartbeatId(testOnly);
            cloneParams = clInfo.getParametersFromXML();
            master = clInfo.getService().isMaster();

        }
        if (!testOnly) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                    applyButton.setToolTipText(null);
                    paramComboBoxGet(GUI_ID, null).setEnabled(false);
                    if (clInfo != null) {
                        clInfo.paramComboBoxGet(GUI_ID, null).setEnabled(
                                                                        false);
                    }
                }
            });

            /* add myself to the hash with service name and id as
             * keys */
            getBrowser().removeFromServiceInfoHash(this);
            final String oldHeartbeatId = getHeartbeatId(testOnly);
            if (oldHeartbeatId != null) {
                getBrowser().getHeartbeatIdToServiceInfo().remove(
                                                               oldHeartbeatId);
            }
            if (getService().isNew()) {
                final String id = getComboBoxValue(GUI_ID);
                getService().setIdAndCrmId(id);
                if (typeRadioGroup != null) {
                    typeRadioGroup.setEnabled(false);
                }
            }
            getBrowser().addNameToServiceInfoHash(this);
            getBrowser().addToHeartbeatIdList(this);
        }
        if (!testOnly) {
            addResourceBefore(dcHost, testOnly);
        }

        final Map<String, String> cloneMetaArgs =
                                            new LinkedHashMap<String, String>();
        final Map<String, String> groupMetaArgs =
                                            new LinkedHashMap<String, String>();
        final Map<String, String> pacemakerResAttrs =
                                            new LinkedHashMap<String, String>();
        final Map<String, String> pacemakerResArgs =
                                            new LinkedHashMap<String, String>();
        final Map<String, String> pacemakerMetaArgs =
                                            new LinkedHashMap<String, String>();
        final String raClass = getService().getResourceClass();
        final String type = getName();
        final String provider = resourceAgent.getProvider();
        final String heartbeatId = getHeartbeatId(testOnly);

        pacemakerResAttrs.put("id", heartbeatId);
        pacemakerResAttrs.put("class", raClass);
        if (!ClusterBrowser.HB_HEARTBEAT_CLASS.equals(raClass)
            && !raClass.equals(ClusterBrowser.HB_LSB_CLASS)
            && !raClass.equals(ClusterBrowser.HB_STONITH_CLASS)) {
            pacemakerResAttrs.put("provider", provider);
        }
        pacemakerResAttrs.put("type", type);
        String groupId = null; /* for pacemaker */
        if (gInfo != null) {
            if (gInfo.getService().isNew()) {
                gInfo.apply(dcHost, testOnly);
            }
            groupId = gInfo.getHeartbeatId(testOnly);
        }
        String cloneMetaAttrsRefIds = null;
        if (clInfo != null) {
            cloneMetaAttrsRefIds = clInfo.getMetaAttrsRefId();
        }
        String groupMetaAttrsRefIds = null;
        if (gInfo != null) {
            groupMetaAttrsRefIds = gInfo.getMetaAttrsRefId();
        }

        if (getService().isNew()) {
            if (clInfo != null) {
                for (String param : cloneParams) {
                    if (GUI_ID.equals(param)
                        || PCMK_ID.equals(param)) {
                        continue;
                    }
                    final String value = clInfo.getComboBoxValue(param);
                    if (value.equals(clInfo.getParamDefault(param))) {
                            continue;
                    }
                    if (!GUI_ID.equals(param) && !"".equals(value)) {
                        cloneMetaArgs.put(param, value);
                    }
                }
            }
            if (gInfo != null) {
                for (String param : groupParams) {
                    if (GUI_ID.equals(param)
                        || PCMK_ID.equals(param)) {
                        continue;
                    }
                    final String value = gInfo.getComboBoxValue(param);
                    if (value.equals(gInfo.getParamDefault(param))) {
                            continue;
                    }
                    if (!GUI_ID.equals(param) && !"".equals(value)) {
                        groupMetaArgs.put(param, value);
                    }
                }
            }
            for (final String param : params) {
                if (GUI_ID.equals(param)
                    || PCMK_ID.equals(param)) {
                    continue;
                }
                final String value = getComboBoxValue(param);
                if (value.equals(getParamDefault(param))) {
                    continue;
                }
                if (!"".equals(value)) {
                    /* for pacemaker */
                    if (isMetaAttr(param)) {
                        pacemakerMetaArgs.put(param, value);
                    } else {
                        pacemakerResArgs.put(param, value);
                    }
                }
            }
            String command = "-C";
            if ((gInfo != null && !gInfo.getService().isNew())
                || (clInfo != null && !clInfo.getService().isNew())) {
                command = "-U";
            }
            CRM.setParameters(dcHost,
                              command,
                              heartbeatId,
                              cloneId,
                              master,
                              cloneMetaArgs,
                              groupMetaArgs,
                              groupId,
                              pacemakerResAttrs,
                              pacemakerResArgs,
                              pacemakerMetaArgs,
                              null,
                              null,
                              getOperations(heartbeatId),
                              null,
                              getMetaAttrsRefId(),
                              cloneMetaAttrsRefIds,
                              groupMetaAttrsRefIds,
                              getOperationsRefId(),
                              resourceAgent.isStonith(),
                              testOnly);
            if (!testOnly) {
                getService().setNew(false);
            }
            if (gInfo == null) {
                String hbId = heartbeatId;
                if (clInfo != null) {
                    hbId = clInfo.getHeartbeatId(testOnly);
                }
                final List<Map<String, String>> colAttrsList =
                                       new ArrayList<Map<String, String>>();
                final List<Map<String, String>> ordAttrsList =
                                       new ArrayList<Map<String, String>>();
                final List<String> parentIds = new ArrayList<String>();
                final List<ServiceInfo> parents =
                            getBrowser().getHeartbeatGraph().getParents(this);
                for (final ServiceInfo parentInfo : parents) {
                    if (parentInfo.isConstraintPH()) {
                        final boolean colocationOnly = false;
                        final boolean orderOnly = false;
                        final List<ServiceInfo> with =
                                                 new ArrayList<ServiceInfo>();
                        with.add(this);
                        final List<ServiceInfo> withFrom =
                                                 new ArrayList<ServiceInfo>();
                        ((ConstraintPHInfo) parentInfo)
                                    .addConstraintWithPlaceholder(
                                             with,
                                             withFrom,
                                             colocationOnly,
                                             orderOnly,
                                             dcHost,
                                             !parentInfo.getService().isNew(),
                                             testOnly);
                    } else {
                        final String parentId =
                                    parentInfo.getService().getHeartbeatId();
                        parentIds.add(parentId);
                        final Map<String, String> colAttrs =
                                           new LinkedHashMap<String, String>();
                        final Map<String, String> ordAttrs =
                                           new LinkedHashMap<String, String>();
                        if (getBrowser().getHeartbeatGraph().isColocation(
                                                                    parentInfo,
                                                                    this)) {
                            colAttrs.put(CRMXML.SCORE_STRING,
                                         CRMXML.INFINITY_STRING);
                            if (parentInfo.getService().isMaster()) {
                                colAttrs.put("with-rsc-role", "Master");
                            }
                            colAttrsList.add(colAttrs);
                        } else {
                            colAttrsList.add(null);
                        }
                        if (getBrowser().getHeartbeatGraph().isOrder(parentInfo,
                                                                     this)) {
                            ordAttrs.put(CRMXML.SCORE_STRING,
                                         CRMXML.INFINITY_STRING);
                            if (parentInfo.getService().isMaster()) {
                                ordAttrs.put("first-action", "promote");
                                ordAttrs.put("then-action", "start");
                            }
                            ordAttrsList.add(ordAttrs);
                        } else {
                            ordAttrsList.add(null);
                        }
                    }
                }
                if (!parentIds.isEmpty()) {
                    CRM.setOrderAndColocation(dcHost,
                                              hbId,
                                              parentIds.toArray(
                                                 new String [parentIds.size()]),
                                              colAttrsList,
                                              ordAttrsList,
                                              testOnly);
                }
            } else {
                gInfo.resetPopup();
            }
        } else {
            if (clInfo != null) {
                for (String param : cloneParams) {
                    if (GUI_ID.equals(param)
                        || PCMK_ID.equals(param)) {
                        continue;
                    }
                    final String value = clInfo.getComboBoxValue(param);
                    if (value.equals(clInfo.getParamDefault(param))) {
                            continue;
                    }
                    if (!"".equals(value)) {
                        cloneMetaArgs.put(param, value);
                    }
                }
            }
            if (gInfo != null) {
                for (String param : groupParams) {
                    if (GUI_ID.equals(param)
                        || PCMK_ID.equals(param)) {
                        continue;
                    }
                    final String value = gInfo.getComboBoxValue(param);
                    if (value == null
                        || value.equals(gInfo.getParamDefault(param))) {
                        continue;
                    }
                    if (!"".equals(value)) {
                        groupMetaArgs.put(param, value);
                    }
                }
            }
            /* update parameters */
            final StringBuffer args = new StringBuffer("");
            for (String param : params) {
                if (GUI_ID.equals(param)
                    || PCMK_ID.equals(param)) {
                    continue;
                }
                final String value = getComboBoxValue(param);
                if (value.equals(getParamDefault(param))) {
                    continue;
                }

                if (!"".equals(value)) {
                    if (isMetaAttr(param)) {
                        pacemakerMetaArgs.put(param, value);
                    } else {
                        pacemakerResArgs.put(param, value);
                    }
                }
            }

            groupId = null; /* we don't want to replace the whole group */
            //TODO: should not be called if only host locations have
            //changed.
            final ClusterStatus cs = getBrowser().getClusterStatus();
            CRM.setParameters(
                        dcHost,
                        "-R",
                        heartbeatId,
                        cloneId,
                        master,
                        cloneMetaArgs,
                        groupMetaArgs,
                        groupId,
                        pacemakerResAttrs,
                        pacemakerResArgs,
                        pacemakerMetaArgs,
                        cs.getResourceInstanceAttrId(heartbeatId),
                        cs.getParametersNvpairsIds(heartbeatId),
                        getOperations(heartbeatId),
                        cs.getOperationsId(heartbeatId),
                        getMetaAttrsRefId(),
                        cloneMetaAttrsRefIds,
                        groupMetaAttrsRefIds,
                        getOperationsRefId(),
                        resourceAgent.isStonith(),
                        testOnly);
            if (isFailed(testOnly)) {
                cleanupResource(dcHost, testOnly);
            }
        }

        if (gInfo == null) {
            if (clInfo == null) {
                setLocations(heartbeatId, dcHost, testOnly);
            } else {
                clInfo.setLocations(heartbeatId, dcHost, testOnly);
            }
        } else {
            setLocations(heartbeatId, dcHost, testOnly);
        }
        if (!testOnly) {
            storeComboBoxValues(params);
            if (clInfo != null) {
                clInfo.storeComboBoxValues(cloneParams);
            }

            getBrowser().reload(getNode());
            getBrowser().getHeartbeatGraph().repaint();
            checkResourceFields(null, params);
        }
        getBrowser().reload(getNode());
    }

    /**
     * Removes order(s).
     */
    public void removeOrder(final ServiceInfo parent,
                            final Host dcHost,
                            final boolean testOnly) {
        if (getService().isNew() || parent.getService().isNew()) {
            return;
        }
        if (!testOnly
            && !getService().isNew() && !parent.getService().isNew()) {
            parent.setUpdated(true);
            setUpdated(true);
        }
        final ClusterStatus clStatus = getBrowser().getClusterStatus();

        String rscId;
        if (isConstraintPH()) {
            rscId = getId();
        } else {
            rscId = getHeartbeatId(testOnly);
        }
        if (isConstraintPH() || parent.isConstraintPH()) {
            ConstraintPHInfo cphi = null;
            if (isConstraintPH()) {
                cphi = (ConstraintPHInfo) this;
            } else {
                cphi = (ConstraintPHInfo) parent;
            }
            final Map<CRMXML.RscSet, Map<String, String>> rscSetsOrdAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
            final CRMXML.RscSetConnectionData rdata =
                                             cphi.getRscSetConnectionDataOrd();
            /** resource set */
            final String ordId = rdata.getConstraintId();
            String idToRemove;
            if (isConstraintPH()) {
                idToRemove = parent.getService().getHeartbeatId();
            } else {
                idToRemove = getService().getHeartbeatId();
            }
            CRMXML.RscSet modifiedRscSet = null;
            final List<CRMXML.RscSet> ordRscSets =
                                               clStatus.getRscSetsOrd(ordId);
            if (ordRscSets != null) {
                for (final CRMXML.RscSet rscSet : ordRscSets) {
                    if (rscSet.equals(rdata.getRscSet1())
                        || rscSet.equals(rdata.getRscSet2())) {
                        final List<String> newRscIds =
                                              new ArrayList<String>();
                        newRscIds.addAll(rscSet.getRscIds());
                        if (newRscIds.remove(idToRemove)) {
                            if (!testOnly) {
                                modifiedRscSet = rscSet;
                            }
                        }
                        if (!newRscIds.isEmpty()) {
                            final CRMXML.RscSet newRscSet =
                                    getBrowser().getCRMXML().new RscSet(
                                                   rscSet.getId(),
                                                   newRscIds,
                                                   rscSet.getSequential(),
                                                   rscSet.getOrderAction(),
                                                   rscSet.getColocationRole());
                            rscSetsOrdAttrs.put(newRscSet, null);
                        }
                    } else {
                        rscSetsOrdAttrs.put(rscSet, null);
                    }
                }
            }
            if (!testOnly && rscSetsOrdAttrs.isEmpty()) {
                cphi.getRscSetConnectionDataOrd().setConstraintId(null);
            }
            final Map<String, String> attrs =
                                        new LinkedHashMap<String, String>();
            final CRMXML.OrderData od = clStatus.getOrderData(ordId);
            if (od != null) {
                final String score = od.getScore();
                attrs.put(CRMXML.SCORE_STRING, score);
            }
            if (!testOnly) {
                ///* so that it will not be removed */
                cphi.setUpdated(false);
            }
            if (CRM.setRscSet(dcHost,
                              null,
                              false,
                              ordId,
                              false,
                              null,
                              rscSetsOrdAttrs,
                              attrs,
                              testOnly)) {
                if (modifiedRscSet != null) {
                    modifiedRscSet.removeRscId(idToRemove);
                }
            }
        } else {
            final String rscFirstId = parent.getHeartbeatId(testOnly);
            final List<CRMXML.OrderData> allData =
                                            clStatus.getOrderDatas(rscFirstId);
            if (allData != null) {
                for (final CRMXML.OrderData orderData : allData) {
                    final String orderId = orderData.getId();
                    final String rscThenId = orderData.getRscThen();
                    if (rscThenId.equals(getHeartbeatId(testOnly))) {
                        CRM.removeOrder(dcHost,
                                        orderId,
                                        testOnly);
                    }
                }
            }
        }
    }

    /**
     * Returns pacemaker id.
     */
    public final String getHeartbeatId(final boolean testOnly) {
        String heartbeatId = getService().getHeartbeatId();
        if (testOnly && heartbeatId == null) {
            heartbeatId = getService().getCrmIdFromId(getComboBoxValue(GUI_ID));
        }
        return heartbeatId;
    }

    /**
     * Adds order constraint from this service to the child.
     */
    public void addOrder(final ServiceInfo child,
                         final Host dcHost,
                         final boolean testOnly) {
        if (!testOnly
            && !getService().isNew() && !child.getService().isNew()) {
            child.setUpdated(true);
            setUpdated(true);
        }
        if (isConstraintPH() || child.isConstraintPH()) {
            if (isConstraintPH() && ((ConstraintPHInfo) this).isReversedCol()) {
                ((ConstraintPHInfo) this).reverseOrder();
            } else if (child.isConstraintPH()
                       && ((ConstraintPHInfo) child).isReversedCol()) {
                ((ConstraintPHInfo) child).reverseOrder();
            }
            final ConstraintPHInfo cphi;
            final ServiceInfo withService;
            final List<ServiceInfo> withFrom = new ArrayList<ServiceInfo>();
            if (isConstraintPH()) {
                cphi = (ConstraintPHInfo) this;
                withService = child;
            } else {
                cphi = (ConstraintPHInfo) child;
                withService = this;
                withFrom.add(this);
            }
            final List<ServiceInfo> with = new ArrayList<ServiceInfo>();
            with.add(withService);
            cphi.addConstraintWithPlaceholder(with,
                                              withFrom,
                                              false,
                                              true,
                                              dcHost,
                                              !cphi.getService().isNew(),
                                              testOnly);
        } else {
            final String childHbId = child.getHeartbeatId(testOnly);
            final Map<String, String> attrs =
                                          new LinkedHashMap<String, String>();
            attrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
            if (child.getCloneInfo() != null
                && child.getCloneInfo().getService().isMaster()) {
                attrs.put("first-action", "promote");
                attrs.put("then-action", "start");
            }
            CRM.addOrder(dcHost,
                         null, /* order id */
                         getHeartbeatId(testOnly),
                         childHbId,
                         attrs,
                         testOnly);
        }
    }

    /** Removes colocation(s). */
    public void removeColocation(final ServiceInfo parent,
                                 final Host dcHost,
                                 final boolean testOnly) {
        if (getService().isNew() || parent.getService().isNew()) {
            return;
        }
        if (!testOnly
            && !getService().isNew() && !parent.getService().isNew()) {
            parent.setUpdated(true);
            setUpdated(true);
        }
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        String rscId;
        if (isConstraintPH()) {
            rscId = getId();
        } else {
            rscId = getHeartbeatId(testOnly);
        }
        if (isConstraintPH() || parent.isConstraintPH()) {
            final Map<CRMXML.RscSet, Map<String, String>> rscSetsColAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
            ConstraintPHInfo cphi = null;
            if (isConstraintPH()) {
                cphi = (ConstraintPHInfo) this;
            } else {
                cphi = (ConstraintPHInfo) parent;
            }
            final CRMXML.RscSetConnectionData rdata =
                                             cphi.getRscSetConnectionDataCol();
            /** resource set */
            final String colId = rdata.getConstraintId();
            String idToRemove;
            if (isConstraintPH()) {
                idToRemove = parent.getService().getHeartbeatId();
            } else {
                idToRemove = getService().getHeartbeatId();
            }
            CRMXML.RscSet modifiedRscSet = null;
            final List<CRMXML.RscSet> colRscSets =
                                               clStatus.getRscSetsCol(colId);
            if (colRscSets != null) {
                for (final CRMXML.RscSet rscSet : colRscSets) {
                    if (rscSet.equals(rdata.getRscSet1())
                        || rscSet.equals(rdata.getRscSet2())) {
                        final List<String> newRscIds =
                                              new ArrayList<String>();
                        newRscIds.addAll(rscSet.getRscIds());
                        if (newRscIds.remove(idToRemove)) {
                            if (!testOnly) {
                                modifiedRscSet = rscSet;
                            }
                        }
                        if (!newRscIds.isEmpty()) {
                            final CRMXML.RscSet newRscSet =
                                    getBrowser().getCRMXML().new RscSet(
                                                   rscSet.getId(),
                                                   newRscIds,
                                                   rscSet.getSequential(),
                                                   rscSet.getOrderAction(),
                                                   rscSet.getColocationRole());
                            rscSetsColAttrs.put(newRscSet, null);
                        }
                    } else {
                        rscSetsColAttrs.put(rscSet, null);
                    }
                }
            }
            if (!testOnly && rscSetsColAttrs.isEmpty()) {
                cphi.getRscSetConnectionDataCol().setConstraintId(null);
            }
            final Map<String, String> attrs =
                                        new LinkedHashMap<String, String>();
            final CRMXML.ColocationData cd = clStatus.getColocationData(colId);
            if (cd != null) {
                final String score = cd.getScore();
                attrs.put(CRMXML.SCORE_STRING, score);
            }
            if (!testOnly) {
                cphi.setUpdated(false);
            }
            if (CRM.setRscSet(dcHost,
                              colId,
                              false,
                              null,
                              false,
                              rscSetsColAttrs,
                              null,
                              attrs,
                              testOnly)) {
                if (modifiedRscSet != null) {
                    modifiedRscSet.removeRscId(idToRemove);
                }
            }
        } else {
            final List<CRMXML.ColocationData> allData =
                                            clStatus.getColocationDatas(rscId);
            if (allData != null) {
                for (final CRMXML.ColocationData colocationData
                                                       : allData) {
                    final String colId = colocationData.getId();
                    final String withRscId =
                                       colocationData.getWithRsc();
                    if (withRscId.equals(
                                parent.getHeartbeatId(testOnly))) {
                        CRM.removeColocation(dcHost,
                                             colId,
                                             testOnly);
                    }
                }
            }
        }
    }

    /**
     * Adds colocation constraint from this service to the parent. The
     * parent - child order is here important, in case colocation
     * constraint is used along with order constraint.
     */
    public void addColocation(final ServiceInfo parent,
                              final Host dcHost,
                              final boolean testOnly) {
        if (!testOnly
            && !getService().isNew() && !parent.getService().isNew()) {
            parent.setUpdated(true);
            setUpdated(true);
        }
        if (isConstraintPH() || parent.isConstraintPH()) {
            if (isConstraintPH() && ((ConstraintPHInfo) this).isReversedOrd()) {
                ((ConstraintPHInfo) this).reverseColocation();
            } else if (parent.isConstraintPH()
                       && ((ConstraintPHInfo) parent).isReversedOrd()) {
                ((ConstraintPHInfo) parent).reverseColocation();
            }
            final ConstraintPHInfo cphi;
            final ServiceInfo withService;
            final List<ServiceInfo> withFrom = new ArrayList<ServiceInfo>();
            if (isConstraintPH()) {
                cphi = (ConstraintPHInfo) this;
                withService = parent;
            } else {
                cphi = (ConstraintPHInfo) parent;
                withService = this;
                withFrom.add(this);
            }
            final List<ServiceInfo> with = new ArrayList<ServiceInfo>();
            with.add(withService);
            cphi.addConstraintWithPlaceholder(with,
                                              withFrom,
                                              true,
                                              false,
                                              dcHost,
                                              !cphi.getService().isNew(),
                                              testOnly);
        } else {
            final String parentHbId = parent.getHeartbeatId(testOnly);
            final Map<String, String> attrs =
                                        new LinkedHashMap<String, String>();
            attrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
            if (parent.getCloneInfo() != null
                && parent.getCloneInfo().getService().isMaster()) {
                attrs.put("with-rsc-role", "Master");
            }
            CRM.addColocation(dcHost,
                              null, /* col id */
                              parentHbId,
                              getHeartbeatId(testOnly),
                              attrs,
                              testOnly);
        }
    }

    /** Returns panel with graph. */
    public JPanel getGraphicalView() {
        return getBrowser().getHeartbeatGraph().getGraphPanel();
    }

    /** Adds service panel to the position 'pos'. */
    public ServiceInfo addServicePanel(final ResourceAgent newRA,
                                       final Point2D pos,
                                       final boolean colocationOnly,
                                       final boolean orderOnly,
                                       final boolean reloadNode,
                                       final boolean master,
                                       final boolean testOnly) {
        ServiceInfo newServiceInfo;

        final String name = newRA.getName();
        if (newRA.isFilesystem()) {
            newServiceInfo = new FilesystemInfo(name, newRA, getBrowser());
        } else if (newRA.isLinbitDrbd()) {
            newServiceInfo = new LinbitDrbdInfo(name, newRA, getBrowser());
        } else if (newRA.isDrbddisk()) {
            newServiceInfo = new DrbddiskInfo(name, newRA, getBrowser());
        } else if (newRA.isIPaddr()) {
            newServiceInfo = new IPaddrInfo(name, newRA, getBrowser());
        } else if (newRA.isVirtualDomain()) {
            newServiceInfo = new VirtualDomainInfo(name, newRA, getBrowser());
        } else if (newRA.isGroup()) {
            newServiceInfo = new GroupInfo(newRA, getBrowser());
        } else if (newRA.isClone()) {
            String cloneName;
            if (master) {
                cloneName = ConfigData.PM_MASTER_SLAVE_SET_NAME;
            } else {
                cloneName = ConfigData.PM_CLONE_SET_NAME;
            }
            newServiceInfo = new CloneInfo(newRA,
                                           cloneName,
                                           master,
                                           getBrowser());
        } else {
            newServiceInfo = new ServiceInfo(name, newRA, getBrowser());
        }
        getBrowser().addToHeartbeatIdList(newServiceInfo);

        addServicePanel(newServiceInfo,
                        pos,
                        colocationOnly,
                        orderOnly,
                        reloadNode,
                        getBrowser().getDCHost(),
                        testOnly);
        return newServiceInfo;
    }

    /**
     * Adds service panel to the position 'pos'.
     * TODO: is it used?
     */
    public void addServicePanel(final ServiceInfo serviceInfo,
                                final Point2D pos,
                                final boolean colocationOnly,
                                final boolean orderOnly,
                                final boolean reloadNode,
                                final Host dcHost,
                                final boolean testOnly) {
        final ResourceAgent ra = serviceInfo.getResourceAgent();
        if (ra != null) {
            serviceInfo.getService().setResourceClass(ra.getResourceClass());
        }
        if (getBrowser().getHeartbeatGraph().addResource(serviceInfo,
                                                         this,
                                                         pos,
                                                         colocationOnly,
                                                         orderOnly,
                                                         testOnly)) {
            /* edge added */
            if (isConstraintPH() || serviceInfo.isConstraintPH()) {
                final ConstraintPHInfo cphi;
                final ServiceInfo withService;
                final List<ServiceInfo> withFrom = new ArrayList<ServiceInfo>();
                if (isConstraintPH()) {
                    cphi = (ConstraintPHInfo) this;
                    withService = serviceInfo;
                } else {
                    cphi = (ConstraintPHInfo) serviceInfo;
                    withService = this;
                    withFrom.add(this);
                }
                final List<ServiceInfo> with = new ArrayList<ServiceInfo>();
                with.add(withService);
                cphi.addConstraintWithPlaceholder(with,
                                                  withFrom,
                                                  colocationOnly,
                                                  orderOnly,
                                                  dcHost,
                                                  !cphi.getService().isNew(),
                                                  testOnly);
                if (!testOnly) {
                    final PcmkRscSetsInfo prsi = cphi.getPcmkRscSetsInfo();
                    final boolean enabled = prsi.checkResourceFields(null,
                                                 prsi.getParametersFromXML());
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            final MyButton ab = prsi.getApplyButton();
                            if (ab != null) {
                                ab.setEnabled(enabled);
                            }
                        }
                    });
                }
            } else {
                final String parentId = getHeartbeatId(testOnly);
                final String heartbeatId = serviceInfo.getHeartbeatId(testOnly);
                final List<Map<String, String>> colAttrsList =
                                          new ArrayList<Map<String, String>>();
                final List<Map<String, String>> ordAttrsList =
                                          new ArrayList<Map<String, String>>();
                final Map<String, String> colAttrs =
                                           new LinkedHashMap<String, String>();
                final Map<String, String> ordAttrs =
                                           new LinkedHashMap<String, String>();
                colAttrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
                ordAttrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
                if (getService().isMaster()) {
                    colAttrs.put("with-rsc-role", "Master");
                    ordAttrs.put("first-action", "promote");
                    ordAttrs.put("then-action", "start");
                }
                if (colocationOnly) {
                    colAttrsList.add(colAttrs);
                    ordAttrsList.add(null);
                } else if (orderOnly) {
                    ordAttrsList.add(ordAttrs);
                    colAttrsList.add(null);
                } else {
                    colAttrsList.add(colAttrs);
                    ordAttrsList.add(ordAttrs);
                }
                if (!getService().isNew()
                    && !serviceInfo.getService().isNew()) {
                    CRM.setOrderAndColocation(dcHost,
                                              heartbeatId,
                                              new String[]{parentId},
                                              colAttrsList,
                                              ordAttrsList,
                                              testOnly);
                }
            }
        } else {
            getBrowser().addNameToServiceInfoHash(serviceInfo);
            final DefaultMutableTreeNode newServiceNode =
                                    new DefaultMutableTreeNode(serviceInfo);
            serviceInfo.setNode(newServiceNode);

            getBrowser().getServicesNode().add(newServiceNode);
            if (reloadNode) {
                getBrowser().reload(getBrowser().getServicesNode());
                getBrowser().reload(newServiceNode);
            }
            getBrowser().reloadAllComboBoxes(serviceInfo);
        }
        if (reloadNode && ra != null && ra.isProbablyMasterSlave()) {
            serviceInfo.changeType(MASTER_SLAVE_TYPE_STRING);
        }
        getBrowser().getHeartbeatGraph().reloadServiceMenus();
        if (reloadNode) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getBrowser().getHeartbeatGraph().scale();
                }
            });
        }
    }

    /**
     * Returns service that belongs to this info object.
     */
    public Service getService() {
        return (Service) getResource();
    }

    /**
     * Starts resource in crm.
     */
    public void startResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.startResource(dcHost, getHeartbeatId(testOnly), testOnly);
    }

    /**
     * Stops resource in crm.
     */
    public void stopResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.stopResource(dcHost, getHeartbeatId(testOnly), testOnly);
    }

    /**
     * Migrates resource in cluster from current location.
     */
    public void migrateResource(final String onHost,
                                final Host dcHost,
                                final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.migrateResource(dcHost,
                            getHeartbeatId(testOnly),
                            onHost,
                            testOnly);
    }

    /**
     * Migrates resource in heartbeat from current location.
     */
    public void migrateFromResource(final Host dcHost,
                                    final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.migrateFromResource(dcHost,
                                getHeartbeatId(testOnly),
                                testOnly);
    }

    /**
     * Migrates resource in cluster from current location with --force option.
     */
    public void forceMigrateResource(final String onHost,
                                     final Host dcHost,
                                     final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.forceMigrateResource(dcHost,
                                 getHeartbeatId(testOnly),
                                 onHost,
                                 testOnly);
    }

    /**
     * Removes constraints created by resource migrate command.
     */
    public void unmigrateResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.unmigrateResource(dcHost, getHeartbeatId(testOnly), testOnly);
    }

    /**
     * Moves resource up in the group.
     */
    public void moveGroupResUp(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.moveGroupResUp(dcHost, getHeartbeatId(testOnly));
    }

    /**
     * Moves resource down in the group.
     */
    public void moveGroupResDown(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.moveGroupResDown(dcHost, getHeartbeatId(testOnly));
    }

    /**
     * Cleans up the resource.
     */
    public void cleanupResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        final List<Host> dirtyHosts = new ArrayList<Host>();
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (final Host host : getBrowser().getClusterHosts()) {
            dirtyHosts.add(host);
        }
        final String rscId = getHeartbeatId(testOnly);
        final Set<String> failedClones = cs.getFailedClones(rscId, testOnly);
        if (failedClones == null) {
            CRM.cleanupResource(dcHost,
                                rscId,
                                dirtyHosts.toArray(new Host[dirtyHosts.size()]),
                                testOnly);
        } else {
            for (final String fc : failedClones) {
                CRM.cleanupResource(
                                dcHost,
                                rscId + ":" + fc,
                                dirtyHosts.toArray(new Host[dirtyHosts.size()]),
                                testOnly);
            }
        }
    }

    /** Removes the service without confirmation dialog. */
    protected void removeMyselfNoConfirm(final Host dcHost,
                                         final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
            getService().setRemoved(true);
        }
        final CloneInfo ci = cloneInfo;
        if (ci != null) {
            ci.removeMyselfNoConfirm(dcHost, testOnly);
            cloneInfo = null;
        }

        if (getService().isNew() && groupInfo == null) {
            if (!testOnly) {
                getService().setNew(false);
                getBrowser().getHeartbeatGraph().killRemovedVertices();
            }
        } else {
            final ClusterStatus cs = getBrowser().getClusterStatus();
            if (groupInfo == null) {
                final HbConnectionInfo[] hbcis =
                       getBrowser().getHeartbeatGraph().getHbConnections(this);
                for (final HbConnectionInfo hbci : hbcis) {
                    if (hbci != null) {
                        getBrowser().getHeartbeatGraph().removeOrder(hbci,
                                                                     dcHost,
                                                                     testOnly);
                        getBrowser().getHeartbeatGraph().removeColocation(
                                                                      hbci,
                                                                      dcHost,
                                                                      testOnly);
                    }
                }

                for (final String locId : cs.getLocationIds(
                                              getHeartbeatId(testOnly))) {
                    // TODO: remove locationMap, is null anyway
                    final HostLocation loc = cs.getHostLocationFromId(locId);
                    CRM.removeLocation(dcHost,
                                       locId,
                                       getHeartbeatId(testOnly),
                                       loc,
                                       testOnly);
                }
            }
            if (!getResourceAgent().isGroup()
                && !getResourceAgent().isClone()) {
                String groupId = null; /* for pacemaker */
                if (groupInfo != null) {
                    /* get group id only if there is only one resource in a
                     * group.
                     */
                    if (getService().isNew()) {
                        if (!testOnly) {
                            super.removeMyself(false);
                        }
                    } else {
                        final String group = groupInfo.getHeartbeatId(testOnly);
                        final Enumeration e = groupInfo.getNode().children();
                        while (e.hasMoreElements()) {
                            final DefaultMutableTreeNode n =
                                      (DefaultMutableTreeNode) e.nextElement();
                            final ServiceInfo child =
                                               (ServiceInfo) n.getUserObject();
                            child.getService().setModified(true);
                            child.getService().doneModifying();
                        }
                        if (cs.getGroupResources(group, testOnly).size() == 1) {
                            if (!testOnly) {
                                groupInfo.getService().setRemoved(true);
                            }
                            groupInfo.removeMyselfNoConfirmFromChild(dcHost,
                                                                     testOnly);
                            groupId = group;
                            groupInfo.getService().doneRemoving();
                        }
                    }
                    groupInfo.resetPopup();
                }
                if (!getService().isNew()) {
                    String cloneId = null;
                    boolean master = false;
                    if (ci != null) {
                        cloneId = ci.getHeartbeatId(testOnly);
                        master = ci.getService().isMaster();
                    }
                    final boolean ret = CRM.removeResource(
                                                      dcHost,
                                                      getHeartbeatId(testOnly),
                                                      groupId,
                                                      cloneId,
                                                      master,
                                                      testOnly);
                    cleanupResource(dcHost, testOnly);
                    if (!testOnly && !ret) {
                        Tools.progressIndicatorFailed(dcHost.getName(),
                                                      "removing failed");
                    }
                }
            }
        }
        if (!testOnly) {
            getBrowser().removeFromServiceInfoHash(this);
            infoPanel = null;
            getService().doneRemoving();
            getBrowser().reloadAllComboBoxes(this);
        }
    }

    /** Removes this service from the crm with confirmation dialog. */
    public void removeMyself(final boolean testOnly) {
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            getService().setNew(false);
            return;
        }
        String desc = Tools.getString(
                        "ClusterBrowser.confirmRemoveService.Description");

        desc  = desc.replaceAll("@SERVICE@", toString());
        if (Tools.confirmDialog(
               Tools.getString("ClusterBrowser.confirmRemoveService.Title"),
               desc,
               Tools.getString("ClusterBrowser.confirmRemoveService.Yes"),
               Tools.getString("ClusterBrowser.confirmRemoveService.No"))) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            getService().setNew(false);
        }
    }

    /**
     * Removes the service from some global hashes and lists.
     */
    public void removeInfo() {
        getBrowser().getHeartbeatIdToServiceInfo().remove(
                                                getService().getHeartbeatId());
        getBrowser().removeFromServiceInfoHash(this);
        super.removeMyself(false);
    }

    /**
     * Sets this service as part of a group.
     */
    public void setGroupInfo(final GroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }

    /**
     * Sets this service as part of a clone set.
     */
    public void setCloneInfo(final CloneInfo cloneInfo) {
        this.cloneInfo = cloneInfo;
    }

    /**
     * Returns the group to which this service belongs or null, if it is
     * not in any group.
     */
    public GroupInfo getGroupInfo() {
        return groupInfo;
    }

    /**
     * Returns the clone set to which this service belongs
     * or null, if it is not in such set.
     */
    public CloneInfo getCloneInfo() {
        return cloneInfo;
    }

    /** Adds existing service menu item for every member of a group. */
    protected void addExistingGroupServiceMenuItems(
                        final ServiceInfo asi,
                        final DefaultListModel dlm,
                        final Map<MyMenuItem, ButtonCallback> callbackHash,
                        final MyList list,
                        final boolean colocationOnly,
                        final boolean orderOnly,
                        final boolean testOnly) {
    }

    /** Adds existing service menu item. */
    protected void addExistingServiceMenuItem(
                        final String name,
                        final ServiceInfo asi,
                        final DefaultListModel dlm,
                        final Map<MyMenuItem, ButtonCallback> callbackHash,
                        final MyList list,
                        final boolean colocationOnly,
                        final boolean orderOnly,
                        final boolean testOnly) {
        final MyMenuItem mmi = new MyMenuItem(name,
                                              null,
                                              null,
                                              ConfigData.AccessType.ADMIN,
                                              ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;
            public void action() {
                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                getPopup().setVisible(false);
                            }
                        });
                        addServicePanel(asi,
                                        null,
                                        colocationOnly,
                                        orderOnly,
                                        true,
                                        getBrowser().getDCHost(),
                                        testOnly);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                repaint();
                            }
                        });
                    }
                });
                thread.start();
            }
        };
        dlm.addElement(mmi);
        final ClusterBrowser.ClMenuItemCallback mmiCallback =
            getBrowser().new ClMenuItemCallback(list, null) {
                                   public void action(final Host dcHost) {
                                       addServicePanel(asi,
                                                       null,
                                                       colocationOnly,
                                                       orderOnly,
                                                       true,
                                                       dcHost,
                                                       true); /* test only */
                                   }
                               };
        callbackHash.put(mmi, mmiCallback);
    }

    /** Returns existing service manu item. */
    private MyMenu getExistingServiceMenuItem(final String name,
                                              final boolean colocationOnly,
                                              final boolean orderOnly,
                                              final boolean enableForNew,
                                              final boolean testOnly) {
        final ServiceInfo thisClass = this;
        return new MyMenu(name,
                          ConfigData.AccessType.ADMIN,
                          ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return !getBrowser().clStatusFailed()
                       && !getService().isRemoved()
                       && (enableForNew || !getService().isNew())
                       && !getService().isOrphaned();
                       //TODO: enableForNew should be always enabled
            }

            public void update() {
                super.update();
                removeAll();

                final DefaultListModel dlm = new DefaultListModel();
                final Map<MyMenuItem, ButtonCallback> callbackHash =
                                 new HashMap<MyMenuItem, ButtonCallback>();
                final MyList list = new MyList(dlm, getBackground());
                for (final ServiceInfo asi
                            : getBrowser().getExistingServiceList(thisClass)) {
                    if (asi.isConstraintPH() && isConstraintPH()) {
                        continue;
                    }
                    if (asi.getCloneInfo() != null
                        || asi.getGroupInfo() != null) {
                        /* skip services that are clones or in groups. */
                        continue;
                    }
                    addExistingServiceMenuItem(asi.toString(),
                                               asi,
                                               dlm,
                                               callbackHash,
                                               list,
                                               colocationOnly,
                                               orderOnly,
                                               testOnly);
                    asi.addExistingGroupServiceMenuItems(thisClass,
                                                         dlm,
                                                         callbackHash,
                                                         list,
                                                         colocationOnly,
                                                         orderOnly,
                                                         testOnly);
                }
                final JScrollPane jsp = Tools.getScrollingMenu(this,
                                                               dlm,
                                                               list,
                                                               callbackHash);
                if (jsp == null) {
                    setEnabled(false);
                } else {
                    add(jsp);
                }
                if (!colocationOnly && !orderOnly) {
                    addSeparator();
                    /* colocation only */
                    final MyMenu colOnlyItem = getExistingServiceMenuItem(
                           Tools.getString("ClusterBrowser.Hb.ColOnlySubmenu"),
                           true,
                           false,
                           enableForNew,
                           testOnly);
                    add(colOnlyItem);

                    /* order only */
                    final MyMenu ordOnlyItem = getExistingServiceMenuItem(
                           Tools.getString("ClusterBrowser.Hb.OrdOnlySubmenu"),
                           false,
                           true,
                           enableForNew,
                           testOnly);
                    add(ordOnlyItem);
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            colOnlyItem.update();
                            ordOnlyItem.update();
                        }
                    });
                    thread.start();
                }
            }
        };
    }

    /** Adds new Service and dependence. */
    private MyMenu getAddServiceMenuItem(final boolean testOnly,
                                         final String name,
                                         final boolean colocationOnly,
                                         final boolean orderOnly) {
        return new MyMenu(name,
                          ConfigData.AccessType.ADMIN,
                          ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return !getBrowser().clStatusFailed()
                       && !getService().isRemoved()
                       && !getService().isNew()
                       && !getService().isOrphaned();
            }

            public void update() {
                super.update();
                removeAll();
                final Point2D pos = getPos();
                final CRMXML crmXML = getBrowser().getCRMXML();
                final ResourceAgent fsService =
                             crmXML.getResourceAgent("Filesystem",
                                                     HB_HEARTBEAT_PROVIDER,
                                                     "ocf");
                if (crmXML.isLinbitDrbdPresent()) { /* just skip it, if it
                                                       is not */
                    final ResourceAgent linbitDrbdService =
                                                  crmXML.getHbLinbitDrbd();
                    final MyMenuItem ldMenuItem = new MyMenuItem(
                       Tools.getString("ClusterBrowser.linbitDrbdMenuName"),
                       null,
                       null,
                       ConfigData.AccessType.ADMIN,
                       ConfigData.AccessType.OP) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    getPopup().setVisible(false);
                                }
                            });
                            if (!getBrowser().linbitDrbdConfirmDialog()) {
                                return;
                            }

                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                colocationOnly,
                                                                orderOnly,
                                                                true,
                                                                false,
                                                                testOnly);
                            fsi.setDrbddiskIsPreferred(false);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    if (getBrowser().atLeastOneDrbddisk()
                        || !crmXML.isLinbitDrbdPresent()) {
                        ldMenuItem.setEnabled(false);
                    }
                    ldMenuItem.setPos(pos);
                    add(ldMenuItem);
                }
                if (crmXML.isDrbddiskPresent()) { /* just skip it,
                                                     if it is not */
                    final ResourceAgent drbddiskService =
                                                   crmXML.getHbDrbddisk();
                    final MyMenuItem ddMenuItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.DrbddiskMenuName"),
                     null,
                     null,
                     ConfigData.AccessType.ADMIN,
                     ConfigData.AccessType.OP) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    getPopup().setVisible(false);
                                }
                            });
                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                colocationOnly,
                                                                orderOnly,
                                                                true,
                                                                false,
                                                                testOnly);
                            fsi.setDrbddiskIsPreferred(true);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    if (getBrowser().isOneLinbitDrbd()
                        || !crmXML.isDrbddiskPresent()) {
                        ddMenuItem.setEnabled(false);
                    }
                    ddMenuItem.setPos(pos);
                    add(ddMenuItem);
                }
                final ResourceAgent ipService = crmXML.getResourceAgent(
                                                     "IPaddr2",
                                                     HB_HEARTBEAT_PROVIDER,
                                                     "ocf");
                if (ipService != null) { /* just skip it, if it is not*/
                    final MyMenuItem ipMenuItem =
                               new MyMenuItem(ipService.getMenuName(),
                                              null,
                                              null,
                                              ConfigData.AccessType.ADMIN,
                                              ConfigData.AccessType.OP) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    getPopup().setVisible(false);
                                }
                            });
                            addServicePanel(ipService,
                                            getPos(),
                                            colocationOnly,
                                            orderOnly,
                                            true,
                                            false,
                                            testOnly);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    ipMenuItem.setPos(pos);
                    add(ipMenuItem);
                }
                if (fsService != null) { /* just skip it, if it is not*/
                    final MyMenuItem fsMenuItem =
                           new MyMenuItem(fsService.getMenuName(),
                                          null,
                                          null,
                                          ConfigData.AccessType.ADMIN,
                                          ConfigData.AccessType.OP) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    getPopup().setVisible(false);
                                }
                            });
                            addServicePanel(fsService,
                                            getPos(),
                                            colocationOnly,
                                            orderOnly,
                                            true,
                                            false,
                                            testOnly);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    fsMenuItem.setPos(pos);
                    add(fsMenuItem);
                }
                for (final String cl : ClusterBrowser.HB_CLASSES) {
                    final MyMenu classItem = new MyMenu(
                                        ClusterBrowser.HB_CLASS_MENU.get(cl),
                                        ConfigData.AccessType.ADMIN,
                                        ConfigData.AccessType.OP);
                    final DefaultListModel dlm = new DefaultListModel();
                    for (final ResourceAgent ra : getAddServiceList(cl)) {
                        final MyMenuItem mmi =
                               new MyMenuItem(ra.getMenuName(),
                                              null,
                                              null,
                                              ConfigData.AccessType.ADMIN,
                                              ConfigData.AccessType.OP) {
                            private static final long serialVersionUID = 1L;
                            public void action() {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        getPopup().setVisible(false);
                                    }
                                });
                                if (ra.isLinbitDrbd()
                                    &&
                                     !getBrowser().linbitDrbdConfirmDialog()) {
                                    return;
                                } else if (ra.isHbDrbd()
                                     && !getBrowser().hbDrbdConfirmDialog()) {
                                    return;
                                }
                                addServicePanel(ra,
                                                getPos(),
                                                colocationOnly,
                                                orderOnly,
                                                true,
                                                false,
                                                testOnly);
                                getBrowser().getHeartbeatGraph().repaint();
                            }
                        };
                        mmi.setPos(pos);
                        dlm.addElement(mmi);
                    }
                    final JScrollPane jsp = Tools.getScrollingMenu(
                                              classItem,
                                              dlm,
                                              new MyList(dlm, getBackground()),
                                              null);
                    if (jsp == null) {
                        classItem.setEnabled(false);
                    } else {
                        classItem.add(jsp);
                    }
                    add(classItem);
                }
                if (!colocationOnly && !orderOnly) {
                    addSeparator();
                    /* colocation only */
                    final MyMenu colOnlyitem = getAddServiceMenuItem(
                            testOnly,
                            Tools.getString("ClusterBrowser.Hb.ColOnlySubmenu"),
                            true,
                            false);
                    add(colOnlyitem);
                    /* order only */
                    final MyMenu ordOnlyItem = getAddServiceMenuItem(
                            testOnly,
                            Tools.getString("ClusterBrowser.Hb.OrdOnlySubmenu"),
                            false,
                            true);
                    add(ordOnlyItem);
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            colOnlyitem.update();
                            ordOnlyItem.update();
                        }
                    });
                    thread.start();
                }
            }
        };
    }

    /** Adds menu items with dependend services and groups. */
    protected void addDependencyMenuItems(final List<UpdatableItem> items,
                                          final boolean enableForNew,
                                          final boolean testOnly) {
        /* add new group and dependency*/
        final MyMenuItem addGroupMenuItem =
            new MyMenuItem(Tools.getString(
                                "ClusterBrowser.Hb.AddDependentGroup"),
                           null,
                           null,
                           ConfigData.AccessType.ADMIN,
                           ConfigData.AccessType.OP) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return !getBrowser().clStatusFailed()
                           && !getService().isRemoved()
                           && !getService().isNew()
                           && !getService().isOrphaned();
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    final StringInfo gi = new StringInfo(
                                            ConfigData.PM_GROUP_NAME,
                                            ConfigData.PM_GROUP_NAME,
                                            getBrowser());
                    final CRMXML crmXML = getBrowser().getCRMXML();
                    addServicePanel(crmXML.getHbGroup(),
                                    getPos(),
                                    false, /* colocation only */
                                    false, /* order only */
                                    true,
                                    false,
                                    testOnly);
                    getBrowser().getHeartbeatGraph().repaint();
                }
            };
        items.add((UpdatableItem) addGroupMenuItem);

        /* add new service and dependency*/
        final MyMenu addServiceMenuItem = getAddServiceMenuItem(
                        testOnly,
                        Tools.getString("ClusterBrowser.Hb.AddDependency"),
                        false,
                        false);
        items.add((UpdatableItem) addServiceMenuItem);

        /* add existing service dependency*/
        final MyMenu existingServiceMenuItem = getExistingServiceMenuItem(
                    Tools.getString("ClusterBrowser.Hb.AddStartBefore"),
                    false,
                    false,
                    enableForNew,
                    testOnly);
        items.add((UpdatableItem) existingServiceMenuItem);
    }

    /**
     * Returns list of items for service popup menu with actions that can
     * be executed on the heartbeat services.
     */
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;

        if (cloneInfo == null) {
            addDependencyMenuItems(items, false, testOnly);
        }
        /* start resource */
        final MyMenuItem startMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.StartResource"),
                           START_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           ConfigData.AccessType.OP,
                           ConfigData.AccessType.OP) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return !getBrowser().clStatusFailed()
                           && getService().isAvailable()
                           && !isStarted(testOnly);
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    startResource(getBrowser().getDCHost(), testOnly);
                }
            };
        final ClusterBrowser.ClMenuItemCallback startItemCallback =
                   getBrowser().new ClMenuItemCallback(startMenuItem, null) {
            public void action(final Host dcHost) {
                startResource(dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(startMenuItem, startItemCallback);
        items.add((UpdatableItem) startMenuItem);

        /* stop resource */
        final MyMenuItem stopMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.StopResource"),
                           STOP_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           ConfigData.AccessType.OP,
                           ConfigData.AccessType.OP) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return !getBrowser().clStatusFailed()
                           && getService().isAvailable()
                           && !isStopped(testOnly);
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    stopResource(getBrowser().getDCHost(), testOnly);
                }
            };
        final ClusterBrowser.ClMenuItemCallback stopItemCallback =
                    getBrowser().new ClMenuItemCallback(stopMenuItem, null) {
            public void action(final Host dcHost) {
                stopResource(dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(stopMenuItem, stopItemCallback);
        items.add((UpdatableItem) stopMenuItem);

        /* clean up resource */
        final MyMenuItem cleanupMenuItem =
            new MyMenuItem(
               Tools.getString("ClusterBrowser.Hb.CleanUpFailedResource"),
               SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,

               Tools.getString("ClusterBrowser.Hb.CleanUpResource"),
               SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,
               ConfigData.AccessType.OP,
               ConfigData.AccessType.OP) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return getService().isAvailable()
                           && isOneFailed(testOnly);
                }

                public boolean enablePredicate() {
                    return !getBrowser().clStatusFailed()
                           && getService().isAvailable()
                           && isOneFailedCount(testOnly);
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    cleanupResource(getBrowser().getDCHost(), testOnly);
                }
            };
        /* cleanup ignores CIB_file */
        items.add((UpdatableItem) cleanupMenuItem);


        /* manage resource */
        final MyMenuItem manageMenuItem =
            new MyMenuItem(
                  Tools.getString("ClusterBrowser.Hb.ManageResource"),
                  START_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,

                  Tools.getString("ClusterBrowser.Hb.UnmanageResource"),
                  UNMANAGE_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,

                  ConfigData.AccessType.OP,
                  ConfigData.AccessType.OP) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return !isManaged(testOnly);
                }
                public boolean enablePredicate() {
                    return !getBrowser().clStatusFailed()
                           && getService().isAvailable();
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    if (this.getText().equals(Tools.getString(
                                    "ClusterBrowser.Hb.ManageResource"))) {
                        setManaged(true, getBrowser().getDCHost(), testOnly);
                    } else {
                        setManaged(false, getBrowser().getDCHost(), testOnly);
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback manageItemCallback =
                  getBrowser().new ClMenuItemCallback(manageMenuItem, null) {
            public void action(final Host dcHost) {
                setManaged(!isManaged(false),
                           dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(manageMenuItem, manageItemCallback);
        items.add((UpdatableItem) manageMenuItem);
        addMigrateMenuItems(items);
        if (cloneInfo == null) {
            /* remove service */
            final MyMenuItem removeMenuItem = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Hb.RemoveService"),
                        ClusterBrowser.REMOVE_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        ConfigData.AccessType.ADMIN,
                        ConfigData.AccessType.OP) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    if (getService().isNew()) {
                        return true;
                    }
                    if (getBrowser().clStatusFailed()
                        || getService().isRemoved()
                        || isRunning(testOnly)) {
                        return false;
                    }
                    if (groupInfo == null) {
                        return true;
                    }
                    final ClusterStatus cs = getBrowser().getClusterStatus();
                    final List<String> gr = cs.getGroupResources(
                                          groupInfo.getHeartbeatId(testOnly),
                                          testOnly);


                    return gr != null && gr.size() > 1;
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    if (getService().isOrphaned()) {
                        cleanupResource(getBrowser().getDCHost(), testOnly);
                    } else {
                        removeMyself(false);
                    }
                    getBrowser().getHeartbeatGraph().repaint();
                }
            };
            final ServiceInfo thisClass = this;
            final ClusterBrowser.ClMenuItemCallback removeItemCallback =
                    getBrowser().new ClMenuItemCallback(removeMenuItem, null) {
                public final boolean isEnabled() {
                    return super.isEnabled() && !getService().isNew();
                }
                public final void action(final Host dcHost) {
                    removeMyselfNoConfirm(dcHost, true); /* test only */
                }
            };
            addMouseOverListener(removeMenuItem, removeItemCallback);
            items.add((UpdatableItem) removeMenuItem);
        }
        /* view log */
        final MyMenuItem viewLogMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Hb.ViewServiceLog"),
                        LOGFILE_ICON,
                        null,
                        ConfigData.AccessType.RO,
                        ConfigData.AccessType.RO) {

            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return !getService().isNew();
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                ServiceLogs l = new ServiceLogs(getBrowser().getCluster(),
                                                getNameForLog(),
                                                getService().getHeartbeatId());
                l.showDialog();
            }
        };
        items.add(viewLogMenu);
        /* expert options */
        final MyMenu expertSubmenu = new MyMenu(
                        Tools.getString("ClusterBrowser.ExpertSubmenu"),
                        ConfigData.AccessType.OP,
                        ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;
            public boolean enablePredicate() {
                return true;
            }
        };
        items.add(expertSubmenu);
        addExpertMenu(expertSubmenu);
        return items;
    }

    /** Adds migrate and unmigrate menu items. */
    protected void addMigrateMenuItems(final List<UpdatableItem> items) {
        /* migrate resource */
        final boolean testOnly = false;
        final ServiceInfo thisClass = this;
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateMenuItem =
                new MyMenuItem(Tools.getString(
                                    "ClusterBrowser.Hb.MigrateResource")
                                    + " " + hostName,
                               MIGRATE_ICON,
                               ClusterBrowser.STARTING_PTEST_TOOLTIP,

                               Tools.getString(
                                    "ClusterBrowser.Hb.MigrateResource")
                                    + " " + hostName + " (offline)",
                               MIGRATE_ICON,
                               ClusterBrowser.STARTING_PTEST_TOOLTIP,
                               ConfigData.AccessType.OP,
                               ConfigData.AccessType.OP) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return host.isClStatus();
                    }

                    public boolean visiblePredicate() {
                        return !host.isClStatus()
                               || enablePredicate();
                    }

                    public boolean enablePredicate() {
                        final List<String> runningOnNodes =
                                               getRunningOnNodes(testOnly);
                        if (runningOnNodes == null
                            || runningOnNodes.isEmpty()) {
                            return false;
                        }
                        final String runningOnNode =
                                runningOnNodes.get(0).toLowerCase(Locale.US);
                        return !getBrowser().clStatusFailed()
                               && getService().isAvailable()
                               && !hostName.toLowerCase(Locale.US).equals(
                                                                 runningOnNode)
                               && host.isClStatus();
                    }

                    public void action() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                getPopup().setVisible(false);
                            }
                        });
                        migrateResource(hostName,
                                        getBrowser().getDCHost(),
                                        testOnly);
                    }
                };
            final ClusterBrowser.ClMenuItemCallback migrateItemCallback =
                 getBrowser().new ClMenuItemCallback(migrateMenuItem, null) {
                public void action(final Host dcHost) {
                    migrateResource(hostName, dcHost, true); /* testOnly */
                }
            };
            addMouseOverListener(migrateMenuItem, migrateItemCallback);
            items.add((UpdatableItem) migrateMenuItem);
        }

        /* unmigrate resource */
        final MyMenuItem unmigrateMenuItem =
            new MyMenuItem(
                    Tools.getString("ClusterBrowser.Hb.UnmigrateResource"),
                    UNMIGRATE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    ConfigData.AccessType.OP,
                    ConfigData.AccessType.OP) {
                private static final long serialVersionUID = 1L;

                public boolean visiblePredicate() {
                    return enablePredicate();
                }

                public boolean enablePredicate() {
                    // TODO: if it was migrated
                    return !getBrowser().clStatusFailed()
                           && getService().isAvailable()
                           && (getMigratedTo(testOnly) != null
                               || getMigratedFrom(testOnly) != null);
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    unmigrateResource(getBrowser().getDCHost(), testOnly);
                }
            };
        final ClusterBrowser.ClMenuItemCallback unmigrateItemCallback =
               getBrowser().new ClMenuItemCallback(unmigrateMenuItem, null) {
            public void action(final Host dcHost) {
                unmigrateResource(dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(unmigrateMenuItem, unmigrateItemCallback);
        items.add((UpdatableItem) unmigrateMenuItem);
    }

    /**
     * Adds "migrate from" and "force migrate" menuitems to the submenu.
     */
    protected void addMoreMigrateMenuItems(final MyMenu submenu) {
        final boolean testOnly = false;
        final ServiceInfo thisClass = this;
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem =
                new MyMenuItem(Tools.getString(
                                    "ClusterBrowser.Hb.MigrateFromResource")
                                    + " " + hostName,
                               MIGRATE_ICON,
                               ClusterBrowser.STARTING_PTEST_TOOLTIP,

                               Tools.getString(
                                    "ClusterBrowser.Hb.MigrateFromResource")
                                    + " " + hostName + " (offline)",
                               MIGRATE_ICON,
                               ClusterBrowser.STARTING_PTEST_TOOLTIP,
                               ConfigData.AccessType.OP,
                               ConfigData.AccessType.OP) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return host.isClStatus();
                    }

                    public boolean visiblePredicate() {
                        return !host.isClStatus()
                               || enablePredicate();
                    }

                    public boolean enablePredicate() {
                        final List<String> runningOnNodes =
                                               getRunningOnNodes(testOnly);
                        if (runningOnNodes == null
                            || runningOnNodes.size() != 1) {
                            return false;
                        }
                        final String runningOnNode =
                                runningOnNodes.get(0).toLowerCase(Locale.US);
                        return !getBrowser().clStatusFailed()
                               && getService().isAvailable()
                               && hostName.toLowerCase(Locale.US).equals(
                                                                 runningOnNode)
                               && host.isClStatus();
                    }

                    public void action() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                getPopup().setVisible(false);
                            }
                        });
                        migrateFromResource(getBrowser().getDCHost(),
                                            testOnly);
                    }
                };
            final ClusterBrowser.ClMenuItemCallback migrateItemCallback =
               getBrowser().new ClMenuItemCallback(migrateFromMenuItem, null) {
                public void action(final Host dcHost) {
                    migrateFromResource(dcHost, true); /* testOnly */
                }
            };
            addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            submenu.add(migrateFromMenuItem);
        }

        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();

            final MyMenuItem forceMigrateMenuItem =
                new MyMenuItem(Tools.getString(
                                    "ClusterBrowser.Hb.ForceMigrateResource")
                                    + " " + hostName,
                               MIGRATE_ICON,
                               ClusterBrowser.STARTING_PTEST_TOOLTIP,

                               Tools.getString(
                                    "ClusterBrowser.Hb.ForceMigrateResource")
                                    + " " + hostName + " (offline)",
                               MIGRATE_ICON,
                               ClusterBrowser.STARTING_PTEST_TOOLTIP,
                               ConfigData.AccessType.OP,
                               ConfigData.AccessType.OP) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return host.isClStatus();
                    }

                    public boolean visiblePredicate() {
                        return !host.isClStatus()
                               || enablePredicate();
                    }

                    public boolean enablePredicate() {
                        final List<String> runningOnNodes =
                                               getRunningOnNodes(testOnly);
                        if (runningOnNodes == null
                            || runningOnNodes.isEmpty()) {
                            return false;
                        }
                        final String runningOnNode =
                                runningOnNodes.get(0).toLowerCase(Locale.US);
                        return !getBrowser().clStatusFailed()
                               && getService().isAvailable()
                               && !hostName.toLowerCase(Locale.US).equals(
                                                                 runningOnNode)
                               && host.isClStatus();
                    }

                    public void action() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                getPopup().setVisible(false);
                            }
                        });
                        forceMigrateResource(hostName,
                                             getBrowser().getDCHost(),
                                             testOnly);
                    }
                };
            final ClusterBrowser.ClMenuItemCallback forceMigrateItemCallback =
                 getBrowser().new ClMenuItemCallback(forceMigrateMenuItem,
                                                     null) {
                public void action(final Host dcHost) {
                    forceMigrateResource(hostName, dcHost, true); /* testOnly */
                }
            };
            addMouseOverListener(forceMigrateMenuItem,
                                 forceMigrateItemCallback);
            submenu.add(forceMigrateMenuItem);
        }
    }

    /**
     * Adds expert submenu.
     */
    public final void addExpertMenu(final MyMenu submenu) {
        if (submenu.getItemCount() > 0) {
            return;
        }
        addMoreMigrateMenuItems(submenu);
    }


    /** Returns tool tip for the service. */
    public String getToolTipText(final boolean testOnly) {
        String nodeString = null;
        final List<String> nodes = getRunningOnNodes(testOnly);
        if (nodes != null && !nodes.isEmpty()) {
            nodeString =
                     Tools.join(", ", nodes.toArray(new String[nodes.size()]));
        }
        final Host[] hosts = getBrowser().getCluster().getHostsArray();
        if (getBrowser().allHostsDown()) {
            nodeString = "unknown";
        }
        final StringBuffer sb = new StringBuffer(200);
        sb.append("<b>");
        sb.append(toString());
        if (isFailed(testOnly)) {
            sb.append("</b> <b>Failed</b>");
        } else if (isStopped(testOnly)
                   || nodeString == null) {
            sb.append("</b> not running");
        } else {
            sb.append("</b> running on: ");
            sb.append(nodeString);
        }
        if (!isManaged(testOnly)) {
            sb.append(" (unmanaged)");
        }
        return sb.toString();
    }

    /**
     * Returns heartbeat service class.
     */
    public ResourceAgent getResourceAgent() {
        return resourceAgent;
    }

    /** Sets whether the info object is being updated. */
    public void setUpdated(final boolean updated) {
        if (updated && !isUpdated()) {
            getBrowser().getHeartbeatGraph().startAnimation(this);
        } else if (!updated) {
            getBrowser().getHeartbeatGraph().stopAnimation(this);
        }
        super.setUpdated(updated);
    }

    /**
     * Returns text that appears in the corner of the graph.
     */
    public Subtext getRightCornerTextForGraph(final boolean testOnly) {
        if (getService().isOrphaned()) {
            if (isFailed(testOnly)) {
                return ORPHANED_FAILED_SUBTEXT;
            } else {
                return ORPHANED_SUBTEXT;
            }
        } else if (!isManaged(testOnly)) {
            return UNMANAGED_SUBTEXT;
        } else if (getMigratedTo(testOnly) != null
                   || getMigratedFrom(testOnly) != null) {
            return MIGRATED_SUBTEXT;
        }
        return null;
    }

    /** Returns text with lines as array that appears in the cluster graph. */
    public Subtext[] getSubtextsForGraph(final boolean testOnly) {
        Color color = null;
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getService().isOrphaned()) {
            texts.add(new Subtext("...", null));
        } else if (isFailed(testOnly)) {
            texts.add(new Subtext("not running:", null));
        } else if (isStopped(testOnly)) {
            texts.add(new Subtext("stopped",
                                  ClusterBrowser.FILL_PAINT_STOPPED));
        } else {
            String runningOnNodeString = null;
            if (getBrowser().allHostsDown()) {
                runningOnNodeString = "unknown";
            } else {
                final List<String> runningOnNodes = getRunningOnNodes(testOnly);
                if (runningOnNodes != null && !runningOnNodes.isEmpty()) {
                    runningOnNodeString = runningOnNodes.get(0);
                    color = getBrowser().getCluster().getHostColors(
                                                        runningOnNodes).get(0);
                }
            }
            if (runningOnNodeString != null) {
                texts.add(new Subtext("running on: " + runningOnNodeString,
                                      color));
            } else {
                texts.add(new Subtext("not running",
                                      ClusterBrowser.FILL_PAINT_STOPPED));
            }
        }
        if (isOneFailedCount(testOnly)) {
            for (final Host host : getBrowser().getClusterHosts()) {
                if (host.isClStatus()
                    && getFailCount(host.getName(), testOnly) != null) {
                    texts.add(new Subtext(ClusterBrowser.IDENT_4
                                          + host.getName()
                                          + getFailCountString(
                                                        host.getName(),
                                                        testOnly),
                              null));
                }
            }
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /**
     * Returns null, when this service is not a clone.
     */
    public ServiceInfo getContainedService() {
        return null;
    }

    /**
     * Returns type radio group.
     */
    public GuiComboBox getTypeRadioGroup() {
        return typeRadioGroup;
    }

    /**
     * Returns units.
     */
    protected final Unit[] getUnits() {
        return new Unit[]{
            new Unit("",    "s",  "Second",      "Seconds"), /* default unit */
            new Unit("ms",  "ms", "Millisecond", "Milliseconds"),
            new Unit("us",  "us", "Microsecond", "Microseconds"),
            new Unit("s",   "s",  "Second",      "Seconds"),
            new Unit("min", "m",  "Minute",      "Minutes"),
            new Unit("h",   "h",  "Hour",        "Hours")
        };
    }

    /** Returns text that appears above the icon in the graph. */
    public String getIconTextForGraph(final boolean testOnly) {
        if (getBrowser().allHostsDown()) {
            return Tools.getString("ClusterBrowser.Hb.NoInfoAvailable");
        }
        final Host dcHost = getBrowser().getDCHost();
        if (getService().isNew()) {
            return "new...";
        } else if (getService().isOrphaned()) {
            return "";
        } else if (isStarted(testOnly)) {
            if (isRunning(testOnly)) {
                final List<Host> migratedTo = getMigratedTo(testOnly);
                if (migratedTo != null) {
                    final List<String> runningOnNodes =
                                               getRunningOnNodes(testOnly);
                    if (runningOnNodes != null) {
                        boolean alreadyThere = false;
                        for (final Host mto : migratedTo) {
                            if (runningOnNodes.contains(mto.getName())) {
                                alreadyThere = true;
                            }
                            if (!alreadyThere) {
                                return Tools.getString(
                                                "ClusterBrowser.Hb.Migrating");
                            }
                        }
                    }
                } else {
                    final List<Host> migratedFrom = getMigratedFrom(testOnly);
                    if (migratedFrom != null) {
                        final List<String> runningOnNodes =
                                                   getRunningOnNodes(testOnly);
                        boolean alreadyThere = false;
                        if (runningOnNodes == null) {
                            alreadyThere = true;
                        } else  {
                            alreadyThere = true;
                            for (final Host mfrom : migratedFrom) {
                                if (runningOnNodes.contains(mfrom.getName())) {
                                    alreadyThere = false;
                                }
                            }
                        }
                        if (!alreadyThere) {
                            return Tools.getString(
                                            "ClusterBrowser.Hb.Migrating");
                        }
                    }
                }
                return null;
            } else if (isFailed(testOnly)) {
                return Tools.getString("ClusterBrowser.Hb.StartingFailed");
            } else if (isGroupStopped(testOnly)) {
                return Tools.getString("ClusterBrowser.Hb.GroupStopped");
            } else {
                return Tools.getString("ClusterBrowser.Hb.Starting");
            }
        } else if (isStopped(testOnly)) {
            if (isRunning(testOnly)) {
                return Tools.getString("ClusterBrowser.Hb.Stopping");
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Returns hash with saved operations.
     */
    public MultiKeyMap getSavedOperation() {
        return savedOperation;
    }

    /** Reload combo boxes. */
    public void reloadComboBoxes() {
        if (sameAsOperationsCB != null) {
            String defaultOpIdRef = null;
            final Info savedOpIdRef = (Info) sameAsOperationsCB.getValue();
            if (savedOpIdRef != null) {
                defaultOpIdRef = savedOpIdRef.toString();
            }
            final String idRef = defaultOpIdRef;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    sameAsOperationsCB.reloadComboBox(
                                                  idRef,
                                                  getSameServicesOperations());
                }
            });
        }
        if (sameAsMetaAttrsCB != null) {
            String defaultMAIdRef = null;
            final Info savedMAIdRef = (Info) sameAsMetaAttrsCB.getValue();
            if (savedMAIdRef != null) {
                defaultMAIdRef = savedMAIdRef.toString();
            }
            final String idRef = defaultMAIdRef;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    sameAsMetaAttrsCB.reloadComboBox(
                                              idRef,
                                              getSameServicesMetaAttrs());
                }
            });
        }
    }

    /**
     * Returns whether info panel is already created.
     */
    public boolean isInfoPanelOk() {
        return infoPanel != null;
    }

    /**
     * Connects with VMSVirtualDomainInfo object.
     */
    public void connectWithVMS() {
        /* for VirtualDomainInfo */
    }

    /** Whether this class is a constraint placeholder. */
    public boolean isConstraintPH() {
        return false;
    }
}
