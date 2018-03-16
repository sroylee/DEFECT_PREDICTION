package org.apache.synapse.util;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDataSourceExt;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.ds.OMDataSourceExtBase;
import org.apache.axiom.om.impl.MTOMXMLStreamWriter;
import org.apache.axiom.om.impl.llom.OMSourcedElementImpl;
import org.apache.axiom.om.impl.serialize.StreamingOMSerializer;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.synapse.transport.base.BaseConstants;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

public class TextFileDataSource extends OMDataSourceExtBase {
    private final TemporaryData temporaryData;
    private final Charset charset;

    public TextFileDataSource(TemporaryData temporaryData, Charset charset) {
        this.temporaryData = temporaryData;
        this.charset = charset;
    }
    
    public static OMSourcedElement createOMSourcedElement(TemporaryData temporaryData, Charset charset) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        TextFileDataSource txtFileDS = new TextFileDataSource(temporaryData, charset);
        return new OMSourcedElementImpl(BaseConstants.DEFAULT_TEXT_WRAPPER, fac, txtFileDS);
    }

    @Override
    public void serialize(OutputStream out, OMOutputFormat format) throws XMLStreamException {
        XMLStreamWriter writer = new MTOMXMLStreamWriter(out, format);
        serialize(writer);
        writer.flush();
    }

    @Override
    public void serialize(Writer writer, OMOutputFormat format) throws XMLStreamException {
        MTOMXMLStreamWriter xmlWriter =
            new MTOMXMLStreamWriter(StAXUtils.createXMLStreamWriter(writer));
        xmlWriter.setOutputFormat(format);
        serialize(xmlWriter);
        xmlWriter.flush();
    }

    @Override
    public void serialize(XMLStreamWriter xmlWriter) throws XMLStreamException {
        StreamingOMSerializer serializer = new StreamingOMSerializer();
        serializer.serialize(getReader(), xmlWriter);
    }

    public XMLStreamReader getReader() throws XMLStreamException {
        InputStream is;
        try {
            is = temporaryData.getInputStream();
        }
        catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
        return new WrappedTextNodeStreamReader(BaseConstants.DEFAULT_TEXT_WRAPPER, new InputStreamReader(is, charset));
    }

    public Object getObject() {
        return temporaryData;
    }

    public boolean isDestructiveRead() {
        return false;
    }

    public boolean isDestructiveWrite() {
        return false;
    }
    
    public byte[] getXMLBytes(String encoding) throws UnsupportedEncodingException {
        throw new UnsupportedOperationException();
    }

    public void close() {
    }

    public OMDataSourceExt copy() {
        return new TextFileDataSource(temporaryData, charset);
    }
}