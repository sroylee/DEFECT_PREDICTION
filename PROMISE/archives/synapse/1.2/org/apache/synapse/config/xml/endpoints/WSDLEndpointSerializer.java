package org.apache.synapse.config.xml.endpoints;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.WSDLEndpoint;
import org.apache.synapse.endpoints.utils.EndpointDefinition;

/**
 * Serializes an WSDL based endpoint to an XML configuration.
 * 
 * @see WSDLEndpointFactory
 */
public class WSDLEndpointSerializer extends EndpointSerializer {

    protected OMElement serializeEndpoint(Endpoint endpoint) {

        if (!(endpoint instanceof WSDLEndpoint)) {
            throw new SynapseException("Invalid endpoint type.");
        }

        fac = OMAbstractFactory.getOMFactory();
        OMElement endpointElement
                = fac.createOMElement("endpoint", SynapseConstants.SYNAPSE_OMNAMESPACE);

        WSDLEndpoint wsdlEndpoint = (WSDLEndpoint) endpoint;
        String name = wsdlEndpoint.getName();
        if (name != null) {
            endpointElement.addAttribute("name", name, null);
        }

        OMElement wsdlElement = fac.createOMElement("wsdl", SynapseConstants.SYNAPSE_OMNAMESPACE);
        String serviceName = wsdlEndpoint.getServiceName();
        if (serviceName != null) {
            wsdlElement.addAttribute("service", serviceName, null);
        }

        String portName = wsdlEndpoint.getPortName();
        if (portName != null) {
            wsdlElement.addAttribute("port", portName, null);
        }

        String uri = wsdlEndpoint.getWsdlURI();
        if (uri != null) {
            wsdlElement.addAttribute("uri", uri, null);
        }

        OMElement wsdlDoc = wsdlEndpoint.getWsdlDoc();
        if (wsdlDoc != null) {
            wsdlElement.addChild(wsdlDoc);
        }

        EndpointDefinition epDefinition = wsdlEndpoint.getEndpoint();
        serializeCommonEndpointProperties(epDefinition, wsdlElement);
        serializeSpecificEndpointProperties(epDefinition, wsdlElement);
        endpointElement.addChild(wsdlElement);

        return endpointElement;
    }

    protected void serializeSpecificEndpointProperties(EndpointDefinition endpointDefinition, OMElement element) {
    }

}