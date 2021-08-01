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

package lcmc.common.ui.utils;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.ImageIcon;

import lcmc.common.domain.AccessMode;

@Named
@Singleton
public class MenuFactory {
    private final Provider<MyMenuItem> menuItemProvider;
    private final Provider<MyMenu> menuProvider;

    public MenuFactory(Provider<MyMenuItem> menuItemProvider, Provider<MyMenu> menuProvider) {
        this.menuItemProvider = menuItemProvider;
        this.menuProvider = menuProvider;
    }

    public MyMenu createMenu(final String text, final AccessMode enableAccessMode, final AccessMode visibleAccessMode) {
        final MyMenu menu = menuProvider.get();
        menu.init(text, enableAccessMode, visibleAccessMode);
        return menu;
    }

    public MyMenuItem createMenuItem(final String text,
                                     final ImageIcon icon,
                                     final AccessMode enableAccessMode,
                                     final AccessMode visibleAccessMode) {
        final MyMenuItem menuItem = menuItemProvider.get();
        menuItem.init(text, icon, enableAccessMode, visibleAccessMode);
        return menuItem;
    }

    public MyMenuItem createMenuItem(final String text,
                                     final ImageIcon icon,
                                     final String shortDesc,
                                     final AccessMode enableAccessMode,
                                     final AccessMode visibleAccessMode) {
        final MyMenuItem menuItem = menuItemProvider.get();
        menuItem.init(text, icon, shortDesc, enableAccessMode, visibleAccessMode);
        return menuItem;

    }

    public MyMenuItem createMenuItem(final String text1a,
                                     final ImageIcon icon1a,
                                     final String shortDesc1a,
                                     final String text2,
                                     final ImageIcon icon2,
                                     final String shortDesc2,
                                     final AccessMode enableAccessMode,
                                     final AccessMode visibleAccessMode) {
        final MyMenuItem menuItem = menuItemProvider.get();
        menuItem.init(text1a,
                      icon1a,
                      shortDesc1a,
                      text2,
                      icon2,
                      shortDesc2,
                      enableAccessMode,
                      visibleAccessMode);
        return menuItem;
    }
}
