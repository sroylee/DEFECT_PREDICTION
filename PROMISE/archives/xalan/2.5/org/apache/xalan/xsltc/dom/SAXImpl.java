package org.apache.xalan.xsltc.dom;

import java.util.Enumeration;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.xalan.xsltc.DOM;
import org.apache.xalan.xsltc.StripFilter;
import org.apache.xalan.xsltc.TransletException;
import org.apache.xalan.xsltc.runtime.BasisLibrary;
import org.apache.xalan.xsltc.runtime.Hashtable;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.dtm.ref.DTMAxisIterNodeList;
import org.apache.xml.dtm.ref.DTMAxisIteratorBase;
import org.apache.xml.dtm.ref.DTMDefaultBase;
import org.apache.xml.dtm.ref.DTMNodeProxy;
import org.apache.xml.dtm.ref.sax2dtm.SAX2DTM2;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.serializer.ToXMLSAXHandler;
import org.apache.xml.utils.XMLStringFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Entity;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
 * SAXImpl is the core model for SAX input source. SAXImpl objects are
 * usually created from an XSLTCDTMManager.
 *
 * <p>DOMSource inputs are handled using DOM2SAX + SAXImpl. SAXImpl has a
 * few specific fields (e.g. _node2Ids, _document) to keep DOM-related
 * information. They are used when the processing behavior between DOM and
 * SAX has to be different. Examples of these include id function and 
 * unparsed entity.
 *
 * <p>SAXImpl extends SAX2DTM2 instead of SAX2DTM for better performance.
 */
public final class SAXImpl extends SAX2DTM2 implements DOM, DOMBuilder
{
    
    /* ------------------------------------------------------------------- */
    /* DOMBuilder fields BEGIN                                             */
    /* ------------------------------------------------------------------- */

    private int       _uriCount     = 0;
    private int       _prefixCount  = 0;

    private int[]   _xmlSpaceStack;
    private int     _idx = 1;
    private boolean _preserve = false;

    private static final String XML_STRING = "xml:";
    private static final String XML_PREFIX   = "xml";   
    private static final String XMLSPACE_STRING = "xml:space";
    private static final String PRESERVE_STRING = "preserve";
    private static final String XMLNS_PREFIX = "xmlns";

    private boolean _escaping = true;
    private boolean _disableEscaping = false;
    private int _textNodeToProcess = DTM.NULL;

    /* ------------------------------------------------------------------- */
    /* DOMBuilder fields END                                               */
    /* ------------------------------------------------------------------- */

    private final static String EMPTYSTRING = "";

    private final static DTMAxisIterator EMPTYITERATOR =
        new DTMAxisIteratorBase() {
            public DTMAxisIterator reset() { return this; }
            public DTMAxisIterator setStartNode(int node) { return this; }
            public int next() { return DTM.NULL; }
            public void setMark() {}
            public void gotoMark() {}
            public int getLast() { return 0; }
            public int getPosition() { return 0; }
            public DTMAxisIterator cloneIterator() { return this; }
            public void setRestartable(boolean isRestartable) { }
        };
    
    private int _namesSize = 0;

    private Hashtable _nsIndex = new Hashtable();
   
    private int _size = 0;
    
    private BitArray  _dontEscape = null;

    private String    _documentURI = null;
    static private int _documentURIIndex = 0;

    private Document _document;

    private Hashtable _node2Ids = null;

    private boolean _hasDOMSource = false;

    private XSLTCDTMManager _dtmManager;

    private Node[] _nodes;
    private NodeList[] _nodeLists;
    private final static String XML_LANG_ATTRIBUTE =

    /**
     * Define the origin of the document from which the tree was built
     */
    public void setDocumentURI(String uri) {
        setDocumentBaseURI(uri);
        _documentURI = uri;
    }

    /**
     * Returns the origin of the document from which the tree was built
     */
    public String getDocumentURI() {
        String baseURI = getDocumentBaseURI();
        return (baseURI != null) ? baseURI : "rtf" + _documentURIIndex++;
    }

    public String getDocumentURI(int node) {
        return getDocumentURI();
    }

    public void setupMapping(String[] names, String[] namespaces) {
    }

    /**
     * Lookup a namespace URI from a prefix starting at node. This method
     * is used in the execution of xsl:element when the prefix is not known
     * at compile time.
     */
    public String lookupNamespace(int node, String prefix)
        throws TransletException
    {
        int anode, nsnode;
        final AncestorIterator ancestors = new AncestorIterator();

        if (isElement(node)) {
            ancestors.includeSelf();
        }

        ancestors.setStartNode(node);
        while ((anode = ancestors.next()) != DTM.NULL) {
            final NamespaceIterator namespaces = new NamespaceIterator();

            namespaces.setStartNode(anode);
            while ((nsnode = namespaces.next()) != DTM.NULL) {
                if (getLocalName(nsnode).equals(prefix)) {
                    return getNodeValue(nsnode);
                }
            }
        }

        BasisLibrary.runTimeError(BasisLibrary.NAMESPACE_PREFIX_ERR, prefix);
        return null;
    }

