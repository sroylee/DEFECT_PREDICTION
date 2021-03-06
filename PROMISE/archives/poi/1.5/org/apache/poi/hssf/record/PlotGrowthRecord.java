package org.apache.poi.hssf.record;



import org.apache.poi.util.BitField;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.StringUtil;
import org.apache.poi.util.HexDump;

/**
 * The plot growth record specifies the scaling factors used when a font is scaled.
 * NOTE: This source is automatically generated please do not modify this file.  Either subclass or
 *       remove the record in src/records/definitions.

 * @author Glen Stampoultzis (glens at apache.org)
 */
public class PlotGrowthRecord
    extends Record
{
    public final static short      sid                             = 0x1064;
    private  int        field_1_horizontalScale;
    private  int        field_2_verticalScale;


    public PlotGrowthRecord()
    {

    }

    /**
     * Constructs a PlotGrowth record and sets its fields appropriately.
     *
     * @param id    id must be 0x1064 or an exception
     *              will be throw upon validation
     * @param size  size the size of the data area of the record
     * @param data  data of the record (should not contain sid/len)
     */

    public PlotGrowthRecord(short id, short size, byte [] data)
    {
        super(id, size, data);
    }

    /**
     * Constructs a PlotGrowth record and sets its fields appropriately.
     *
     * @param id    id must be 0x1064 or an exception
     *              will be throw upon validation
     * @param size  size the size of the data area of the record
     * @param data  data of the record (should not contain sid/len)
     * @param offset of the record's data
     */

    public PlotGrowthRecord(short id, short size, byte [] data, int offset)
    {
        super(id, size, data, offset);
    }

    /**
     * Checks the sid matches the expected side for this record
     *
     * @param id   the expected sid.
     */
    protected void validateSid(short id)
    {
        if (id != sid)
        {
            throw new RecordFormatException("Not a PlotGrowth record");
        }
    }

    protected void fillFields(byte [] data, short size, int offset)
    {
        field_1_horizontalScale         = LittleEndian.getInt(data, 0x0 + offset);
        field_2_verticalScale           = LittleEndian.getInt(data, 0x4 + offset);

    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[PlotGrowth]\n");

        buffer.append("    .horizontalScale      = ")
            .append("0x")
            .append(HexDump.toHex((int)getHorizontalScale()))
            .append(" (").append(getHorizontalScale()).append(" )\n");

        buffer.append("    .verticalScale        = ")
            .append("0x")
            .append(HexDump.toHex((int)getVerticalScale()))
            .append(" (").append(getVerticalScale()).append(" )\n");

        buffer.append("[/PlotGrowth]\n");
        return buffer.toString();
    }

    public int serialize(int offset, byte[] data)
    {
        LittleEndian.putShort(data, 0 + offset, sid);
        LittleEndian.putShort(data, 2 + offset, (short)(getRecordSize() - 4));

        LittleEndian.putInt(data, 4 + offset, field_1_horizontalScale);
        LittleEndian.putInt(data, 8 + offset, field_2_verticalScale);

        return getRecordSize();
    }

    /**
     * Size of record (exluding 4 byte header)
     */
    public int getRecordSize()
    {
        return 4 + 4 + 4;
    }

    public short getSid()
    {
        return this.sid;
    }


    /**
     * Get the horizontalScale field for the PlotGrowth record.
     */
    public int getHorizontalScale()
    {
        return field_1_horizontalScale;
    }

    /**
     * Set the horizontalScale field for the PlotGrowth record.
     */
    public void setHorizontalScale(int field_1_horizontalScale)
    {
        this.field_1_horizontalScale = field_1_horizontalScale;
    }

    /**
     * Get the verticalScale field for the PlotGrowth record.
     */
    public int getVerticalScale()
    {
        return field_2_verticalScale;
    }

    /**
     * Set the verticalScale field for the PlotGrowth record.
     */
    public void setVerticalScale(int field_2_verticalScale)
    {
        this.field_2_verticalScale = field_2_verticalScale;
    }






