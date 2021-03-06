package org.apache.tools.ant.taskdefs.email;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.tools.ant.ProjectComponent;

/**
 * Class representing an email message.
 *
 * @since Ant 1.5
 */
public class Message extends ProjectComponent {
    private File messageSource = null;
    private StringBuffer buffer = new StringBuffer();
    private String mimeType = "text/plain";
    private boolean specified = false;
    private String charset = null;

    /** Creates a new empty message  */
    public Message() {
    }


    /**
     * Creates a new message based on the given string
     *
     * @param text the message
     */
    public Message(String text) {
        addText(text);
    }


    /**
     * Creates a new message using the contents of the given file.
     *
     * @param file the source of the message
     */
    public Message(File file) {
        messageSource = file;
    }


    /**
     * Adds a textual part of the message
     *
     * @param text some text to add
     */
    public void addText(String text) {
        buffer.append(text);
    }


    /**
     * Sets the source file of the message
     *
     * @param src the source of the message
     */
    public void setSrc(File src) {
        this.messageSource = src;
    }


    /**
     * Sets the content type for the message
     *
     * @param mimeType a mime type e.g. "text/plain"
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
        specified = true;
    }


    /**
     * Returns the content type
     *
     * @return the mime type
     */
    public String getMimeType() {
        return mimeType;
    }


    /**
     * Prints the message onto an output stream
     *
     * @param ps The print stream to write to
     * @throws IOException if an error occurs
     */
    public void print(PrintStream ps)
         throws IOException {
        PrintWriter out
            = charset != null ? new PrintWriter(new OutputStreamWriter(ps, charset))
                              : new PrintWriter(ps);
        if (messageSource != null) {
            FileReader freader = new FileReader(messageSource);

            try {
                BufferedReader in = new BufferedReader(freader);
                String line = null;
                while ((line = in.readLine()) != null) {
                    out.println(getProject().replaceProperties(line));
                }
            } finally {
                freader.close();
            }
        } else {
            out.println(getProject().replaceProperties(buffer.substring(0)));
        }
        out.flush();
    }


    /**
     * Returns true if the mimeType has been set.
     *
     * @return false if the default value is in use
     */
    public boolean isMimeTypeSpecified() {
        return specified;
    }

    /**
     * Sets the character set of mail message.
     * Will be ignored if mimeType contains ....; Charset=... substring.
     * @param charset the character set name.
     * @since Ant 1.6
     */
    public void setCharset(String charset) {
      this.charset = charset;
    }
    /**
     * Returns the charset of mail message.
     *
     * @return Charset of mail message.
     * @since Ant 1.6
     */
    public String getCharset() {
      return charset;
    }
}

