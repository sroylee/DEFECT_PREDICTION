package org.apache.camel.component.mina;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.impl.DefaultExchange;
import org.apache.mina.common.IoSession;

/**
 * A {@link Exchange} for Apache MINA.
 * 
 * @version $Revision: 729204 $
 */
public class MinaExchange extends DefaultExchange {

    private IoSession session;

    public MinaExchange(CamelContext camelContext, ExchangePattern pattern, IoSession session) {
        super(camelContext, pattern);
        this.session = session;
    }
    
    public MinaExchange(DefaultExchange parent, IoSession session) {
        super(parent);
        this.session = session;
    }


    /**
     * The associated Mina session, is <b>only</b> available for {@link MinaConsumer}.
     * 
     * @return the Mina session.
     */
    public IoSession getSession() {
        return session;
    }
    
    @Override
    public Exchange newInstance() {
        return new MinaExchange(this, getSession());
    }


}
