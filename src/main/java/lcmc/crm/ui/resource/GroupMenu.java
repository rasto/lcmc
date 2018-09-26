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

package lcmc.crm.ui.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JMenuItem;

import lcmc.common.ui.main.MainData;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.crm.domain.ResourceAgent;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.domain.EnablePredicate;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyList;
import lcmc.common.ui.utils.MyListModel;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GroupMenu extends ServiceMenu {

    private final Supplier<ServiceMenu> serviceMenuProvider;
    private final MenuFactory menuFactory;
    private final Application application;
    private final SwingUtils swingUtils;
    private final MainData mainData;

    @Override
    public List<UpdatableItem> getPulldownMenu(final ServiceInfo serviceInfo) {
        final GroupInfo groupInfo = (GroupInfo) serviceInfo;

        /* add group service */
        final MyMenu addGroupServiceMenuItem = menuFactory.createMenu(
                        Tools.getString("ClusterBrowser.Hb.AddGroupService"),
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (groupInfo.getBrowser().crmStatusFailed()) {
                            return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                        } else {
                            return null;
                        }
                    }
                });
        addGroupServiceMenuItem.onUpdate(new Runnable() {
            @Override
            public void run() {
                swingUtils.isSwingThread();
                addGroupServiceMenuItem.removeAll();
                final Collection<JDialog> popups = new ArrayList<JDialog>();
                for (final String cl : ClusterBrowser.CRM_CLASSES) {
                    final MyMenu classItem = menuFactory.createMenu(
                                                        ClusterBrowser.CRM_CLASS_MENU.get(cl),
                                                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                        new AccessMode(AccessMode.OP, AccessMode.NORMAL));
                    final MyListModel<MyMenuItem> dlm = new MyListModel<MyMenuItem>();
                    for (final ResourceAgent ra : groupInfo.getAddGroupServiceList(cl)) {
                        final MyMenuItem mmi = menuFactory.createMenuItem(
                                        ra.getPullDownMenuName(),
                                        null,
                                        null,
                                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                        new AccessMode(AccessMode.OP, AccessMode.NORMAL));
                        mmi.addAction(new MenuAction() {
                                @Override
                                public void run(final String text) {
                                    final CloneInfo ci = groupInfo.getCloneInfo();
                                    if (ci != null) {
                                        ci.hidePopup();
                                    }
                                    groupInfo.hidePopup();
                                    swingUtils.invokeLater(new Runnable() {
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
                                    mmi.repaint();
                                }
                            });
                        dlm.addElement(mmi);
                    }
                    final boolean ret = mainData.getScrollingMenu(
                            ClusterBrowser.CRM_CLASS_MENU.get(cl),
                            null, /* options */
                            classItem,
                            dlm,
                            new MyList<MyMenuItem>(dlm, addGroupServiceMenuItem.getBackground()),
                            groupInfo,
                            popups,
                            null);
                    if (!ret) {
                        classItem.setEnabled(false);
                    }
                    addGroupServiceMenuItem.add(classItem);
                }
                addGroupServiceMenuItem.updateMenuComponents();
                addGroupServiceMenuItem.processAccessMode();
            }
        });
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        items.add(addGroupServiceMenuItem);
        for (final UpdatableItem item : super.getPulldownMenu(groupInfo)) {
            items.add(item);
        }

        /* group services */
        if (!mainData.isSlow()) {
            for (final ServiceInfo child : groupInfo.getSubServices()) {
                final MyMenu groupServicesMenu = menuFactory.createMenu(
                        child.toString(),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL));
                groupServicesMenu.onUpdate(new Runnable() {
                    @Override
                    public void run() {
                        swingUtils.isSwingThread();
                        groupServicesMenu.removeAll();
                        final Collection<UpdatableItem> serviceMenus = new ArrayList<UpdatableItem>();
                        for (final UpdatableItem u : child.createPopup()) {
                            serviceMenus.add(u);
                            u.updateAndWait();
                        }
                        for (final UpdatableItem u : serviceMenus) {
                            groupServicesMenu.add((JMenuItem) u);
                        }
                        groupServicesMenu.updateMenuComponents();
                        groupServicesMenu.processAccessMode();
                    }
                });
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
        for (final ServiceInfo child : ((GroupInfo) existingService).getSubServices()) {
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
