package org.apache.xerces.dom;

import org.w3c.dom.*;
import org.w3c.dom.traversal.*;

/** This class implements the TreeWalker interface. */
public class TreeWalkerImpl implements TreeWalker {
    
    
    /** When TRUE, the children of entites references are returned in the iterator. */
    private boolean fEntityReferenceExpansion = false;
    /** The whatToShow mask. */
    int fWhatToShow = NodeFilter.SHOW_ALL;
    /** The NodeFilter reference. */
    NodeFilter fNodeFilter;
    /** The current Node. */
    Node fCurrentNode;
    /** The root Node. */
    Node fRoot;
    
    
    
    
    /** Public constructor */
    public TreeWalkerImpl(Node root, 
                          int whatToShow, 
                          NodeFilter nodeFilter,
                          boolean entityReferenceExpansion) {
        fCurrentNode = root;
        fRoot = root;
        fWhatToShow = whatToShow;
        fNodeFilter = nodeFilter;
        fEntityReferenceExpansion = entityReferenceExpansion;
    }
    
    public Node getRoot() {
	return fRoot;
    }

    /** Return the whatToShow value */
    public int                getWhatToShow() {
        return fWhatToShow;
    }

    /** Return the NodeFilter */
    public NodeFilter         getFilter() {
        return fNodeFilter;
    }
    
    /** Return whether children entity references are included in the iterator. */
    public boolean            getExpandEntityReferences() {
        return fEntityReferenceExpansion;
    }
            
    /** Return the current Node. */
    public Node               getCurrentNode() {
        return fCurrentNode;
    }
    /** Return the current Node. */
    public void               setCurrentNode(Node node) {
        fCurrentNode = node;
    }
    
