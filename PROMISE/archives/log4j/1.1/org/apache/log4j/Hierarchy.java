 
package org.apache.log4j;


import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.spi.RootCategory;
import org.apache.log4j.spi.CategoryFactory;
import org.apache.log4j.or.RendererMap;
import org.apache.log4j.or.ObjectRenderer;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;

/**
   This class is specialized in retreiving categories by name and
   also maintaining the category hierarchy.

   <p><em>The casual user should not have to deal with this class
   firectly.</em> In fact, up until version 0.9.0, this class had
   default package access. However, if you are in an environment where
   multiple applications run in the same VM, then read on.

   <p>The structure of the category hierachy is maintained by the
   {@link #getInstance} method. The hierrachy is such that children
   link to their parent but parents do not have any pointers to their
   children. Moreover, categories can be instantiated in any order, in
   particular decendant before ancestor.

   <p>In case a decendant is created before a particular ancestor,
   then it creates a provision node for the ancestor and adds itself
   to the provision node. Other decendants of the same ancestor add
   themselves to the previously created provision node.

   <p>See the code below for further details.
   
   @author Ceki G&uuml;lc&uuml; 

*/
public class Hierarchy {

  static final int DISABLE_OFF = -1;
  static final int DISABLE_OVERRIDE = -2;  
  
  static 
  private
  CategoryFactory defaultFactory = new DefaultCategoryFactory();


  Hashtable ht;
  Category root;
  RendererMap rendererMap;
  
  int disable;

  boolean emittedNoAppenderWarning = false;
  boolean emittedNoResourceBundleWarning = false;  

  /**
     Create a new Category hierarchy.

     @param root The root of the new hierarchy.

   */
  public
  Hierarchy(Category root) {
    ht = new Hashtable();
    this.root = root;
    disable = DISABLE_OFF;
    this.root.setHierarchy(this);
    rendererMap = new RendererMap();
  }

  /**
     Add an object renderer for a specific class.       
   */
  public
  void addRenderer(Class classToRender, ObjectRenderer or) {
    rendererMap.put(classToRender, or);
  }
  

  /**
     This call will clear all category definitions from the internal
     hashtable. Invoking this method will irrevocably mess up the
     category hiearchy.
     
     <p>You should <em>really</em> know what you are doing before
     invoking this method.

     @since 0.9.0 */
  public
  void clear() {
    ht.clear();
  }

  /**
     Check if the named category exists in the hirarchy. If so return
     its reference, otherwise returns <code>null</code>.
     
     @param name The name of the category to search for.
     
  */
  public
  Category exists(String name) {    
    Object o = ht.get(new CategoryKey(name));
    if(o instanceof Category) {
      return (Category) o;
    } else {
      return null;
    }
  }


  /**
     Similar to {@link #disable(Priority)} except that the priority
     argument is given as a String.  */
  public
  void disable(String priorityStr) {
    if(disable != DISABLE_OVERRIDE) {  
      Priority p = Priority.toPriority(priorityStr, null);
      if(p != null) {
	disable = p.level;
      } else {
	LogLog.warn("Could not convert ["+priorityStr+"] to Priority.");
      }
    }
  }


  /**
     Disable all logging requests of priority <em>equal to or
     below</em> the priority parameter <code>p</code>, for
     <em>all</em> categories in this hierarchy. Logging requests of
     higher priority then <code>p</code> remain unaffected.

     <p>Nevertheless, if the {@link
     BasicConfigurator#DISABLE_OVERRIDE_KEY} system property is set to
     "true" or any value other than "false", then logging requests are
     evaluated as usual, i.e. according to the <a
     href="../../../../manual.html#selectionRule">Basic Selection Rule</a>.

     <p>The "disable" family of methods are there for speed. They
     allow printing methods such as debug, info, etc. to return
     immediately after an interger comparison without walking the
     category hierarchy. In most modern computers an integer
     comparison is measured in nanoseconds where as a category walk is
     measured in units of microseconds.

     <p>Other configurators define alternate ways of overriding the
     disable override flag. See {@link PropertyConfigurator} and
     {@link org.apache.log4j.xml.DOMConfigurator}.


     @since 0.8.5 */
  public
  void disable(Priority p) {
    if((disable != DISABLE_OVERRIDE) && (p != null)) {
      disable = p.level;
    }
  }
  
