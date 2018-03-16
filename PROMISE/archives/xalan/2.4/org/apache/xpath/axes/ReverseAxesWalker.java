package org.apache.xpath.axes;

import java.util.Vector;

import org.apache.xpath.axes.LocPathIterator;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.OpCodes;
import org.apache.xpath.objects.XObject;

import javax.xml.transform.TransformerException;

import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.Axis;

/**
 * Walker for a reverse axes.
 */
public class ReverseAxesWalker extends AxesWalker
{

  /**
   * Construct an AxesWalker using a LocPathIterator.
   *
   * @param locPathIterator The location path iterator that 'owns' this walker.
   */
  ReverseAxesWalker(LocPathIterator locPathIterator, int axis)
  {
    super(locPathIterator, axis);
  }
  
  /**
   * Set the root node of the TreeWalker.
   * (Not part of the DOM2 TreeWalker interface).
   *
   * @param root The context node of this step.
   */
  public void setRoot(int root)
  {
    super.setRoot(root);
    m_iterator = getDTM(root).getAxisIterator(m_axis);
    m_iterator.setStartNode(root);
  }
  
  /**
   * Get the next node in document order on the axes.
   *
   * @return the next node in document order on the axes, or null.
   */
  protected int getNextNode()
  {
    if (m_foundLast)
      return DTM.NULL;

    int next = m_iterator.next();
    
    if (m_isFresh)
      m_isFresh = false;

    if (DTM.NULL == next)
      this.m_foundLast = true;

    return next;
  }


  /**
   * Tells if this is a reverse axes.  Overrides AxesWalker#isReverseAxes.
   *
   * @return true for this class.
   */
  public boolean isReverseAxes()
  {
    return true;
  }


  /**
   * Get the current sub-context position.  In order to do the
   * reverse axes count, for the moment this re-searches the axes
   * up to the predicate.  An optimization on this is to cache
   * the nodes searched, but, for the moment, this case is probably
   * rare enough that the added complexity isn't worth it.
   *
   * @param predicateIndex The predicate index of the proximity position.
   *
   * @return The pridicate index, or -1.
   */
  protected int getProximityPosition(int predicateIndex)
  {
    if(predicateIndex < 0)
      return -1;
      
    int count = m_proximityPositions[predicateIndex];
      
    if (count <= 0)
    {
      AxesWalker savedWalker = wi().getLastUsedWalker();

      try
      {
        ReverseAxesWalker clone = (ReverseAxesWalker) this.clone();

        clone.setRoot(this.getRoot());

        clone.setPredicateCount(predicateIndex);

        clone.setPrevWalker(null);
        clone.setNextWalker(null);
        wi().setLastUsedWalker(clone);

        count++;
        int next;

        while (DTM.NULL != (next = clone.nextNode()))
        {
          count++;
        }

        m_proximityPositions[predicateIndex] = count;
      }
      catch (CloneNotSupportedException cnse)
      {

      }
      finally
      {
        wi().setLastUsedWalker(savedWalker);
      }
    }
    
    return count;
  }

  /**
   * Count backwards one proximity position.
   *
   * @param i The predicate index.
   */
  protected void countProximityPosition(int i)
  {
    if (i < m_proximityPositions.length)
      m_proximityPositions[i]--;
  }

  /**
   * Get the number of nodes in this node list.  The function is probably ill
   * named?
   *
   *
   * @param xctxt The XPath runtime context.
   *
   * @return the number of nodes in this node list.
   */
  public int getLastPos(XPathContext xctxt)
  {

    int count = 0;
    AxesWalker savedWalker = wi().getLastUsedWalker();

    try
    {
      ReverseAxesWalker clone = (ReverseAxesWalker) this.clone();

      clone.setRoot(this.getRoot());

      clone.setPredicateCount(this.getPredicateCount() - 1);

      clone.setPrevWalker(null);
      clone.setNextWalker(null);
      wi().setLastUsedWalker(clone);

      int next;

      while (DTM.NULL != (next = clone.nextNode()))
      {
        count++;
      }
    }
    catch (CloneNotSupportedException cnse)
    {

    }
    finally
    {
      wi().setLastUsedWalker(savedWalker);
    }

    return count;
  }
  
  /**
   * Returns true if all the nodes in the iteration well be returned in document 
   * order.
   * Warning: This can only be called after setRoot has been called!
   * 
   * @return false.
   */
  public boolean isDocOrdered()
  {
  }
  
  /** The DTM inner traversal class, that corresponds to the super axis. */
  protected DTMAxisIterator m_iterator;
}