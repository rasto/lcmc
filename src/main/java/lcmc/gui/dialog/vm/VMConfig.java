/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, Rastislav Levrinc
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
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


package lcmc.gui.dialog.vm;

import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.resources.VMSVirtualDomainInfo;

/**
 * VMConfig super class from which all the vm wizards can be
 * extended.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public abstract class VMConfig extends WizardDialog {
    /** VMS Virtual domain info object. */
    private final VMSVirtualDomainInfo vmsVirtualDomainInfo;

    /** Prepares a new {@code VMConfig} object. */
    protected VMConfig(final WizardDialog previousDialog,
                       final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(previousDialog);
        this.vmsVirtualDomainInfo = vmsVirtualDomainInfo;
    }

    /** Returns vms virtual domain info object. */
    protected final VMSVirtualDomainInfo getVMSVirtualDomainInfo() {
        return vmsVirtualDomainInfo;
    }
}