    /**
     * Returns 'true' if a specific node is an element (of any type)
     */
    public boolean isElement(final int node) {
        return getNodeType(node) == DTM.ELEMENT_NODE;
    }

    /**
     * Returns 'true' if a specific node is an attribute (of any type)
     */
    public boolean isAttribute(final int node) {
        return getNodeType(node) == DTM.ATTRIBUTE_NODE;
    }

    /**
     * Returns the number of nodes in the tree (used for indexing)
     */
    public int getSize() {
        return getNumberOfNodes();
    }

    /**
     * Part of the DOM interface - no function here.
     */
    public void setFilter(StripFilter filter) {
    }


    /**
     * Returns true if node1 comes before node2 in document order
     */
    public boolean lessThan(int node1, int node2) {
        if (node1 == DTM.NULL) {
            return false;
        }

        if (node2 == DTM.NULL) {
            return true;
        }

        return (node1 < node2);
    }

    /**
     * Create an org.w3c.dom.Node from a node in the tree
     */
    public Node makeNode(int index) {
        if (_nodes == null) {
            _nodes = new Node[_namesSize];
        }

        return (_nodes[index] != null)
                    ? _nodes[index]
                    : (_nodes[index] = new DTMNodeProxy((DTM)this, index));
    }

    /**
     * Create an org.w3c.dom.Node from a node in an iterator
     * The iterator most be started before this method is called
     */
    public Node makeNode(DTMAxisIterator iter) {
        return makeNode(iter.next());
    }

    /**
     * Create an org.w3c.dom.NodeList from a node in the tree
     */
    public NodeList makeNodeList(int index) {
        if (_nodeLists == null) {
            _nodeLists = new NodeList[_namesSize];
        }
        return (_nodeLists[index] != null)
                 ? _nodeLists[index]
                 : (_nodeLists[index] =
                         new DTMAxisIterNodeList(this,
                                                 new SingletonIterator(index)));
    }

    /**
     * Create an org.w3c.dom.NodeList from a node iterator
     * The iterator most be started before this method is called
     */
    public NodeList makeNodeList(DTMAxisIterator iter) {
        return new DTMAxisIterNodeList(this, iter);
    }

    /**
     * Iterator that returns the namespace nodes as defined by the XPath data
     * model for a given node, filtered by extended type ID.
     */
    public class TypedNamespaceIterator extends NamespaceIterator {
        /** The extended type ID that was requested. */
        private final int _nodeType;

        /**
         * Constructor TypedChildrenIterator
         *
         *
         * @param nodeType The extended type ID being requested.
         */
        public TypedNamespaceIterator(int nodeType) {
            super();
            _nodeType = nodeType;
        }

       /**
        * Get the next node in the iteration.
        *
        * @return The next node handle in the iteration, or END.
        */
        public int next() {
            int node;

            for (node = super.next(); node != END; node = super.next()) {
                if (getExpandedTypeID(node) == _nodeType
                      || getNodeType(node) == _nodeType
                      || getIdForNamespace(getStringValueX(node))
                             == _nodeType) {
                    return returnNode(node);
                }
            }

            return (END);
        }


    /**************************************************************
     * This is a specialised iterator for predicates comparing node or
     * attribute values to variable or parameter values.
     */
    private final class NodeValueIterator extends InternalAxisIteratorBase
    {

	private DTMAxisIterator _source;
	private String _value;
	private boolean _op;
	private final boolean _isReverse;
	private int _returnType = RETURN_PARENT;

	public NodeValueIterator(DTMAxisIterator source, int returnType,
				 String value, boolean op)
        {
	    _source = source;
	    _returnType = returnType;
	    _value = value;
	    _op = op;
	    _isReverse = source.isReverse();
	}

	public boolean isReverse()
        {
	    return _isReverse;
	}

        public DTMAxisIterator cloneIterator()
        {
            try {
                NodeValueIterator clone = (NodeValueIterator)super.clone();
                clone._isRestartable = false;
                clone._source = _source.cloneIterator();
                clone._value = _value;
                clone._op = _op;
                return clone.reset();
            }
            catch (CloneNotSupportedException e) {
                BasisLibrary.runTimeError(BasisLibrary.ITERATOR_CLONE_ERR,
                                          e.toString());
                return null;
            }
        }
  
        public void setRestartable(boolean isRestartable)
        {
	    _isRestartable = isRestartable;
	    _source.setRestartable(isRestartable);
	}

	public DTMAxisIterator reset()
        {
	    _source.reset();
	    return resetPosition();
	}

	public int next()
        {
            int node;
            while ((node = _source.next()) != END) {
                String val = getStringValueX(node);
                if (_value.equals(val) == _op) {
                    if (_returnType == RETURN_CURRENT) {
                        return returnNode(node);
                    }
                    else {
                        return returnNode(getParent(node));
                    }
                }
            }
            return END;
        }

	public DTMAxisIterator setStartNode(int node)
        {
            if (_isRestartable) {
                _source.setStartNode(_startNode = node);
                return resetPosition();
            }
            return this;
        }

