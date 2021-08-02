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
package lcmc.cluster.infrastructure.ssh;

public class LastSuccessfulPassword {
    private String password;
    private String rsaKey;
    private String dsaKey;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRsaKey() {
        return rsaKey;
    }

    public void setRsaKey(String rsaKey) {
        this.rsaKey = rsaKey;
    }

    public String getDsaKey() {
        return dsaKey;
    }

    public void setDsaKey(String dsaKey) {
        this.dsaKey = dsaKey;
    }

    public void setPasswordsIfNoneIsSet(final String dsaKey, final String rsaKey, final String password) {
        if (this.dsaKey == null && this.rsaKey == null && this.password == null) {
            this.dsaKey = dsaKey;
            this.rsaKey = rsaKey;
            this.password = password;
        }
    }
}