  /**
     Disable all logging requests regardless of category and priority.
     This method is equivalent to calling {@link #disable} with the
     argument {@link Priority#FATAL}, the highest possible priority.

     @since 0.8.5 */
  public
  void disableAll() {
    disable(Priority.FATAL);
  }


  /**
     Disable all logging requests of priority DEBUG regardless of
     category.  Invoking this method is equivalent to calling {@link
     #disable} with the argument {@link Priority#DEBUG}.

     @since 0.8.5 */
  public
  void disableDebug() {
    disable(Priority.DEBUG);
  }


  /**
     Disable all logging requests of priority INFO and below
     regardless of category. Note that DEBUG messages are also
     disabled.  

     <p>Invoking this method is equivalent to calling {@link #disable}
     with the argument {@link Priority#INFO}.

     @since 0.8.5 */
  public
  void disableInfo() {
    disable(Priority.INFO);
  }  

  /**
     Undoes the effect of calling any of {@link #disable}, {@link
     #disableAll}, {@link #disableDebug} and {@link #disableInfo}
     methods. More precisely, invoking this method sets the Category
     class internal variable called <code>disable</code> to its
     default "off" value.

     @since 0.8.5 */
  public
  void enableAll() {
    disable = DISABLE_OFF;
  }
  
  /**
     Override the shipped code flag if the <code>override</code>
     parameter is not null.

     <p>If <code>override</code> is null then there is nothing to do.
     Otherwise, set Category.shippedCode to false if override has a
     value other than "false".     
  */
  public
  void overrideAsNeeded(String override) {
    if(override != null) {
      LogLog.debug("Handling non-null disable override directive: \""+
		   override +"\".");
      if(OptionConverter.toBoolean(override, true)) {
	LogLog.debug("Overriding all disable methods.");
	disable = DISABLE_OVERRIDE;
      }
    }
  }


  /**
     Return a new category instance named as the first parameter using
     the default factory. 
     
     <p>If a category of that name already exists, then it will be
     returned.  Otherwise, a new category will be instantiated and
     lthen inked with its existing ancestors as well as children.
     
     @param name The name of the category to retreive.

 */
  public
  Category getInstance(String name) {
    return getInstance(name, defaultFactory);
  }

 /**
     Return a new category instance named as the first parameter using
     <code>factory</code>.
     
     <p>If a category of that name already exists, then it will be
     returned.  Otherwise, a new category will be instantiated by the
     <code>factory</code> parameter and linked with its existing
     ancestors as well as children.
     
     @param name The name of the category to retreive.
     @param factory The factory that will make the new category instance.

 */
  public
  Category getInstance(String name, CategoryFactory factory) {
    CategoryKey key = new CategoryKey(name);    
    Category category;
    
    synchronized(ht) {
      Object o = ht.get(key);
      if(o == null) {
	category = factory.makeNewCategoryInstance(name);
	category.setHierarchy(this);
	ht.put(key, category);      
	updateParents(category);
	return category;
      } else if(o instanceof Category) {
	return (Category) o;
      } else if (o instanceof ProvisionNode) {
	category = factory.makeNewCategoryInstance(name);
	category.setHierarchy(this); 
	ht.put(key, category);
	updateChildren((ProvisionNode) o, category);
	updateParents(category);	
	return category;
      }
      else {
      }
    }
  }


  /**
     Returns all the currently defined categories in this hierarchy as
     an {@link java.util.Enumeration Enumeration}.

     <p>The root category is <em>not</em> included in the returned
     {@link Enumeration}.  */
  public
  Enumeration getCurrentCategories() {
    Vector v = new Vector(ht.size());
    
    Enumeration elems = ht.elements();
    while(elems.hasMoreElements()) {
      Object o = elems.nextElement();
      if(o instanceof Category) {
	v.addElement(o);
      }
    }
    return v.elements();
  }


  public
  boolean isDisabled(int level) {
    return disable >=  level;
  }

  /**
     Get the renderer map for this hierarchy.
  */
  public
  RendererMap getRendererMap() {
    return rendererMap;
  }


  /**
     Get the root of this hierarchy.
     
     @since 0.9.0
   */
  public
  Category getRoot() {
    return root;
  }