	public void setMark()
        {
	    _source.setMark();
	}

	public void gotoMark()
        {
	    _source.gotoMark();
	}

    public DTMAxisIterator getNodeValueIterator(DTMAxisIterator iterator, int type,
					     String value, boolean op)
    {
        return(DTMAxisIterator)(new NodeValueIterator(iterator, type, value, op));
    }

    /**
     * Encapsulates an iterator in an OrderedIterator to ensure node order
     */
    public DTMAxisIterator orderNodes(DTMAxisIterator source, int node)
    {
        return new DupFilterIterator(source);
    }

    /**
     * Returns singleton iterator containg the document root
     * Works for them main document (mark == 0)
     */
    public DTMAxisIterator getIterator()
    {
        return new SingletonIterator(getDocument());
    }

     /**
     * Get mapping from DOM namespace types to external namespace types
     */
    public int getNSType(int node)
    {
    	String s = getNamespaceURI(node);
    	if (s == null) {
    	    return 0;
    	}
    	int eType = getIdForNamespace(s);
    	return ((Integer)_nsIndex.get(new Integer(eType))).intValue();        
    }
    
    

    /**
     * Returns the namespace type of a specific node
     */
    public int getNamespaceType(final int node)
    {
    	return super.getNamespaceType(node);
    }

    /**
     * Sets up a translet-to-dom type mapping table
     */
    private int[] setupMapping(String[] namesArray, int nNames) {
        final int[] types = new int[m_expandedNameTable.getSize()];
        for (int i = 0; i < nNames; i++)      {
            int type = getGeneralizedType(namesArray[i]);
            types[type] = type;
        }
        return types;
    }

    /**
     * Returns the internal type associated with an expanded QName
     */
    public int getGeneralizedType(final String name) {
        String lName, ns = null;
        int index = -1;
        int code;

        if ((index = name.lastIndexOf(":"))> -1) {
            ns = name.substring(0, index);
        }

        int lNameStartIdx = index+1;

        if (name.charAt(lNameStartIdx) == '@') {
            code = DTM.ATTRIBUTE_NODE;
            lNameStartIdx++;
        }
        else {
            code = DTM.ELEMENT_NODE;
        }

        lName = (lNameStartIdx == 0) ? name : name.substring(lNameStartIdx);

        return this.getExpandedTypeID(ns, lName, code);
    }

    /**
     * Get mapping from DOM element/attribute types to external types
     */
    public short[] getMapping(String[] names)
    {
        int i;
        final int namesLength = names.length;
        final int exLength = m_expandedNameTable.getSize();
        int[] generalizedTypes = null;
        if (namesLength > 0) {
            generalizedTypes = new int[namesLength];
        }
      
        int resultLength = exLength;
      
        for (i = 0; i < namesLength; i++) {
            generalizedTypes[i] = getGeneralizedType(names[i]);
            if (_namesSize == 0 && generalizedTypes[i] >= resultLength) {
                resultLength = generalizedTypes[i] + 1;
            }
        }
      
        final short[] result = new short[resultLength];

        for (i = 0; i < DTM.NTYPES; i++) {
            result[i] = (short)i;
        }
        
        for (i = NTYPES; i < exLength; i++) { 
      	    result[i] = m_expandedNameTable.getType(i);
      	}
      	
        for (i = 0; i < namesLength; i++) {
            int genType = generalizedTypes[i];         
            if (_namesSize > 0) {
                if (genType < result.length) {
                    result[genType] = (short)(i + DTM.NTYPES);
                }
            }
            else {
                result[genType] = (short)(i + DTM.NTYPES);
            }
        }

        return(result);
    }

    /**
     * Get mapping from external element/attribute types to DOM types
     */
    public int[] getReverseMapping(String[] names)
    {
        int i;
        final int[] result = new int[names.length + DTM.NTYPES];
        
        for (i = 0; i < DTM.NTYPES; i++) {
            result[i] = i;
        }
        
        for (i = 0; i < names.length; i++) {
            int type = getGeneralizedType(names[i]);
            result[i+DTM.NTYPES] = type;
        }
        return(result);
    }
    
    /**
     * Get mapping from DOM namespace types to external namespace types
     */
    public short[] getNamespaceMapping(String[] namespaces)
    {
        int i;
        final int nsLength = namespaces.length;
        final int mappingLength = _uriCount;

        final short[] result = new short[mappingLength];

        for (i=0; i<mappingLength; i++) {
            result[i] = (short)(-1);
        }

        for (i=0; i<nsLength; i++) {
            int eType = getIdForNamespace(namespaces[i]); 
            Integer type = (Integer)_nsIndex.get(new Integer(eType));
            if (type != null) {
                result[type.intValue()] = (short)i;
            }
        }

        return(result);
    }

    /**
     * Get mapping from external namespace types to DOM namespace types
     */
    public short[] getReverseNamespaceMapping(String[] namespaces)
    {
        int i;
        final int length = namespaces.length;
        final short[] result = new short[length];

        for (i = 0; i < length; i++) {
            int eType = getIdForNamespace(namespaces[i]);
            Integer type = (Integer)_nsIndex.get(new Integer(eType));
            result[i] = (type == null) ? -1 : type.shortValue();
        }

        return result;
    }

