package org.apache.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * will set a limit on the maximum number of message exchanges which can be sent
 * to a processor within a specific time period. <p/> This pattern can be
 * extremely useful if you have some external system which meters access; such
 * as only allowing 100 requests per second; or if huge load can cause a
 * particular systme to malfunction or to reduce its throughput you might want
 * to introduce some throttling.
 * 
 * @version $Revision: 731493 $
 */
public class Throttler extends DelayProcessorSupport {
    private long maximumRequestsPerPeriod;
    private long timePeriodMillis;
    private TimeSlot slot;

    public Throttler(Processor processor, long maximumRequestsPerPeriod) {
        this(processor, maximumRequestsPerPeriod, 1000);
    }

    public Throttler(Processor processor, long maximumRequestsPerPeriod, long timePeriodMillis) {
        super(processor);
        this.maximumRequestsPerPeriod = maximumRequestsPerPeriod;
        this.timePeriodMillis = timePeriodMillis;
    }

    @Override
    public String toString() {
        return "Throttler[requests: " + maximumRequestsPerPeriod + " per: " + timePeriodMillis + " (ms) to: "
               + getProcessor() + "]";
    }

    public long getMaximumRequestsPerPeriod() {
        return maximumRequestsPerPeriod;
    }

    /**
     * Sets the maximum number of requests per time period
     */
    public void setMaximumRequestsPerPeriod(long maximumRequestsPerPeriod) {
        this.maximumRequestsPerPeriod = maximumRequestsPerPeriod;
    }

    public long getTimePeriodMillis() {
        return timePeriodMillis;
    }

    /**
     * Sets the time period during which the maximum number of requests apply
     */
    public void setTimePeriodMillis(long timePeriodMillis) {
        this.timePeriodMillis = timePeriodMillis;
    }

    protected void delay(Exchange exchange) throws Exception {
        TimeSlot slot = nextSlot();
        if (!slot.isActive()) {
            waitUntil(slot.startTime, exchange);
        }
    }
    
    /*
     * Determine what the next available time slot is for handling an Exchange
     */
    protected synchronized TimeSlot nextSlot() {
        if (slot == null) {
            slot = new TimeSlot();
        }
        if (slot.isFull()) {
            slot = slot.next();
        }
        slot.assign();
        return slot;
    }
    
    /*
     * A time slot is capable of handling a number of exchanges within a certain period of time.
     */
    protected class TimeSlot {
        
        private long capacity = Throttler.this.maximumRequestsPerPeriod;
        private final long duration = Throttler.this.timePeriodMillis;
        private final long startTime;

        protected TimeSlot() {
            this(System.currentTimeMillis());
        }

        protected TimeSlot(long startTime) {
            this.startTime = startTime;
        }
        
        protected void assign() {
            capacity--;
        }
        
        /*
         * Start the next time slot either now or in the future
         * (no time slots are being created in the past)
         */
        protected TimeSlot next() {
            return new TimeSlot(Math.max(System.currentTimeMillis(), this.startTime + this.duration));
        }
        
        protected boolean isActive() {
            return startTime <= System.currentTimeMillis();
        }
        
        protected boolean isFull() {
            return capacity <= 0;
        }        
    }
}
