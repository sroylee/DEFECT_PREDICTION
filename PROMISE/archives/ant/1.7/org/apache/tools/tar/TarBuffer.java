package org.apache.tools.tar;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * The TarBuffer class implements the tar archive concept
 * of a buffered input stream. This concept goes back to the
 * days of blocked tape drives and special io devices. In the
 * Java universe, the only real function that this class
 * performs is to ensure that files have the correct "block"
 * size, or other tars will complain.
 * <p>
 * You should never have a need to access this class directly.
 * TarBuffers are created by Tar IO Streams.
 *
 */

public class TarBuffer {

    /** Default record size */
    public static final int DEFAULT_RCDSIZE = (512);

    /** Default block size */
    public static final int DEFAULT_BLKSIZE = (DEFAULT_RCDSIZE * 20);

    private InputStream     inStream;
    private OutputStream    outStream;
    private byte[]          blockBuffer;
    private int             currBlkIdx;
    private int             currRecIdx;
    private int             blockSize;
    private int             recordSize;
    private int             recsPerBlock;
    private boolean         debug;

    /**
     * Constructor for a TarBuffer on an input stream.
     * @param inStream the input stream to use
     */
    public TarBuffer(InputStream inStream) {
        this(inStream, TarBuffer.DEFAULT_BLKSIZE);
    }

    /**
     * Constructor for a TarBuffer on an input stream.
     * @param inStream the input stream to use
     * @param blockSize the block size to use
     */
    public TarBuffer(InputStream inStream, int blockSize) {
        this(inStream, blockSize, TarBuffer.DEFAULT_RCDSIZE);
    }

    /**
     * Constructor for a TarBuffer on an input stream.
     * @param inStream the input stream to use
     * @param blockSize the block size to use
     * @param recordSize the record size to use
     */
    public TarBuffer(InputStream inStream, int blockSize, int recordSize) {
        this.inStream = inStream;
        this.outStream = null;

        this.initialize(blockSize, recordSize);
    }

    /**
     * Constructor for a TarBuffer on an output stream.
     * @param outStream the output stream to use
     */
    public TarBuffer(OutputStream outStream) {
        this(outStream, TarBuffer.DEFAULT_BLKSIZE);
    }

    /**
     * Constructor for a TarBuffer on an output stream.
     * @param outStream the output stream to use
     * @param blockSize the block size to use
     */
    public TarBuffer(OutputStream outStream, int blockSize) {
        this(outStream, blockSize, TarBuffer.DEFAULT_RCDSIZE);
    }

    /**
     * Constructor for a TarBuffer on an output stream.
     * @param outStream the output stream to use
     * @param blockSize the block size to use
     * @param recordSize the record size to use
     */
    public TarBuffer(OutputStream outStream, int blockSize, int recordSize) {
        this.inStream = null;
        this.outStream = outStream;

        this.initialize(blockSize, recordSize);
    }

    /**
     * Initialization common to all constructors.
     */
    private void initialize(int blockSize, int recordSize) {
        this.debug = false;
        this.blockSize = blockSize;
        this.recordSize = recordSize;
        this.recsPerBlock = (this.blockSize / this.recordSize);
        this.blockBuffer = new byte[this.blockSize];

        if (this.inStream != null) {
            this.currBlkIdx = -1;
            this.currRecIdx = this.recsPerBlock;
        } else {
            this.currBlkIdx = 0;
            this.currRecIdx = 0;
        }
    }

    /**
     * Get the TAR Buffer's block size. Blocks consist of multiple records.
     * @return the block size
     */
    public int getBlockSize() {
        return this.blockSize;
    }

    /**
     * Get the TAR Buffer's record size.
     * @return the record size
     */
    public int getRecordSize() {
        return this.recordSize;
    }