    /**
     * Construct a SAXImpl object using the default block size.
     */
    public SAXImpl(XSLTCDTMManager mgr, Source source,
                   int dtmIdentity, DTMWSFilter whiteSpaceFilter,
                   XMLStringFactory xstringfactory,
                   boolean doIndexing, boolean buildIdIndex)
    {
        this(mgr, source, dtmIdentity, whiteSpaceFilter, xstringfactory,
            doIndexing, DEFAULT_BLOCKSIZE, buildIdIndex);
    }
    
    /**
     * Construct a SAXImpl object using the given block size.
     */
    public SAXImpl(XSLTCDTMManager mgr, Source source,
                   int dtmIdentity, DTMWSFilter whiteSpaceFilter,
                   XMLStringFactory xstringfactory,
                   boolean doIndexing, int blocksize, 
                   boolean buildIdIndex)
    {
        super(mgr, source, dtmIdentity, whiteSpaceFilter, xstringfactory,
            doIndexing, blocksize, false, buildIdIndex);
      
        _dtmManager = mgr;      
        _size = blocksize;
      
        _xmlSpaceStack = new int[blocksize <= 64 ? 4 : 64];
                                  
        /* From DOMBuilder */ 
        _xmlSpaceStack[0] = DTMDefaultBase.ROOTNODE;
      
        if (source instanceof DOMSource) {
            _hasDOMSource = true;
            DOMSource domsrc = (DOMSource)source;
            Node node = domsrc.getNode();
            if (node instanceof Document) {
                _document = (Document)node;
            }
            else {
                _document = node.getOwnerDocument();
            }
            _node2Ids = new Hashtable();
        }                          
    }
        
    /**
     * Return the node identity for a given id String
     * 
     * @param idString The id String
     * @return The identity of the node whose id is the given String.
     */
    public int getElementById(String idString)
    {
        Node node = _document.getElementById(idString);
        if (node != null) {
            Integer id = (Integer)_node2Ids.get(node);
            return (id != null) ? id.intValue() : DTM.NULL;
        }
        else {
            return DTM.NULL;
        }
    }
    
    /**
     * Return true if the input source is DOMSource.
     */
    public boolean hasDOMSource()
    {
        return _hasDOMSource;	
    }

    /*---------------------------------------------------------------------------*/
    /* DOMBuilder methods begin                                                  */
    /*---------------------------------------------------------------------------*/
    
    /**
     * Call this when an xml:space attribute is encountered to
     * define the whitespace strip/preserve settings.
     */
    private void xmlSpaceDefine(String val, final int node)
    {
        final boolean setting = val.equals(PRESERVE_STRING);
        if (setting != _preserve) {
            _xmlSpaceStack[_idx++] = node;
            _preserve = setting;
        }
    }

    /**
     * Call this from endElement() to revert strip/preserve setting
     * to whatever it was before the corresponding startElement().
     */
    private void xmlSpaceRevert(final int node)
    {
        if (node == _xmlSpaceStack[_idx - 1]) {
            _idx--;
            _preserve = !_preserve;
        }
    }

    /**
     * Find out whether or not to strip whitespace nodes.
     *
     *
     * @return whether or not to strip whitespace nodes.
     */
    protected boolean getShouldStripWhitespace()
    {
        return _preserve ? false : super.getShouldStripWhitespace();
    }

    /**
     * Creates a text-node and checks if it is a whitespace node.
     */
    private void handleTextEscaping() {
        if (_disableEscaping && _textNodeToProcess != DTM.NULL
            && _type(_textNodeToProcess) == DTM.TEXT_NODE) {
            if (_dontEscape == null) {
                _dontEscape = new BitArray(_size);
            }
          
            if (_textNodeToProcess >= _dontEscape.size()) {
                _dontEscape.resize(_dontEscape.size() * 2);
            }
          
            _dontEscape.setBit(_textNodeToProcess);
            _disableEscaping = false;
        }
        _textNodeToProcess = DTM.NULL;
    }


    /****************************************************************/
    /*               SAX Interface Starts Here                      */
    /****************************************************************/

    /**
     * SAX2: Receive notification of character data.
     */
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        super.characters(ch, start, length);
        
        _disableEscaping = !_escaping;  
        _textNodeToProcess = getNumberOfNodes();
    }

    /**
     * SAX2: Receive notification of the beginning of a document.
     */
    public void startDocument() throws SAXException
    {
        super.startDocument();

        _nsIndex.put(new Integer(0), new Integer(_uriCount++));
        
        super.startPrefixMapping(XML_PREFIX, XML_URI);
        Integer eType = new Integer(getIdForNamespace(XML_URI));
        _nsIndex.put(eType, new Integer(_uriCount++));
    }

