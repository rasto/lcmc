/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2011-2013, Rastislav Levrinc.
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
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.data;

import lcmc.Exceptions;
import lcmc.utilities.Tools;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * DRBD Proxy functions.
 */
public final class DrbdProxy {
    private static final boolean PROXY = true;
    private static final boolean NOT_PROXY = false;
    private static final boolean DONE = true;
    private static final boolean NOT_DONE = false;

    /** Prefix for the parameter. */
    public static final String PLUGIN_PREFIX = "plugin-";

    private DrbdProxy() {
        /* utility class. */
    }

    /**
     * Since DRBD 8.4 drbdadm dump-xml doesn't create xml in common section
     * parse the config.
     *
     * proxy {
     *     memlimit     100M;
     *     plugin {
     *         zlib level 9;
     *     }
     * }
     *
     * Return whether there's a valid proxy config.
     */
    static boolean parse(final String text,
                         final Map<String, String> nameValueMap)
    throws Exceptions.DrbdConfigException {
        final StringTokenizer st = new StringTokenizer(text);
        while (st.hasMoreTokens()) {
            if ("proxy".equals(st.nextToken())) {
                Tools.debug(null, "proxy: " + text, 1);
                final String nextToken = st.nextToken();
                if (!"{".equals(nextToken)) {
                    throw new Exceptions.DrbdConfigException(
                      "proxy config: unexpected token: " + nextToken + "/'{'");
                }
                while (st.hasMoreTokens()) {
                     final boolean done = parseStatement("", st, nameValueMap);
                     if (done) {
                         return PROXY;
                     }
                }
                throw new Exceptions.DrbdConfigException(
                                                "proxy config: parsing error");
            }
        }
        return NOT_PROXY;
    }

    /**
     * Parse plugin definition.
     *
     * plugin {
     *     zlib level 9;
     * }
     */
    private static void parsePlugin(final StringTokenizer st,
                                    final Map<String, String> nameValueMap)
    throws Exceptions.DrbdConfigException {
        final String nextToken = st.nextToken();
        if (!"{".equals(nextToken)) {
            throw new Exceptions.DrbdConfigException(
              "proxy plugin config: unexpected token: " + nextToken + "/'{'");
        }
        while (st.hasMoreTokens()) {
            final boolean done = parseStatement(PLUGIN_PREFIX,
                                                st,
                                                nameValueMap);
            if (done) {
                return;
            }
        }
        throw new Exceptions.DrbdConfigException(
                                         "proxy plugin config: parsing error");
    }

    /**
     * Parse statement like: memlimit 100M;
     * Return whether the parsing has reached the end of parsing.
     */
    private static boolean parseStatement(
                                    final String prefix,
                                    final StringTokenizer st,
                                    final Map<String, String> nameValueMap)
    throws Exceptions.DrbdConfigException {
        String nextToken = st.nextToken();
        if ("plugin".equals(nextToken)) {
            parsePlugin(st, nameValueMap);
            return NOT_DONE;
        } else if ("}".equals(nextToken)) {
            return DONE;
        } else if (endOfStatement(nextToken)) {
            final String param = nextToken.substring(0, nextToken.length() - 1);
            final String value = DrbdXML.CONFIG_YES;
            nameValueMap.put(prefix + param, value);
            return NOT_DONE;
        }
        final String param = nextToken;
        String value = null;
        boolean eos = false;
        while (st.hasMoreTokens() && !eos) {
            nextToken = st.nextToken();
            if (endOfStatement(nextToken)) {
                eos = true;
                nextToken = nextToken.substring(0, nextToken.length() - 1);
            }
            if (value == null) {
                value = nextToken;
            } else {
                value += " " + nextToken;
            }
            if (eos) {
                break;
            }
        }
        nameValueMap.put(prefix + param, value);
        if (!eos) {
            throw new Exceptions.DrbdConfigException(
                                            "proxy config: statement error");
        }
        return NOT_DONE;
    }

    /**
     * Return whether it ends with ';'
     * Token can be null.
     */
    private static boolean endOfStatement(final String token) {
        if (token == null) {
            return false;
        }
        return token.endsWith(";");
    }
}
