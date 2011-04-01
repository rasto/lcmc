/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: TransformSnapshot.java,v 1.4 2004/02/16 20:41:30 minchau Exp $
 */
package org.apache.xalan.transformer;

/**
 * This is an opaque interface that allows the transformer to return a 
 * "snapshot" of it's current state, which can later be restored.
 * 
 * @deprecated It doesn't look like this code, which is for tooling, has
 * functioned propery for a while, so it doesn't look like it is being used.
 */
public interface TransformSnapshot
{

}
