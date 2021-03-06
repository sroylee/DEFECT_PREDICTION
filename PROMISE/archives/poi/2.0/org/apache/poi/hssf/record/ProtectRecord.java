package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndian;

/**
 * Title: Protect Record<P>
 * Description:  defines whether a sheet or workbook is protected (HSSF DOES NOT SUPPORT ENCRYPTION)<P>
 * (kindly ask the US government to stop having arcane stupid encryption laws and we'll support it) <P>
 * (after all terrorists will all use US-legal encrypton right??)<P>
 * REFERENCE:  PG 373 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @version 2.0-pre
 */

public class ProtectRecord
    extends Record
{
    public final static short sid = 0x12;
    private short             field_1_protect;

    public ProtectRecord()
    {
    }

    /**
     * Constructs a Protect record and sets its fields appropriately.
     *
     * @param id id must be 0x12 or an exception will be throw upon validation
     * @param size size the size of the data area of the record
     * @param data data of the record (should not contain sid/len)
     */

    public ProtectRecord(short id, short size, byte [] data)
    {
        super(id, size, data);
    }

    /**
     * Constructs a Protect record and sets its fields appropriately.
     *
     * @param id id must be 0x12 or an exception will be throw upon validation
     * @param size size the size of the data area of the record
     * @param data data of the record (should not contain sid/len)
     * @param offset of the data
     */

    public ProtectRecord(short id, short size, byte [] data, int offset)
    {
        super(id, size, data, offset);
    }

    protected void validateSid(short id)
    {
        if (id != sid)
        {
            throw new RecordFormatException("NOT A PROTECT RECORD");
        }
    }

    protected void fillFields(byte [] data, short size, int offset)
    {
        field_1_protect = LittleEndian.getShort(data, 0 + offset);
    }

    /**
     * set whether the sheet is protected or not
     * @param protect whether to protect the sheet or not
     */

    public void setProtect(boolean protect)
    {
        if (protect)
        {
            field_1_protect = 1;
        }
        else
        {
            field_1_protect = 0;
        }
    }

    /**
     * get whether the sheet is protected or not
     * @return whether to protect the sheet or not
     */

    public boolean getProtect()
    {
        return (field_1_protect == 1);
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[PROTECT]\n");
	    buffer.append("    .protect         = ").append(getProtect())
            .append("\n");
        buffer.append("[/PROTECT]\n");
        return buffer.toString();
    }

    public int serialize(int offset, byte [] data)
    {
        LittleEndian.putShort(data, 0 + offset, sid);
        LittleEndian.putShort(data, 2 + offset,
        LittleEndian.putShort(data, 4 + offset, field_1_protect);
        return getRecordSize();
    }

    public int getRecordSize()
    {
        return 6;
    }

    public short getSid()
    {
        return this.sid;
    }

    public Object clone() {
        ProtectRecord rec = new ProtectRecord();
        rec.field_1_protect = field_1_protect;
        return rec;
    }
}