    /**
     * SAX2: Receive notification of the end of a document.
     */
    public void endDocument() throws SAXException
    {
        super.endDocument();
        
        handleTextEscaping();
        _namesSize = m_expandedNameTable.getSize();
    }

    /**
     * Specialized interface used by DOM2SAX. This one has an extra Node
     * parameter to build the Node -> id map.
     */
    public void startElement(String uri, String localName,
                             String qname, Attributes attributes,
                             Node node)
        throws SAXException
    {
    	this.startElement(uri, localName, qname, attributes);
    	
    	if (m_buildIdIndex) {
    	    _node2Ids.put(node, new Integer(m_parents.peek()));
    	}
    }
    
    /**
     * SAX2: Receive notification of the beginning of an element.
     */
    public void startElement(String uri, String localName,
                 String qname, Attributes attributes)
        throws SAXException
    {
        super.startElement(uri, localName, qname, attributes);
        
        handleTextEscaping();

        if (m_wsfilter != null) {
            final int index = attributes.getIndex(XMLSPACE_STRING);
            if (index >= 0) {
                xmlSpaceDefine(attributes.getValue(index), m_parents.peek());
            }
        }
    }

    /**
     * SAX2: Receive notification of the end of an element.
     */
    public void endElement(String namespaceURI, String localName, String qname)
        throws SAXException
    {
        super.endElement(namespaceURI, localName, qname);
        
        handleTextEscaping();

        if (m_wsfilter != null) {
            xmlSpaceRevert(m_previous);
        }
    }

    /**
     * SAX2: Receive notification of a processing instruction.
     */
    public void processingInstruction(String target, String data)
        throws SAXException
    {
        super.processingInstruction(target, data);
        handleTextEscaping();
    }

    /**
     * SAX2: Receive notification of ignorable whitespace in element
     * content. Similar to characters(char[], int, int).
     */
    public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException
    {
        super.ignorableWhitespace(ch, start, length);
        _textNodeToProcess = getNumberOfNodes();
    }

    /**
     * SAX2: Begin the scope of a prefix-URI Namespace mapping.
     */
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException
    {
        super.startPrefixMapping(prefix, uri);
        handleTextEscaping();

        definePrefixAndUri(prefix, uri);
    }

    private void definePrefixAndUri(String prefix, String uri) 
        throws SAXException 
    {
        Integer eType = new Integer(getIdForNamespace(uri));
        if ((Integer)_nsIndex.get(eType) == null) {
            _nsIndex.put(eType, new Integer(_uriCount++));
        }
    }
 
    /**
     * SAX2: Report an XML comment anywhere in the document.
     */
    public void comment(char[] ch, int start, int length)
        throws SAXException
    {
        super.comment(ch, start, length);
        handleTextEscaping();
    }

    public boolean setEscaping(boolean value) {
        final boolean temp = _escaping;
        _escaping = value; 
        return temp;
    }
   
   /*---------------------------------------------------------------------------*/
   /* DOMBuilder methods end                                                    */
   /*---------------------------------------------------------------------------*/

    /**
     * Prints the whole tree to standard output
     */
    public void print(int node, int level)
    {
        switch(getNodeType(node))
        {
	    case DTM.ROOT_NODE:
	    case DTM.DOCUMENT_NODE:
	        print(getFirstChild(node), level);
	        break;
	    case DTM.TEXT_NODE:
	    case DTM.COMMENT_NODE:
	    case DTM.PROCESSING_INSTRUCTION_NODE:
	        System.out.print(getStringValueX(node));
	        break;
	    default:
	        final String name = getNodeName(node);
	        System.out.print("<" + name);
	        for (int a = getFirstAttribute(node); a != DTM.NULL; a = getNextAttribute(a))
                {
		    System.out.print("\n" + getNodeName(a) + "=\"" + getStringValueX(a) + "\"");
	        }
	        System.out.print('>');
	        for (int child = getFirstChild(node); child != DTM.NULL;
		    child = getNextSibling(child)) {
		    print(child, level + 1);
	        }
	        System.out.println("</" + name + '>');
	        break;
	}
    }

    /**
     * Returns the name of a node (attribute or element).
     */
    public String getNodeName(final int node)
    {
	int nodeh = node;
	final short type = getNodeType(nodeh);
	switch(type)
        {
	    case DTM.ROOT_NODE:
	    case DTM.DOCUMENT_NODE:
	    case DTM.TEXT_NODE:
	    case DTM.COMMENT_NODE:
	        return EMPTYSTRING;
	    case DTM.NAMESPACE_NODE:
		return this.getLocalName(nodeh);
	    default:
	        return super.getNodeName(nodeh);
	}
    }    

    /**
     * Returns the namespace URI to which a node belongs
     */
    public String getNamespaceName(final int node)
    {
    	if (node == DTM.NULL) {
    	    return "";
    	}
    	
        String s;
        return (s = getNamespaceURI(node)) == null ? EMPTYSTRING : s;
    }

 
    /**
     * Returns the attribute node of a given type (if any) for an element
     */
    public int getAttributeNode(final int type, final int element)
    {
        for (int attr = getFirstAttribute(element);
           attr != DTM.NULL;
           attr = getNextAttribute(attr))
        {
            if (getExpandedTypeID(attr) == type) return attr;
        }
        return DTM.NULL;
    }

