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

import java.io.File;
import java.io.FileFilter;

/**
 *
 * XsdFileFilter is a file filter used to find XML Schema files (*.xsd). 
 *
 * @author Dan Jemiolo (danj)
 *
 */

public class XsdFileFilter implements FileFilter
{
    /**
     * 
     * @return True if the file is a "regular" file (not a directory) and 
     *         has a name that ends with <em>.xsd</em>. The comparison is 
     *         case-insensitive.
     *
     */
    public boolean accept(File file)
    {
        String name = file.getName().toLowerCase();
        return file.isFile() && name.endsWith(".xsd");
    }
}
