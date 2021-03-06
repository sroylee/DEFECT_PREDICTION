package org.apache.synapse.core;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.statistics.StatisticsCollector;

import java.util.concurrent.ExecutorService;

/**
 * The SynapseEnvironment allows access into the the host SOAP engine. It allows
 * the sending of messages, classloader access etc.
 */
public interface SynapseEnvironment {

    /**
     * This method injects a new message into the Synapse engine. This is used by
     * the underlying SOAP engine to inject messages into Synapse for mediation.
     * e.g. The SynapseMessageReceiver used by Axis2 invokes this to inject new messages
     *
     * @param smc - Synapse MessageContext to be injected
     * @return boolean true if the message processing should be continued
     *  and false if it should be aborted
     */
    public boolean injectMessage(MessageContext smc);

    /**
     * This method injects a new message into the synapse engine for the mediation
     * by the specified sequence. This is used by custom mediation tasks like splitting message
     * in EIP mediations. This method will do the mediation asynchronouslly using a separate
     * thread from the environment thread pool
     *
     * @param smc - Synapse message context to be injected
     * @param seq - Sequence to be used for mediation
     */
    public void injectAsync(MessageContext smc, SequenceMediator seq);

    /**
     * This method allows a message to be sent through the underlying SOAP engine. This will
     * send request messages on (forward), and send the response messages back to the client
     *
     * @param endpoint  - Endpoint to be used for sending
     * @param smc       - Synapse MessageContext to be sent
     */
    public void send(EndpointDefinition endpoint, MessageContext smc);

    /**
     * Creates a new Synapse <code>MessageContext</code> instance.
     *
     * @return a MessageContext
     */
    public MessageContext createMessageContext();

    /**
     * This method returns the StatisticsCollector
     *
     * @return Returns the StatisticsCollector
     */
    public StatisticsCollector getStatisticsCollector();

    /**
     * To set the StatisticsCollector to the environment
     *
     * @param statisticsCollector - StatisticsCollector to be set
     */
    public void setStatisticsCollector(StatisticsCollector statisticsCollector);

    /**
     * This is used by anyone who needs access to a SynapseThreadPool.
     * It offers the ability to start work.
     * 
     * @return Returns the ExecutorService
     */
     public ExecutorService getExecutorService();

    /**
     * Has the Synapse Environment properly initialized?
     * @return true if the environment is ready for processing
     */
    public boolean isInitialized();

    /**
     * Set the environment as ready for message processing
     * @param state true means ready for processing
     */
    public void setInitialized(boolean state);
}