    /** Return the parent Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               parentNode() {

        if (fCurrentNode == null) return null;
                
        Node node = getParentNode(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
        
    }

    /** Return the first child Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               firstChild() {
        
        if (fCurrentNode == null) return null;
                
        Node node = getFirstChild(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }
    /** Return the last child Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               lastChild() {

        if (fCurrentNode == null) return null;
                
        Node node = getLastChild(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }
    
    /** Return the previous sibling Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               previousSibling() {

        if (fCurrentNode == null) return null;
                
        Node node = getPreviousSibling(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }
    
    /** Return the next sibling Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               nextSibling(){
        if (fCurrentNode == null) return null;
                
        Node node = getNextSibling(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }
    
    /** Return the previous Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               previousNode() {
        Node result;
        
        if (fCurrentNode == null) return null;
        
        result = getPreviousSibling(fCurrentNode);
        if (result == null) {
            result = getParentNode(fCurrentNode);
            if (result != null) {
                fCurrentNode = result;
                return fCurrentNode;
            } 
            return null;
        }
        
        Node lastChild  = getLastChild(result);
        
        Node prev = lastChild ;
        while (lastChild != null) {
          prev = lastChild ;
          lastChild = getLastChild(prev) ;
        }

        lastChild = prev ;
        
        if (lastChild != null) {
            fCurrentNode = lastChild;
            return fCurrentNode;
        } 
        
        if (result != null) {
            fCurrentNode = result;
            return fCurrentNode;
        }
        
        return null;
    }
    
    /** Return the next Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               nextNode() {
        
        if (fCurrentNode == null) return null;
        
        Node result = getFirstChild(fCurrentNode);
        
        if (result != null) {
            fCurrentNode = result;
            return result;
        }
        
        result = getNextSibling(fCurrentNode);
        
        if (result != null) {
            fCurrentNode = result;
            return result;
        }
                
        Node parent = getParentNode(fCurrentNode);
        while (parent != null) {
            result = getNextSibling(parent);
            if (result != null) {
                fCurrentNode = result;
                return result;
            } else {
                parent = getParentNode(parent);
            }
        }
        
        return null;
    }
    
    /** Internal function.
     *  Return the parent Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getParentNode(Node node) {
        
        if (node == null || node == fRoot) return null;
        
        Node newNode = node.getParentNode();
        if (newNode == null)  return null; 
                        
        int accept = acceptNode(newNode);
        
        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else 
        {
            return getParentNode(newNode);
        }
        
        
    }
    
    /** Internal function.
     *  Return the nextSibling Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getNextSibling(Node node) {
        
        if (node == null || node == fRoot) return null;
        
        Node newNode = node.getNextSibling();
        if (newNode == null) {
                
            newNode = node.getParentNode();
                
            if (newNode == null || node == fRoot)  return null; 
                
            int parentAccept = acceptNode(newNode);
                
            if (parentAccept==NodeFilter.FILTER_SKIP) {
                return getNextSibling(newNode);
            }
                
            return null;
        }
        
        int accept = acceptNode(newNode);
        
        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else 
        if (accept == NodeFilter.FILTER_SKIP) {
            Node fChild =  getFirstChild(newNode);
            if (fChild == null) {
                return getNextSibling(newNode);
            }
            return fChild;
        }
        else 
        {
            return getNextSibling(newNode);
        }
        
    
    /** Internal function.
     *  Return the previous sibling Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getPreviousSibling(Node node) {
        
        if (node == null || node == fRoot) return null;
        
        Node newNode = node.getPreviousSibling();
        if (newNode == null) {
                
            newNode = node.getParentNode();
            if (newNode == null || node == fRoot)  return null; 
                
            int parentAccept = acceptNode(newNode);
                
            if (parentAccept==NodeFilter.FILTER_SKIP) {
                return getPreviousSibling(newNode);
            }
            
            return null;
        }
        
        int accept = acceptNode(newNode);
        
        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else 
        if (accept == NodeFilter.FILTER_SKIP) {
            Node fChild =  getLastChild(newNode);
            if (fChild == null) {
                return getPreviousSibling(newNode);
            }
            return fChild;
        }
        else 
        {
            return getPreviousSibling(newNode);
        }
        
    
    /** Internal function.
     *  Return the first child Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getFirstChild(Node node) {
        
        if (node == null) return null;
        
        if ( !fEntityReferenceExpansion
             && node.getNodeType() == Node.ENTITY_REFERENCE_NODE)
            return null;
   
        Node newNode = node.getFirstChild();
        if (newNode == null)  return null;
        
        int accept = acceptNode(newNode);
        
        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else 
        if (accept == NodeFilter.FILTER_SKIP
            && newNode.hasChildNodes()) 
        {
            return getFirstChild(newNode);
        }
        else 
        {
            return getNextSibling(newNode);
        }
        
        
    }
   
    /** Internal function.
     *  Return the last child Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getLastChild(Node node) {
        
        if (node == null) return null;
        
        if ( !fEntityReferenceExpansion
             && node.getNodeType() == Node.ENTITY_REFERENCE_NODE)
            return null;
            
        Node newNode = node.getLastChild();
        if (newNode == null)  return null; 
        
        int accept = acceptNode(newNode);
        
        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else 
        if (accept == NodeFilter.FILTER_SKIP
            && newNode.hasChildNodes()) 
        {
            return getLastChild(newNode);
        }
        else 
        {
            return getPreviousSibling(newNode);
        }
        
        
    }
    
    /** Internal function. 
     *  The node whatToShow and the filter are combined into one result. */
    short acceptNode(Node node) {
        /***
         7.1.2.4. Filters and whatToShow flags 

         Iterator and TreeWalker apply whatToShow flags before applying Filters. If a node is rejected by the
         active whatToShow flags, a Filter will not be called to evaluate that node. When a node is rejected by
         the active whatToShow flags, children of that node will still be considered, and Filters may be called to
         evaluate them.
         ***/
                
        if (fNodeFilter == null) {
            if ( ( fWhatToShow & (1 << node.getNodeType()-1)) != 0) {
                return NodeFilter.FILTER_ACCEPT;
            } else {
                return NodeFilter.FILTER_SKIP;
            }
        } else {
            if ((fWhatToShow & (1 << node.getNodeType()-1)) != 0 ) {
                return fNodeFilter.acceptNode(node);
            } else {
                return NodeFilter.FILTER_SKIP;
            }
        }
    } 
}
