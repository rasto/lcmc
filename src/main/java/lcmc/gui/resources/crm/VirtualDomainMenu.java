package lcmc.gui.resources.crm;

import java.util.Collection;
import java.util.List;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.data.VMSXML;
import lcmc.gui.resources.vms.DomainInfo;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class VirtualDomainMenu extends ServiceMenu {
    
    private final VirtualDomainInfo virtualDomainInfo;

    private final DomainInfo domainInfo;

    public VirtualDomainMenu(VirtualDomainInfo virtualDomainInfo) {
        super(virtualDomainInfo);
        this.virtualDomainInfo = virtualDomainInfo;
        domainInfo = virtualDomainInfo.getDomainInfo();
    }

    @Override
    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = super.getPulldownMenu();
        addVncViewersToTheMenu(items);
        return items;
    }

    /** Adds vnc viewer menu items. */
    private void addVncViewersToTheMenu(final Collection<UpdatableItem> items) {
        if (Tools.getApplication().isTightvnc()) {
            /* tight vnc test menu */
            final UpdatableItem tightvncViewerMenu = new MyMenuItem(
                            "start TIGHT VNC viewer",
                            null,
                            null,
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    final VMSXML vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml == null || domainInfo == null) {
                        return "VM is not available";
                    }
                    final int remotePort = vxml.getRemotePort(
                                               domainInfo.getName());
                    if (remotePort <= 0) {
                        return "remote port is not greater than 0";
                    }
                    return null;
                }

                @Override
                public void action() {
                    virtualDomainInfo.hidePopup();
                    final DomainInfo vvdi = domainInfo;
                    final VMSXML vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml != null && vvdi != null) {
                        final int remotePort = vxml.getRemotePort(
                                                               vvdi.getName());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startTightVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(tightvncViewerMenu);
        }

        if (Tools.getApplication().isUltravnc()) {
            /* ultra vnc test menu */
            final UpdatableItem ultravncViewerMenu = new MyMenuItem(
                            "start ULTRA VNC viewer",
                            null,
                            null,
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    final VMSXML vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml == null || domainInfo == null) {
                        return "VM is not available";
                    }
                    final int remotePort = vxml.getRemotePort(
                                               domainInfo.getName());
                    if (remotePort <= 0) {
                        return "remote port is not greater than 0";
                    }
                    return null;
                }

                @Override
                public void action() {
                    virtualDomainInfo.hidePopup();
                    final DomainInfo vvdi = domainInfo;
                    final VMSXML vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml != null && vvdi != null) {
                        final int remotePort = vxml.getRemotePort(
                                                           vvdi.getName());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startUltraVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(ultravncViewerMenu);
        }

        if (Tools.getApplication().isRealvnc()) {
            /* real vnc test menu */
            final UpdatableItem realvncViewerMenu = new MyMenuItem(
                            "start REAL VNC test",
                            null,
                            null,
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    final VMSXML vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml == null || domainInfo == null) {
                        return "VM is not available";
                    }
                    final int remotePort = vxml.getRemotePort(
                                               domainInfo.getName());
                    if (remotePort <= 0) {
                        return "remote port is not greater than 0";
                    }
                    return null;
                }

                @Override
                public void action() {
                    virtualDomainInfo.hidePopup();
                    final DomainInfo vvdi = domainInfo;
                    final VMSXML vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml != null && vvdi != null) {
                        final int remotePort = vxml.getRemotePort(
                                                            vvdi.getName());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startRealVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(realvncViewerMenu);
        }
    }

    /** Returns the first on which this vm is running. */
    private Host getRunningOnHost() {
        final List<String> nodes = virtualDomainInfo.getRunningOnNodes(Application.RunMode.LIVE);
        if (nodes != null
            && !nodes.isEmpty()) {
            return virtualDomainInfo.getBrowser().getCluster().getHostByName(nodes.get(0));
        }
        return null;
    }
}
