package org.apache.xalan.xsltc.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.PUSH;
import org.apache.xalan.xsltc.compiler.util.BooleanType;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.ErrorMsg;
import org.apache.xalan.xsltc.compiler.util.IntType;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.MethodType;
import org.apache.xalan.xsltc.compiler.util.MultiHashtable;
import org.apache.xalan.xsltc.compiler.util.ObjectType;
import org.apache.xalan.xsltc.compiler.util.ReferenceType;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @author Erwin Bolwidt <ejb@klomp.org>
 * @author Todd Miller
 */
class FunctionCall extends Expression {

    private QName  _fname;
    private final Vector _arguments;
    private final static Vector EMPTY_ARG_LIST = new Vector(0);

    protected final static String EXT_XSLTC = 
	TRANSLET_URI;

    protected final static String JAVA_EXT_XSLTC = 
	EXT_XSLTC + "/java";

    protected final static String EXT_XALAN =

    protected final static String JAVA_EXT_XALAN =

    protected final static String JAVA_EXT_XALAN_OLD =
	
    protected final static String EXSLT_COMMON =

    protected final static String EXSLT_MATH =
	
    protected final static String EXSLT_SETS =

    protected final static String EXSLT_DATETIME =

    protected final static String EXSLT_STRINGS =
	
    protected final static int NAMESPACE_FORMAT_JAVA = 0;
    protected final static int NAMESPACE_FORMAT_CLASS = 1;
    protected final static int NAMESPACE_FORMAT_PACKAGE = 2;
    protected final static int NAMESPACE_FORMAT_CLASS_OR_PACKAGE = 3;
	
    private int _namespace_format = NAMESPACE_FORMAT_JAVA;
        
    /**
     * Stores reference to object for non-static Java calls
     */
    Expression _thisArgument = null;

    private String      _className;
    private Class       _clazz;
    private Method      _chosenMethod;
    private Constructor _chosenConstructor;
    private MethodType  _chosenMethodType;

    private boolean    unresolvedExternal;

    private boolean     _isExtConstructor = false; 

    private boolean 	  _isStatic = false;

    private static final MultiHashtable _internal2Java = new MultiHashtable();

    private static final Hashtable _java2Internal = new Hashtable();
    
    private static final Hashtable _extensionNamespaceTable = new Hashtable();

    private static final Hashtable _extensionFunctionTable = new Hashtable();
    /**
     * inner class to used in internal2Java mappings, contains
     * the Java type and the distance between the internal type and
     * the Java type. 
     */
    static class JavaType {
	public Class  type;
	public int distance;
	
	public JavaType(Class type, int distance){
	    this.type = type;
	    this.distance = distance;
	}
	public boolean equals(Object query){
	    return query.equals(type);
	}
    } 

