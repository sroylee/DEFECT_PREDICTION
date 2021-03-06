import java.util.StringTokenizer;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import java.io.File;
import java.io.Writer;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.texen.Generator;
import org.apache.velocity.util.StringUtils;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.commons.collections.ExtendedProperties;

/**
 * An ant task for generating output by using Velocity
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="robertdonkin@mac.com">Robert Burrell Donkin</a>
 * @version $Id: TexenTask.java 75955 2004-03-03 23:23:08Z geirm $
 */
public class TexenTask 
    extends Task
{
    /**
     * This message fragment (telling users to consult the log or
     * invoke ant with the -debug flag) is appended to rethrown
     * exception messages.
     */
    private final static String ERR_MSG_FRAGMENT = 
        ". For more information consult the velocity log, or invoke ant " +
        "with the -debug flag.";
    
    /**
     * This is the control template that governs the output.
     * It may or may not invoke the services of worker
     * templates.
     */
    protected String controlTemplate;
    
    /**
     * This is where Velocity will look for templates
     * using the file template loader.
     */
    protected String templatePath;
    
    /**
     * This is where texen will place all the output
     * that is a product of the generation process.
     */
    protected String outputDirectory;
    
    /**
     * This is the file where the generated text
     * will be placed.
     */
    protected String outputFile;
    
    /**
     * This is the encoding for the output file(s).
     */
    protected String outputEncoding;

    /**
     * This is the encoding for the input file(s)
     * (templates).
     */
    protected String inputEncoding;

    /**
     * <p>
     * These are properties that are fed into the
     * initial context from a properties file. This
     * is simply a convenient way to set some values
     * that you wish to make available in the context.
     * </p>
     * <p>
     * These values are not critical, like the template path
     * or output path, but allow a convenient way to
     * set a value that may be specific to a particular
     * generation task.
     * </p>
     * <p>
     * For example, if you are generating scripts to allow
     * user to automatically create a database, then
     * you might want the <code>$databaseName</code> 
     * to be placed
     * in the initial context so that it is available
     * in a script that might look something like the
     * following:
     * <code><pre>
     * #!bin/sh
     * 
     * echo y | mysqladmin create $databaseName
     * </pre></code>
     * The value of <code>$databaseName</code> isn't critical to
     * output, and you obviously don't want to change
     * the ant task to simply take a database name.
     * So initial context values can be set with
     * properties file.
     */
    protected ExtendedProperties contextProperties;

    /**
     * Property which controls whether the classpath
     * will be used when trying to locate templates.
     */
    protected boolean useClasspath;

    /**
     * Path separator.
     */
    private String fileSeparator = System.getProperty("file.separator");

    /**
     * [REQUIRED] Set the control template for the
     * generating process.
     */
    public void setControlTemplate (String controlTemplate)
    {
        this.controlTemplate = controlTemplate;
    }

    /**
     * Get the control template for the
     * generating process.
     */
    public String getControlTemplate()
    {
        return controlTemplate;
    }

    /**
     * [REQUIRED] Set the path where Velocity will look
     * for templates using the file template
     * loader.
     */
    
    public void setTemplatePath(String templatePath) throws Exception
    {
        StringBuffer resolvedPath = new StringBuffer();
        StringTokenizer st = new StringTokenizer(templatePath, ",");
        while ( st.hasMoreTokens() )
        {
            File fullPath = project.resolveFile(st.nextToken());
            resolvedPath.append(fullPath.getCanonicalPath());
            if ( st.hasMoreTokens() )
            {
                resolvedPath.append(",");
            }
        }
        this.templatePath = resolvedPath.toString();
        
        System.out.println(templatePath);
     }

    /**
     * Get the path where Velocity will look
     * for templates using the file template
     * loader.
     */
    public String getTemplatePath()
    {
        return templatePath;
    }        

    /**
     * [REQUIRED] Set the output directory. It will be
     * created if it doesn't exist.
     */
    public void setOutputDirectory(File outputDirectory)
    {
        try
        {
            this.outputDirectory = outputDirectory.getCanonicalPath();
        }
        catch (java.io.IOException ioe)
        {
            throw new BuildException(ioe);
        }
    }
      
    /**
     * Get the output directory.
     */
    public String getOutputDirectory()
    {
        return outputDirectory;
    }        

    /**
     * [REQUIRED] Set the output file for the
     * generation process.
     */
    public void setOutputFile(String outputFile)
    {
        this.outputFile = outputFile;
    }
    
    /**
     * Set the output encoding.
     */
    public void setOutputEncoding(String outputEncoding)
    {
        this.outputEncoding = outputEncoding;
    }

    /**
     * Set the input (template) encoding.
     */
    public void setInputEncoding(String inputEncoding)
    {
        this.inputEncoding = inputEncoding;
    }

    /**
     * Get the output file for the
     * generation process.
     */
    public String getOutputFile()
    {
        return outputFile;
    }        

    /**
     * Set the context properties that will be
     * fed into the initial context be the
     * generating process starts.
     */
    public void setContextProperties( String file )
    {
        String[] sources = StringUtils.split(file,",");
        contextProperties = new ExtendedProperties();
        
        for (int i = 0; i < sources.length; i++)
        {
            ExtendedProperties source = new ExtendedProperties();
            
            try
            {
                File fullPath = project.resolveFile(sources[i]);
                log("Using contextProperties file: " + fullPath);
                source.load(new FileInputStream(fullPath));
            }
            catch (Exception e)
            {
                ClassLoader classLoader = this.getClass().getClassLoader();
            
                try
                {
                    InputStream inputStream = classLoader.getResourceAsStream(sources[i]);
                
                    if (inputStream == null)
                    {
                        throw new BuildException("Context properties file " + sources[i] +
                            " could not be found in the file system or on the classpath!");
                    }
                    else
                    {
                        source.load(inputStream);
                    }
                }
                catch (IOException ioe)
                {
                    source = null;
                }
            }
        
            Iterator j = source.getKeys();
            
            while (j.hasNext())
            {
                String name = (String) j.next();
                String value = source.getString(name);
                contextProperties.setProperty(name,value);
            }
        }
    }

    /**
     * Get the context properties that will be
     * fed into the initial context be the
     * generating process starts.
     */
    public ExtendedProperties getContextProperties()
    {
        return contextProperties;
    }
    
    /**
     * Set the use of the classpath in locating templates
     *
     * @param boolean true means the classpath will be used.
     */
    public void setUseClasspath(boolean useClasspath)
    {
        this.useClasspath = useClasspath;
    }        
    
    /**
     * Creates a VelocityContext.
     *
     * @return new Context
     * @throws Exception the execute method will catch 
     *         and rethrow as a <code>BuildException</code>
     */
    public Context initControlContext() 
        throws Exception
    {
        return new VelocityContext();
    }
    
    /**
     * Execute the input script with Velocity
     *
     * @throws BuildException  
     * BuildExceptions are thrown when required attributes are missing.
     * Exceptions thrown by Velocity are rethrown as BuildExceptions.
     */
    public void execute () 
        throws BuildException
    {
        if (templatePath == null && useClasspath == false)
        {
            throw new BuildException(
                "The template path needs to be defined if you are not using " +
                "the classpath for locating templates!");
        }            
    
        if (controlTemplate == null)
        {
            throw new BuildException("The control template needs to be defined!");
        }            

        if (outputDirectory == null)
        {
            throw new BuildException("The output directory needs to be defined!");
        }            
        
        if (outputFile == null)
        {
            throw new BuildException("The output file needs to be defined!");
        }            
        
        VelocityEngine ve = new VelocityEngine();
        
        try
        {
            if (templatePath != null)
            {
            	log("Using templatePath: " + templatePath, project.MSG_VERBOSE);
                ve.setProperty(
                    ve.FILE_RESOURCE_LOADER_PATH, templatePath);
            }
            
            if (useClasspath)
            {
            	log("Using classpath");
                ve.addProperty(
                    VelocityEngine.RESOURCE_LOADER, "classpath");
            
                ve.setProperty(
                    "classpath." + VelocityEngine.RESOURCE_LOADER + ".class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

                ve.setProperty(
                    "classpath." + VelocityEngine.RESOURCE_LOADER + 
                        ".cache", "false");

                ve.setProperty(
                    "classpath." + VelocityEngine.RESOURCE_LOADER + 
                        ".modificationCheckInterval", "2");
            }
            
            ve.init();

            Generator generator = Generator.getInstance();
            generator.setVelocityEngine(ve);
            generator.setOutputPath(outputDirectory);
            generator.setInputEncoding(inputEncoding);
            generator.setOutputEncoding(outputEncoding);

            if (templatePath != null)
            {
                generator.setTemplatePath(templatePath);
            }
            
            File file = new File(outputDirectory);
            if (! file.exists())
            {
                file.mkdirs();
            }
            
            String path = outputDirectory + File.separator + outputFile;
            log("Generating to file " + path, project.MSG_INFO);
            Writer writer = generator.getWriter(path, outputEncoding);
            
            Context c = initControlContext();
            
            populateInitialContext(c);
            
            if (contextProperties != null)
            {
                Iterator i = contextProperties.getKeys();
        
                while (i.hasNext())
                {
                    String property = (String) i.next();
                    String value = contextProperties.getString(property);
                    
                    try
                    {
                        c.put(property, new Integer(value)); 
                    }
                    catch (NumberFormatException nfe)
                    {
                        String booleanString = 
                            contextProperties.testBoolean(value);
                        
                        if (booleanString != null)
                        {    
                            c.put(property, new Boolean(booleanString));
                        }
                        else
                        {
                            if (property.endsWith("file.contents"))
                            {
                                value = StringUtils.fileContentsToString(   
                                    project.resolveFile(value).getCanonicalPath());
                            
                                property = property.substring(
                                    0, property.indexOf("file.contents") - 1);
                            }
                        
                            c.put(property, value);
                        }
                    }
                }
            }
            
            writer.write(generator.parse(controlTemplate, c));
            writer.flush();
            writer.close();
            generator.shutdown();
            cleanup();
        }
        catch( BuildException e)
        {
            throw e;
        }
        catch( MethodInvocationException e )
        {
            throw new BuildException(
                "Exception thrown by '" + e.getReferenceName() + "." + 
                    e.getMethodName() +"'" + ERR_MSG_FRAGMENT,
                        e.getWrappedThrowable());
        }       
        catch( ParseErrorException e )
        {
            throw new BuildException("Velocity syntax error" + ERR_MSG_FRAGMENT ,e);
        }        
        catch( ResourceNotFoundException e )
        {
            throw new BuildException("Resource not found" + ERR_MSG_FRAGMENT,e);
        }
        catch( Exception e )
        {
            throw new BuildException("Generation failed" + ERR_MSG_FRAGMENT ,e);
        }
    }

    /**
     * <p>Place useful objects into the initial context.</p>
     *
     * <p>TexenTask places <code>Date().toString()</code> into the
     * context as <code>$now</code>.  Subclasses who want to vary the
     * objects in the context should override this method.</p>
     *
     * <p><code>$generator</code> is not put into the context in this
     * method.</p>
     *
     * @param context The context to populate, as retrieved from
     * {@link #initControlContext()}.
     *
     * @throws Exception Error while populating context.  The {@link
     * #execute()} method will catch and rethrow as a
     * <code>BuildException</code>.
     */
    protected void populateInitialContext(Context context) 
        throws Exception
    {
        context.put("now", new Date().toString());
    }

    /**
     * A hook method called at the end of {@link #execute()} which can
     * be overridden to perform any necessary cleanup activities (such
     * as the release of database connections, etc.).  By default,
     * does nothing.
     *
     * @exception Exception Problem cleaning up.
     */
    protected void cleanup()
        throws Exception
    {
    }
}
