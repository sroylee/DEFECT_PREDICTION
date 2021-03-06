package org.apache.html.dom;


import org.w3c.dom.*;
import org.w3c.dom.html.*;


/**
 * @version $Revision: 316847 $ $Date: 2001-01-31 07:58:06 +0800 (周三, 2001-01-31) $
 * @author <a href="mailto:arkin@exoffice.com">Assaf Arkin</a>
 * @see org.w3c.dom.html.HTMLOListElement
 * @see ElementImpl
 */
public class HTMLOListElementImpl
    extends HTMLElementImpl
    implements HTMLOListElement
{

    
    public boolean getCompact()
    {
        return getBinary( "compact" );
    }
    
    
    public void setCompact( boolean compact )
    {
        setAttribute( "compact", compact );
    }
    
    
      public int getStart()
    {
        return getInteger( getAttribute( "start" ) );
    }
    
    
    public void setStart( int start )
    {
        setAttribute( "start", String.valueOf( start ) );
    }
  
  
    public String getType()
    {
        return getAttribute( "type" );
    }
    
    
    public void setType( String type )
    {
        setAttribute( "type", type );
    }
        
        
      /**
     * Constructor requires owner document.
     * 
     * @param owner The owner HTML document
     */
    public HTMLOListElementImpl( HTMLDocumentImpl owner, String name )
    {
        super( owner, name );
    }


}

