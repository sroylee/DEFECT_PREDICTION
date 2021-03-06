package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.eip.splitter.CloneMediator;
import org.apache.synapse.mediators.eip.Target;

import java.util.Iterator;

/**
 * This will serialize the CloneMediator to the xml configuration as specified bellow
 *
 * <pre>
 *  &lt;clone [continueParent=(true | false)]&gt;
 *   &lt;target [to="uri"] [soapAction="qname"] [sequence="sequence_ref"]
 *          [endpoint="endpoint_ref"]&gt;
 *     &lt;sequence&gt;
 *       (mediator)+
 *     &lt;/sequence&gt;?
 *     &lt;endpoint&gt;
 *       endpoint
 *     &lt;/endpoint&gt;?
 *   &lt;/target&gt;+
 * &lt;/clone&gt;
 * </pre>
 */
public class CloneMediatorSerializer extends AbstractMediatorSerializer {

    /**
     * This method will implement the serializeMediator method of the MediatorSerializer interface
     * and implements the serialization of CloneMediator to its configuration
     *
     * @param parent OMElement describing the parent element to which the newlly generated
     *          clone element should be attached as a child, if provided
     * @param m Mediator of the type CloneMediator which is subjected to the serialization
     * @return OMElement serialized in to xml from the given parameters
     */
    public OMElement serializeMediator(OMElement parent, Mediator m) {

        OMElement cloneElem = fac.createOMElement("clone", synNS);
        saveTracingState(cloneElem, m);

        CloneMediator clone = (CloneMediator) m;
        if (clone.isContinueParent()) {
            cloneElem.addAttribute("continueParent", Boolean.toString(true), nullNS);
        }

        for (Iterator itr = clone.getTargets().iterator(); itr.hasNext();) {
            Object o = itr.next();
            if (o instanceof Target) {
                cloneElem.addChild(TargetSerializer.serializeTarget((Target) o));
            }
        }

        if (parent != null) {
            parent.addChild(cloneElem);
        }

        return cloneElem;
    }

    /**
     * This method will implement the getMediatorClassName method of the
     * MediatorSerializer interface
     * 
     * @return full class name of the Mediator which is serialized by this Serializer
     */
    public String getMediatorClassName() {
        return CloneMediator.class.getName();
    }
}
