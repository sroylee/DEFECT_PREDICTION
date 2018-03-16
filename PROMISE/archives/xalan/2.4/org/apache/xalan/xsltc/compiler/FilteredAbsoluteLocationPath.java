package org.apache.xalan.xsltc.compiler;

import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.bcel.generic.*;
import org.apache.xalan.xsltc.compiler.util.*;

final class FilteredAbsoluteLocationPath extends Expression {

    public FilteredAbsoluteLocationPath() {
	_path = null;
    }

    public FilteredAbsoluteLocationPath(Expression path) {
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
	return "FilteredAbsoluteLocationPath(" +
	    (_path != null ? _path.toString() : "null") + ')';
    }
	
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	if (_path != null) {
	    final Type ptype = _path.typeCheck(stable);
		_path = new CastExpr(_path, Type.NodeSet);
	    }
	}
	return _type = Type.NodeSet;	
    }
	
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	if (_path != null) {
	    final int initDFI = cpg.addMethodref(DUP_FILTERED_ITERATOR,
						"<init>",
						"("
						+ NODE_ITERATOR_SIG
						+ ")V");
	    il.append(new NEW(cpg.addClass(DUP_FILTERED_ITERATOR)));
	    il.append(DUP);

	    _path.translate(classGen, methodGen);

	    il.append(new INVOKESPECIAL(initDFI));
	}
	else {
	    final int git = cpg.addInterfaceMethodref(DOM_INTF,
						      "getIterator",
						      "()"+NODE_ITERATOR_SIG);
	    il.append(methodGen.loadDOM());
	    il.append(new INVOKEINTERFACE(git, 1));
	}
    }
}