  /**
     Reset all values contained in this hierarchy instance to their
     default.  This removes all appenders from all categories, sets
     the priority of all non-root categories to <code>null</code>,
     sets their additivity flag to <code>true</code> and sets the priority
     of the root category to {@link Priority#DEBUG DEBUG}.  Moreover,
     message disabling is set its default "off" value.

     <p>Existing categories are not removed. They are just reset.

     <p>This method should be used sparingly and with care as it will
     block all logging until it is completed.</p>

     @since version 0.8.5 */
  public
  void resetConfiguration() {

    getRoot().setPriority(Priority.DEBUG);
    root.setResourceBundle(null);
    disable = Hierarchy.DISABLE_OFF;
    
    synchronized(ht) {    
    
      Enumeration cats = getCurrentCategories();
      while(cats.hasMoreElements()) {
	Category c = (Category) cats.nextElement();
	c.setPriority(null);
	c.setAdditivity(true);
	c.setResourceBundle(null);
      }
    }
    rendererMap.clear();
  }

  /**
     Set the disable override value given a string.
 
     @since 1.1
   */
  public
  void setDisableOverride(String override) {
    if(OptionConverter.toBoolean(override, true)) {
      LogLog.debug("Overriding disable.");
      disable =  DISABLE_OVERRIDE;
    }
  }



  /**
     Shutting down a hierarchy will <em>safely</em> close and remove
     all appenders in all categories including the root category.
     
     <p>Some appenders such as {@link org.apache.log4j.net.SocketAppender}
     and {@link AsyncAppender} need to be closed before the
     application exists. Otherwise, pending logging events might be
     lost.

     <p>The <code>shutdown</code> method is careful to close nested
     appenders before closing regular appenders. This is allows
     configurations where a regular appender is attached to a category
     and again to a nested appender.
     

     @since 1.0 */
  public 
  void shutdown() {
    Category root = getRoot();    

    root.closeNestedAppenders();

    synchronized(ht) {
      Enumeration cats = this.getCurrentCategories();
      while(cats.hasMoreElements()) {
	Category c = (Category) cats.nextElement();
	c.closeNestedAppenders();
      }

      root.removeAllAppenders();
      cats = this.getCurrentCategories();
      while(cats.hasMoreElements()) {
	Category c = (Category) cats.nextElement();
	c.removeAllAppenders();
      }      
    }
  }


  /**
     This method loops through all the *potential* parents of
     'cat'. There 3 possible cases:

     1) No entry for the potential parent of 'cat' exists

        We create a ProvisionNode for this potential parent and insert
        'cat' in that provision node.

     2) There entry is of type Category for the potential parent.

        The entry is 'cat's nearest existing parent. We update cat's
        parent field with this entry. We also break from the loop
        because updating our parent's parent is our parent's
        responsibility.
	 
     3) There entry is of type ProvisionNode for this potential parent.

        We add 'cat' to the list of children for this potential parent.
   */
  final
  private
  void updateParents(Category cat) {
    String name = cat.name;
    int length = name.length();
    boolean parentFound = false;
    
    
    for(int i = name.lastIndexOf('.', length-1); i >= 0; 
	                                 i = name.lastIndexOf('.', i-1))  {
      String substr = name.substring(0, i);

      Object o = ht.get(key);
      if(o == null) {
	ProvisionNode pn = new ProvisionNode(cat);
	ht.put(key, pn);
      } else if(o instanceof Category) {
	parentFound = true;
	cat.parent = (Category) o;
      } else if(o instanceof ProvisionNode) {
	((ProvisionNode) o).addElement(cat);
      } else {
	Exception e = new IllegalStateException("unexpected object type " + 
					o.getClass() + " in ht.");
	e.printStackTrace();			   
      }
    }
    if(!parentFound) 
      cat.parent = root;
  }

  /** 
      We update the links for all the children that placed themselves
      in the provision node 'pn'. The second argument 'cat' is a
      reference for the newly created Category, parent of all the
      children in 'pn'

      We loop on all the children 'c' in 'pn':

         If the child 'c' has been already linked to a child of
         'cat' then there is no need to update 'c'.

	 Otherwise, we set cat's parent field to c's parent and set
	 c's parent field to cat.

  */
  final
  private
  void updateChildren(ProvisionNode pn, Category cat) {
    final int last = pn.size();

    for(int i = 0; i < last; i++) {
      Category c = (Category) pn.elementAt(i);

      if(!c.parent.name.startsWith(cat.name)) {
	cat.parent = c.parent;
	c.parent = cat;      
      }
    }
  }    

}


