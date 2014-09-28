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


package lcmc.common.domain;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Clusters;
import lcmc.cluster.ui.ClusterTab;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;
import lcmc.host.domain.Hosts;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.cluster.ui.ClusterTabFactory;
import lcmc.cluster.ui.ClustersPanel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class parses xml from user configs and creates data objects,
 * that describe the hosts and clusters.
 */
@Named
@Singleton
public final class UserConfig extends XML {
    private static final Logger LOG = LoggerFactory.getLogger(XML.class);
    private static final String HOST_NAME_ATTR = "name";
    private static final String HOST_SSHPORT_ATTR = "ssh";
    private static final String HOST_COLOR_ATTR = "color";
    private static final String HOST_USESUDO_ATTR = "sudo";
    private static final String CLUSTER_NAME_ATTR = "name";
    private static final String HOST_NODE_STRING = "host";
    private static final String PROXY_HOST_NODE_STRING = "proxy-host";
    private static final String DOWNLOAD_USER_ATTR = "dwuser";
    private static final String DOWNLOAD_PASSWD_ATTR = "dwpasswd";
    private static final String ENCODING = "UTF-8";
    public static final boolean PROXY_HOST = true;

    @Inject
    private ClusterTabFactory clusterTabFactory;
    @Inject
    private HostFactory hostFactory;
    @Inject
    private ClustersPanel clustersPanel;
    @Inject
    private Provider<Cluster> clusterProvider;
    @Inject
    private Application application;
    @Inject
    private Hosts allHosts;
    @Inject
    private Clusters allClusters;