    /**
     * Returns the value of a given attribute type of a given element
     */
    public String getAttributeValue(final int type, final int element)
    {
        final int attr = getAttributeNode(type, element);
        return (attr != DTM.NULL) ? getStringValueX(attr) : EMPTYSTRING;
    }

    /**
     * This method is for testing/debugging only
     */
    public String getAttributeValue(final String name, final int element)
    {
        return getAttributeValue(getGeneralizedType(name), element);
    }

    /**
     * Returns an iterator with all the children of a given node
     */
    public DTMAxisIterator getChildren(final int node)
    {
        return (new ChildrenIterator()).setStartNode(node);
    }

    /**
     * Returns an iterator with all children of a specific type
     * for a given node (element)
     */
    public DTMAxisIterator getTypedChildren(final int type)
    {
        return(new TypedChildrenIterator(type));
    }

    /**
     * This is a shortcut to the iterators that implement the
     * supported XPath axes (only namespace::) is not supported.
     * Returns a bare-bones iterator that must be initialized
     * with a start node (using iterator.setStartNode()).
     */
    public DTMAxisIterator getAxisIterator(final int axis)
    {
        switch (axis)
        {
            case Axis.SELF:
                return new SingletonIterator();
            case Axis.CHILD:
                return new ChildrenIterator();
            case Axis.PARENT:
                return new ParentIterator();
            case Axis.ANCESTOR:
                return new AncestorIterator();
            case Axis.ANCESTORORSELF:
                return (new AncestorIterator()).includeSelf();
            case Axis.ATTRIBUTE:
                return new AttributeIterator();
            case Axis.DESCENDANT:
                return new DescendantIterator();
            case Axis.DESCENDANTORSELF:
                return (new DescendantIterator()).includeSelf();
            case Axis.FOLLOWING:
                return new FollowingIterator();
            case Axis.PRECEDING:
                return new PrecedingIterator();
            case Axis.FOLLOWINGSIBLING:
                return new FollowingSiblingIterator();
            case Axis.PRECEDINGSIBLING:
                return new PrecedingSiblingIterator();
            case Axis.NAMESPACE:
                return new NamespaceIterator();
            default:
                BasisLibrary.runTimeError(BasisLibrary.AXIS_SUPPORT_ERR, Axis.names[axis]);
        }
        return null;
    }

    /**
     * Similar to getAxisIterator, but this one returns an iterator
     * containing nodes of a typed axis (ex.: child::foo)
     */
    public DTMAxisIterator getTypedAxisIterator(int axis, int type)
    {
        if (axis == Axis.CHILD) {
            return new TypedChildrenIterator(type);
        }

        if (type == NO_TYPE) {
            return(EMPTYITERATOR);
        }

        switch (axis)
        {
            case Axis.SELF:
                return new TypedSingletonIterator(type);
            case Axis.CHILD:
                return new TypedChildrenIterator(type);
            case Axis.PARENT:
                return new ParentIterator().setNodeType(type);
            case Axis.ANCESTOR:
                return new TypedAncestorIterator(type);
            case Axis.ANCESTORORSELF:
                return (new TypedAncestorIterator(type)).includeSelf();
            case Axis.ATTRIBUTE:
                return new TypedAttributeIterator(type);
            case Axis.DESCENDANT:
                return new TypedDescendantIterator(type);
            case Axis.DESCENDANTORSELF:
                return (new TypedDescendantIterator(type)).includeSelf();
            case Axis.FOLLOWING:
                return new TypedFollowingIterator(type);
            case Axis.PRECEDING:
                return new TypedPrecedingIterator(type);
            case Axis.FOLLOWINGSIBLING:
                return new TypedFollowingSiblingIterator(type);
            case Axis.PRECEDINGSIBLING:
                return new TypedPrecedingSiblingIterator(type);
            case Axis.NAMESPACE:
                return (type == DTM.ELEMENT_NODE)
                       ? new NamespaceIterator()
                       : new TypedNamespaceIterator(type);
            default:
                BasisLibrary.runTimeError(BasisLibrary.TYPED_AXIS_SUPPORT_ERR, Axis.names[axis]);
        }
        return null;
    }

    /**
     * Do not thing that this returns an iterator for the namespace axis.
     * It returns an iterator with nodes that belong in a certain namespace,
     * such as with <xsl:apply-templates select="blob/foo:*"/>
     * The 'axis' specifies the axis for the base iterator from which the
     * nodes are taken, while 'ns' specifies the namespace URI type.
     */
    public DTMAxisIterator getNamespaceAxisIterator(int axis, int ns)
    {

        DTMAxisIterator iterator = null;

        if (ns == NO_TYPE) {
            return EMPTYITERATOR;
        }
        else {
            switch (axis) {
                case Axis.CHILD:
                    return new NamespaceChildrenIterator(ns);
                case Axis.ATTRIBUTE:
                    return new NamespaceAttributeIterator(ns);
                default:
                    BasisLibrary.runTimeError(BasisLibrary.TYPED_AXIS_SUPPORT_ERR,
                        Axis.names[axis]);
            }
        }
        return null;
    }