    /**
     * Defines 2 conversion tables:
     * 1. From internal types to Java types and
     * 2. From Java types to internal types.
     * These two tables are used when calling external (Java) functions.
     */
    static {
	try {
	    final Class nodeClass     = Class.forName("org.w3c.dom.Node");
	    final Class nodeListClass = Class.forName("org.w3c.dom.NodeList");

            
	    _internal2Java.put(Type.Boolean, new JavaType(Boolean.TYPE, 0));
	    _internal2Java.put(Type.Boolean, new JavaType(Boolean.class, 1));
	    _internal2Java.put(Type.Boolean, new JavaType(Object.class, 2));

	    _internal2Java.put(Type.Real, new JavaType(Double.TYPE, 0));
	    _internal2Java.put(Type.Real, new JavaType(Double.class, 1));
	    _internal2Java.put(Type.Real, new JavaType(Float.TYPE, 2));
	    _internal2Java.put(Type.Real, new JavaType(Long.TYPE, 3));
	    _internal2Java.put(Type.Real, new JavaType(Integer.TYPE, 4));
	    _internal2Java.put(Type.Real, new JavaType(Short.TYPE, 5));
	    _internal2Java.put(Type.Real, new JavaType(Byte.TYPE, 6));
	    _internal2Java.put(Type.Real, new JavaType(Character.TYPE, 7)); 
	    _internal2Java.put(Type.Real, new JavaType(Object.class, 8));
            
	    _internal2Java.put(Type.Int, new JavaType(Double.TYPE, 0));
	    _internal2Java.put(Type.Int, new JavaType(Double.class, 1));
	    _internal2Java.put(Type.Int, new JavaType(Float.TYPE, 2));
	    _internal2Java.put(Type.Int, new JavaType(Long.TYPE, 3));
	    _internal2Java.put(Type.Int, new JavaType(Integer.TYPE, 4));
	    _internal2Java.put(Type.Int, new JavaType(Short.TYPE, 5));
	    _internal2Java.put(Type.Int, new JavaType(Byte.TYPE, 6));
	    _internal2Java.put(Type.Int, new JavaType(Character.TYPE, 7)); 
	    _internal2Java.put(Type.Int, new JavaType(Object.class, 8));
            
	    _internal2Java.put(Type.String, new JavaType(String.class, 0)); 
	    _internal2Java.put(Type.String, new JavaType(Object.class, 1));

	    _internal2Java.put(Type.NodeSet, new JavaType(nodeListClass, 0)); 
	    _internal2Java.put(Type.NodeSet, new JavaType(nodeClass, 1)); 
	    _internal2Java.put(Type.NodeSet, new JavaType(Object.class, 2));
	    _internal2Java.put(Type.NodeSet, new JavaType(String.class, 3)); 

	    _internal2Java.put(Type.Node, new JavaType(nodeListClass, 0));
	    _internal2Java.put(Type.Node, new JavaType(nodeClass, 1));  
	    _internal2Java.put(Type.Node, new JavaType(Object.class, 2));
	    _internal2Java.put(Type.Node, new JavaType(String.class, 3));

	    _internal2Java.put(Type.ResultTree, new JavaType(nodeListClass, 0));
	    _internal2Java.put(Type.ResultTree, new JavaType(nodeClass, 1)); 
	    _internal2Java.put(Type.ResultTree, new JavaType(Object.class, 2));
	    _internal2Java.put(Type.ResultTree, new JavaType(String.class, 3));

	    _internal2Java.put(Type.Reference, new JavaType(Object.class, 0));

	    _java2Internal.put(Boolean.TYPE, Type.Boolean); 
	    _java2Internal.put(Void.TYPE, Type.Void);
	    _java2Internal.put(Character.TYPE, Type.Real); 
	    _java2Internal.put(Byte.TYPE, Type.Real);
	    _java2Internal.put(Short.TYPE, Type.Real);
	    _java2Internal.put(Integer.TYPE, Type.Real);
	    _java2Internal.put(Long.TYPE, Type.Real);
	    _java2Internal.put(Float.TYPE, Type.Real);
	    _java2Internal.put(Double.TYPE, Type.Real);

	    _java2Internal.put(String.class, Type.String);

	    _java2Internal.put(Object.class, Type.Reference);

	    _java2Internal.put(nodeListClass, Type.NodeSet);
	    _java2Internal.put(nodeClass, Type.NodeSet);
	    
	    _extensionNamespaceTable.put(EXT_XALAN, "org.apache.xalan.lib.Extensions");
	    _extensionNamespaceTable.put(EXSLT_COMMON, "org.apache.xalan.lib.ExsltCommon");
	    _extensionNamespaceTable.put(EXSLT_MATH, "org.apache.xalan.lib.ExsltMath");
	    _extensionNamespaceTable.put(EXSLT_SETS, "org.apache.xalan.lib.ExsltSets");
	    _extensionNamespaceTable.put(EXSLT_DATETIME, "org.apache.xalan.lib.ExsltDatetime");
	    _extensionNamespaceTable.put(EXSLT_STRINGS, "org.apache.xalan.lib.ExsltStrings");
	    
	    _extensionFunctionTable.put(EXSLT_COMMON + ":nodeSet", "nodeset");
	    _extensionFunctionTable.put(EXSLT_COMMON + ":objectType", "objectType");	    
	    _extensionFunctionTable.put(EXT_XALAN + ":nodeset", "nodeset");
	}
	catch (ClassNotFoundException e) {
	    System.err.println(e);
	}
    }
		
    public FunctionCall(QName fname, Vector arguments) {
	_fname = fname;
	_arguments = arguments;
	_type = null;
    }