    /**
     * Set the debugging flag for the buffer.
     *
     * @param debug If true, print debugging output.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Determine if an archive record indicate End of Archive. End of
     * archive is indicated by a record that consists entirely of null bytes.
     *
     * @param record The record data to check.
     * @return true if the record data is an End of Archive
     */
    public boolean isEOFRecord(byte[] record) {
        for (int i = 0, sz = this.getRecordSize(); i < sz; ++i) {
            if (record[i] != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Skip over a record on the input stream.
     * @throws IOException on error
     */
    public void skipRecord() throws IOException {
        if (this.debug) {
            System.err.println("SkipRecord: recIdx = " + this.currRecIdx
                               + " blkIdx = " + this.currBlkIdx);
        }

        if (this.inStream == null) {
            throw new IOException("reading (via skip) from an output buffer");
        }

        if (this.currRecIdx >= this.recsPerBlock) {
            if (!this.readBlock()) {
            }
        }

        this.currRecIdx++;
    }

    /**
     * Read a record from the input stream and return the data.
     *
     * @return The record data.
     * @throws IOException on error
     */
    public byte[] readRecord() throws IOException {
        if (this.debug) {
            System.err.println("ReadRecord: recIdx = " + this.currRecIdx
                               + " blkIdx = " + this.currBlkIdx);
        }

        if (this.inStream == null) {
            throw new IOException("reading from an output buffer");
        }

        if (this.currRecIdx >= this.recsPerBlock) {
            if (!this.readBlock()) {
                return null;
            }
        }

        byte[] result = new byte[this.recordSize];

        System.arraycopy(this.blockBuffer,
                         (this.currRecIdx * this.recordSize), result, 0,
                         this.recordSize);

        this.currRecIdx++;

        return result;
    }

    /**
     * @return false if End-Of-File, else true
     */
    private boolean readBlock() throws IOException {
        if (this.debug) {
            System.err.println("ReadBlock: blkIdx = " + this.currBlkIdx);
        }

        if (this.inStream == null) {
            throw new IOException("reading from an output buffer");
        }

        this.currRecIdx = 0;

        int offset = 0;
        int bytesNeeded = this.blockSize;

        while (bytesNeeded > 0) {
            long numBytes = this.inStream.read(this.blockBuffer, offset,
                                               bytesNeeded);

            if (numBytes == -1) {
                if (offset == 0) {
                    return false;
                }

                Arrays.fill(blockBuffer, offset, offset + bytesNeeded, (byte) 0);

                break;
            }

            offset += numBytes;
            bytesNeeded -= numBytes;

            if (numBytes != this.blockSize) {
                if (this.debug) {
                    System.err.println("ReadBlock: INCOMPLETE READ "
                                       + numBytes + " of " + this.blockSize
                                       + " bytes read.");
                }
            }
        }

        this.currBlkIdx++;

        return true;
    }

    /**
     * Get the current block number, zero based.
     *
     * @return The current zero based block number.
     */
    public int getCurrentBlockNum() {
        return this.currBlkIdx;
    }

    /**
     * Get the current record number, within the current block, zero based.
     * Thus, current offset = (currentBlockNum * recsPerBlk) + currentRecNum.
     *
     * @return The current zero based record number.
     */
    public int getCurrentRecordNum() {
        return this.currRecIdx - 1;
    }

    /**
     * Write an archive record to the archive.
     *
     * @param record The record data to write to the archive.
     * @throws IOException on error
     */
    public void writeRecord(byte[] record) throws IOException {
        if (this.debug) {
            System.err.println("WriteRecord: recIdx = " + this.currRecIdx
                               + " blkIdx = " + this.currBlkIdx);
        }

        if (this.outStream == null) {
            throw new IOException("writing to an input buffer");
        }

        if (record.length != this.recordSize) {
            throw new IOException("record to write has length '"
                                  + record.length
                                  + "' which is not the record size of '"
                                  + this.recordSize + "'");
        }

        if (this.currRecIdx >= this.recsPerBlock) {
            this.writeBlock();
        }

        System.arraycopy(record, 0, this.blockBuffer,
                         (this.currRecIdx * this.recordSize),
                         this.recordSize);

        this.currRecIdx++;
    }

    /**
     * Write an archive record to the archive, where the record may be
     * inside of a larger array buffer. The buffer must be "offset plus
     * record size" long.
     *
     * @param buf The buffer containing the record data to write.
     * @param offset The offset of the record data within buf.
     * @throws IOException on error
     */
    public void writeRecord(byte[] buf, int offset) throws IOException {
        if (this.debug) {
            System.err.println("WriteRecord: recIdx = " + this.currRecIdx
                               + " blkIdx = " + this.currBlkIdx);
        }

        if (this.outStream == null) {
            throw new IOException("writing to an input buffer");
        }

        if ((offset + this.recordSize) > buf.length) {
            throw new IOException("record has length '" + buf.length
                                  + "' with offset '" + offset
                                  + "' which is less than the record size of '"
                                  + this.recordSize + "'");
        }

        if (this.currRecIdx >= this.recsPerBlock) {
            this.writeBlock();
        }

        System.arraycopy(buf, offset, this.blockBuffer,
                         (this.currRecIdx * this.recordSize),
                         this.recordSize);

        this.currRecIdx++;
    }

    /**
     * Write a TarBuffer block to the archive.
     */
    private void writeBlock() throws IOException {
        if (this.debug) {
            System.err.println("WriteBlock: blkIdx = " + this.currBlkIdx);
        }

        if (this.outStream == null) {
            throw new IOException("writing to an input buffer");
        }

        this.outStream.write(this.blockBuffer, 0, this.blockSize);
        this.outStream.flush();

        this.currRecIdx = 0;
        this.currBlkIdx++;
    }

    /**
     * Flush the current data block if it has any data in it.
     */
    private void flushBlock() throws IOException {
        if (this.debug) {
            System.err.println("TarBuffer.flushBlock() called.");
        }

        if (this.outStream == null) {
            throw new IOException("writing to an input buffer");
        }

        if (this.currRecIdx > 0) {
            this.writeBlock();
        }
    }

    /**
     * Close the TarBuffer. If this is an output buffer, also flush the
     * current block before closing.
     * @throws IOException on error
     */
    public void close() throws IOException {
        if (this.debug) {
            System.err.println("TarBuffer.closeBuffer().");
        }

        if (this.outStream != null) {
            this.flushBlock();

            if (this.outStream != System.out
                    && this.outStream != System.err) {
                this.outStream.close();

                this.outStream = null;
            }
        } else if (this.inStream != null) {
            if (this.inStream != System.in) {
                this.inStream.close();

                this.inStream = null;
            }
        }
    }
}
