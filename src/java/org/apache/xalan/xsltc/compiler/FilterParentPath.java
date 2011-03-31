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
 * $Id: FilterParentPath.java,v 1.14 2005/06/30 21:04:16 zongaro Exp $
 */

package org.apache.xalan.xsltc.compiler;

import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.NEW;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.NodeSetType;
import org.apache.xalan.xsltc.compiler.util.NodeType;
import org.apache.xalan.xsltc.compiler.util.ReferenceType;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;
import org.apache.xalan.xsltc.compiler.util.Util;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class FilterParentPath extends Expression {

    private Expression _filterExpr;
    private Expression _path;
    private boolean _hasDescendantAxis = false;

    public FilterParentPath(Expression filterExpr, Expression path) {
	(_path = path).setParent(this);
	(_filterExpr = filterExpr).setParent(this);
    }
		
    public void setParser(Parser parser) {
	super.setParser(parser);
	_filterExpr.setParser(parser);
	_path.setParser(parser);
    }
    
    public String toString() {
	return "FilterParentPath(" + _filterExpr + ", " + _path + ')';
    }

    public void setDescendantAxis() {
	_hasDescendantAxis = true;
    }

    /**
     * Type check a FilterParentPath. If the filter is not a node-set add a 
     * cast to node-set only if it is of reference type. This type coercion is
     * needed for expressions like $x/LINE where $x is a parameter reference.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	final Type ftype = _filterExpr.typeCheck(stable);
	if (ftype instanceof NodeSetType == false) {
	    if (ftype instanceof ReferenceType)  {
		_filterExpr = new CastExpr(_filterExpr, Type.NodeSet);
	    }
	    /*
	    else if (ftype instanceof ResultTreeType)  {
		_filterExpr = new CastExpr(_filterExpr, Type.NodeSet);
	    }
	    */
	    else if (ftype instanceof NodeType)  {
		_filterExpr = new CastExpr(_filterExpr, Type.NodeSet);
	    }
	    else {
		throw new TypeCheckError(this);
	    }
	}

	// Wrap single node path in a node set
	final Type ptype = _path.typeCheck(stable);
	if (!(ptype instanceof NodeSetType)) {
	    _path = new CastExpr(_path, Type.NodeSet);
	}

	return _type = Type.NodeSet;	
    }
	
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	// Create new StepIterator
	final int initSI = cpg.addMethodref(STEP_ITERATOR_CLASS,
					    "<init>",
					    "("
					    +NODE_ITERATOR_SIG
					    +NODE_ITERATOR_SIG
					    +")V");

        // Backwards branches are prohibited if an uninitialized object is
        // on the stack by section 4.9.4 of the JVM Specification, 2nd Ed.
        // We don't know whether this code might contain backwards branches,
        // so we mustn't create the new object until after we've created
        // the suspect arguments to its constructor.  Instead we calculate
        // the values of the arguments to the constructor first, store them
        // in temporary variables, create the object and reload the
        // arguments from the temporaries to avoid the problem.

	// Recursively compile 2 iterators
	_filterExpr.translate(classGen, methodGen);
        LocalVariableGen filterTemp =
                methodGen.addLocalVariable("filter_parent_path_tmp1",
                                           Util.getJCRefType(NODE_ITERATOR_SIG),
                                           il.getEnd(), null);
        il.append(new ASTORE(filterTemp.getIndex()));

	_path.translate(classGen, methodGen);
        LocalVariableGen pathTemp =
                methodGen.addLocalVariable("filter_parent_path_tmp2",
                                           Util.getJCRefType(NODE_ITERATOR_SIG),
                                           il.getEnd(), null);
        il.append(new ASTORE(pathTemp.getIndex()));

	il.append(new NEW(cpg.addClass(STEP_ITERATOR_CLASS)));
	il.append(DUP);
        il.append(new ALOAD(filterTemp.getIndex()));
        il.append(new ALOAD(pathTemp.getIndex()));

	// Initialize StepIterator with iterators from the stack
	il.append(new INVOKESPECIAL(initSI));

	// This is a special case for the //* path with or without predicates
        if (_hasDescendantAxis) {
	    final int incl = cpg.addMethodref(NODE_ITERATOR_BASE,
					      "includeSelf",
					      "()" + NODE_ITERATOR_SIG);
	    il.append(new INVOKEVIRTUAL(incl));
	}

	if (!(getParent() instanceof RelativeLocationPath) &&
	    !(getParent() instanceof FilterParentPath)) {
	    final int order = cpg.addInterfaceMethodref(DOM_INTF,
							ORDER_ITERATOR,
							ORDER_ITERATOR_SIG);
	    il.append(methodGen.loadDOM());
	    il.append(SWAP);
	    il.append(methodGen.loadContextNode());
	    il.append(new INVOKEINTERFACE(order, 3));
	}
    }
}
