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
import java.util.List;

import lcmc.gui.CallbackAction;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ConstraintPHMenu extends ServiceMenu {

    private ConstraintPHInfo constraintPHInfo;
    @Autowired
    private MenuFactory menuFactory;

    @Override
    public List<UpdatableItem> getPulldownMenu(final ServiceInfo serviceInfo) {
        this.constraintPHInfo = (ConstraintPHInfo) serviceInfo;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Application.RunMode runMode = Application.RunMode.LIVE;
        addDependencyMenuItems(constraintPHInfo, items, true, runMode);
        /* remove the placeholder and all constraints associated with it. */
        final MyMenuItem removeMenuItem = menuFactory.createMenuItem(
                Tools.getString("ConstraintPHInfo.Remove"),
                ClusterBrowser.REMOVE_ICON,
                ClusterBrowser.STARTING_PTEST_TOOLTIP,
                new AccessMode(Application.AccessType.ADMIN, false),
                new AccessMode(Application.AccessType.OP, false))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (constraintPHInfo.getBrowser().crmStatusFailed()) {
                            return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                        } else if (constraintPHInfo.getService().isRemoved()) {
                            return ConstraintPHInfo.IS_BEING_REMOVED_STRING;
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        constraintPHInfo.hidePopup();
                        constraintPHInfo.removeMyself(Application.RunMode.LIVE);
                        constraintPHInfo.getBrowser().getCrmGraph().repaint();
                    }
                });
        final ButtonCallback removeItemCallback = constraintPHInfo.getBrowser().new ClMenuItemCallback(null) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !constraintPHInfo.getService().isNew();
            }
        }
                .addAction(new CallbackAction() {
                    @Override
                    public void run(final Host dcHost) {
                        constraintPHInfo.removeMyselfNoConfirm(dcHost, Application.RunMode.TEST);
                    }
                });
        constraintPHInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add(removeMenuItem);
        return items;
    }
}
