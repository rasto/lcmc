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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class parses xml.
 */
public class XMLTools {
    private static final Logger LOG = LoggerFactory.getLogger(XMLTools.class);

    public static Node getChildNode(final Node node, final String tag) {
        final NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node n = nodeList.item(i);
            if (n.getNodeName().equals(tag)) {
                return n;
            }
        }
        return null;
    }

    public static Node getChildNode(final Node node, final String tag, final int pos) {
        final NodeList nodeList = node.getChildNodes();
        int foundPos = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node n = nodeList.item(i);
            if (n.getNodeName().equals(tag)) {
                if (pos == foundPos) {
                    return n;
                }
                foundPos++;
            }
        }
        return null;
    }

    public static String getAttribute(final Node node, final String name) {
        if (node.getAttributes().getNamedItem(name) == null) {
            return null;
        } else {
            return node.getAttributes().getNamedItem(name).getNodeValue();
        }
    }

    public static String getText(final Node node) {
        final Node ch = getChildNode(node, "#text");
        if (ch == null) {
            return "";
        }
        return ch.getNodeValue();
    }

    public static Document getXMLDocument(final String xmlraw) {
        if (xmlraw == null) {
            return null;
        }
        final String xml = xmlraw.trim();
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        if (factory == null || xml.isEmpty() || "no resources defined!".equals(xml)) {
            return null;
        }
        final Document document;
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        } catch (final SAXException sxe) {
            LOG.appError("getXMLDocument: could not parse: " + xml, sxe);
            return null;
        } catch (final ParserConfigurationException pce) {
            throw new RuntimeException("getXMLException: parser configuration", pce);
        } catch (final IOException ioe) {
            throw new RuntimeException("getXMLException: io error", ioe);
        }
        return document;
    }
}