    /** Saves data about clusters and hosts to the supplied output stream. */
    public String saveXML(final OutputStream outputStream, final boolean saveAll) throws IOException {
        LOG.debug1("saveXML: start");
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        final DocumentBuilder db;
        try {
             db = dbf.newDocumentBuilder();
        } catch (final ParserConfigurationException pce) {
             throw new IOException("saveXML: cannot configure parser", pce);
        }
        final Document doc = db.newDocument();
        final Element root = (Element) doc.appendChild(doc.createElement("drbdgui"));
        if (application.getLoginSave()) {
            final String downloadUser = application.getDownloadUser();
            final String downloadPasswd = application.getDownloadPassword();
            if (downloadUser != null && downloadPasswd != null) {
                root.setAttribute(DOWNLOAD_USER_ATTR, downloadUser);
                root.setAttribute(DOWNLOAD_PASSWD_ATTR, downloadPasswd);
            }
        }
        final Node hostsNode = root.appendChild(doc.createElement("hosts"));
        final Set<Host> hosts = allHosts.getHostSet();
        for (final Host host : hosts) {
            if (!saveAll && !host.isSavable()) {
                continue;
            }
            host.setSavable(true);
            addHostConfigNode(doc, hostsNode, HOST_NODE_STRING, host);
        }
        final Node clusters = root.appendChild(doc.createElement("clusters"));

        final Set<Cluster> clusterSet = allClusters.getClusterSet();
        for (final Cluster cluster : clusterSet) {
            if (!saveAll && !cluster.isSavable()) {
                continue;
            }
            cluster.setSavable(true);
            final String clusterName = cluster.getName();
            final Element clusterNode = (Element) clusters.appendChild(doc.createElement("cluster"));
            clusterNode.setAttribute(CLUSTER_NAME_ATTR, clusterName);
            final Set<Host> clusterHosts = cluster.getHosts();
            for (final Host host : clusterHosts) {
                final String hostName = host.getHostname();
                final Node hostNode = clusterNode.appendChild(doc.createElement(HOST_NODE_STRING));
                hostNode.appendChild(doc.createTextNode(hostName));
            }
            for (final Host pHost : cluster.getProxyHosts()) {
                if (clusterHosts.contains(pHost)) {
                    continue;
                }
                final String hostName = pHost.getHostname();
                final Node hostNode = clusterNode.appendChild(doc.createElement(PROXY_HOST_NODE_STRING));
                hostNode.appendChild(doc.createTextNode(hostName));
            }
        }

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, ENCODING);
        } catch (final TransformerConfigurationException tce) {
            throw new IOException("saveXML: transformer config failed", tce);
        }
        final Source doms = new DOMSource(doc);
        final Result streamResult = new StreamResult(outputStream);
        try {
            transformer.transform(doms, streamResult);
        } catch (final TransformerException te) {
            throw new IOException("saveXML: transform failed", te);
        }
        LOG.debug1("saveXML: end");
        return "";
    }

    /**
     * Starts specified clusters and connects to the hosts of this clusters.
     */
    public void startClusters(final Collection<Cluster> selectedClusters) {
        final Set<Cluster> clusters = allClusters.getClusterSet();
        if (clusters != null) {
            /* clusters */
            for (final Cluster cluster : clusters) {
                if (selectedClusters != null && !selectedClusters.contains(cluster)) {
                    continue;
                }
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        clusterTabFactory.createClusterTab(cluster);
                    }
                });
                if (cluster.getHosts().isEmpty()) {
                    continue;
                }
                final boolean ok = cluster.connect(null, true, 1);
                if (!ok) {
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            clustersPanel.removeTabWithCluster(cluster);
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
                        application.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                final ClusterTab clusterTab = cluster.getClusterTab();
                                if (clusterTab != null) {
                                    clusterTab.addClusterView();
                                    clusterTab.requestFocus();
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
        final Map<String, List<Host>> hostMap = new LinkedHashMap<String, List<Host>>();
        if (rootNode != null) {
            /* download area */
            final String downloadUser = getAttribute(rootNode, DOWNLOAD_USER_ATTR);
            final String downloadPasswd = getAttribute(rootNode, DOWNLOAD_PASSWD_ATTR);
            if (downloadUser != null && downloadPasswd != null) {
                application.setDownloadLogin(downloadUser, downloadPasswd, true);
            }
            /* hosts */
            final Node hostsNode = getChildNode(rootNode, "hosts");
            if (hostsNode != null) {
                final NodeList hosts = hostsNode.getChildNodes();
                if (hosts != null) {
                    for (int i = 0; i < hosts.getLength(); i++) {
                        final Node hostNode = hosts.item(i);
                        if (hostNode.getNodeName().equals(HOST_NODE_STRING)) {
                            final String nodeName = getAttribute(hostNode, HOST_NAME_ATTR);
                            final String sshPort = getAttribute(hostNode, HOST_SSHPORT_ATTR);
                            final String color = getAttribute(hostNode, HOST_COLOR_ATTR);
                            final String useSudo = getAttribute(hostNode, HOST_USESUDO_ATTR);
                            final Node ipNode = getChildNode(hostNode, "ip");
                            String ip = null;
                            if (ipNode != null) {
                                ip = getText(ipNode);
                            }
                            final Node usernameNode = getChildNode(hostNode, "user");
                            final String username = getText(usernameNode);
                            setHost(hostMap, username, nodeName, ip, sshPort, color, "true".equals(useSudo), true);
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
                        if ("cluster".equals(clusterNode.getNodeName())) {
                            final String clusterName = getAttribute(clusterNode, CLUSTER_NAME_ATTR);
                            final Cluster cluster = clusterProvider.get();
                            cluster.setName(clusterName);
                            application.addClusterToClusters(cluster);
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
        application.setLastEnteredUser(username);
        final Host host = hostFactory.createInstance();
        host.setSavable(savable);
        host.setHostname(nodeName);
        if (sshPort == null) {
            sshPort = "22";
        }
        host.setSSHPort(sshPort);
        application.setLastEnteredSSHPort(sshPort);
        if (color != null) {
            host.setSavedColor(color);
        }
        host.setUseSudo(sudo);
        application.setLastEnteredUseSudo(sudo);
        application.addHostToHosts(host);

        host.setIpAddress(ip);
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
                } else if (hostNode.getNodeName().equals(PROXY_HOST_NODE_STRING)) {
                    final String nodeName = getText(hostNode);
                    setHostCluster(hostMap, cluster, nodeName, PROXY_HOST);
                }
            }
        }
    }

    private void addHostConfigNode(final Document doc, final Node parent, final String nodeName, final Host host) {
        final String hostName = host.getHostname();
        final String ip = host.getIpAddress();
        final String username = host.getUsername();
        final String sshPort = host.getSSHPort();
        final Boolean useSudo = host.isUseSudo();
        final String color = host.getColor();
        final Element hostNode = (Element) parent.appendChild(doc.createElement(nodeName));
        hostNode.setAttribute(HOST_NAME_ATTR, hostName);
        hostNode.setAttribute(HOST_SSHPORT_ATTR, sshPort);
        if (color != null) {
            hostNode.setAttribute(HOST_COLOR_ATTR, color);
        }
        if (useSudo != null && useSudo) {
            hostNode.setAttribute(HOST_USESUDO_ATTR, "true");
        }
        if (ip != null) {
            final Node ipNode = hostNode.appendChild(doc.createElement("ip"));

            ipNode.appendChild(doc.createTextNode(ip));
        }
        if (username != null) {
            final Node usernameNode = hostNode.appendChild(doc.createElement("user"));

            usernameNode.appendChild(doc.createTextNode(username));
        }
    }
}
