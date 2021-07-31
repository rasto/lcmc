/*
 * This file is part of LCMC
 *
 * Copyright (C) 2012, Rastislav Levrinc.
 *
 * LCMC is free software; you can redistribute it and/or
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

package lcmc.logger;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.Buffer;
import org.apache.commons.collections15.BufferUtils;
import org.apache.commons.collections15.buffer.CircularFifoBuffer;

public final class LoggerFactory {
    private static final Map<String, Logger> LOGGER_MAP = new HashMap<>();
    private static int debugLevel = -1;
    private static boolean showAppWarning = false;
    /** Whether application errors should show a dialog. */
    private static boolean showAppError = false;
    private static final int CIRCULAR_LOG_SIZE = 200;
    /** Synchronized, Circular log. */
    static final Buffer<String> LOG_BUFFER =
                                  BufferUtils.synchronizedBuffer(new CircularFifoBuffer<>(CIRCULAR_LOG_SIZE));
    public static void incrementDebugLevel() {
        debugLevel++;
        System.out.println("debug level: " + debugLevel);
    }

    public static void decrementDebugLevel() {
        debugLevel--;
        System.out.println("debug level: " + debugLevel);
    }

    public static int getDebugLevel() {
        return debugLevel;
    }

    /**
     * @param level
     *          debug level usually from 0 to 2. 0 means no debug output.
     */
    public static void setDebugLevel(final int level) {
        debugLevel = level;
    }

    public static void setShowAppWarning(final boolean aw) {
        showAppWarning = aw;
    }

    public static void setShowAppError(final boolean ae) {
        showAppError = ae;
    }

    public static boolean getShowAppWarning() {
        return showAppWarning;
    }

    public static boolean getShowAppError() {
        return showAppError;
    }

    public static Logger getLogger(final Class<?> clazz) {
        Logger logger;
        final String name = clazz.getName();
        synchronized (LoggerFactory.class) {
            logger = LOGGER_MAP.get(name);
            if (logger == null) {
                logger = new Logger(name);
                LOGGER_MAP.put(name, logger);
            }
        }
        return logger;
    }

    /** Return the whole log buffer. */
    public static String getLogBuffer() {
        final StringBuilder lb = new StringBuilder();
        synchronized (LOG_BUFFER) {
            for (final String l : LOG_BUFFER) {
                lb.append(l).append('\n');
            }
        }
        return lb.toString();
    }

    private LoggerFactory() {
    }
}
