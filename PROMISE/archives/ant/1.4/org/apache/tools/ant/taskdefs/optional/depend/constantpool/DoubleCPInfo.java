package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * The constant pool entry subclass used to represent double constant values.
 * 
 * @author Conor MacNeill
 */
public class DoubleCPInfo extends ConstantCPInfo {
    public DoubleCPInfo() {
        super(CONSTANT_Double, 2);
    }

    /**
     * read a constant pool entry from a class stream.
     * 
     * @param cpStream the DataInputStream which contains the constant pool entry to be read.
     * 
     * @throws IOException if there is a problem reading the entry from the stream.
     */
    public void read(DataInputStream cpStream) throws IOException {
        setValue(new Double(cpStream.readDouble()));
    } 

    /**
     * Print a readable version of the constant pool entry.
     * 
     * @return the string representation of this constant pool entry.
     */
    public String toString() {
        return "Double Constant Pool Entry: " + getValue();
    } 

}
