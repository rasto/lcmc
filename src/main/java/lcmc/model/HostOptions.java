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
package lcmc.model;

/**
 * Host command line options.
 */
public final class HostOptions {
    /** Host name or ip. */
    private final String host;
    private String loginUser = null;
    private String port = null;
    private boolean useSudo = false;

    public HostOptions(final String host) {
        this.host = host;
    }

    /** Return host name or ip. */
    public String getHost() {
        return host;
    }

    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(final String loginUser) {
        this.loginUser = loginUser;
    }

    public String getPort() {
        return port;
    }

    public void setPort(final String port) {
        this.port = port;
    }

    public boolean getUseSudo() {
        return useSudo;
    }

    public void setUseSudo(final boolean useSudo) {
        this.useSudo = useSudo;
    }
}