    /**
     * Returns an iterator with all descendants of a node that are of
     * a given type.
     */
    public DTMAxisIterator getTypedDescendantIterator(int type)
    {
        return new TypedDescendantIterator(type);
    }

    /**
     * Returns the nth descendant of a node
     */
    public DTMAxisIterator getNthDescendant(int type, int n, boolean includeself)
    {
        DTMAxisIterator source = (DTMAxisIterator) new TypedDescendantIterator(type);
        return new NthDescendantIterator(n);
    }

    /**
     * Copy the string value of a node directly to an output handler
     */
    public void characters(final int node, SerializationHandler handler)
        throws TransletException
    {
        if (node != DTM.NULL) {
            try {
                dispatchCharactersEvents(node, handler, false);
            } catch (SAXException e) {
                throw new TransletException(e);
            }
        }
    }
    
    /**
     * Copy a node-set to an output handler
     */
    public void copy(DTMAxisIterator nodes, SerializationHandler handler)
        throws TransletException
    {
        int node;
        while ((node = nodes.next()) != DTM.NULL) {
            copy(node, handler);
        }
    }

    /**
     * Copy the whole tree to an output handler
     */
    public void copy(SerializationHandler handler) throws TransletException
    {
        copy(getDocument(), handler);
    }

    /**
     * Performs a deep copy (ref. XSLs copy-of())
     *
     * TODO: Copy namespace declarations. Can't be done until we
     *       add namespace nodes and keep track of NS prefixes
     * TODO: Copy comment nodes
     */
    public void copy(final int node, SerializationHandler handler)
        throws TransletException
    {
        int nodeID = makeNodeIdentity(node);
        int eType = _exptype2(nodeID);
        int type = _exptype2Type(eType);

        try {
            switch(type)
            {
                case DTM.ROOT_NODE:
                case DTM.DOCUMENT_NODE:
                    for(int c = _firstch2(nodeID); c != DTM.NULL; c = _nextsib2(c)) {
                        copy(makeNodeHandle(c), handler);
                    }
                    break;
                case DTM.PROCESSING_INSTRUCTION_NODE:
                    copyPI(node, handler);
                    break;
                case DTM.COMMENT_NODE:
                    handler.comment(getStringValueX(node));
                    break;
                case DTM.TEXT_NODE:
                    boolean oldEscapeSetting = false;
                    boolean escapeBit = false;

                    if (_dontEscape != null) {
                        escapeBit = _dontEscape.getBit(getNodeIdent(node));
                        if (escapeBit) {
                            oldEscapeSetting = handler.setEscaping(false);
                        }
                    }
                    
                    copyTextNode(nodeID, handler);
        
                    if (escapeBit) {
                        handler.setEscaping(oldEscapeSetting);
                    }
                    break;
                case DTM.ATTRIBUTE_NODE:
                    copyAttribute(nodeID, eType, handler);
                    break;
                case DTM.NAMESPACE_NODE:
                    handler.namespaceAfterStartElement(getNodeNameX(node), getNodeValue(node));
                    break;
                default:
                    if (type == DTM.ELEMENT_NODE) 
                    {
                        final String name = copyElement(nodeID, eType, handler);
          
                        int current = nodeID;
                        while (true)
                        {
                            current++;
                            eType = _exptype2(current);
                            type = _exptype2Type(eType);
            
                            if (type == DTM.ATTRIBUTE_NODE) {
                                copyAttribute(current, eType, handler);
                            }
                            else if (type == DTM.NAMESPACE_NODE) {
                                handler.namespaceAfterStartElement(getNodeNameX(makeNodeHandle(current)), getNodeValue(makeNodeHandle(current)));            
                            }
                            else
                                break;
                        }

                        for (int c = _firstch2(nodeID); c != DTM.NULL; c = _nextsib2(c)) {
                            copy(makeNodeHandle(c), handler);
                        }
          
                        handler.endElement(name);
                    }
                    else {
                        final String uri = getNamespaceName(node);
                        if (uri.length() != 0) {
                            final String prefix = getPrefix(node);
                            handler.namespaceAfterStartElement(prefix, uri);
                        }
                        handler.addAttribute(getNodeName(node), getNodeValue(node));
                    }
                    break;
            }
        } 
        catch (Exception e) {
            throw new TransletException(e);
        }
    }

    /**
     * Copies a processing instruction node to an output handler
     */
    private void copyPI(final int node, SerializationHandler handler)
	throws TransletException
    {
        final String target = getNodeName(node);
        final String value = getStringValueX(node);
      
        try {
            handler.processingInstruction(target, value);
        } catch (Exception e) {
            throw new TransletException(e);
        }
    }

