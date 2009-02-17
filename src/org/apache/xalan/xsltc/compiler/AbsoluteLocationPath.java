/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
 * $Id: AbsoluteLocationPath.java,v 1.9 2005/06/30 21:04:16 zongaro Exp $
 */

package org.apache.xalan.xsltc.compiler;

import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.NEW;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.NodeType;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;
import org.apache.xalan.xsltc.compiler.util.Util;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class AbsoluteLocationPath extends Expression {
    private Expression _path;	// may be null 

    public AbsoluteLocationPath() {
	_path = null;
    }

    public AbsoluteLocationPath(Expression path) {
	_path = path;
	if (path != null) {
	    _path.setParent(this);
	}
    }

    public void setParser(Parser parser) {
	super.setParser(parser);
	if (_path != null) {
	    _path.setParser(parser);
	}
    }

    public Expression getPath() {
	return(_path);
    }
    
    public String toString() {
	return "AbsoluteLocationPath(" +
	    (_path != null ? _path.toString() : "null") + ')';
    }
	
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	if (_path != null) {
	    final Type ptype = _path.typeCheck(stable);
	    if (ptype instanceof NodeType) {		// promote to node-set
		_path = new CastExpr(_path, Type.NodeSet);
	    }
	}
	return _type = Type.NodeSet;	
    }
	
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	if (_path != null) {
	    final int initAI = cpg.addMethodref(ABSOLUTE_ITERATOR,
						"<init>",
						"("
						+ NODE_ITERATOR_SIG
						+ ")V");

	    // Compile relative path iterator(s)
            //
            // Backwards branches are prohibited if an uninitialized object is
            // on the stack by section 4.9.4 of the JVM Specification, 2nd Ed.
            // We don't know whether this code might contain backwards branches,
            // so we mustn't create the new object until after we've created
            // this argument to its constructor.  Instead we calculate the
            // value of the argument to the constructor first, store it in
            // a temporary variable, create the object and reload the argument
            // from the temporary to avoid the problem.
	    _path.translate(classGen, methodGen);
            LocalVariableGen relPathIterator
                    = methodGen.addLocalVariable("abs_location_path_tmp",
                                       Util.getJCRefType(NODE_ITERATOR_SIG),
                                       il.getEnd(), null);
            il.append(new ASTORE(relPathIterator.getIndex()));

	    // Create new AbsoluteIterator
	    il.append(new NEW(cpg.addClass(ABSOLUTE_ITERATOR)));
	    il.append(DUP);
            il.append(new ALOAD(relPathIterator.getIndex()));

	    // Initialize AbsoluteIterator with iterator from the stack
	    il.append(new INVOKESPECIAL(initAI));
	}
	else {
	    final int gitr = cpg.addInterfaceMethodref(DOM_INTF,
						       "getIterator",
						       "()"+NODE_ITERATOR_SIG);
	    il.append(methodGen.loadDOM());
	    il.append(new INVOKEINTERFACE(gitr, 1));
	}
    }
}
