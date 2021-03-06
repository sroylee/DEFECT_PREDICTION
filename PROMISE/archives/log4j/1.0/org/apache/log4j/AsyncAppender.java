package org.apache.log4j;

import org.apache.log4j.Category;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.helpers.BoundedFIFO;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.helpers.LogLog;
import java.util.Enumeration;

/**
   The AsyncAppender lets users log events asynchronously. It uses a
   bounded buffer to store logging events.
   
   <p>The AsyncAppender will collect the events sent to it and then
   dispatch them to all the appenders that are attached to it. You can
   attach multiple appenders to an AsyncAppender.

   <p>The AsyncAppender uses a separate thread to serve the events in
   its bounded buffer. 

   <p>Refer to the results in {@link org.apache.log4j.performance.Logging}
   for the impact of using this appender.


   @author Ceki G&uuml;lc&uuml;
   @since version 0.9.1 */
public class AsyncAppender extends AppenderSkeleton 
                                            implements AppenderAttachable {


  /**
     A string constant used in naming the option for setting the the
     location information flag.  Current value of this string
     constant is <b>LocationInfo</b>.  

     <p>Note that all option keys are case sensitive.
  */
  public static final String LOCATION_INFO_OPTION = "LocationInfo";
  
  static final int BUFFER_SIZE = 128;

  BoundedFIFO bf = new BoundedFIFO(BUFFER_SIZE);
  AppenderAttachableImpl aai;
  Dispatcher dispatcher;
  boolean locationInfo = false;

  public
  AsyncAppender() {
    aai = new AppenderAttachableImpl();
    dispatcher = new Dispatcher(bf, aai);
    dispatcher.start();
  }
  
  synchronized  
  public 
  void addAppender(Appender newAppender) {
    aai.addAppender(newAppender);
  }

  public
  void append(LoggingEvent event) {
    event.getNDC();
    event.getThreadName();
    if(locationInfo) {
      event.setLocationInformation();	
    }
    synchronized(bf) {
      if(bf.isFull()) {
	try {
	  bf.wait();
	} catch(InterruptedException e) {
	  LogLog.error("AsyncAppender cannot be interrupted.", e);
	}
      }
      bf.put(event);
      if(bf.wasEmpty()) {
	bf.notify();
      }
    }
  }

  /**
     Close this <code>AsyncAppender</code> by interrupting the
     dispatcher thread which will process all pending events before
     exiting. */
  public 
  void close() {
    closed = true;
    dispatcher.interrupt();
    try {
      dispatcher.join();
    } catch(InterruptedException e) {
      LogLog.error("Got an InterruptedException while waiting for the "+
		   "dispatcher to finish.", e);
    }
    dispatcher = null;
    bf = null;
  }

  public
  Enumeration getAllAppenders() {
    return aai.getAllAppenders();
  }

  public
  Appender getAppender(String name) {
    return aai.getAppender(name);
  }

 /**
     Retuns the option names for this component in addition in
     addition to the options of its super class {@link
     AppenderSkeleton}.  */
  public
  String[] getOptionStrings() {
    return OptionConverter.concatanateArrays(super.getOptionStrings(),
          new String[] {LOCATION_INFO_OPTION});
  }

  
  /**
     The <code>AsyncAppender</code> does not require a layout. Hence,
     this method always returns <code>false</code>. */
  public 
  boolean requiresLayout() {
    return false;
  }

  synchronized
  public
  void removeAllAppenders() {
    aai.removeAllAppenders();
  }
  
  synchronized
  public
  void removeAppender(Appender appender) {
   aai.removeAppender(appender);
  }

  synchronized
  public
  void removeAppender(String name) {
    aai.removeAppender(name);
  }

 
 /**
     Set SMTPAppender specific options.

     <p>On top of the options of the super class {@link
     AppenderSkeleton}, the only recognized options is
     <b>LocationInfo</b>.
     
     <p>The <b>LocationInfo</b> option takes a boolean value. By
     default, it is set to false which means there will be no effort
     to extract the location information related to the event. As a
     result, the appender and layout will ultimately log the event are
     likely to log the wrong location information (if present in the
     log format).

     <p>Location information extraction is comparatively very slow and
     should be avoided unless performance is not a concern.

 */
  public
  void setOption(String option, String value) {
    if(value == null) return;
    super.setOption(option, value);

    if (option.equals(LOCATION_INFO_OPTION))
      locationInfo = OptionConverter.toBoolean(value, locationInfo);
  }

}
class Dispatcher extends Thread {

  BoundedFIFO bf;
  AppenderAttachableImpl aai;
  
  Dispatcher(BoundedFIFO bf, AppenderAttachableImpl aai) {
    this.bf = bf;
    this.aai = aai;
    this.setPriority(Thread.MIN_PRIORITY);
    

  }


  /**
     The dispatching strategy is to wait until there are events in the
     buffer to process. After having processed an event, we release
     the monitor (variable bf) so that new events can be placed in the
     buffer, instead of keeping the monitor and processing remaning
     events in the buffer. 

    <p>Other approaches might yield better results.

  */
  public
  void run() {


    LoggingEvent event;
    
    while(true) {
      synchronized(bf) {
	if(bf.length() == 0) {
	  if(interrupted()) { 
	    return;
	  }
	  try {
	    bf.wait();
	  } catch(InterruptedException e) {
	    break;
	  }
	}
	event = bf.get();
	if(bf.wasFull()) {
	  bf.notify();
	}
      
      if(aai != null)
	aai.appendLoopOnAppenders(event);
  }
}