    /**
     * Performs a shallow copy (ref. XSLs copy())
     */
    public String shallowCopy(final int node, SerializationHandler handler)
        throws TransletException
    {
        int nodeID = makeNodeIdentity(node);
        int exptype = _exptype2(nodeID);
        int type = _exptype2Type(exptype);
      
        try {
            switch(type)
            {
                case DTM.ELEMENT_NODE:
                    return(copyElement(nodeID, exptype, handler));
                case DTM.ROOT_NODE:
                case DTM.DOCUMENT_NODE:
                    return EMPTYSTRING;
                case DTM.TEXT_NODE:
                    copyTextNode(nodeID, handler);
                    return null;
                case DTM.PROCESSING_INSTRUCTION_NODE:
                    copyPI(node, handler);
                    return null;
                case DTM.COMMENT_NODE:
                    handler.comment(getStringValueX(node));
                    return null;
                case DTM.NAMESPACE_NODE:
                    handler.namespaceAfterStartElement(getNodeNameX(node), getNodeValue(node));
                    return null;
                case DTM.ATTRIBUTE_NODE:
                    copyAttribute(nodeID, exptype, handler);
                    return null;  
                default:
                    final String uri1 = getNamespaceName(node);
                    if (uri1.length() != 0) {
                        final String prefix = getPrefix(node);
                        handler.namespaceAfterStartElement(prefix, uri1);
                    }
                    handler.addAttribute(getNodeName(node), getNodeValue(node));
                    return null;
            }
        } catch (Exception e) {
            throw new TransletException(e);
        }   
    }
    
    /**
     * Returns a node' defined language for a node (if any)
     */
    public String getLanguage(int node)
    {
        int parent = node;
    	while (DTM.NULL != parent) {
            if (DTM.ELEMENT_NODE == getNodeType(parent)) {

                if (DTM.NULL != langAttr) {
                    return getNodeValue(langAttr);     
                }
            }

            parent = getParent(parent);
        }      
        return(null);
    }

    /**
     * Returns an instance of the DOMBuilder inner class
     * This class will consume the input document through a SAX2
     * interface and populate the tree.
     */
    public DOMBuilder getBuilder()
    {
	return this;
    }
    
    /**
     * Return a SerializationHandler for output handling.
     * This method is used by Result Tree Fragments.
     */
    public SerializationHandler getOutputDomBuilder()
    {
        return new ToXMLSAXHandler(this, "UTF-8");
    }
    
    /**
     * Return a instance of a DOM class to be used as an RTF
     */ 
    public DOM getResultTreeFrag(int initSize, int rtfType)
    {
    	if (rtfType == DOM.SIMPLE_RTF) {
            int dtmPos = _dtmManager.getFirstFreeDTMID();
    	    SimpleResultTreeImpl rtf = new SimpleResultTreeImpl(_dtmManager,
    	                               dtmPos << DTMManager.IDENT_DTM_NODE_BITS);
    	    _dtmManager.addDTM(rtf, dtmPos, 0);
    	    return rtf;
    	}
    	else if (rtfType == DOM.ADAPTIVE_RTF) {
            int dtmPos = _dtmManager.getFirstFreeDTMID();
    	    AdaptiveResultTreeImpl rtf = new AdaptiveResultTreeImpl(_dtmManager,
    	                               dtmPos << DTMManager.IDENT_DTM_NODE_BITS,
    	                               m_wsfilter, initSize, m_buildIdIndex);
    	    _dtmManager.addDTM(rtf, dtmPos, 0);
    	    return rtf;
    	
    	}
    	else {
    	    return (SAXImpl) _dtmManager.getDTM(null, true, m_wsfilter,
                                                true, false, false,
                                                initSize, m_buildIdIndex);
        }
    }

    /**
     * %HZ% Need Javadoc
     */
    public Hashtable getElementsWithIDs() {
        if (m_idAttributes == null) {
            return null;
        }

        Enumeration idValues = m_idAttributes.keys();
        if (!idValues.hasMoreElements()) {
            return null;
        }

        Hashtable idAttrsTable = new Hashtable();

        while (idValues.hasMoreElements()) {
            Object idValue = idValues.nextElement();

            idAttrsTable.put(idValue, m_idAttributes.get(idValue));
        }

        return idAttrsTable;
    }

    /**
     * The getUnparsedEntityURI function returns the URI of the unparsed
     * entity with the specified name in the same document as the context
     * node (see [3.3 Unparsed Entities]). It returns the empty string if
     * there is no such entity.
     */
    public String getUnparsedEntityURI(String name)
    {
        if (_document != null) {
            String uri = "";
            DocumentType doctype = _document.getDoctype();
            if (doctype != null) {
                NamedNodeMap entities = doctype.getEntities();
                
                if (entities == null) {
                    return uri;
                }
                
                Entity entity = (Entity) entities.getNamedItem(name);
                
                if (entity == null) {
                    return uri;
                }
                
                String notationName = entity.getNotationName();
                if (notationName != null) {
                    uri = entity.getSystemId();
                    if (uri == null) {
                        uri = entity.getPublicId();
                    }
                }
            }
            return uri;
        }
        else {
            return super.getUnparsedEntityURI(name);
        }	
    }

}
