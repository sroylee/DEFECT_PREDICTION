package org.apache.xml.serializer;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

/**
 * @author minchau
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
abstract public class ToSAXHandler extends SerializerBase 
{
    public ToSAXHandler()
    {
    }

    public ToSAXHandler(
        ContentHandler hdlr,
        LexicalHandler lex,
        String encoding)
    {
        setContentHandler(hdlr);
        setLexHandler(lex);
        setEncoding(encoding);
    }
    public ToSAXHandler(ContentHandler handler, String encoding)
    {
        setContentHandler(handler);
        setEncoding(encoding);
    }

    /**
     * Underlying SAX handler. Taken from XSLTC
     */
    protected ContentHandler m_saxHandler;

    /**
     * Underlying LexicalHandler. Taken from XSLTC
     */
    protected LexicalHandler m_lexHandler;

    
    /** If this is true, then the content handler wrapped by this
     * serializer implements the TransformState interface which
     * will give the content handler access to the state of
     * the transform. */
    protected TransformStateSetter m_state = null;

    /**
     * Pass callback to the SAX Handler
     */
    protected void startDocumentInternal() throws SAXException
    {
        if (m_needToCallStartDocument)  
        {
            super.startDocumentInternal();

            m_saxHandler.startDocument();
            m_needToCallStartDocument = false;
        }
    }
    /**
     * Do nothing.
     * @see org.xml.sax.ext.LexicalHandler#startDTD(String, String, String)
     */
    public void startDTD(String arg0, String arg1, String arg2)
        throws SAXException
    {
    }

    /**
     * Receive notification of character data.
     *
     * @param chars The string of characters to process.
     *
     * @throws org.xml.sax.SAXException
     *
     * @see org.apache.xml.serializer.ExtendedContentHandler#characters(String)
     */
    public void characters(String characters) throws SAXException
    {
        characters(characters.toCharArray(), 0, characters.length());
    }

    /**
     * Receive notification of a comment.
     *
     * @see org.apache.xml.serializer.ExtendedLexicalHandler#comment(String)
     */
    public void comment(String comment) throws SAXException
    {

        if (m_startTagOpen)
        {
            closeStartTag();
        }
        else if (m_cdataTagOpen)
        {
            closeCDATA();
        }

        if (m_lexHandler != null)
        {
            m_lexHandler.comment(comment.toCharArray(), 0, comment.length());

            super.fireCommentEvent(comment.toCharArray(), 0, comment.length());
        }

    }

    /**
     * Do nothing as this is an abstract class. All subclasses will need to
     * define their behavior if it is different.
     * @see org.xml.sax.ContentHandler#processingInstruction(String, String)
     */
    public void processingInstruction(String target, String data)
        throws SAXException
    {
    }

    protected void closeStartTag() throws SAXException
    {
    }

    protected void closeCDATA() throws SAXException
    {
    }
    
    /**
     * Receive notification of the beginning of an element, although this is a
     * SAX method additional namespace or attribute information can occur before
     * or after this call, that is associated with this element.
     *
     *
     * @param namespaceURI The Namespace URI, or the empty string if the
     *        element has no Namespace URI or if Namespace
     *        processing is not being performed.
     * @param localName The local name (without prefix), or the
     *        empty string if Namespace processing is not being
     *        performed.
     * @param name The element type name.
     * @param atts The attributes attached to the element, if any.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startElement
     * @see org.xml.sax.ContentHandler#endElement
     * @see org.xml.sax.AttributeList
     *
     * @throws org.xml.sax.SAXException
     *
     * @see org.xml.sax.ContentHandler#startElement(String,String,String,Attributes)
     */
    public void startElement(
        String arg0,
        String arg1,
        String arg2,
        Attributes arg3)
        throws SAXException
    {
        if (m_state != null) {
            m_state.resetState(getTransformer());
        }

    	super.fireStartElem(arg2);    	
    }

    /**
     * Sets the LexicalHandler.
     * @param _lexHandler The LexicalHandler to set
     */
    public void setLexHandler(LexicalHandler _lexHandler)
    {
        this.m_lexHandler = _lexHandler;
    }

    /**
     * Sets the SAX ContentHandler.
     * @param m_saxHandler The ContentHandler to set
     */
    public void setContentHandler(ContentHandler _saxHandler)
    {
        this.m_saxHandler = _saxHandler;
        if (m_lexHandler == null && _saxHandler instanceof LexicalHandler)
        {
            m_lexHandler = (LexicalHandler) _saxHandler;
        }
    }

    /**
     * Does nothing. The setting of CDATA section elements has an impact on
     * stream serializers.
     * @see org.apache.xml.serializer.SerializationHandler#setCdataSectionElements(java.util.Vector)
     */
    public void setCdataSectionElements(Vector URI_and_localNames)
    {
    }

    /**
     * This method flushes any pending events, which can be startDocument()
     * closing the opening tag of an element, or closing an open CDATA section.
     */
    public void flushPending()
    {
        try
        {
            if (m_needToCallStartDocument)
            {
                startDocumentInternal();
                m_needToCallStartDocument = false;
            }

            if (m_startTagOpen)
            {
                closeStartTag();
                m_startTagOpen = false;
            }
            
            if (m_cdataTagOpen)
            {
                closeCDATA();
                m_cdataTagOpen = false;
            }
        }
        catch (SAXException e)
        {
        }

    }

    /**
     * Pass in a reference to a TransformState object, which
     * can be used during SAX ContentHandler events to obtain
     * information about he state of the transformation. This
     * method will be called  before each startDocument event.
     *
     * @param ts A reference to a TransformState object
     */
    public void setTransformState(TransformStateSetter ts) {
        this.m_state = ts;
    }

    /**
     * Receives notification that an element starts, but attributes are not
     * fully known yet.
     *
     * @param uri the URI of the namespace of the element (optional)
     * @param localName the element name, but without prefix (optional)
     * @param qName the element name, with prefix, if any (required)
     *
     * @see org.apache.xml.serializer.ExtendedContentHandler#startElement(String, String, String)
     */
    public void startElement(String uri, String localName, String qName)
        throws SAXException {
            
        if (m_state != null) {
            m_state.resetState(getTransformer());
        }

    	super.fireStartElem(qName);    	        
    }

    /**
     * An element starts, but attributes are not fully known yet.
     *
     * @param elementName the element name, with prefix (if any).

     * @see org.apache.xml.serializer.ExtendedContentHandler#startElement(String)
     */
    public void startElement(String qName) throws SAXException {
        if (m_state != null) {
            m_state.resetState(getTransformer());
        }        
    	super.fireStartElem(qName);    	                
    }
    
    /**
     * This method gets the nodes value as a String and uses that String as if
     * it were an input character notification.
     * @param node the Node to serialize
     * @throws org.xml.sax.SAXException
     */    
    public void characters(org.w3c.dom.Node node)
        throws org.xml.sax.SAXException
    {
        if (m_state != null)
        {
            m_state.setCurrentNode(node);
        }
        
        super.characters(node);
       }    

    /**
     * @see org.xml.sax.ErrorHandler#fatalError(SAXParseException)
     */
    public void fatalError(SAXParseException exc) throws SAXException {
        super.fatalError(exc);
        
        m_needToCallStartDocument = false;
        
        if (m_saxHandler instanceof ErrorHandler) {
            ((ErrorHandler)m_saxHandler).fatalError(exc);            
        }
    }

    /**
     * @see org.xml.sax.ErrorHandler#error(SAXParseException)
     */
    public void error(SAXParseException exc) throws SAXException {
        super.error(exc);
        
        if (m_saxHandler instanceof ErrorHandler)
            ((ErrorHandler)m_saxHandler).error(exc);        
        
    }

    /**
     * @see org.xml.sax.ErrorHandler#warning(SAXParseException)
     */
    public void warning(SAXParseException exc) throws SAXException {
        super.warning(exc);
        
        if (m_saxHandler instanceof ErrorHandler)
            ((ErrorHandler)m_saxHandler).warning(exc);        
    }

}
