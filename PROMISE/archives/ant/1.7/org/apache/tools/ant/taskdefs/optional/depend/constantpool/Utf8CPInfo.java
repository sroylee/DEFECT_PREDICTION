package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * A UTF8 Constant Pool Entry.
 *
 */
public class Utf8CPInfo extends ConstantPoolEntry {
    /** The String value of the UTF-8 entry */
    private String value;

    /** Constructor.  */
    public Utf8CPInfo() {
        super(CONSTANT_UTF8, 1);
    }

    /**
     * read a constant pool entry from a class stream.
     *
     * @param cpStream the DataInputStream which contains the constant pool
     *      entry to be read.
     * @exception IOException if there is a problem reading the entry from
     *      the stream.
     */
    public void read(DataInputStream cpStream) throws IOException {
        value = cpStream.readUTF();
    }

    /**
     * Print a readable version of the constant pool entry.
     *
     * @return the string representation of this constant pool entry.
     */
    public String toString() {
        return "UTF8 Value = " + value;
    }

    /**
     * Get the string value of the UTF-8 entry
     *
     * @return the UTF-8 value as a Java string
     */
    public String getValue() {
        return value;
    }

}

