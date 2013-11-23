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


package lcmc.data;

import lcmc.utilities.Tools;
import lcmc.gui.TerminalPanel;
import lcmc.gui.ClusterTab;

import java.io.IOException;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * This class parses xml from user configs and creates data objects,
 * that describe the hosts and clusters.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class UserConfig extends XML {
    /** Host name attribute string. */
    private static final String HOST_NAME_ATTR = "name";
    /** Host ssh port attribute string. */
    private static final String HOST_SSHPORT_ATTR = "ssh";
    /** Host color attribute string. */
    private static final String HOST_COLOR_ATTR = "color";
    /** Host use sudo attribute string. */
    private static final String HOST_USESUDO_ATTR = "sudo";
    /** Cluster name attribute string. */
    private static final String CLUSTER_NAME_ATTR = "name";
    /** Name of the host node. */
    private static final String HOST_NODE_STRING = "host";
    /** Name of the proxy host node. */
    private static final String PROXY_HOST_NODE_STRING = "proxy-host";
    /** Download user. */
    private static final String DOWNLOAD_USER_ATTR = "dwuser";
    /** Download user password. */
    private static final String DOWNLOAD_PASSWD_ATTR = "dwpasswd";
    /** Whether the host is a proxy host. */
    public static final boolean PROXY_HOST = true;

    /** Saves data about clusters and hosts to the supplied output stream. */
    public String saveXML(final OutputStream outputStream,
                          final boolean saveAll) throws IOException {
        final String encoding = "UTF-8";
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;

        try {
             db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
             assert false;
        }
        final Document doc = db.newDocument();
        final Element root = (Element) doc.appendChild(
                                                doc.createElement("drbdgui"));
        if (Tools.getConfigData().getLoginSave()) {
            final String downloadUser =
                                Tools.getConfigData().getDownloadUser();
            final String downloadPasswd =
                                Tools.getConfigData().getDownloadPassword();
            if (downloadUser != null && downloadPasswd != null) {
                root.setAttribute(DOWNLOAD_USER_ATTR, downloadUser);
                root.setAttribute(DOWNLOAD_PASSWD_ATTR, downloadPasswd);
            }
        }
        final Element hostsNode = (Element) root.appendChild(
                                                doc.createElement("hosts"));
        final Set<Host> hosts = Tools.getConfigData().getHosts().getHostSet();
        for (final Host host : hosts) {
            if (!saveAll && !host.isSavable()) {
                continue;
            }
            host.setSavable(true);
            addHostConfigNode(doc, hostsNode, HOST_NODE_STRING, host);
        }
        final Element clusters = (Element) root.appendChild(
                                                doc.createElement("clusters"));

        final Set<Cluster> clusterSet =
                        Tools.getConfigData().getClusters().getClusterSet();
        for (final Cluster cluster : clusterSet) {
            if (!saveAll && !cluster.isSavable()) {
                continue;
            }
            cluster.setSavable(true);
            final String clusterName = cluster.getName();
            final Element clusterNode = (Element) clusters.appendChild(
                                                doc.createElement("cluster"));
            clusterNode.setAttribute(CLUSTER_NAME_ATTR, clusterName);
            final Set<Host> clusterHosts = cluster.getHosts();
            for (final Host host : clusterHosts) {
                final String hostName = host.getHostname();
                final Element hostNode = (Element) clusterNode.appendChild(
                                        doc.createElement(HOST_NODE_STRING));
                hostNode.appendChild(doc.createTextNode(hostName));
            }
            for (final Host pHost : cluster.getProxyHosts()) {
                if (clusterHosts.contains(pHost)) {
                    continue;
                }
                final String hostName = pHost.getHostname();
                final Element hostNode = (Element) clusterNode.appendChild(
                                    doc.createElement(PROXY_HOST_NODE_STRING));
                hostNode.appendChild(doc.createTextNode(hostName));
            }
        }

        final TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = null;
        try {
            t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.ENCODING, encoding);
        } catch (TransformerConfigurationException tce) {
            assert false;
        }
        final DOMSource doms = new DOMSource(doc);
        final StreamResult sr = new StreamResult(outputStream);
        try {
            t.transform(doms, sr);
        } catch (TransformerException te) {
            final IOException ioe = new IOException();
            ioe.initCause(te);
            throw ioe;
        }
        return "";
    }

    /**
     * Starts specified clusters and connects to the hosts of this clusters.
     */
    public void startClusters(final List<Cluster> selectedClusters) {
        final Set<Cluster> clusters =
                        Tools.getConfigData().getClusters().getClusterSet();
        if (clusters != null) {
            /* clusters */
            for (final Cluster cluster : clusters) {
                if (selectedClusters != null
                    && !selectedClusters.contains(cluster)) {
                    continue;
                }
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Tools.getGUIData().addClusterTab(cluster);
                    }
                });
                if (cluster.getHosts().isEmpty()) {
                    continue;
                }
                final boolean ok = cluster.connect(null, true, 1);
                if (!ok) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Tools.getGUIData().getClustersPanel().removeTab(
                                                                       cluster);
                        }
                    });
                    continue;
                }
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        for (final Host host : cluster.getHosts()) {
                            host.waitOnLoading();
                        }
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                final ClusterTab ct = cluster.getClusterTab();
                                if (ct != null) {
                                    ct.addClusterView();
                                    ct.requestFocus();
                                }
                            }
                        });
                    }
                };
                final Thread thread = new Thread(runnable);
                thread.start();
            }
        }
    }

    /**
     * Loads info from xml that is supplied as an argument to the internal
     * data objects.
     */
    public void loadXML(final String xml) {
        final Document document = getXMLDocument(xml);
        if (document == null) {
            return;
        }
        /* get root <drbdgui> */
        final Node rootNode = getChildNode(document, "drbdgui");
        final Map<String, List<Host>> hostMap =
                                       new LinkedHashMap<String, List<Host>>();
        if (rootNode != null) {
            /* download area */
            final String downloadUser = getAttribute(rootNode,
                                                     DOWNLOAD_USER_ATTR);
            final String downloadPasswd = getAttribute(rootNode,
                                                       DOWNLOAD_PASSWD_ATTR);
            if (downloadUser != null && downloadPasswd != null) {
                Tools.getConfigData().setDownloadLogin(downloadUser,
                                                       downloadPasswd,
                                                       true);
            }
            /* hosts */
            final Node hostsNode = getChildNode(rootNode, "hosts");
            if (hostsNode != null) {
                final NodeList hosts = hostsNode.getChildNodes();
                if (hosts != null) {
                    for (int i = 0; i < hosts.getLength(); i++) {
                        final Node hostNode = hosts.item(i);
                        if (hostNode.getNodeName().equals(HOST_NODE_STRING)) {
                            final String nodeName =
                                                getAttribute(hostNode,
                                                             HOST_NAME_ATTR);
                            final String sshPort =
                                                getAttribute(hostNode,
                                                             HOST_SSHPORT_ATTR);
                            final String color = getAttribute(hostNode,
                                                              HOST_COLOR_ATTR);
                            final String useSudo =
                                                getAttribute(hostNode,
                                                             HOST_USESUDO_ATTR);
                            final Node ipNode = getChildNode(hostNode, "ip");
                            String ip = null;
                            if (ipNode != null) {
                                ip = getText(ipNode);
                            }
                            final Node usernameNode = getChildNode(hostNode,
                                                                   "user");
                            final String username = getText(usernameNode);
                            setHost(hostMap,
                                    username,
                                    nodeName,
                                    ip,
                                    sshPort,
                                    color,
                                    "true".equals(useSudo),
                                    true);
                        }
                    }
                }
            }

            /* clusters */
            final Node clustersNode = getChildNode(rootNode, "clusters");
            if (clustersNode != null) {
                final NodeList clusters = clustersNode.getChildNodes();
                if (clusters != null) {
                    for (int i = 0; i < clusters.getLength(); i++) {
                        final Node clusterNode = clusters.item(i);
                        if (clusterNode.getNodeName().equals("cluster")) {
                            final String clusterName =
                                               getAttribute(clusterNode,
                                                            CLUSTER_NAME_ATTR);
                            final Cluster cluster = new Cluster();
                            cluster.setName(clusterName);
                            Tools.getConfigData().addClusterToClusters(cluster);
                            loadClusterHosts(clusterNode, cluster, hostMap);
                        }
                    }
                }
            }
        }
    }

    /** Create host object and initialize it from user config. */
    public void setHost(final Map<String, List<Host>> hostMap,
                        String username,
                        final String nodeName,
                        final String ip,
                        String sshPort,
                        final String color,
                        final boolean sudo,
                        final boolean savable) {
        Tools.getConfigData().setLastEnteredUser(username);
        final Host host = new Host();
        host.setSavable(savable);
        host.setHostname(nodeName);
        if (sshPort == null) {
            sshPort = "22";
        }
        host.setSSHPort(sshPort);
        Tools.getConfigData().setLastEnteredSSHPort(sshPort);
        if (color != null) {
            host.setSavedColor(color);
        }
        host.setUseSudo(sudo);
        Tools.getConfigData().setLastEnteredUseSudo(sudo);
        Tools.getConfigData().addHostToHosts(host);

        new TerminalPanel(host);
        host.setIp(ip);
        if (username == null && sudo) {
            username = System.getProperty("user.name");
        }
        if (username == null) {
            username = Host.ROOT_USER;
        }
        host.setUsername(username);
        List<Host> hostList = hostMap.get(nodeName);
        if (hostList == null) {
            hostList = new ArrayList<Host>();
            hostMap.put(nodeName, hostList);
        }
        hostList.add(host);
    }

    /** Set host as a cluster host. */
    public void setHostCluster(final Map<String, List<Host>> hostMap,
                               final Cluster cluster,
                               final String nodeName,
                               final boolean proxy) {
        final List<Host> hostList = hostMap.get(nodeName);
        if (hostList == null || hostList.isEmpty()) {
            return;
        }
        final Host host = hostList.get(0);
        hostList.remove(0);
        if (host != null && !host.isInCluster()) {
            host.setCluster(cluster);
            if (proxy) {
                cluster.addProxyHost(host);
            } else {
                cluster.addHost(host);
            }
        }
    }

    /**
     * Loads info about hosts from the specified cluster to the internal data
     * objects.
     */
    private void loadClusterHosts(final Node clusterNode,
                                  final Cluster cluster,
                                  final Map<String, List<Host>> hostMap) {
        final NodeList hosts = clusterNode.getChildNodes();
        if (hosts != null) {
            for (int i = 0; i < hosts.getLength(); i++) {
                final Node hostNode = hosts.item(i);
                if (hostNode.getNodeName().equals(HOST_NODE_STRING)) {
                    final String nodeName = getText(hostNode);
                    setHostCluster(hostMap, cluster, nodeName, !PROXY_HOST);
                } else if (hostNode.getNodeName().equals(
                                                    PROXY_HOST_NODE_STRING)) {
                    final String nodeName = getText(hostNode);
                    setHostCluster(hostMap, cluster, nodeName, PROXY_HOST);
                }
            }
        }
    }

    /** Host node. */
    private void addHostConfigNode(final Document doc,
                                   final Element parent,
                                   final String nodeName,
                                   final Host host) {
        final String hostName = host.getHostname();
        final String ip = host.getIp();
        final String username = host.getUsername();
        final String sshPort = host.getSSHPort();
        final Boolean useSudo = host.isUseSudo();
        final String color = host.getColor();
        final Element hostNode = (Element) parent.appendChild(
                                                  doc.createElement(nodeName));
        hostNode.setAttribute(HOST_NAME_ATTR, hostName);
        hostNode.setAttribute(HOST_SSHPORT_ATTR, sshPort);
        if (color != null) {
            hostNode.setAttribute(HOST_COLOR_ATTR, color);
        }
        if (useSudo != null && useSudo) {
            hostNode.setAttribute(HOST_USESUDO_ATTR, "true");
        }
        if (ip != null) {
            final Node ipNode = (Element) hostNode.appendChild(
                                                   doc.createElement("ip"));

            ipNode.appendChild(doc.createTextNode(ip));
        }
        if (username != null) {
            final Node usernameNode =
                            (Element) hostNode.appendChild(
                                                doc.createElement("user"));

            usernameNode.appendChild(doc.createTextNode(username));
        }
    }
}