    public FunctionCall(QName fname) {
	this(fname, EMPTY_ARG_LIST);
    }

    public String getName() {
	return(_fname.toString());
    }

    public void setParser(Parser parser) {
	super.setParser(parser);
	if (_arguments != null) {
	    final int n = _arguments.size();
	    for (int i = 0; i < n; i++) {
		final Expression exp = (Expression)_arguments.elementAt(i);
		exp.setParser(parser);
		exp.setParent(this);
	    }
	}
    }

    public String getClassNameFromUri(String uri) 
    {   
        String className = (String)_extensionNamespaceTable.get(uri);
    
        if (className != null)
            return className;
        else {
            if (uri.startsWith(JAVA_EXT_XSLTC)) {
      	    	int length = JAVA_EXT_XSLTC.length() + 1;
            	return (uri.length() > length) ? uri.substring(length) : EMPTYSTRING;
            }
            else if (uri.startsWith(JAVA_EXT_XALAN)) {
      	    	int length = JAVA_EXT_XALAN.length() + 1;
            	return (uri.length() > length) ? uri.substring(length) : EMPTYSTRING;
            }
            else if (uri.startsWith(JAVA_EXT_XALAN_OLD)) {
      	    	int length = JAVA_EXT_XALAN_OLD.length() + 1;
            	return (uri.length() > length) ? uri.substring(length) : EMPTYSTRING;
            }
            else {
      	    	int index = uri.lastIndexOf('/');
      	    	return (index > 0) ? uri.substring(index+1) : uri;
            }      
        }
    }

    /**
     * Type check a function call. Since different type conversions apply,
     * type checking is different for standard and external (Java) functions.
     */
    public Type typeCheck(SymbolTable stable) 
	throws TypeCheckError 
    {
        if (_type != null) return _type;

	final String namespace = _fname.getNamespace();
	String local = _fname.getLocalPart();

	if (isExtension()) {
	    _fname = new QName(null, null, local);
	    return typeCheckStandard(stable);
	}
	else if (isStandard()) {
	    return typeCheckStandard(stable);
	}
	else {
	    try {
	    	_className = getClassNameFromUri(namespace);
		  
                final int pos = local.lastIndexOf('.');
		if (pos > 0) {
		    _isStatic = true;
		    if (_className != null && _className.length() > 0) {
		    	_namespace_format = NAMESPACE_FORMAT_PACKAGE;
		     	_className = _className + "." + local.substring(0, pos);
		    }
		    else {
		     	_namespace_format = NAMESPACE_FORMAT_JAVA;
		     	_className = local.substring(0, pos);
		    }
			  
		    _fname = new QName(namespace, null, local.substring(pos + 1));
		}
		else {
		    if (_className != null && _className.length() > 0) {
		    	try {
                            _clazz = ObjectFactory.findProviderClass(
                                _className, ObjectFactory.findClassLoader(), true);
		            _namespace_format = NAMESPACE_FORMAT_CLASS;
		    	}
		    	catch (ClassNotFoundException e) {
		      	    _namespace_format = NAMESPACE_FORMAT_PACKAGE;	
		        }
		    }
		    else
	            	_namespace_format = NAMESPACE_FORMAT_JAVA;
			
		    if (local.indexOf('-') > 0) {
		        local = replaceDash(local);
		    }
		    
		    String extFunction = (String)_extensionFunctionTable.get(namespace + ":" + local);
		    if (extFunction != null) {
		      	_fname = new QName(null, null, extFunction);
		      	return typeCheckStandard(stable);
		    }
		    else
		      	_fname = new QName(namespace, null, local);
		}
		  
		return typeCheckExternal(stable);
	    } 
	    catch (TypeCheckError e) {
		ErrorMsg errorMsg = e.getErrorMsg();
		if (errorMsg == null) {
		    final String name = _fname.getLocalPart();
		    errorMsg = new ErrorMsg(ErrorMsg.METHOD_NOT_FOUND_ERR, name);
		}
		getParser().reportError(ERROR, errorMsg);
		return _type = Type.Void;
	    }
	  }
    }

