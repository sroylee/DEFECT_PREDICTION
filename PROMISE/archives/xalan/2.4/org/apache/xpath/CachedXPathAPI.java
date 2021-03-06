package org.apache.xpath;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.NodeList;

import org.apache.xpath.XPathContext;
import org.apache.xpath.XPath;
import org.apache.xpath.compiler.XPathParser;
import org.apache.xpath.XPathContext;
import org.apache.xml.utils.PrefixResolverDefault;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.objects.XObject;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.ref.DTMNodeIterator;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.apache.xml.dtm.ref.DTMManagerDefault;

/**
 * The methods in this class are convenience methods into the
 * low-level XPath API.
 *
 * These functions tend to be a little slow, since a number of objects must be
 * created for each evaluation.  A faster way is to precompile the
 * XPaths using the low-level API, and then just use the XPaths
 * over and over.
 *
 * This is an alternative for the old XPathAPI class, which provided
 * static methods for the purpose but had the drawback of
 * instantiating a new XPathContext (and thus building a new DTMManager,
 * and new DTMs) each time it was called. XPathAPIObject instead retains
 * its context as long as the object persists, reusing the DTMs. This
 * does have a downside: if you've changed your source document, you should
 * obtain a new XPathAPIObject to continue searching it, since trying to use
 * the old DTMs will probably yield bad results or malfunction outright... and
 * the cached DTMs may consume memory until this object and its context are
 * returned to the heap. Essentially, it's the caller's responsibility to
 * decide when to discard the cache.
 *
 * */
public class CachedXPathAPI
{
  /** XPathContext, and thus the document model system (DTMs), persists through multiple
      calls to this object. This is set in the constructor.
  */
  protected XPathContext xpathSupport;

  /** Default constructor. Establishes its own XPathContext, and hence
   *  its own DTMManager.  Good choice for simple uses.
   * */
  public CachedXPathAPI()
  {
    xpathSupport = new XPathContext();
  }
  
  /** This constructor shares its XPathContext with a pre-existing
   *  CachedXPathAPI.  That allows sharing document models (DTMs) and
   *  previously established location state.
   *
   *  Note that the original CachedXPathAPI and the new one should not
   *  be operated concurrently; we do not support multithreaded access
   *  to a single DTM at this time.
   *
   *  %REVIEW% Should this instead do a clone-and-reset on the XPathSupport object?
   * */
  public CachedXPathAPI(CachedXPathAPI priorXPathAPI)
  {
    xpathSupport = priorXPathAPI.xpathSupport;
  }


  /** Returns the XPathSupport object used in this CachedXPathAPI
   *
   * %REVIEW% I'm somewhat concerned about the loss of encapsulation
   * this causes, but the xml-security folks say they need it.
   * */
  public XPathContext getXPathContext()
  {
    return this.xpathSupport;
  }
  

  /**
   * Use an XPath string to select a single node. XPath namespace
   * prefixes are resolved from the context node, which may not
   * be what you want (see the next method).
   *
   * @param contextNode The node to start searching from.
   * @param str A valid XPath string.
   * @return The first node found that matches the XPath, or null.
   *
   * @throws TransformerException
   */
  public  Node selectSingleNode(Node contextNode, String str)
          throws TransformerException
  {
    return selectSingleNode(contextNode, str, contextNode);
  }

  /**
   * Use an XPath string to select a single node.
   * XPath namespace prefixes are resolved from the namespaceNode.
   *
   * @param contextNode The node to start searching from.
   * @param str A valid XPath string.
   * @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   * @return The first node found that matches the XPath, or null.
   *
   * @throws TransformerException
   */
  public  Node selectSingleNode(
          Node contextNode, String str, Node namespaceNode)
            throws TransformerException
  {

    NodeIterator nl = selectNodeIterator(contextNode, str, namespaceNode);

    return nl.nextNode();
  }

  /**
   *  Use an XPath string to select a nodelist.
   *  XPath namespace prefixes are resolved from the contextNode.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @return A NodeIterator, should never be null.
   *
   * @throws TransformerException
   */
  public  NodeIterator selectNodeIterator(Node contextNode, String str)
          throws TransformerException
  {
    return selectNodeIterator(contextNode, str, contextNode);
  }

  /**
   *  Use an XPath string to select a nodelist.
   *  XPath namespace prefixes are resolved from the namespaceNode.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   *  @return A NodeIterator, should never be null.
   *
   * @throws TransformerException
   */
  public  NodeIterator selectNodeIterator(
          Node contextNode, String str, Node namespaceNode)
            throws TransformerException
  {

    XObject list = eval(contextNode, str, namespaceNode);

    return list.nodeset();
  }

