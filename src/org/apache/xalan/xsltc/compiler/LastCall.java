/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
 * $Id: LastCall.java,v 1.11 2004/02/16 22:24:28 minchau Exp $
 */

package org.apache.xalan.xsltc.compiler;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.InstructionList;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.CompareGenerator;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.TestGenerator;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class LastCall extends FunctionCall {

    public LastCall(QName fname) {
	super(fname);
    }

    public boolean hasPositionCall() {
	return true;
    }

    public boolean hasLastCall() {
	return true;
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final InstructionList il = methodGen.getInstructionList();

	if (methodGen instanceof CompareGenerator) {
	    il.append(((CompareGenerator)methodGen).loadLastNode());
	}
	else if (methodGen instanceof TestGenerator) {
	    il.append(new ILOAD(LAST_INDEX));
	}
	else {
	    final ConstantPoolGen cpg = classGen.getConstantPool();
	    final int getLast = cpg.addInterfaceMethodref(NODE_ITERATOR,
							  "getLast", 
							  "()I");
	    il.append(methodGen.loadIterator());
	    il.append(new INVOKEINTERFACE(getLast, 1));
	}
    }
}