    /**
     * Type check a call to a standard function. Insert CastExprs when needed.
     * If as a result of the insertion of a CastExpr a type check error is 
     * thrown, then catch it and re-throw it with a new "this".
     */
    public Type typeCheckStandard(SymbolTable stable) throws TypeCheckError {

	final int n = _arguments.size();
	final Vector argsType = typeCheckArgs(stable);
	final MethodType args = new MethodType(Type.Void, argsType);
	final MethodType ptype =
	    lookupPrimop(stable, _fname.getLocalPart(), args);

	if (ptype != null) {
	    for (int i = 0; i < n; i++) {
		final Type argType = (Type) ptype.argsType().elementAt(i);
		final Expression exp = (Expression)_arguments.elementAt(i);
		if (!argType.identicalTo(exp.getType())) {
		    try {
			_arguments.setElementAt(new CastExpr(exp, argType), i);
		    }
		    catch (TypeCheckError e) {
		    }
		}
	    }
	    _chosenMethodType = ptype;
	    return _type = ptype.resultType();
	}
	throw new TypeCheckError(this);
    }

   

    public Type typeCheckConstructor(SymbolTable stable) throws TypeCheckError{
        final Vector constructors = findConstructors();
	if (constructors == null) {
            throw new TypeCheckError(ErrorMsg.CONSTRUCTOR_NOT_FOUND, 
		_className);
        
	}

	final int nConstructors = constructors.size();
	final int nArgs = _arguments.size();
	final Vector argsType = typeCheckArgs(stable);

	int bestConstrDistance = Integer.MAX_VALUE;
	for (int j, i = 0; i < nConstructors; i++) {
	    final Constructor constructor = 
		(Constructor)constructors.elementAt(i);
	    final Class[] paramTypes = constructor.getParameterTypes();

	    Class extType = null;
	    int currConstrDistance = 0;
	    for (j = 0; j < nArgs; j++) {
		extType = paramTypes[j];
		final Type intType = (Type)argsType.elementAt(j);
		Object match = _internal2Java.maps(intType, extType);
		if (match != null) {
		    currConstrDistance += ((JavaType)match).distance;
		}
		else if (intType instanceof ObjectType) {
		    ObjectType objectType = (ObjectType)intType;
		    if (objectType.getJavaClass() == extType)
		        continue;
		    else if (extType.isAssignableFrom(objectType.getJavaClass()))
		        currConstrDistance += 1;
		    else {
			currConstrDistance = Integer.MAX_VALUE;
			break;
		    }
		}
		else {
		    currConstrDistance = Integer.MAX_VALUE;
		    break;
		} 
	    }

	    if (j == nArgs && currConstrDistance < bestConstrDistance ) {
	        _chosenConstructor = constructor;
	        _isExtConstructor = true;
		bestConstrDistance = currConstrDistance;
		
                _type = (_clazz != null) ? Type.newObjectType(_clazz)
                    : Type.newObjectType(_className);
	    }
	}

	if (_type != null) {
	    return _type;
	}

	throw new TypeCheckError(ErrorMsg.ARGUMENT_CONVERSION_ERR, getMethodSignature(argsType));
    }


