package org.apache.synapse.endpoints;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.FaultHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents the endpoints referred by keys. It does not store the actual referred
 * endpoint as a private variable as it could expire. Therefore, it only stores the key and gets the
 * actual endpoint from the synapse configuration.
 *
 * As this is also an instance of endpoint, this can be used any place, where a normal endpoint is used.
 */
public class IndirectEndpoint implements Endpoint {

    private static final Log log = LogFactory.getLog(IndirectEndpoint.class);

    private String name = null;
    private String key = null;
    private boolean active = true;
    private Endpoint parentEndpoint = null;

    /**
     * This should have a reference to the current message context as it gets the referred endpoint
     * from it.
     */
    private MessageContext currentMsgCtx = null;

    public void send(MessageContext synMessageContext) {
        Endpoint endpoint = synMessageContext.getEndpoint(key);
        if (endpoint == null) {
            handleException("Reference to non-existent endpoint for key : " + key);
        }

        if (endpoint.isActive(synMessageContext)) {
            endpoint.send(synMessageContext);
        } else {

            if (parentEndpoint != null) {
                parentEndpoint.onChildEndpointFail(this, synMessageContext);
            } else {
                Object o = synMessageContext.getFaultStack().pop();
                if (o != null) {
                    ((FaultHandler) o).handleFault(synMessageContext);
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * IndirectEndpoints are active if its referref endpoint is active and vise versa. Therefore,
     * this returns if its referred endpoint is active or not.
     *
     * @param synMessageContext MessageContext of the current message.
     *
     * @return true if the referred endpoint is active. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext) {
        Endpoint endpoint = synMessageContext.getEndpoint(key);
        if (endpoint == null) {
            handleException("Reference to non-existent endpoint for key : " + key);
        }

        return endpoint.isActive(synMessageContext);
    }

    /**
     * Activating or deactivating an IndirectEndpoint is the activating or deactivating its
     * referref endpoint. Therefore, this sets the active state of its referred endpoint.
     *
     * @param active true if active. false otherwise.
     *
     * @param synMessageContext MessageContext of the current message.
     */
    public void setActive(boolean active, MessageContext synMessageContext) {
        Endpoint endpoint = synMessageContext.getEndpoint(key);
        if (endpoint == null) {
            handleException("Reference to non-existent endpoint for key : " + key);
        }

        endpoint.setActive(active, synMessageContext);
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {        

        if (parentEndpoint != null) {
            parentEndpoint.onChildEndpointFail(this, synMessageContext);
        } else {
            Object o = synMessageContext.getFaultStack().pop();
            if (o != null) {
                ((FaultHandler) o).handleFault(synMessageContext);
            }
        }
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
