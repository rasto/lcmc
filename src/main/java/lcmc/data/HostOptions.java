/*
 * This file is part of Linux Cluster Management Console by Rasto Levrinc
 *
 * Copyright (C) 2011, Rasto Levrinc
 *
 * LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package lcmc.data;

/**
 * Host command line options.
 */
public final class HostOptions {
    /** Host name or ip. */
    private final String host;
    /** User to log in. */
    private String user = null;
    /** Port. */
    private String port = null;
    /** Whether to use sudo. */
    private boolean sudo = false;

    /** Create new HostOptions object. */
    public HostOptions(final String host) {
        this.host = host;
    }

    /** Return host name or ip. */
    public String getHost() {
        return host;
    }

    /** Return user. */
    public String getUser() {
        return user;
    }

    /** Set user. */
    public void setUser(final String user) {
        this.user = user;
    }

    /** Return port. */
    public String getPort() {
        return port;
    }

    /** Set port. */
    public void setPort(final String port) {
        this.port = port;
    }

    /** Return whether to use sudo. */
    public boolean getSudo() {
        return sudo;
    }

    /** Set whether to use sudo. */
    public void setSudo(final boolean sudo) {
        this.sudo = sudo;
    }
}
