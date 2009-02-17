/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.muse.util.xml;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;

/**
 *
 * XPathUtils is a collection of utility methods related to XPath 1.0. It 
 * depends on Apache Xalan.
 *
 * @author Dan Jemiolo (danj)
 *
 */

public class XPathUtils
{
    /**
     * 
     * The XPath 1.0 namespace URI.
     * 
     */
    public static final String NAMESPACE_URI = 
        "http://www.w3.org/TR/1999/REC-xpath-19991116";
    
    /**
     * 
     * Evaluates the given XPath as a Boolean expression against the given XML.
     *
     * @param context
     *        The node from which to start all XPath evaluations. This 
     *        node becomes irrelevant if the expression is an absolute path.
     * 
     * @param xpath
     *        The XPath expression to evaluate.
     * 
     * @return True if the XPath evaluates to "true" or a collection of Nodes.
     * 
     * @throws TransformerException
     *         <ul>
     *         <li>If the XPath expression is invalid.</li>
     *         </ul>
     *
     */
    public static boolean isMatch(Node context, String xpath) 
        throws TransformerException
    {
        XObject result = XPathAPI.eval(context, xpath);
        return result.bool();
    }
    
    /**
     * 
     * Returns the first Node that matches the given XPath expression. The 
     * expression is evaluated in the context of the first parameter.
     *
     * @param context
     *        The node from which to start all XPath evaluations. This 
     *        node becomes irrelevant if the expression is an absolute path.
     * 
     * @param xpath
     *        The XPath expression to evaluate.
     * 
     * @return The set of Nodes that match the expression. If there are no 
     *         matches, the method returns null. If the expression evaluates 
     *         to a Boolean, string, or number, the Node is a DOM Text with 
     *         the appropriate value.
     * 
     * @throws TransformerException
     *         <ul>
     *         <li>If the XPath expression is invalid.</li>
     *         </ul>
     *
     */
    public static Node[] select(Node context, String xpath)
        throws TransformerException
    {
        XObject result = XPathAPI.eval(context, xpath, XmlUtils.getDocumentRoot(context));
        int type = result.getType();
        
        //
        // if the xpath resolves to a set of nodes, return them
        //
        if (type == XObject.CLASS_NODESET)
        {
            NodeList list = result.nodelist();
            return XmlUtils.convertToArray(list);
        }
        
        //
        // otherwise, it's a scalar - turn it into a text node
        //
        String resultValue = result.str();
        Node resultNode = XmlUtils.EMPTY_DOC.createTextNode(resultValue);
        return new Node[]{ resultNode };
    }
    
    /**
     * 
     * Returns a set of all Nodes that match the given XPath expression. The 
     * expression is evaluated in the context of the first parameter.
     *
     * @param context
     *        The node from which to start all XPath evaluations. This 
     *        node becomes irrelevant if the expression is an absolute path.
     * 
     * @param xpath
     *        The XPath expression to evaluate.
     * 
     * @return The set of Nodes that match the expression. If there are no 
     *         matches, the method returns null.
     * 
     * @throws TransformerException
     *         <ul>
     *         <li>If the XPath expression is invalid.</li>
     *         </ul>
     *
     */
    public static Node[] selectNodeList(Node context, String xpath)
        throws TransformerException
    {
        Element root = XmlUtils.getDocumentRoot(context);
        NodeList result = XPathAPI.selectNodeList(context, xpath, root);
        return XmlUtils.convertToArray(result);
    }
}
