package org.apache.xpath.operations;

import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

/**
 * The '>' operation expression executer.
 */
public class Gt extends Operation
{

  /**
   * Apply the operation to two operands, and return the result.
   *
   *
   * @param left non-null reference to the evaluated left operand.
   * @param right non-null reference to the evaluated right operand.
   *
   * @return non-null reference to the XObject that represents the result of the operation.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject operate(XObject left, XObject right)
          throws javax.xml.transform.TransformerException
  {
    return left.greaterThan(right) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
  }
}
