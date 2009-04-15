/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.data;

import drbd.utilities.Tools;
import drbd.gui.TerminalPanel;

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
 * This class parses xml from drbdsetup and drbdadm, stores the
 * information in the hashes and provides methods to get this
 * information.
 * The xml is obtained with drbdsetp xml command and drbdadm dump-xml.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class DrbdGuiXML extends XML {
    /** Host name attribute string. */
    private static final String HOST_NAME_ATTR = "name";
    /** Host ssh port attribute string. */
    private static final String HOST_SSHPORT_ATTR = "ssh";
    /** Cluster name attribute string. */
    private static final String CLUSTER_NAME_ATTR = "name";
    /** Name of the host node. */
    private static final String HOST_NODE_STRING = "host";

    /**
     * Saves data about clusters and hosts to the supplied output stream.
     */
    public final String saveXML(final OutputStream outputStream)
    throws IOException {
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
        final Element hosts = (Element) root.appendChild(
                                                doc.createElement("hosts"));
        for (final Host host : Tools.getConfigData().getHosts().getHostSet()) {
            final String hostName = host.getHostname();
            final String ip = host.getIp();
            final String username = host.getUsername();
            final String sshPort = host.getSSHPort();
            final Element hostNode = (Element) hosts.appendChild(
                                        doc.createElement(HOST_NODE_STRING));
            hostNode.setAttribute(HOST_NAME_ATTR, hostName);
            hostNode.setAttribute(HOST_SSHPORT_ATTR, sshPort);
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
        final Element clusters = (Element) root.appendChild(
                                                doc.createElement("clusters"));

        final Set<Cluster> clusterSet =
                        Tools.getConfigData().getClusters().getClusterSet();
        for (final Cluster cluster : clusterSet) {
            final String clusterName = cluster.getName();
            final Element clusterNode = (Element) clusters.appendChild(
                                                doc.createElement("cluster"));
            clusterNode.setAttribute(CLUSTER_NAME_ATTR, clusterName);
            for (final Host host : cluster.getHosts()) {
                final String hostName = host.getHostname();
                final Element hostNode =
                                (Element) clusterNode.appendChild(
                                        doc.createElement(HOST_NODE_STRING));
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
    public final void startClusters(final List<Cluster> selectedClusters) {
        final Set<Cluster> clusters =
                        Tools.getConfigData().getClusters().getClusterSet();
        if (clusters != null) {
            /* clusters */
            for (final Cluster cluster : clusters) {
                if (selectedClusters != null
                    && !selectedClusters.contains(cluster)) {
                    continue;
                }
                Tools.getGUIData().addClusterTab(cluster);
                boolean first = true;
                String dsaKey = null;
                String rsaKey = null;
                String pwd = null;
                for (final Host host : cluster.getHosts()) {
                    host.setIsLoading();
                    if (!first) {
                        host.getSSH().setPasswords(dsaKey, rsaKey, pwd);
                    }
                    host.connect();
                    if (first) {
                        /* wait till it's connected and try the others with the
                         * same password/key. */
                        host.getSSH().waitForConnection();
                        if (host.isConnected()) {
                            dsaKey = host.getSSH().getLastDSAKey();
                            rsaKey = host.getSSH().getLastRSAKey();
                            pwd = host.getSSH().getLastPassword();
                        }
                    }
                    first = false;
                }

                final Runnable runnable = new Runnable() {
                    public void run() {
                        for (final Host host : cluster.getHosts()) {
                            host.waitOnLoading();
                        }
                        cluster.getClusterTab().addClusterView();
                        cluster.getClusterTab().requestFocus();
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
    public final void loadXML(final String xml) {
        final Document document = getXMLDocument(xml);
        /* get root <drbdgui> */
        final Node rootNode = getChildNode(document, "drbdgui");
        final Map<String, Host> hostMap = new LinkedHashMap<String, Host>();
        if (rootNode != null) {
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
                            final Node ipNode = getChildNode(hostNode, "ip");
                            final String ip = getText(ipNode);
                            final Node usernameNode = getChildNode(hostNode,
                                                                   "user");
                            final String username = getText(usernameNode);
                            final Host host = new Host();
                            host.setHostname(nodeName);
                            if (sshPort != null) {
                                host.setSSHPort(sshPort);
                            }
                            Tools.getConfigData().addHostToHosts(host);

                            new TerminalPanel(host);
                            host.setIp(ip);
                            host.setUsername(username);
                            hostMap.put(nodeName, host);
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

    /**
     * Loads info about hosts from the specified cluster to the internal data
     * objects.
     */
    private void loadClusterHosts(final Node clusterNode,
                                  final Cluster cluster,
                                  final Map<String, Host> hostMap) {
        final NodeList hosts = clusterNode.getChildNodes();
        if (hosts != null) {
            for (int i = 0; i < hosts.getLength(); i++) {
                final Node hostNode = hosts.item(i);
                if (hostNode.getNodeName().equals(HOST_NODE_STRING)) {
                    final String nodeName = getText(hostNode);
                    final Host host = hostMap.get(nodeName);
                    if (host != null) {
                        host.setCluster(cluster);
                        cluster.addHost(host);
                    }
                }
            }
        }
    }
}
