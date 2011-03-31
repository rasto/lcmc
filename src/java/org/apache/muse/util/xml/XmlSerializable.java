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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * XmlSerializable is an interface for types that can be converted into an 
 * XML representation. 
 *
 * @author Dan Jemiolo (danj)
 *
 */

public interface XmlSerializable
{
    /**
     * 
     * Converts this object into an XML representation, as defined by its 
     * related schema or specification. The format of the XML is dependent 
     * on the concrete type.
     *
     * @return An XML representation of this object.
     *
     */
    Element toXML();
    
    /**
     * 
     * Converts this object into an XML representation, as defined by its 
     * related schema or specification. The format of the XML is dependent 
     * on the concrete type.
     *
     * @param factory
     *        The DOM Document that will be used to create all of the nodes 
     *        in the resulting XML fragment.
     * 
     * @return An XML representation of this object.
     *
     */
    Element toXML(Document factory);
}
