package org.apache.xerces.dom;

import java.util.Enumeration;
import java.util.Hashtable;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import org.apache.xerces.utils.XMLCharacterProperties;


/**
 * The Document interface represents the entire HTML or XML document.
 * Conceptually, it is the root of the document tree, and provides the
 * primary access to the document's data.
 * <P>
 * Since elements, text nodes, comments, processing instructions,
 * etc. cannot exist outside the context of a Document, the Document
 * interface also contains the factory methods needed to create these
 * objects. The Node objects created have a ownerDocument attribute
 * which associates them with the Document within whose context they
 * were created.
 * <p>
 * The CoreDocumentImpl class only implements the DOM Core. Additional modules
 * are supported by the more complete DocumentImpl subclass.
 * <p>
 * <b>Note:</b> When any node in the document is serialized, the
 * entire document is serialized along with it.
 *
 * @author Arnaud  Le Hors, IBM
 * @author Joe Kesselman, IBM
 * @author Andy Clark, IBM
 * @author Ralf Pfeiffer, IBM
 * @version
 * @since  PR-DOM-Level-1-19980818.
 */
public class CoreDocumentImpl
    extends ParentNode implements Document {


    /** Serialization version. */
    static final long serialVersionUID = 0;



    /** Document type. */
    protected DocumentTypeImpl docType;

    /** Document element. */
    protected ElementImpl docElement;


    /**Experimental DOM Level 3 feature: Document encoding */
    protected String encoding;

    /**Experimental DOM Level 3 feature: Document version */
    protected String version;

    /**Experimental DOM Level 3 feature: Document standalone */
    protected boolean standalone;


    /** Identifiers. */
    protected Hashtable identifiers;

    /** Table for quick check of child insertion. */
    protected static int[] kidOK;

    /**
     * Number of alterations made to this document since its creation.
     * Serves as a "dirty bit" so that live objects such as NodeList can
     * recognize when an alteration has been made and discard its cached
     * state information.
     * <p>
     * Any method that alters the tree structure MUST cause or be
     * accompanied by a call to changed(), to inform it that any outstanding
     * NodeLists may have to be updated.
     * <p>
     * (Required because NodeList is simultaneously "live" and integer-
     * indexed -- a bad decision in the DOM's design.)
     * <p>
     * Note that changes which do not affect the tree's structure -- changing
     * the node's name, for example -- do _not_ have to call changed().
     * <p>
     * Alternative implementation would be to use a cryptographic
     * Digest value rather than a count. This would have the advantage that
     * "harmless" changes (those producing equal() trees) would not force
     * NodeList to resynchronize. Disadvantage is that it's slightly more prone
     * to "false negatives", though that's the difference between "wildly
     * unlikely" and "absurdly unlikely". IF we start maintaining digests,
     * we should consider taking advantage of them.
     *
     * Note: This used to be done a node basis, so that we knew what
     * subtree changed. But since only DeepNodeList really use this today,
     * the gain appears to be really small compared to the cost of having
     * an int on every (parent) node plus having to walk up the tree all the
     * way to the root to mark the branch as changed everytime a node is
     * changed.
     * So we now have a single counter global to the document. It means that
     * some objects may flush their cache more often than necessary, but this
     * makes nodes smaller and only the document needs to be marked as changed.
     */
    protected int changes = 0;


    /** Allow grammar access. */
    protected boolean allowGrammarAccess;

    /** Bypass error checking. */
    protected boolean errorChecking = true;


    static {

        kidOK = new int[13];

        kidOK[DOCUMENT_NODE] =
            1 << ELEMENT_NODE | 1 << PROCESSING_INSTRUCTION_NODE |
            1 << COMMENT_NODE | 1 << DOCUMENT_TYPE_NODE;

        kidOK[DOCUMENT_FRAGMENT_NODE] =
        kidOK[ENTITY_NODE] =
        kidOK[ENTITY_REFERENCE_NODE] =
        kidOK[ELEMENT_NODE] =
            1 << ELEMENT_NODE | 1 << PROCESSING_INSTRUCTION_NODE |
            1 << COMMENT_NODE | 1 << TEXT_NODE |
            1 << CDATA_SECTION_NODE | 1 << ENTITY_REFERENCE_NODE ;


        kidOK[ATTRIBUTE_NODE] =
            1 << TEXT_NODE | 1 << ENTITY_REFERENCE_NODE;

        kidOK[DOCUMENT_TYPE_NODE] =
        kidOK[PROCESSING_INSTRUCTION_NODE] =
        kidOK[COMMENT_NODE] =
        kidOK[TEXT_NODE] =
        kidOK[CDATA_SECTION_NODE] =
        kidOK[NOTATION_NODE] =
            0;



    /**
     * NON-DOM: Actually creating a Document is outside the DOM's spec,
     * since it has to operate in terms of a particular implementation.
     */
    public CoreDocumentImpl() {
        this(false);
        XMLCharacterProperties.initCharFlags();
    }

    /** Constructor. */
    public CoreDocumentImpl(boolean grammarAccess) {
        super(null);
        ownerDocument = this;
        allowGrammarAccess = grammarAccess;
        XMLCharacterProperties.initCharFlags();
    }

    /**
     * For DOM2 support.
     * The createDocument factory method is in DOMImplementation.
     */
    public CoreDocumentImpl(DocumentType doctype)
    {
        this(doctype, false);
        XMLCharacterProperties.initCharFlags();
    }

    /** For DOM2 support. */
    public CoreDocumentImpl(DocumentType doctype, boolean grammarAccess) {
        this(grammarAccess);
        if (doctype != null) {
            DocumentTypeImpl doctypeImpl;
            try {
                doctypeImpl = (DocumentTypeImpl) doctype;
            } catch (ClassCastException e) {
                throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,
                                       "DOM005 Wrong document");
            }
            doctypeImpl.ownerDocument = this;
            appendChild(doctype);
        }
        XMLCharacterProperties.initCharFlags();
    }


    final public Document getOwnerDocument() {
        return null;
    }

    /** Returns the node type. */
    public short getNodeType() {
        return Node.DOCUMENT_NODE;
    }

    /** Returns the node name. */
    public String getNodeName() {
        return "#document";
    }

    /**
     * Deep-clone a document, including fixing ownerDoc for the cloned
     * children. Note that this requires bypassing the WRONG_DOCUMENT_ERR
     * protection. I've chosen to implement it by calling importNode
     * which is DOM Level 2.
     *
     * @return org.w3c.dom.Node
     * @param deep boolean, iff true replicate children
     */
    public Node cloneNode(boolean deep) {

        CoreDocumentImpl newdoc = new CoreDocumentImpl();
        cloneNode(newdoc, deep);

    	return newdoc;



    /**
     * internal method to share code with subclass
     **/
    protected void cloneNode(CoreDocumentImpl newdoc, boolean deep) {

        if (needsSyncChildren()) {
            synchronizeChildren();
        }

        if (deep) {
            Hashtable reversedIdentifiers = null;

            if (identifiers != null) {
                reversedIdentifiers = new Hashtable();
                Enumeration elementIds = identifiers.keys();
                while (elementIds.hasMoreElements()) {
                    Object elementId = elementIds.nextElement();
                    reversedIdentifiers.put(identifiers.get(elementId),
                                            elementId);
                }
            }

            for (ChildNode kid = firstChild; kid != null;
                 kid = kid.nextSibling) {
                newdoc.appendChild(newdoc.importNode(kid, true,
                                                     reversedIdentifiers));
            }
        }

        newdoc.allowGrammarAccess = allowGrammarAccess;
        newdoc.errorChecking = errorChecking;


    /**
     * Since a Document may contain at most one top-level Element child,
     * and at most one DocumentType declaraction, we need to subclass our
     * add-children methods to implement this constraint.
     * Since appendChild() is implemented as insertBefore(,null),
     * altering the latter fixes both.
     * <p>
     * While I'm doing so, I've taken advantage of the opportunity to
     * cache documentElement and docType so we don't have to
     * search for them.
     *
     * REVISIT: According to the spec it is not allowed to alter neither the
     * document element nor the document type in any way
     */
    public Node insertBefore(Node newChild, Node refChild)
        throws DOMException {

        int type = newChild.getNodeType();
        if (errorChecking) {
            if((type == Node.ELEMENT_NODE && docElement != null) ||
               (type == Node.DOCUMENT_TYPE_NODE && docType != null)) {
                throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                                           "DOM006 Hierarchy request error");
            }
        }

    	super.insertBefore(newChild,refChild);

        if (type == Node.ELEMENT_NODE) {
    	    docElement = (ElementImpl)newChild;
        }
        else if (type == Node.DOCUMENT_TYPE_NODE) {
    	    docType=(DocumentTypeImpl)newChild;
        }

    	return newChild;


    /**
     * Since insertBefore caches the docElement (and, currently, docType),
     * removeChild has to know how to undo the cache
     *
     * REVISIT: According to the spec it is not allowed to alter neither the
     * document element nor the document type in any way
     */
    public Node removeChild(Node oldChild)
        throws DOMException {
        super.removeChild(oldChild);

        int type = oldChild.getNodeType();
        if(type == Node.ELEMENT_NODE) {
    	    docElement = null;
        }
        else if (type == Node.DOCUMENT_TYPE_NODE) {
    	    docType=null;
        }

    	return oldChild;


    /**
     * Since we cache the docElement (and, currently, docType),
     * replaceChild has to update the cache
     *
     * REVISIT: According to the spec it is not allowed to alter neither the
     * document element nor the document type in any way
     */
    public Node replaceChild(Node newChild, Node oldChild)
        throws DOMException {

        super.replaceChild(newChild, oldChild);

        int type = oldChild.getNodeType();
        if(type == Node.ELEMENT_NODE) {
    	    docElement = (ElementImpl)newChild;
        }
        else if (type == Node.DOCUMENT_TYPE_NODE) {
    	    docType = (DocumentTypeImpl)newChild;
        }
        return oldChild;



    /**
     * Factory method; creates an Attribute having this Document as its
     * OwnerDoc.
     *
     * @param name The name of the attribute. Note that the attribute's value
     * is _not_ established at the factory; remember to set it!
     *
     * @throws DOMException(INVALID_NAME_ERR) if the attribute name is not
     * acceptable.
     */
    public Attr createAttribute(String name)
        throws DOMException {

    	if (errorChecking && !isXMLName(name)) {
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
                                   "DOM002 Illegal character");
        }
    	return new AttrImpl(this, name);


    /**
     * Factory method; creates a CDATASection having this Document as
     * its OwnerDoc.
     *
     * @param data The initial contents of the CDATA
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents. (HTML
     * not yet implemented.)
     */
    public CDATASection createCDATASection(String data)
        throws DOMException {
        return new CDATASectionImpl(this, data);
    }

    /**
     * Factory method; creates a Comment having this Document as its
     * OwnerDoc.
     *
     * @param data The initial contents of the Comment. */
    public Comment createComment(String data) {
        return new CommentImpl(this, data);
    }

    /**
     * Factory method; creates a DocumentFragment having this Document
     * as its OwnerDoc.
     */
    public DocumentFragment createDocumentFragment() {
        return new DocumentFragmentImpl(this);
    }

    /**
     * Factory method; creates an Element having this Document
     * as its OwnerDoc.
     *
     * @param tagName The name of the element type to instantiate. For
     * XML, this is case-sensitive. For HTML, the tagName parameter may
     * be provided in any case, but it must be mapped to the canonical
     * uppercase form by the DOM implementation.
     *
     * @throws DOMException(INVALID_NAME_ERR) if the tag name is not
     * acceptable.
     */
    public Element createElement(String tagName)
        throws DOMException {

    	if (errorChecking && !isXMLName(tagName)) {
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
                                   "DOM002 Illegal character");
        }
    	return new ElementImpl(this, tagName);


    /**
     * Factory method; creates an EntityReference having this Document
     * as its OwnerDoc.
     *
     * @param name The name of the Entity we wish to refer to
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents, where
     * nonstandard entities are not permitted. (HTML not yet
     * implemented.)
     */
    public EntityReference createEntityReference(String name)
        throws DOMException {

    	if (errorChecking && !isXMLName(name)) {
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
                                   "DOM002 Illegal character");
        }
    	return new EntityReferenceImpl(this, name);


    /**
     * Factory method; creates a ProcessingInstruction having this Document
     * as its OwnerDoc.
     *
     * @param target The target "processor channel"
     * @param data Parameter string to be passed to the target.
     *
     * @throws DOMException(INVALID_NAME_ERR) if the target name is not
     * acceptable.
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents. (HTML
     * not yet implemented.)
     */
    public ProcessingInstruction createProcessingInstruction(String target,
                                                             String data)
        throws DOMException {

    	if (errorChecking && !isXMLName(target)) {
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
                                   "DOM002 Illegal character");
        }
    	return new ProcessingInstructionImpl(this, target, data);


    /**
     * Factory method; creates a Text node having this Document as its
     * OwnerDoc.
     *
     * @param data The initial contents of the Text.
     */
    public Text createTextNode(String data) {
        return new TextImpl(this, data);
    }


    /**
     * For XML, this provides access to the Document Type Definition.
     * For HTML documents, and XML documents which don't specify a DTD,
     * it will be null.
     */
    public DocumentType getDoctype() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return docType;
    }


   /**
    * DOM Level 3 WD - Experimental.
    * The encoding of this document (part of XML Declaration)
    */
    public String getEncoding() {
	return encoding;
    }

    /**
     * DOM Level 3 WD - Experimental.
     * The version of this document (part of XML Declaration)
     */
    public String getVersion() {
	return version;
    }

    /**
     * DOM Level 3 WD - Experimental.
     * standalone that specifies whether this document is standalone
     * (part of XML Declaration)
     */
    public boolean getStandalone() {
        return standalone;
    }


    /**
     * Convenience method, allowing direct access to the child node
     * which is considered the root of the actual document content. For
     * HTML, where it is legal to have more than one Element at the top
     * level of the document, we pick the one with the tagName
     * "HTML". For XML there should be only one top-level
     *
     * (HTML not yet supported.)
     */
    public Element getDocumentElement() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return docElement;
    }

    /**
     * Return a <em>live</em> collection of all descendent Elements (not just
     * immediate children) having the specified tag name.
     *
     * @param tagname The type of Element we want to gather. "*" will be
     * taken as a wildcard, meaning "all elements in the document."
     *
     * @see DeepNodeListImpl
     */
    public NodeList getElementsByTagName(String tagname) {
        return new DeepNodeListImpl(this,tagname);
    }

    /**
     * Retrieve information describing the abilities of this particular
     * DOM implementation. Intended to support applications that may be
     * using DOMs retrieved from several different sources, potentially
     * with different underlying representations.
     */
    public DOMImplementation getImplementation() {
        return CoreDOMImplementationImpl.getDOMImplementation();
    }



    /**
     * Sets whether the DOM implementation performs error checking
     * upon operations. Turning off error checking only affects
     * the following DOM checks:
     * <ul>
     * <li>Checking strings to make sure that all characters are
     *     legal XML characters
     * <li>Hierarchy checking such as allowed children, checks for
     *     cycles, etc.
     * </ul>
     * <p>
     * Turning off error checking does <em>not</em> turn off the
     * following checks:
     * <ul>
     * <li>Read only checks
     * <li>Checks related to DOM events
     * </ul>
     */

    public void setErrorChecking(boolean check) {
        errorChecking = check;
    }

    /*
     * DOM Level 3 WD - Experimental.
     */
    public void setStrictErrorChecking(boolean check) {
        errorChecking = check;
    }


    /**
      * DOM Level 3 WD - Experimental.
      * An attribute specifying, as part of the XML declaration,
      * the encoding of this document. This is null when unspecified.
      */
    public void setEncoding(String value) {
        encoding = value;
    }

    /**
      * DOM Level 3 WD - Experimental.
      * version - An attribute specifying, as part of the XML declaration,
      * the version number of this document. This is null when unspecified
      */
    public void setVersion(String value) {
       version = value;
    }

    /**
      * DOM Level 3 WD - Experimental.
      * standalone - An attribute specifying, as part of the XML declaration,
      * whether this document is standalone
      */
    public void setStandalone(boolean value) {
        standalone = value;
    }


    /**
     * Returns true if the DOM implementation performs error checking.
     */
    public boolean getErrorChecking() {
        return errorChecking;
    }

    /*
     * DOM Level 3 WD - Experimental.
     */
    public boolean getStrictErrorChecking() {
        return errorChecking;
    }

    /**
     * Sets whether the DOM implementation generates mutation events
     * upon operations.
     */
    void setMutationEvents(boolean set) {
    }

    /**
     * Returns true if the DOM implementation generates mutation events.
     */
    boolean getMutationEvents() {
        return false;
    }




    /**
     * NON-DOM
     * Factory method; creates a DocumentType having this Document
     * as its OwnerDoc. (REC-DOM-Level-1-19981001 left the process of building
     * DTD information unspecified.)
     *
     * @param name The name of the Entity we wish to provide a value for.
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents, where
     * DTDs are not permitted. (HTML not yet implemented.)
     */
    public DocumentType createDocumentType(String qualifiedName,
                                           String publicID,
                                           String systemID)
        throws DOMException {

    	if (errorChecking && !isXMLName(qualifiedName)) {
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
                                   "DOM002 Illegal character");
        }
    	return new DocumentTypeImpl(this, qualifiedName, publicID, systemID);


    /**
     * NON-DOM
     * Factory method; creates an Entity having this Document
     * as its OwnerDoc. (REC-DOM-Level-1-19981001 left the process of building
     * DTD information unspecified.)
     *
     * @param name The name of the Entity we wish to provide a value for.
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents, where
     * nonstandard entities are not permitted. (HTML not yet
     * implemented.)
     */
    public Entity createEntity(String name)
        throws DOMException {

    	if (errorChecking && !isXMLName(name)) {
    		throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
    		                           "DOM002 Illegal character");
        }
    	return new EntityImpl(this, name);


    /**
     * NON-DOM
     * Factory method; creates a Notation having this Document
     * as its OwnerDoc. (REC-DOM-Level-1-19981001 left the process of building
     * DTD information unspecified.)
     *
     * @param name The name of the Notation we wish to describe
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents, where
     * notations are not permitted. (HTML not yet
     * implemented.)
     */
    public Notation createNotation(String name)
        throws DOMException {

    	if (errorChecking && !isXMLName(name)) {
    		throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
    		                           "DOM002 Illegal character");
        }
    	return new NotationImpl(this, name);


    /**
     * NON-DOM Factory method: creates an element definition. Element
     * definitions hold default attribute values.
     */
    public ElementDefinitionImpl createElementDefinition(String name)
        throws DOMException {

    	if (errorChecking && !isXMLName(name)) {
    		throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
    		                           "DOM002 Illegal character");
        }
        return new ElementDefinitionImpl(this, name);



    /**
     * Copies a node from another document to this document. The new nodes are
     * created using this document's factory methods and are populated with the
     * data from the source's accessor methods defined by the DOM interfaces.
     * Its behavior is otherwise similar to that of cloneNode.
     * <p>
     * According to the DOM specifications, document nodes cannot be imported
     * and a NOT_SUPPORTED_ERR exception is thrown if attempted.
     */
    public Node importNode(Node source, boolean deep)
	throws DOMException {
        return importNode(source, deep, null);

    /**
     * Overloaded implementation of DOM's importNode method. This method
     * provides the core functionality for the public importNode and cloneNode
     * methods.
     *
     * The reversedIdentifiers parameter is provided for cloneNode to
     * preserve the document's identifiers. The Hashtable has Elements as the
     * keys and their identifiers as the values. When an element is being
     * imported, a check is done for an associated identifier. If one exists,
     * the identifier is registered with the new, imported element. If
     * reversedIdentifiers is null, the parameter is not applied.
     */
    private Node importNode(Node source, boolean deep,
                            Hashtable reversedIdentifiers)
	throws DOMException {
        Node newnode=null;



        int type = source.getNodeType();

        switch (type) {
            case ELEMENT_NODE: {
                Element newElement;
                boolean domLevel20 = source.getOwnerDocument().getImplementation().hasFeature("XML", "2.0");
                if(domLevel20 == false || source.getLocalName() == null)
                    newElement = createElement(source.getNodeName());
                else
                    newElement = createElementNS(source.getNamespaceURI(),
                                                 source.getNodeName());

                NamedNodeMap sourceAttrs = source.getAttributes();
                if (sourceAttrs != null) {
                    int length = sourceAttrs.getLength();
                    for (int index = 0; index < length; index++) {
                        Attr attr = (Attr)sourceAttrs.item(index);

                        if (attr.getSpecified()) {
                            Attr newAttr = (Attr)importNode(attr, true,
                                                          reversedIdentifiers);

                            if (domLevel20 == false ||
                                attr.getLocalName() == null)
                                newElement.setAttributeNode(newAttr);
                            else
                                newElement.setAttributeNodeNS(newAttr);
                        }
                    }
                }

                if (reversedIdentifiers != null) {
                    Object elementId = reversedIdentifiers.get(source);
                    if (elementId != null) {
                        if (identifiers == null)
                            identifiers = new Hashtable();

                        identifiers.put(elementId, newElement);
                    }
                }

                newnode = newElement;
                break;
            }

            case ATTRIBUTE_NODE: {

                if(source.getOwnerDocument().getImplementation().hasFeature("XML", "2.0")){
                    if (source.getLocalName() == null) {
         	        newnode = createAttribute(source.getNodeName());
         	    } else {
          	        newnode = createAttributeNS(source.getNamespaceURI(),
                                                    source.getNodeName());
         	    }
                }
                else {
                    newnode = createAttribute(source.getNodeName());
                }
                if (source instanceof AttrImpl) {
                    AttrImpl attr = (AttrImpl) source;
                    if (attr.hasStringValue()) {
                        AttrImpl newattr = (AttrImpl) newnode;
                        newattr.setValue(attr.getValue());
                        deep = false;
                    }
                    else {
                        deep = true;
                    }
                }
                else {
                    if (source.getFirstChild() == null) {
                        newnode.setNodeValue(source.getNodeValue());
                        deep = false;
                    } else {
                        deep = true;
                    }
                }
		break;
            }

	    case TEXT_NODE: {
		newnode = createTextNode(source.getNodeValue());
		break;
            }

	    case CDATA_SECTION_NODE: {
		newnode = createCDATASection(source.getNodeValue());
		break;
            }

    	    case ENTITY_REFERENCE_NODE: {
		newnode = createEntityReference(source.getNodeName());
                ((EntityReferenceImpl)newnode).isReadOnly(false);
		break;
            }

    	    case ENTITY_NODE: {
		Entity srcentity = (Entity)source;
		EntityImpl newentity =
		    (EntityImpl)createEntity(source.getNodeName());
		newentity.setPublicId(srcentity.getPublicId());
		newentity.setSystemId(srcentity.getSystemId());
		newentity.setNotationName(srcentity.getNotationName());
		newnode = newentity;
		break;
            }

    	    case PROCESSING_INSTRUCTION_NODE: {
		newnode = createProcessingInstruction(source.getNodeName(),
						      source.getNodeValue());
		break;
            }

    	    case COMMENT_NODE: {
		newnode = createComment(source.getNodeValue());
		break;
            }

    	    case DOCUMENT_FRAGMENT_NODE: {
		newnode = createDocumentFragment();
		break;
            }

    	    case NOTATION_NODE: {
		Notation srcnotation = (Notation)source;
		NotationImpl newnotation =
		    (NotationImpl)createNotation(source.getNodeName());
		newnotation.setPublicId(srcnotation.getPublicId());
		newnotation.setSystemId(srcnotation.getSystemId());
		newnode = newnotation;
		break;
            }
            case DOCUMENT_TYPE_NODE: 
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                                  "Node type being imported is not supported");
            }
        }

    	if (deep) {
	    for (Node srckid = source.getFirstChild();
                 srckid != null;
                 srckid = srckid.getNextSibling()) {
		newnode.appendChild(importNode(srckid, true,
                                               reversedIdentifiers));
	    }
        }
        if (newnode.getNodeType() == Node.ENTITY_REFERENCE_NODE
            || newnode.getNodeType() == Node.ENTITY_NODE) {
          ((NodeImpl)newnode).setReadOnly(true, true);
        }
    	return newnode;


    /**
     * DOM Level 3 WD - Experimental
     * Change the node's ownerDocument, and its subtree, to this Document
     *
     * @param source The node to adopt.
     * @see DocumentImpl.importNode
     **/
    public Node adoptNode(Node source) {
        NodeImpl node;
        try {
            node = (NodeImpl) source;
        } catch (ClassCastException e) {
            return null;
        }
        switch (node.getNodeType()) {
            case ATTRIBUTE_NODE: {
                AttrImpl attr = (AttrImpl) node;
                attr.getOwnerElement().removeAttributeNode(attr);
                attr.isSpecified(true);
                attr.setOwnerDocument(this);
                break;
            }
            case DOCUMENT_NODE:
            case DOCUMENT_TYPE_NODE: {
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                                       "cannot adopt this type of node.");
            }
            case ENTITY_REFERENCE_NODE: {
                Node parent = node.getParentNode();
                if (parent != null) {
                    parent.removeChild(source);
                }
                Node child;
                while ((child = node.getFirstChild()) != null) {
                    node.removeChild(child);
                }
                node.setOwnerDocument(this);
                if (docType == null) {
                    break;
                }
                NamedNodeMap entities = docType.getEntities();
                Node entityNode = entities.getNamedItem(node.getNodeName());
                if (entityNode == null) {
                    break;
                }
                EntityImpl entity = (EntityImpl) entityNode;
                for (child = entityNode.getFirstChild();
                     child != null; child = child.getNextSibling()) {
                    Node childClone = child.cloneNode(true);
                    node.appendChild(childClone);
                }
                break;
            }
            case ELEMENT_NODE: {
                Node parent = node.getParentNode();
                if (parent != null) {
                    parent.removeChild(source);
                }
                node.setOwnerDocument(this);
                ((ElementImpl)node).reconcileDefaultAttributes();
                break;
            }
            default: {
                Node parent = node.getParentNode();
                if (parent != null) {
                    parent.removeChild(source);
                }
                node.setOwnerDocument(this);
            }
        }
        return node;
    }

    /**
     * Introduced in DOM Level 2
     * Returns the Element whose ID is given by elementId. If no such element
     * exists, returns null. Behavior is not defined if more than one element
     * has this ID.
     * <p>
     * Note: The DOM implementation must have information that says which
     * attributes are of type ID. Attributes with the name "ID" are not of type
     * ID unless so defined. Implementations that do not know whether
     * attributes are of type ID or not are expected to return null.
     * @see #getIdentifier
     */
    public Element getElementById(String elementId) {
        return getIdentifier(elementId);
    }

    /**
     * Registers an identifier name with a specified element node.
     * If the identifier is already registered, the new element
     * node replaces the previous node. If the specified element
     * node is null, removeIdentifier() is called.
     *
     * @see #getIdentifier
     * @see #removeIdentifier
     */
    public void putIdentifier(String idName, Element element) {

        if (element == null) {
            removeIdentifier(idName);
            return;
        }

        if (needsSyncData()) {
            synchronizeData();
        }

        if (identifiers == null) {
            identifiers = new Hashtable();
        }

        identifiers.put(idName, element);


    /**
     * Returns a previously registered element with the specified
     * identifier name, or null if no element is registered.
     *
     * @see #putIdentifier
     * @see #removeIdentifier
     */
    public Element getIdentifier(String idName) {

        if (needsSyncData()) {
            synchronizeData();
        }

        if (identifiers == null) {
            return null;
        }

        return (Element)identifiers.get(idName);


    /**
     * Removes a previously registered element with the specified
     * identifier name.
     *
     * @see #putIdentifier
     * @see #getIdentifier
     */
    public void removeIdentifier(String idName) {

        if (needsSyncData()) {
            synchronizeData();
        }

        if (identifiers == null) {
            return;
        }

        identifiers.remove(idName);


    /** Returns an enumeration registered of identifier names. */
    public Enumeration getIdentifiers() {

        if (needsSyncData()) {
            synchronizeData();
        }

        if (identifiers == null) {
            identifiers = new Hashtable();
        }

        return identifiers.keys();



    /**
     * Introduced in DOM Level 2. <p>
     * Creates an element of the given qualified name and namespace URI.
     * If the given namespaceURI is null or an empty string and the
     * qualifiedName has a prefix that is "xml", the created element
     * is bound to the predefined namespace
     * @param namespaceURI The namespace URI of the element to
     *                     create.
     * @param qualifiedName The qualified name of the element type to
     *                      instantiate.
     * @return Element A new Element object with the following attributes:
     * @throws DOMException INVALID_CHARACTER_ERR: Raised if the specified
                            name contains an invalid character.
     * @throws DOMException NAMESPACE_ERR: Raised if the qualifiedName has a
     *                      prefix that is "xml" and the namespaceURI is
     *                      neither null nor an empty string nor
     *                      if the qualifiedName has a prefix different
     *                      from "xml" and the namespaceURI is null or an
     *                      empty string.
     * @since WD-DOM-Level-2-19990923
     */
    public Element createElementNS(String namespaceURI, String qualifiedName)
        throws DOMException
    {
    	if (errorChecking && !isXMLName(qualifiedName)) {
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
                                   "DOM002 Illegal character");
        }
        return new ElementNSImpl(this, namespaceURI, qualifiedName);
    }

    /**
     * Introduced in DOM Level 2. <p>
     * Creates an attribute of the given qualified name and namespace URI.
     * If the given namespaceURI is null or an empty string and the
     * qualifiedName has a prefix that is "xml", the created element
     * is bound to the predefined namespace
     *
     * @param namespaceURI  The namespace URI of the attribute to
     *                      create. When it is null or an empty string,
     *                      this method behaves like createAttribute.
     * @param qualifiedName The qualified name of the attribute to
     *                      instantiate.
     * @return Attr         A new Attr object.
     * @throws DOMException INVALID_CHARACTER_ERR: Raised if the specified
                            name contains an invalid character.
     * @since WD-DOM-Level-2-19990923
     */
    public Attr createAttributeNS(String namespaceURI, String qualifiedName)
        throws DOMException
    {
    	if (errorChecking && !isXMLName(qualifiedName)) {
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
                                   "DOM002 Illegal character");
        }
        return new AttrNSImpl(this, namespaceURI, qualifiedName);
    }

    /**
     * Introduced in DOM Level 2. <p>
     * Returns a NodeList of all the Elements with a given local name and
     * namespace URI in the order in which they would be encountered in a
     * preorder traversal of the Document tree.
     * @param namespaceURI  The namespace URI of the elements to match
     *                      on. The special value "*" matches all
     *                      namespaces. When it is null or an empty
     *                      string, this method behaves like
     *                      getElementsByTagName.
     * @param localName     The local name of the elements to match on.
     *                      The special value "*" matches all local names.
     * @return NodeList     A new NodeList object containing all the matched
     *                      Elements.
     * @since WD-DOM-Level-2-19990923
     */
    public NodeList getElementsByTagNameNS(String namespaceURI,
                                           String localName)
    {
        return new DeepNodeListImpl(this, namespaceURI, localName);
    }


    /** Clone. */
    public Object clone() throws CloneNotSupportedException {
        CoreDocumentImpl newdoc = (CoreDocumentImpl) super.clone();
        newdoc.docType = null;
        newdoc.docElement = null;
        return newdoc;
    }


    /**
     * Check the string against XML's definition of acceptable names for
     * elements and attributes and so on using the XMLCharacterProperties
     * utility class
     */
    public static boolean isXMLName(String s) {

        if (s == null) {
            return false;
        }
        return XMLCharacterProperties.validName(s);



    /**
     * Uses the kidOK lookup table to check whether the proposed
     * tree structure is legal.
     */
    protected boolean isKidOK(Node parent, Node child) {
        if (allowGrammarAccess &&
            parent.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            return child.getNodeType() == Node.ELEMENT_NODE;
        }
    	return 0 != (kidOK[parent.getNodeType()] & 1 << child.getNodeType());
    }

    /**
     * Denotes that this node has changed.
     */
    protected void changed() {
        changes++;
    }

    /**
     * Returns the number of changes to this node.
     */
    protected int changes() {
        return changes;
    }

    /**
     * Store user data related to a given node
     * This is a place where we could use weak references! Indeed, the node
     * here won't be GC'ed as long as some user data is attached to it, since
     * the userData table will have a reference to the node.
     */
    protected void setUserData(NodeImpl n, Object data) {
    }

    /**
     * Retreive user data related to a given node
     */
    protected Object getUserData(NodeImpl n) {
        return null;
    }

    protected void addEventListener(NodeImpl node, String type,
                                    EventListener listener,
                                    boolean useCapture) {
    }

    protected void removeEventListener(NodeImpl node, String type,
                                       EventListener listener,
                                       boolean useCapture) {
    }

    protected boolean dispatchEvent(NodeImpl node, Event event) {
        return false;
    }


    /**
     * A method to be called when some text was changed in a text node,
     * so that live objects can be notified.
     */
    void replacedText(NodeImpl node) {
    }

    /**
     * A method to be called when some text was deleted from a text node,
     * so that live objects can be notified.
     */
    void deletedText(NodeImpl node, int offset, int count) {
    }

    /**
     * A method to be called when some text was inserted into a text node,
     * so that live objects can be notified.
     */
    void insertedText(NodeImpl node, int offset, int count) {
    }

    /**
     * A method to be called when a character data node has been modified
     */
    void modifyingCharacterData(NodeImpl node) {
    }

    /**
     * A method to be called when a character data node has been modified
     */
    void modifiedCharacterData(NodeImpl node, String oldvalue, String value) {
    }

    /**
     * A method to be called when a node is about to be inserted in the tree.
     */
    void insertingNode(NodeImpl node, boolean replace) {
    }

    /**
     * A method to be called when a node has been inserted in the tree.
     */
    void insertedNode(NodeImpl node, NodeImpl newInternal, boolean replace) {
    }

    /**
     * A method to be called when a node is about to be removed from the tree.
     */
    void removingNode(NodeImpl node, NodeImpl oldChild, boolean replace) {
    }

    /**
     * A method to be called when a node has been removed from the tree.
     */
    void removedNode(NodeImpl node, boolean replace) {
    }

    /**
     * A method to be called when a node is about to be replaced in the tree.
     */
    void replacingNode(NodeImpl node) {
    }

    /**
     * A method to be called when a node has been replaced in the tree.
     */
    void replacedNode(NodeImpl node) {
    }

    /**
     * A method to be called when an attribute value has been modified
     */
    void modifiedAttrValue(AttrImpl attr, String oldvalue) {
    }

    /**
     * A method to be called when an attribute node has been set
     */
    void setAttrNode(AttrImpl attr, AttrImpl previous) {
    }

    /**
     * A method to be called when an attribute node has been removed
     */
    void removedAttrNode(AttrImpl attr, NodeImpl oldOwner, String name) {
    }

