package org.apache.camel.component.jms;

import java.io.File;
import java.util.Enumeration;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

import org.apache.camel.impl.DefaultMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a {@link org.apache.camel.Message} for working with JMS
 * 
 * @version $Revision:520964 $
 */
public class JmsMessage extends DefaultMessage {
    private static final transient Log LOG = LogFactory.getLog(JmsMessage.class);
    private Message jmsMessage;

    public JmsMessage() {
    }

    public JmsMessage(Message jmsMessage) {
        setJmsMessage(jmsMessage);
    }

    @Override
    public String toString() {
        if (jmsMessage != null) {
            return "JmsMessage: " + jmsMessage;
        } else {
            return "JmsMessage: " + getBody();
        }
    }

    /**
     * Returns the underlying JMS message
     * 
     * @return the underlying JMS message
     */
    public Message getJmsMessage() {
        return jmsMessage;
    }

    public void setJmsMessage(Message jmsMessage) {
        this.jmsMessage = jmsMessage;
        try {
            String id = getDestinationAsString(jmsMessage.getJMSDestination());
            id += getSanitizedString(jmsMessage.getJMSMessageID());
            setMessageId(id);
        } catch (JMSException e) {
            LOG.error("Failed to get message id from message " + jmsMessage, e);
        }
    }

    public Object getHeader(String name) {
        Object answer = null;
        
        if (jmsMessage != null && !name.startsWith("JMS")) {
            try {
                answer = jmsMessage.getObjectProperty(name);
            } catch (JMSException e) {
                throw new MessagePropertyAccessException(name, e);
            }
        }
        if (answer == null) {
            answer = super.getHeader(name);
        }
        return answer;
    }

    @Override
    public JmsMessage newInstance() {
        return new JmsMessage();
    }

    @Override
    protected Object createBody() {
        if (jmsMessage != null && getExchange() instanceof JmsExchange) {
            JmsExchange exchange = (JmsExchange)getExchange();
            return exchange.getBinding().extractBodyFromJms(exchange, jmsMessage);
        }
        return null;
    }

    @Override
    protected void populateInitialHeaders(Map<String, Object> map) {
        if (jmsMessage != null) {
            try {
                map.put("JMSCorrelationID", jmsMessage.getJMSCorrelationID());
                map.put("JMSDeliveryMode", jmsMessage.getJMSDeliveryMode());
                map.put("JMSDestination", jmsMessage.getJMSDestination());
                map.put("JMSExpiration", jmsMessage.getJMSExpiration());
                map.put("JMSMessageID", jmsMessage.getJMSMessageID());
                map.put("JMSPriority", jmsMessage.getJMSPriority());
                map.put("JMSRedelivered", jmsMessage.getJMSRedelivered());
                map.put("JMSReplyTo", jmsMessage.getJMSReplyTo());
                map.put("JMSTimestamp", jmsMessage.getJMSTimestamp());
                map.put("JMSType", jmsMessage.getJMSType());

                map.put("JMSXGroupID", jmsMessage.getStringProperty("JMSXGroupID"));

            }
            catch (JMSException e) {
                throw new MessageJMSPropertyAccessException(e);
            }

            Enumeration names;
            try {
                names = jmsMessage.getPropertyNames();
            } catch (JMSException e) {
                throw new MessagePropertyNamesAccessException(e);
            }
            while (names.hasMoreElements()) {
                String name = names.nextElement().toString();
                try {
                    Object value = jmsMessage.getObjectProperty(name);
                    map.put(name, value);
                } catch (JMSException e) {
                    throw new MessagePropertyAccessException(name, e);
                }
            }
        }
    }

    private String getDestinationAsString(Destination destination) throws JMSException {
        String result = "";
        if (destination == null) {
            result = "null destination!";
        } else if (destination instanceof Topic) {
            result += "topic" + File.separator + getSanitizedString(((Topic)destination).getTopicName());
        } else {
            result += "queue" + File.separator + getSanitizedString(((Queue)destination).getQueueName());
        }
        result += File.separator;
        return result;
    }

    private String getSanitizedString(Object value) {
        return value != null ? value.toString().replaceAll("[^a-zA-Z0-9\\.\\_\\-]", "_") : "";
    }
}
