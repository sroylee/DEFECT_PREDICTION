package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;

/**
 * The header mediator is able to set a given value as a SOAP header, or remove a given
 * header from the current message instance. This supports the headers currently
 * supported by the HeaderType class. If an expression is supplied, its runtime value
 * is evaluated using the current message. Unless the action is set to remove, the
 * default behaviour of this mediator is to set a header value.
 */
public class HeaderMediator extends AbstractMediator {

    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;

    /** The qName of the header @see HeaderType */
    private QName qName = null;
    /** The literal value to be set as the header (if one was specified) */
    private String value = null;
    /** Set the header (ACTION_SET) or remove it (ACTION_REMOVE). Defaults to ACTION_SET */
    private int action = ACTION_SET;
    /** An expression which should be evaluated, and the result set as the header value */
    private AXIOMXPath expression = null;

    /**
     * Sets/Removes a SOAP header on the current message
     *
     * @param synCtx the current message which is altered as necessary
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Header mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        if (action == ACTION_SET) {

            String value = (getValue() != null ? getValue() :
                    Axis2MessageContext.getStringValue(expression, synCtx));

            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Set SOAP header : " + qName + " to : " + value);
            }

            if (qName.getNamespaceURI() == null || "".equals(qName.getNamespaceURI())) {

                if (SynapseConstants.HEADER_TO.equals(qName.getLocalPart())) {
                    synCtx.setTo(new EndpointReference(value));
                } else if (SynapseConstants.HEADER_FROM.equals(qName.getLocalPart())) {
                    synCtx.setFrom(new EndpointReference(value));
                } else if (SynapseConstants.HEADER_ACTION.equals(qName.getLocalPart())) {
                    synCtx.setWSAAction(value);
                } else if (SynapseConstants.HEADER_FAULT.equals(qName.getLocalPart())) {
                    synCtx.setFaultTo(new EndpointReference(value));
                } else if (SynapseConstants.HEADER_REPLY_TO.equals(qName.getLocalPart())) {
                    synCtx.setReplyTo(new EndpointReference(value));
                } else {
                    addCustomHeader(synCtx);
                }
            } else {
                addCustomHeader(synCtx);
            }

        } else {

             if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Removing SOAP Header : " + qName);
            }

            if (qName.getNamespaceURI() == null || "".equals(qName.getNamespaceURI())) {

                if (SynapseConstants.HEADER_TO.equals(qName.getLocalPart())) {
                    synCtx.setTo(null);
                } else if (SynapseConstants.HEADER_FROM.equals(qName.getLocalPart())) {
                    synCtx.setFrom(null);
                } else if (SynapseConstants.HEADER_ACTION.equals(qName.getLocalPart())) {
                    synCtx.setWSAAction(null);
                } else if (SynapseConstants.HEADER_FAULT.equals(qName.getLocalPart())) {
                    synCtx.setFaultTo(null);
                } else if (SynapseConstants.HEADER_REPLY_TO.equals(qName.getLocalPart())) {
                    synCtx.setReplyTo(null);
                } else {
                    SOAPEnvelope envelope = synCtx.getEnvelope();
                    if (envelope != null) {
                        SOAPHeader header = envelope.getHeader();
                        if (header != null) {
                            removeFromHeaderList(header.
                                getHeaderBlocksWithNSURI(""));
                        }
                    }
                }

            } else {
                SOAPEnvelope envelope = synCtx.getEnvelope();
                if (envelope != null) {
                    SOAPHeader header = envelope.getHeader();
                    if (header != null) {
                        removeFromHeaderList(header.
                            getHeaderBlocksWithNSURI(qName.getNamespaceURI()));
                    }
                }
            }
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Header mediator");
        }
        return true;
    }

    private void addCustomHeader(MessageContext synCtx) {
        SOAPEnvelope env = synCtx.getEnvelope();
        if (env == null) {
            return;
        }
        SOAPFactory fac = (SOAPFactory) env.getOMFactory();
        SOAPHeader header = env.getHeader();
        if (header == null) {
            header = fac.createSOAPHeader(env);
        }
        SOAPHeaderBlock hb = header.addHeaderBlock(qName.getLocalPart(),
                fac.createOMNamespace(qName.getNamespaceURI(), qName.getPrefix()));
        hb.setText(value);
    }

    private void removeFromHeaderList(List headersList) {
        if (headersList == null || headersList.isEmpty()) {
            return;
        }
        for ( Iterator iter = headersList.iterator();iter.hasNext();) {
            Object o = iter.next();
            if (o instanceof SOAPHeaderBlock) {
                SOAPHeaderBlock header = (SOAPHeaderBlock) o;
                if (header.getLocalName().equals(qName.getLocalPart())) {
                    header.detach();
                }
            } else if (o instanceof OMElement) {
                OMElement omElem = (OMElement) o;
                if (omElem.getLocalName().equals(qName.getLocalPart())) {
                    omElem.detach();
                }
            }
        }
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public QName getQName() {
        return qName;
    }

    public void setQName(QName qName) {
        this.qName = qName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AXIOMXPath getExpression() {
        return expression;
    }

    public void setExpression(AXIOMXPath expression) {
        this.expression = expression;
    }
}