    /**
     * Type check a call to an external (Java) method.
     * The method must be static an public, and a legal type conversion
     * must exist for all its arguments and its return type.
     * Every method of name <code>_fname</code> is inspected
     * as a possible candidate.
     */
    public Type typeCheckExternal(SymbolTable stable) throws TypeCheckError {
	int nArgs = _arguments.size();
	final String name = _fname.getLocalPart();
    
	if (_fname.getLocalPart().equals("new")) {
	    return typeCheckConstructor(stable);
	}
	else {
	    boolean hasThisArgument = false;
	  
	    if (nArgs == 0)
	        _isStatic = true;
	  
	    if (!_isStatic) {
	        if (_namespace_format == NAMESPACE_FORMAT_JAVA
	  	    || _namespace_format == NAMESPACE_FORMAT_PACKAGE)
	   	    hasThisArgument = true;
	  	  
	  	Expression firstArg = (Expression)_arguments.elementAt(0);
	  	Type firstArgType = (Type)firstArg.typeCheck(stable);
	  	
	  	if (_namespace_format == NAMESPACE_FORMAT_CLASS
	  	    && firstArgType instanceof ObjectType
	  	    && _clazz != null
	  	    && _clazz.isAssignableFrom(((ObjectType)firstArgType).getJavaClass()))
	  	    hasThisArgument = true;
	  	
	  	if (hasThisArgument) {
	  	    _thisArgument = (Expression) _arguments.elementAt(0);
	  	    _arguments.remove(0); nArgs--;
		    if (firstArgType instanceof ObjectType) {
		    	_className = ((ObjectType) firstArgType).getJavaClassName();
		    }
		    else
		    	throw new TypeCheckError(ErrorMsg.NO_JAVA_FUNCT_THIS_REF, name);  	  	
	  	}
	    }
	    else if (_className.length() == 0) {
		/*
		 * Warn user if external function could not be resolved.
		 * Warning will _NOT_ be issued is the call is properly
		 * wrapped in an <xsl:if> or <xsl:when> element. For details
		 * see If.parserContents() and When.parserContents()
		 */
		final Parser parser = getParser();
		if (parser != null) {
		    reportWarning(this, parser, ErrorMsg.FUNCTION_RESOLVE_ERR,
				  _fname.toString());
		}
		unresolvedExternal = true;
	    }
	}
	
	final Vector methods = findMethods();
	
	if (methods == null) {
	    throw new TypeCheckError(ErrorMsg.METHOD_NOT_FOUND_ERR, _className + "." + name);
	}

	Class extType = null;
	final int nMethods = methods.size();
	final Vector argsType = typeCheckArgs(stable);

	int bestMethodDistance  = Integer.MAX_VALUE;
	for (int j, i = 0; i < nMethods; i++) {
	    final Method method = (Method)methods.elementAt(i);
	    final Class[] paramTypes = method.getParameterTypes();
	    
	    int currMethodDistance = 0;
	    for (j = 0; j < nArgs; j++) {
		extType = paramTypes[j];
		final Type intType = (Type)argsType.elementAt(j);
		Object match = _internal2Java.maps(intType, extType);
		if (match != null) {
		    currMethodDistance += ((JavaType)match).distance; 
		}
		else {
		    if (intType instanceof ReferenceType) {
		       currMethodDistance += 1; 
		    }
		    else if (intType instanceof ObjectType) {
		        ObjectType object = (ObjectType)intType;
		        if (extType.getName().equals(object.getJavaClassName()))
		            currMethodDistance += 0;
		      	else if (extType.isAssignableFrom(object.getJavaClass()))
		            currMethodDistance += 1;
		      	else {
		      	    currMethodDistance = Integer.MAX_VALUE;
		      	    break;
		        }
		    }
		    else {
		        currMethodDistance = Integer.MAX_VALUE;
		        break;
		    }
		}
	    }

	    if (j == nArgs) {
		  extType = method.getReturnType();
		
		  _type = (Type) _java2Internal.get(extType);
		  if (_type == null) {
		      _type = Type.newObjectType(extType);
		  }		

		  if (_type != null && currMethodDistance < bestMethodDistance) {
		      _chosenMethod = method;
		      bestMethodDistance = currMethodDistance;
		  }
	    }
	}
	
	if (_chosenMethod != null && _thisArgument == null &&
	    !Modifier.isStatic(_chosenMethod.getModifiers())) {
	    throw new TypeCheckError(ErrorMsg.NO_JAVA_FUNCT_THIS_REF, getMethodSignature(argsType));
	}

	if (_type != null) {
	    if (_type == Type.NodeSet) {
            	getXSLTC().setMultiDocument(true);
            }
	    return _type;
	}

	throw new TypeCheckError(ErrorMsg.ARGUMENT_CONVERSION_ERR, getMethodSignature(argsType));
    }

    /**
     * Type check the actual arguments of this function call.
     */
    public Vector typeCheckArgs(SymbolTable stable) throws TypeCheckError {
	final Vector result = new Vector();
	final Enumeration e = _arguments.elements();	
	while (e.hasMoreElements()) {
	    final Expression exp = (Expression)e.nextElement();
	    result.addElement(exp.typeCheck(stable));
	}
	return result;
    }

    protected final Expression argument(int i) {
	return (Expression)_arguments.elementAt(i);
    }

    protected final Expression argument() {
	return argument(0);
    }
    
    protected final int argumentCount() {
	return _arguments.size();
    }

