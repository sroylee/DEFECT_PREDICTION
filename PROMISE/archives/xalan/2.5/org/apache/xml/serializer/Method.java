package org.apache.xml.serializer;

/**
 * This class defines the constants which are the names of the four default
 * output methods.
 * <p>
 * Four default output methods are defined: XML, HTML, XHTML and TEXT.
 * Serializers may support additional output methods. The names of
 * these output methods should be encoded as <tt>namespace:local</tt>.
 *
 * @version Alpha
 * @author <a href="mailto:arkin@exoffice.com">Assaf Arkin</a>
 */
public final class Method
{

  /**
   * The output method for XML documents: <tt>xml</tt>.
   */
  public static final String XML = "xml";

  /**
   * The output method for HTML documents: <tt>html</tt>.
   */
  public static final String HTML = "html";

  /**
   * The output method for XHTML documents: <tt>xhtml</tt>.
   */
  public static final String XHTML = "xhtml";

  /**
   * The output method for text documents: <tt>text</tt>.
   */
  public static final String TEXT = "text";
  
  /**
   * The "internal" method, just used when no method is 
   * specified in the style sheet, and a serializer of this type wraps either an
   * XML or HTML type (depending on the first tag in the output being html or
   * not)
   */  
  public static final String UNKNOWN = "";
}
