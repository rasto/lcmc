/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2015, Rastislav Levrinc.
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

package lcmc.common.ui.main;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.JComponent;

import lcmc.common.ui.MainMenu;

@Named
@Singleton
public class ProgressIndicator {

    @Inject
    private ProgressIndicatorPanel progressIndicatorPanel;
    @Inject
    private Provider<MainMenu> mainMenu;

    public void init() {
        progressIndicatorPanel.init();
    }

    public void startProgressIndicator(final String text) {
        turnOffMainMenu();
        progressIndicatorPanel.start(text, null);
    }

    public void startProgressIndicator(final String name, final String text) {
        startProgressIndicator(name + ": " + text);
    }

    public void stopProgressIndicator(final String text) {
        turnOnMainMenu();
        progressIndicatorPanel.stop(text);
    }

    public void stopProgressIndicator(final String name, final String text) {
        stopProgressIndicator(name + ": " + text);
    }

    public void progressIndicatorFailed(final String text) {
        turnOnMainMenu();
        progressIndicatorPanel.failure(text);
    }

    public void progressIndicatorFailed(final String name, final String text) {
        progressIndicatorFailed(name + ": " + text);
    }

    /** Progress indicator with failure message that shows for n seconds. */
    public void progressIndicatorFailed(final String text, final int n) {
        turnOnMainMenu();
        progressIndicatorPanel.failure(text, n);
    }

    /**
     * Progress indicator with failure message for host or cluster command,
     * that shows for n seconds.
     */
    public void progressIndicatorFailed(final String name, final String text, final int n) {
        progressIndicatorFailed(name + ": " + text, n);
    }

    public JComponent getPane() {
        return progressIndicatorPanel;
    }

    private void turnOffMainMenu() {
        mainMenu.get()
                .turnOff();
    }
    private void turnOnMainMenu() {
        mainMenu.get()
                .turnOn();
    }
}
