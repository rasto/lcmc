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

package lcmc.common.ui.utils;

import lcmc.common.domain.util.Tools;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

@Named
@Singleton
public class SwingUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SwingUtils.class);
    private boolean checkSwing = false;

    public boolean isCheckSwing() {
        return checkSwing;
    }

    public void setCheckSwing(final boolean checkSwing) {
        this.checkSwing = checkSwing;
    }

    /**
     * Print stack trace if it's not in a swing thread.
     */
    public void isSwingThread() {
        if (!isCheckSwing()) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            System.out.println("not a swing thread: " + Tools.getStackTrace());
        }
    }

    /**
     * Print stack trace if it's in a swing thread.
     */
    public void isNotSwingThread() {
        if (!isCheckSwing()) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            System.out.println("swing thread: " + Tools.getStackTrace());
        }
    }

    /** Wait for next swing threads to finish. It's used for synchronization */
    public void waitForSwing() {
        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                /* just wait */
            }
        });
    }

    /**
     * Convenience invoke and wait function if not already in an event
     * dispatch thread.
     */
    public void invokeAndWait(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (final InterruptedException ix) {
                Thread.currentThread().interrupt();
            } catch (final InvocationTargetException x) {
                LOG.appError("invokeAndWait: exception", x);
            }
        }
    }

    public void invokeInEdt(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public void invokeLater(final Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }

}
