package org.apache.xalan.lib;

import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.ref.DTMNodeIterator;
import org.apache.xpath.NodeSet;

/**
 * <meta name="usage" content="general"/>
 * This class contains EXSLT common extension functions.
 * It is accessed by specifying a namespace URI as follows:
 * <pre>
 * </pre>
 * 
 * The documentation for each function has been copied from the relevant
 * EXSLT Implementer page.
 * 
 */
public class ExsltCommon
{
  /**
   * The exsl:object-type function returns a string giving the type of the object passed 
   * as the argument. The possible object types are: 'string', 'number', 'boolean', 
   * 'node-set', 'RTF', or 'external'. 
   * 
   * Most XSLT object types can be coerced to each other without error. However, there are 
   * certain coercions that raise errors, most importantly treating anything other than a 
   * node set as a node set. Authors of utilities such as named templates or user-defined 
   * extension functions may wish to give some flexibility in the parameter and argument values 
   * that are accepted by the utility; the exsl:object-type function enables them to do so.
   * 
   * The Xalan extensions MethodResolver converts 'object-type' to 'objectType'.
   * 
   * @param obj The object to be typed.
   * @return objectType 'string', 'number', 'boolean', 'node-set', 'RTF', or 'external'.
   * 
   */
  public static String objectType (Object obj)
  {
    if (obj instanceof String)
      return "string";
    else if (obj instanceof Boolean)
      return "boolean";
    else if (obj instanceof Number)
      return "number";
    else if (obj instanceof DTMNodeIterator)
    {
      DTMIterator dtmI = ((DTMNodeIterator)obj).getDTMIterator();
      if (dtmI instanceof org.apache.xpath.axes.RTFIterator)
      	return "RTF";
      else
        return "node-set";
    }
    else
      return "unknown";
  }
    
  /**
   * The exsl:node-set function converts a result tree fragment (which is what you get 
   * when you use the content of xsl:variable rather than its select attribute to give 
   * a variable value) into a node set. This enables you to process the XML that you create 
   * within a variable, and therefore do multi-step processing. 
   * 
   * You can also use this function to turn a string into a text node, which is helpful 
   * if you want to pass a string to a function that only accepts a node set.
   * 
   * The Xalan extensions MethodResolver converts 'node-set' to 'nodeSet'.
   * 
   * @param myProcesser is passed in by the Xalan extension processor
   * @param rtf The result tree fragment to be converted to a node-set.
   * 
   * @returns node-set with the contents of the result tree fragment.
   * 
   * Note: Already implemented in the xalan namespace as nodeset.
   * 
   */
  public static NodeSet nodeSet(ExpressionContext myProcessor, Object rtf)
  {
    return Extensions.nodeset(myProcessor, rtf);
  }
 
}
