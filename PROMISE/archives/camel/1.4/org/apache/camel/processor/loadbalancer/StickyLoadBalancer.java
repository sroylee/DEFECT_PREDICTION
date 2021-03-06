package org.apache.camel.processor.loadbalancer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;

/**
 * Implements a sticky load balancer using an {@link Expression} to calculate
 * a correlation key to perform the sticky load balancing; rather like jsessionid in the web
 * or JMSXGroupID in JMS.
 *
 * @version $Revision: 659849 $
 */
public class StickyLoadBalancer extends QueueLoadBalancer {
    private Expression<Exchange> correlationExpression;
    private QueueLoadBalancer loadBalancer;
    private int numberOfHashGroups = 64 * 1024;
    private final Map<Object, Processor> stickyMap = new HashMap<Object, Processor>();

    public StickyLoadBalancer() {
        this.loadBalancer = new RoundRobinLoadBalancer();
    }

    public StickyLoadBalancer(Expression<Exchange> correlationExpression) {
        this(correlationExpression, new RoundRobinLoadBalancer());
    }

    public StickyLoadBalancer(Expression<Exchange> correlationExpression, QueueLoadBalancer loadBalancer) {
        this.correlationExpression = correlationExpression;
        this.loadBalancer = loadBalancer;
    }

    public void setCorrelationExpression(Expression<Exchange> correlationExpression) {
        this.correlationExpression = correlationExpression;
    }

    public void setLoadBalancer(QueueLoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    protected synchronized Processor chooseProcessor(List<Processor> processors, Exchange exchange) {
        Object value = correlationExpression.evaluate(exchange);
        Object key = getStickyKey(value);

        Processor processor;
        synchronized (stickyMap) {
            processor = stickyMap.get(key);
            if (processor == null) {
                processor = loadBalancer.chooseProcessor(processors, exchange);
                stickyMap.put(key, processor);
            }
        }
        return processor;
    }

    @Override
    public void removeProcessor(Processor processor) {
        synchronized (stickyMap) {
            Iterator<Map.Entry<Object, Processor>> iter = stickyMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Object, Processor> entry = iter.next();
                if (processor.equals(entry.getValue())) {
                    iter.remove();
                }
            }
        }
        super.removeProcessor(processor);
    }


    public int getNumberOfHashGroups() {
        return numberOfHashGroups;
    }

    public void setNumberOfHashGroups(int numberOfHashGroups) {
        this.numberOfHashGroups = numberOfHashGroups;
    }


    /**
     * A strategy to create the key for the sticky load balancing map.
     * The default implementation uses the hash code of the value
     * then modulos by the numberOfHashGroups to avoid the sticky map getting too big
     *
     * @param value the correlation value
     * @return the key to be used in the sticky map
     */
    protected Object getStickyKey(Object value) {
        int hashCode = 37;
        if (value != null) {
            hashCode = value.hashCode();
        }
        if (numberOfHashGroups > 0) {
            hashCode = hashCode % numberOfHashGroups;
        }
        return hashCode;
    }
}
