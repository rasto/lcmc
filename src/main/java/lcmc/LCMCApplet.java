/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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

/*
 * LCMCApplet
 * (c) Rasto Levrinc, Linbit
 */
package lcmc;

import javax.swing.JApplet;
import javax.swing.JFrame;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;


/**
 * This is the central class with main function. It starts the DRBD GUI.
 */
public final class LCMCApplet extends JApplet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(LCMCApplet.class);
    public static final String[] NO_PARAMS = {};

    /** Start the applet. */
    @Override
    public void init() {
        Tools.init();
        LOG.debug1("init: start");
        final String[] params;
        final String paramsLine = getParameter("params");
        if (paramsLine == null) {
            params = NO_PARAMS;
        } else {
            params = paramsLine.split("\\s+");
        }

        LCMC.initApp(params);

        if (Tools.getApplication().isEmbedApplet()) {
            LCMC.MAIN_FRAME = this;
            setJMenuBar(LCMC.getMenuBar());
            setContentPane(LCMC.getMainPanel());
            setGlassPane(LCMC.getMainGlassPane());
            LCMC.createAndShowGUI(this);
        } else {
            final JFrame mainFrame = new JFrame();
            LCMC.MAIN_FRAME = mainFrame;
            LCMC.createMainFrame(mainFrame);
            LCMC.createAndShowGUI(mainFrame);
        }
        //TODO: save on quit
        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //addWindowListener(new ExitListener());
    }
}
