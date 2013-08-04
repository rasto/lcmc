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

package lcmc.utilities;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections15.Buffer;
import org.apache.commons.collections15.BufferUtils;
import org.apache.commons.collections15.buffer.CircularFifoBuffer;

/**
 * @author Rasto Levrinc
 */
public final class LoggerFactory {
    private final static Map<String, Logger> loggerMap =
                                              new HashMap<String, Logger>();
    /** Debug level. */
    private static int debugLevel = -1;
    /** Whether the warnings should be shown. */
    private static boolean appWarning = false;
    /** Whether application errors should show a dialog. */
    private static boolean appError = false;
    /** Size of circular log buffer. */
    private static final int CIRCULAR_LOG_SIZE = 200;
    /** Synchronized, Circular log. */
    static final Buffer<String> LOG_BUFFER =
              BufferUtils.synchronizedBuffer(new CircularFifoBuffer<String>(
                                                        CIRCULAR_LOG_SIZE));
    /** Increments the debug level. */
    public static void incrementDebugLevel() {
        debugLevel++;
        System.out.println("debug level: " + debugLevel);
    }

    /** Decrements the debug level. */
    public static void decrementDebugLevel() {
        debugLevel--;
        System.out.println("debug level: " + debugLevel);
    }

    /** Return debug level. */
    public static int getDebugLevel() {
        return debugLevel;
    }

    /**
     * Sets debug level.
     *
     * @param level
     *          debug level usually from 0 to 2. 0 means no debug output.
     */
    public static void setDebugLevel(final int level) {
        debugLevel = level;
    }

    public static void setAppWarning(final boolean aw) {
        appWarning = aw;
    }

    public static void setAppError(final boolean ae) {
        appError = ae;
    }

    public static boolean getAppWarning() {
        return appWarning;
    }

    public static boolean getAppError() {
        return appError;
    }

    private LoggerFactory() {
    }

    public static Logger getLogger(final Class<?> clazz) {
        Logger logger;
        final String name = clazz.getName();
        synchronized (LoggerFactory.class) {
            logger = loggerMap.get(name);
            if (logger == null) {
                logger = new Logger(name);
                loggerMap.put(name, logger);
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
}
