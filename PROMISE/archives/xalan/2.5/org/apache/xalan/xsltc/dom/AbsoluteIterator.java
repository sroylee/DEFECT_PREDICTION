package org.apache.xalan.xsltc.dom;

import org.apache.xalan.xsltc.runtime.BasisLibrary;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.ref.DTMAxisIteratorBase;
import org.apache.xml.dtm.ref.DTMDefaultBase;

/**
 * Absolute iterators ignore the node that is passed to setStartNode(). 
 * Instead, they always start from the root node. The node passed to 
 * setStartNode() is not totally useless, though. It is needed to obtain the 
 * DOM mask, i.e. the index into the MultiDOM table that corresponds to the 
 * DOM "owning" the node. 
 * 
 * The DOM mask is cached, so successive calls to setStartNode() passing 
 * nodes from other DOMs will have no effect (i.e. this iterator cannot 
 * migrate between DOMs).
 */
public final class AbsoluteIterator extends DTMAxisIteratorBase {

    /**
     * Source for this iterator.
     */
    private DTMAxisIterator _source;

    public AbsoluteIterator(DTMAxisIterator source) {
	_source = source;
    }

    public void setRestartable(boolean isRestartable) {
	_isRestartable = isRestartable;
	_source.setRestartable(isRestartable);
    }

    public DTMAxisIterator setStartNode(int node) {
	_startNode = DTMDefaultBase.ROOTNODE;
	if (_isRestartable) {
	    _source.setStartNode(_startNode);
	    resetPosition();
	}
	return this;
    }

    public int next() {
	return returnNode(_source.next());
    }

    public DTMAxisIterator cloneIterator() {
	try {
	    final AbsoluteIterator clone = (AbsoluteIterator) super.clone();
	    clone.resetPosition();
	    clone._isRestartable = false;
	    return clone;
	}
	catch (CloneNotSupportedException e) {
	    BasisLibrary.runTimeError(BasisLibrary.ITERATOR_CLONE_ERR,
				      e.toString());
	    return null;
	}
    }

    public DTMAxisIterator reset() {
	_source.reset();
	return resetPosition();
    }
    
    public void setMark() {
	_source.setMark();
    }

    public void gotoMark() {
	_source.gotoMark();
    }
}