    protected final void setArgument(int i, Expression exp) {
	_arguments.setElementAt(exp, i);
    }

    /**
     * Compile the function call and treat as an expression
     * Update true/false-lists.
     */
    public void translateDesynthesized(ClassGenerator classGen,
				       MethodGenerator methodGen) 
    {
	Type type = Type.Boolean;
	if (_chosenMethodType != null)
	    type = _chosenMethodType.resultType();

	final InstructionList il = methodGen.getInstructionList();
	translate(classGen, methodGen);

	if ((type instanceof BooleanType) || (type instanceof IntType)) {
	    _falseList.add(il.append(new IFEQ(null)));
	}
    }


    /**
     * Translate a function call. The compiled code will leave the function's
     * return value on the JVM's stack.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final int n = argumentCount();
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	final boolean isSecureProcessing = classGen.getParser().getXSLTC().isSecureProcessing();
	int index;

	if (isStandard() || isExtension()) {
	    for (int i = 0; i < n; i++) {
		final Expression exp = argument(i);
		exp.translate(classGen, methodGen);
		exp.startIterator(classGen, methodGen);
	    }

	    final String name = _fname.toString().replace('-', '_') + "F";
	    String args = Constants.EMPTYSTRING;

	    if (name.equals("sumF")) {
		args = DOM_INTF_SIG;
		il.append(methodGen.loadDOM());
	    }
	    else if (name.equals("normalize_spaceF")) {
		if (_chosenMethodType.toSignature(args).
		    equals("()Ljava/lang/String;")) {
		    args = "I"+DOM_INTF_SIG;
		    il.append(methodGen.loadContextNode());
		    il.append(methodGen.loadDOM());
		}
	    }

	    index = cpg.addMethodref(BASIS_LIBRARY_CLASS, name,
				     _chosenMethodType.toSignature(args));
	    il.append(new INVOKESTATIC(index));
	}
	else if (unresolvedExternal) {
	    index = cpg.addMethodref(BASIS_LIBRARY_CLASS,
				     "unresolved_externalF",
				     "(Ljava/lang/String;)V");
	    il.append(new PUSH(cpg, _fname.toString()));
	    il.append(new INVOKESTATIC(index));
	}
	else if (_isExtConstructor) {
	    if (isSecureProcessing)
	        translateUnallowedExtension(cpg, il);
	    
	    final String clazz = 
		_chosenConstructor.getDeclaringClass().getName();
	    Class[] paramTypes = _chosenConstructor.getParameterTypes();
            LocalVariableGen[] paramTemp = new LocalVariableGen[n];


	    for (int i = 0; i < n; i++) {
		final Expression exp = argument(i);
                Type expType = exp.getType();
		exp.translate(classGen, methodGen);
		exp.startIterator(classGen, methodGen);
		expType.translateTo(classGen, methodGen, paramTypes[i]);
                paramTemp[i] =
                    methodGen.addLocalVariable("function_call_tmp"+i,
                                               expType.toJCType(),
                                               il.getEnd(), null);
                il.append(expType.STORE(paramTemp[i].getIndex()));
	    }

	    il.append(new NEW(cpg.addClass(_className)));
	    il.append(InstructionConstants.DUP);

            for (int i = 0; i < n; i++) {
                final Expression arg = argument(i);
                il.append(arg.getType().LOAD(paramTemp[i].getIndex()));
            }

	    final StringBuffer buffer = new StringBuffer();
	    buffer.append('(');
	    for (int i = 0; i < paramTypes.length; i++) {
		buffer.append(getSignature(paramTypes[i]));
	    }
	    buffer.append(')');
	    buffer.append("V");

	    index = cpg.addMethodref(clazz,
				     "<init>", 
				     buffer.toString());
	    il.append(new INVOKESPECIAL(index));

	    (Type.Object).translateFrom(classGen, methodGen, 
				_chosenConstructor.getDeclaringClass());
	    
	}
	else {
	    if (isSecureProcessing)
	        translateUnallowedExtension(cpg, il);
	    
	    final String clazz = _chosenMethod.getDeclaringClass().getName();
	    Class[] paramTypes = _chosenMethod.getParameterTypes();

	    if (_thisArgument != null) {
		_thisArgument.translate(classGen, methodGen);
	    }	    

	    for (int i = 0; i < n; i++) {
		final Expression exp = argument(i);
		exp.translate(classGen, methodGen);
		exp.startIterator(classGen, methodGen);
		exp.getType().translateTo(classGen, methodGen, paramTypes[i]);
	    }

	    final StringBuffer buffer = new StringBuffer();
	    buffer.append('(');
	    for (int i = 0; i < paramTypes.length; i++) {
		buffer.append(getSignature(paramTypes[i]));
	    }
	    buffer.append(')');
	    buffer.append(getSignature(_chosenMethod.getReturnType()));

	    if (_thisArgument != null && _clazz.isInterface()) {
	        index = cpg.addInterfaceMethodref(clazz,
				     _fname.getLocalPart(),
				     buffer.toString());
		il.append(new INVOKEINTERFACE(index, n+1));
            }
            else {
	        index = cpg.addMethodref(clazz,
				     _fname.getLocalPart(),
				     buffer.toString());
	        il.append(_thisArgument != null ? (InvokeInstruction) new INVOKEVIRTUAL(index) :
	    		  (InvokeInstruction) new INVOKESTATIC(index));
            }
 
	    _type.translateFrom(classGen, methodGen,
				_chosenMethod.getReturnType());
	}
    }

    public String toString() {
	return "funcall(" + _fname + ", " + _arguments + ')';
    }

    public boolean isStandard() {
	final String namespace = _fname.getNamespace();
	return (namespace == null) || (namespace.equals(Constants.EMPTYSTRING));
    }

    public boolean isExtension() {
	final String namespace = _fname.getNamespace();
	return (namespace != null) && (namespace.equals(EXT_XSLTC));
    }

    /**
     * Returns a vector with all methods named <code>_fname</code>
     * after stripping its namespace or <code>null</code>
     * if no such methods exist.
     */
    private Vector findMethods() {
	  
	  Vector result = null;
	  final String namespace = _fname.getNamespace();

	  if (_className != null && _className.length() > 0) {
	    final int nArgs = _arguments.size();
	    try {
	      if (_clazz == null) {
                _clazz = ObjectFactory.findProviderClass(
                  _className, ObjectFactory.findClassLoader(), true);

		if (_clazz == null) {
		  final ErrorMsg msg =
		        new ErrorMsg(ErrorMsg.CLASS_NOT_FOUND_ERR, _className);
		  getParser().reportError(Constants.ERROR, msg);
		}
	      }

	      final String methodName = _fname.getLocalPart();
	      final Method[] methods = _clazz.getMethods();

	      for (int i = 0; i < methods.length; i++) {
		final int mods = methods[i].getModifiers();
		if (Modifier.isPublic(mods)
		    && methods[i].getName().equals(methodName)
		    && methods[i].getParameterTypes().length == nArgs)
		{
		  if (result == null) {
		    result = new Vector();
	          }
		  result.addElement(methods[i]);
		}
	      }
	    }
	    catch (ClassNotFoundException e) {
		  final ErrorMsg msg = new ErrorMsg(ErrorMsg.CLASS_NOT_FOUND_ERR, _className);
		  getParser().reportError(Constants.ERROR, msg);
	    }
	  }
	  return result;
    }

