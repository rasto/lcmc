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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 * This class parses xml.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class XML {
    /** Returns child node of the node identified by the tag. */
    protected final Node getChildNode(final Node node, final String tag) {
        final NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node n = nodeList.item(i);
            if (n.getNodeName().equals(tag)) {
                return n;
            }
        }
        return null;
    }

    /** Returns attribute value for node and name of the attribute. */
    public final String getAttribute(final Node node, final String name) {
        if (node.getAttributes().getNamedItem(name) == null) {
            return null;
        } else {
            return node.getAttributes().getNamedItem(name).getNodeValue();
        }
    }

    /** Returns CDATA section. */
    public final String getCDATA(final Node node) {
        final Node n = getChildNode(node, "#cdata-section");
        return n.getNodeValue();
    }

    /** Returns text in the node. */
    public final String getText(final Node node) {
        final Node ch = getChildNode(node, "#text");
        if (ch == null) {
            return "";
        }
        return ch.getNodeValue();
    }

    /**
     * Parses xml passed as a string and returns document object with
     * the tree.
     */
    protected final Document getXMLDocument(final String xmlraw) {
        if (xmlraw == null) {
            return null;
        }
        final String xml = xmlraw.trim();
        final DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
        if (factory == null
            || xml.equals("")
            || xml.equals("no resources defined!")) {
            return null;
        }
        Document document;
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (SAXException sxe) {
            // Error generated during parsing)
            Exception  x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            Tools.appWarning("could not parse: " + xml);
            return null;
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
            return null;
        } catch (IOException ioe) {
            // I/O error
            ioe.printStackTrace();
            return null;
        }
        return document;
    }
}
