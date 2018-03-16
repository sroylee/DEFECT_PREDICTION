   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at


   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */


/*
 * MemErrPtg.java
 *
 * Created on November 21, 2001, 8:46 AM
 */
package org.apache.poi.hssf.record.formula;

import org.apache.poi.util.LittleEndian;
import org.apache.poi.hssf.model.Workbook;
import org.apache.poi.hssf.record.RecordInputStream;

/**
 *
 * @author  andy
 * @author Jason Height (jheight at chariot dot net dot au)
 * @author Daniel Noll (daniel at nuix dot com dot au)
 */

public class MemErrPtg
    extends MemAreaPtg
{
    public final static short sid  = 0x27;

    /** Creates new MemErrPtg */

    public MemErrPtg()
    {
    }

    public MemErrPtg(RecordInputStream in)
    {
        super(in);
    }

    public void writeBytes(byte [] array, int offset)
    {
        super.writeBytes(array, offset);
        array[offset] = (byte) (sid + ptgClass);
    }

    public String toFormulaString(Workbook book)
    {
        return "ERR#";
    }

    public Object clone() {
      MemErrPtg ptg = new MemErrPtg();
      ptg.setReserved(getReserved());
      ptg.setSubexpressionLength(getSubexpressionLength());
      return ptg;
    }
}