    /**
     * Returns a vector with all constructors named <code>_fname</code>
     * after stripping its namespace or <code>null</code>
     * if no such methods exist.
     */
    private Vector findConstructors() {
        Vector result = null;
        final String namespace = _fname.getNamespace();

        final int nArgs = _arguments.size();
        try {
          if (_clazz == null) {
            _clazz = ObjectFactory.findProviderClass(
              _className, ObjectFactory.findClassLoader(), true);

            if (_clazz == null) {
              final ErrorMsg msg = new ErrorMsg(ErrorMsg.CLASS_NOT_FOUND_ERR, _className);
              getParser().reportError(Constants.ERROR, msg);
            }          
          }

          final Constructor[] constructors = _clazz.getConstructors();

          for (int i = 0; i < constructors.length; i++) {
              final int mods = constructors[i].getModifiers();
              if (Modifier.isPublic(mods) &&
                  constructors[i].getParameterTypes().length == nArgs)
              {
                if (result == null) {
                  result = new Vector();
                }
                result.addElement(constructors[i]);
              }
          }
        }
        catch (ClassNotFoundException e) {
          final ErrorMsg msg = new ErrorMsg(ErrorMsg.CLASS_NOT_FOUND_ERR, _className);
          getParser().reportError(Constants.ERROR, msg);
        }
            
        return result;
    }


