package org.apache.log4j.test; 

import org.apache.log4j.*;
import org.apache.log4j.spi.*;

/** 
   This class is a shallow test of the various appenders and
   layouts. It also tests their reading of the configuration file.
   @author  Ceki G&uuml;lc&uuml;
*/
public class FQCNTest {

  
  public 
  static 
  void main(String argv[]) throws Exception  {
    if(argv.length == 1) 
      init(argv[0]);
    else 
      usage("Wrong number of arguments.");
    test();
  }

  static
  void usage(String msg) {
    System.err.println(msg);
    System.err.println( "Usage: java "+ FQCNTest.class.getName()+"outputFile");
    System.exit(1);
  } 

  static 
  void init(String file) throws Exception {
    Layout layout = new PatternLayout("%p %c (%C{2}#%M) - %m%n");
    FileAppender appender = new FileAppender(layout, file, false);
    appender.setLayout(layout);
    Category root = Category.getRoot();
    root.addAppender(appender);
  }


  static
  void test() {
    X1Logger x1 = X1Logger.getX1Logger("x1");
    x1.debug("hello");    
    x1.debug1("hello");  
    x1.debug2("hello");  
  }  
}



class X1Logger extends Logger {
  static String FQCN = X1Logger.class.getName() + ".";

  private static X1LoggerFactory factory = new X1LoggerFactory();

  public X1Logger(String name) {
    super(name);
  }

  public 
  void debug1(Object message) {    
    super.log(FQCN, Priority.DEBUG, message + " world.", null);    
  }

  public
  void debug2(Object message) {
    super.log(FQCN, Priority.DEBUG, message, null); 
  }

  protected
  String getFQCN() {
    return X1Logger.FQCN;
  }

  public 
  static
  X1Logger getX1Logger(String name) {
    return ((X1Logger) Logger.getLogger(name, factory)); 
  }
}

class X1LoggerFactory implements LoggerFactory {

  public
  X1LoggerFactory() {
  }
  
  public
  Logger makeNewLoggerInstance(String name) {
    return new X1Logger(name);
  }
}
