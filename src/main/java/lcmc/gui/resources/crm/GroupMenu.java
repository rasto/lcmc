/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
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

package lcmc.gui.resources.crm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JMenuItem;

import lcmc.gui.GUIData;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.crm.ResourceAgent;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.MyList;
import lcmc.utilities.MyListModel;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GroupMenu extends ServiceMenu {

    @Autowired
    private GUIData drbdGui;
    @Autowired @Qualifier("serviceMenu")
    private Provider<ServiceMenu> serviceMenuProvider;

    @Override
    public List<UpdatableItem> getPulldownMenu(final ServiceInfo serviceInfo) {
        final GroupInfo groupInfo = (GroupInfo) serviceInfo;

        /* add group service */
        final UpdatableItem addGroupServiceMenuItem = new MyMenu(
                        Tools.getString("ClusterBrowser.Hb.AddGroupService"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (groupInfo.getBrowser().crmStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                } else {
                    return null;
                }
            }

            @Override
            public void updateAndWait() {
                Tools.isSwingThread();
                removeAll();
                final Collection<JDialog> popups = new ArrayList<JDialog>();
                for (final String cl : ClusterBrowser.CRM_CLASSES) {
                    final MyMenu classItem =
                            new MyMenu(ClusterBrowser.CRM_CLASS_MENU.get(cl),
                                   new AccessMode(Application.AccessType.ADMIN,
                                                  false),
                                   new AccessMode(Application.AccessType.OP,
                                                  false));
                    final MyListModel<MyMenuItem> dlm = new MyListModel<MyMenuItem>();
                    for (final ResourceAgent ra : groupInfo.getAddGroupServiceList(cl)) {
                        final MyMenuItem mmi =
                            new MyMenuItem(
                                   ra.getPullDownMenuName(),
                                   null,
                                   null,
                                   new AccessMode(Application.AccessType.ADMIN,
                                                  false),
                                   new AccessMode(Application.AccessType.OP,
                                                  false)) {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void action() {
                                final CloneInfo ci = groupInfo.getCloneInfo();
                                if (ci != null) {
                                    ci.hidePopup();
                                }
                                groupInfo.hidePopup();
                                Tools.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        for (final JDialog otherP : popups) {
                                            otherP.dispose();
                                        }
                                    }
                                });
                                if (ra.isLinbitDrbd() && !groupInfo.getBrowser().linbitDrbdConfirmDialog()) {
                                    return;
                                }
                                groupInfo.addGroupServicePanel(ra, true);
                                repaint();
                            }
                        };
                        dlm.addElement(mmi);
                    }
                    final boolean ret = drbdGui.getScrollingMenu(
                            ClusterBrowser.CRM_CLASS_MENU.get(cl),
                            null, /* options */
                            classItem,
                            dlm,
                            new MyList<MyMenuItem>(dlm, getBackground()),
                            groupInfo,
                            popups,
                            null);
                    if (!ret) {
                        classItem.setEnabled(false);
                    }
                    add(classItem);
                }
                super.updateAndWait();
            }
        };
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        items.add(addGroupServiceMenuItem);
        for (final UpdatableItem item : super.getPulldownMenu(groupInfo)) {
            items.add(item);
        }

        /* group services */
        if (!Tools.getApplication().isSlow()) {
            for (final ServiceInfo child : groupInfo.getGroupServices()) {
                final UpdatableItem groupServicesMenu = new MyMenu(
                        child.toString(),
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void updateAndWait() {
                        Tools.isSwingThread();
                        removeAll();
                        final Collection<UpdatableItem> serviceMenus =
                                        new ArrayList<UpdatableItem>();
                        for (final UpdatableItem u : child.createPopup()) {
                            serviceMenus.add(u);
                            u.updateAndWait();
                        }
                        for (final UpdatableItem u : serviceMenus) {
                            add((JMenuItem) u);
                        }
                        super.updateAndWait();
                    }
                };
                items.add(groupServicesMenu);
            }
        }
        return items;
    }

    /** Adds existing service menu item for every member of a group. */
    @Override
    protected void addExistingGroupServiceMenuItems(
                        final ServiceInfo serviceInfo,
                        final ServiceInfo existingService,
                        final MyListModel<MyMenuItem> dlm,
                        final Map<MyMenuItem, ButtonCallback> callbackHash,
                        final MyList<MyMenuItem> list,
                        final JCheckBox colocationWi,
                        final JCheckBox orderWi,
                        final List<JDialog> popups,
                        final Application.RunMode runMode) {
        for (final ServiceInfo child : ((GroupInfo) existingService).getGroupServices()) {
            final ServiceMenu subServiceMenu = serviceMenuProvider.get();
            subServiceMenu.addExistingServiceMenuItem(serviceInfo,
                                                      "         " + child,
                                                      child,
                                                      dlm,
                                                      callbackHash,
                                                      list,
                                                      colocationWi,
                                                      orderWi,
                                                      popups,
                                                      runMode);
        }
    }
}