  /**
   *  Use an XPath string to select a nodelist.
   *  XPath namespace prefixes are resolved from the contextNode.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @return A NodeIterator, should never be null.
   *
   * @throws TransformerException
   */
  public  NodeList selectNodeList(Node contextNode, String str)
          throws TransformerException
  {
    return selectNodeList(contextNode, str, contextNode);
  }

  /**
   *  Use an XPath string to select a nodelist.
   *  XPath namespace prefixes are resolved from the namespaceNode.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   *  @return A NodeIterator, should never be null.
   *
   * @throws TransformerException
   */
  public  NodeList selectNodeList(
          Node contextNode, String str, Node namespaceNode)
            throws TransformerException
  {

    XObject list = eval(contextNode, str, namespaceNode);

    return list.nodelist();
  }

  /**
   *  Evaluate XPath string to an XObject.  Using this method,
   *  XPath namespace prefixes will be resolved from the namespaceNode.
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   *  @return An XObject, which can be used to obtain a string, number, nodelist, etc, should never be null.
   *  @see org.apache.xpath.objects.XObject
   *  @see org.apache.xpath.objects.XNull
   *  @see org.apache.xpath.objects.XBoolean
   *  @see org.apache.xpath.objects.XNumber
   *  @see org.apache.xpath.objects.XString
   *  @see org.apache.xpath.objects.XRTreeFrag
   *
   * @throws TransformerException
   */
  public  XObject eval(Node contextNode, String str)
          throws TransformerException
  {
    return eval(contextNode, str, contextNode);
  }

  /**
   *  Evaluate XPath string to an XObject. 
   *  XPath namespace prefixes are resolved from the namespaceNode.
   *  The implementation of this is a little slow, since it creates
   *  a number of objects each time it is called.  This could be optimized
   *  to keep the same objects around, but then thread-safety issues would arise.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   *  @return An XObject, which can be used to obtain a string, number, nodelist, etc, should never be null.
   *  @see org.apache.xpath.objects.XObject
   *  @see org.apache.xpath.objects.XNull
   *  @see org.apache.xpath.objects.XBoolean
   *  @see org.apache.xpath.objects.XNumber
   *  @see org.apache.xpath.objects.XString
   *  @see org.apache.xpath.objects.XRTreeFrag
   *
   * @throws TransformerException
   */
  public  XObject eval(Node contextNode, String str, Node namespaceNode)
          throws TransformerException
  {


    PrefixResolverDefault prefixResolver = new PrefixResolverDefault(
      (namespaceNode.getNodeType() == Node.DOCUMENT_NODE)
      ? ((Document) namespaceNode).getDocumentElement() : namespaceNode);

    XPath xpath = new XPath(str, null, prefixResolver, XPath.SELECT, null);

    int ctxtNode = xpathSupport.getDTMHandleFromNode(contextNode);

    return xpath.execute(xpathSupport, ctxtNode, prefixResolver);
  }

  /**
   *   Evaluate XPath string to an XObject.
   *   XPath namespace prefixes are resolved from the namespaceNode.
   *   The implementation of this is a little slow, since it creates
   *   a number of objects each time it is called.  This could be optimized
   *   to keep the same objects around, but then thread-safety issues would arise.
   *
   *   @param contextNode The node to start searching from.
   *   @param str A valid XPath string.
   *   @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   *   @param prefixResolver Will be called if the parser encounters namespace
   *                         prefixes, to resolve the prefixes to URLs.
   *   @return An XObject, which can be used to obtain a string, number, nodelist, etc, should never be null.
   *   @see org.apache.xpath.objects.XObject
   *   @see org.apache.xpath.objects.XNull
   *   @see org.apache.xpath.objects.XBoolean
   *   @see org.apache.xpath.objects.XNumber
   *   @see org.apache.xpath.objects.XString
   *   @see org.apache.xpath.objects.XRTreeFrag
   *
   * @throws TransformerException
   */
  public  XObject eval(
          Node contextNode, String str, PrefixResolver prefixResolver)
            throws TransformerException
  {

    XPath xpath = new XPath(str, null, prefixResolver, XPath.SELECT, null);

    XPathContext xpathSupport = new XPathContext();
    int ctxtNode = xpathSupport.getDTMHandleFromNode(contextNode);

    return xpath.execute(xpathSupport, ctxtNode, prefixResolver);
  }
}