    /**
     * Compute the JVM signature for the class.
     */
    static final String getSignature(Class clazz) {
	if (clazz.isArray()) {
	    final StringBuffer sb = new StringBuffer();
	    Class cl = clazz;
	    while (cl.isArray()) {
		sb.append("[");
		cl = cl.getComponentType();
	    }
	    sb.append(getSignature(cl));
	    return sb.toString();
	}
	else if (clazz.isPrimitive()) {
	    if (clazz == Integer.TYPE) {
		return "I";
	    }
	    else if (clazz == Byte.TYPE) {
		return "B";
	    }
	    else if (clazz == Long.TYPE) {
		return "J";
	    }
	    else if (clazz == Float.TYPE) {
		return "F";
	    }
	    else if (clazz == Double.TYPE) {
		return "D";
	    }
	    else if (clazz == Short.TYPE) {
		return "S";
	    }
	    else if (clazz == Character.TYPE) {
		return "C";
	    }
	    else if (clazz == Boolean.TYPE) {
		return "Z";
	    }
	    else if (clazz == Void.TYPE) {
		return "V";
	    }
	    else {
		final String name = clazz.toString();
		ErrorMsg err = new ErrorMsg(ErrorMsg.UNKNOWN_SIG_TYPE_ERR,name);
		throw new Error(err.toString());
	    }
	}
	else {
	    return "L" + clazz.getName().replace('.', '/') + ';';
	}
    }

    /**
     * Compute the JVM method descriptor for the method.
     */
    static final String getSignature(Method meth) {
	final StringBuffer sb = new StringBuffer();
	sb.append('(');
	for (int j = 0; j < params.length; j++) {
	    sb.append(getSignature(params[j]));
	}
	return sb.append(')').append(getSignature(meth.getReturnType()))
	    .toString();
    }

    /**
     * Compute the JVM constructor descriptor for the constructor.
     */
    static final String getSignature(Constructor cons) {
	final StringBuffer sb = new StringBuffer();
	sb.append('(');
	for (int j = 0; j < params.length; j++) {
	    sb.append(getSignature(params[j]));
	}
	return sb.append(")V").toString();
    }
    
    /**
     * Return the signature of the current method
     */
    private String getMethodSignature(Vector argsType) {
 	final StringBuffer buf = new StringBuffer(_className);
        buf.append('.').append(_fname.getLocalPart()).append('(');
	  
	int nArgs = argsType.size();	    
	for (int i = 0; i < nArgs; i++) {
	    final Type intType = (Type)argsType.elementAt(i);
	    buf.append(intType.toString());
	    if (i < nArgs - 1) buf.append(", ");
	}
	  
	buf.append(')');
	return buf.toString();
    }

    /**
     * To support EXSLT extensions, convert names with dash to allowable Java names: 
     * e.g., convert abc-xyz to abcXyz.
     * Note: dashes only appear in middle of an EXSLT function or element name.
     */
    protected static String replaceDash(String name)
    {
        char dash = '-';
        StringBuffer buff = new StringBuffer("");
        for (int i = 0; i < name.length(); i++) {
        if (i > 0 && name.charAt(i-1) == dash)
            buff.append(Character.toUpperCase(name.charAt(i)));
        else if (name.charAt(i) != dash)
            buff.append(name.charAt(i));
        }
        return buff.toString();
    }
 	 
    /**
     * Translate code to call the BasisLibrary.unallowed_extensionF(String)
     * method.
     */
    private void translateUnallowedExtension(ConstantPoolGen cpg,
                                             InstructionList il) {
	int index = cpg.addMethodref(BASIS_LIBRARY_CLASS,
				     "unallowed_extension_functionF",
				     "(Ljava/lang/String;)V");
	il.append(new PUSH(cpg, _fname.toString()));
	il.append(new INVOKESTATIC(index));   
    } 	 
}
