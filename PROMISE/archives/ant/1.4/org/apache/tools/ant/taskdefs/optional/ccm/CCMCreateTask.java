package org.apache.tools.ant.taskdefs.optional.ccm;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.NullPointerException;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * Task allows to create new ccm task and set it as the default
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMCreateTask extends Continuus implements ExecuteStreamHandler  {

    private String comment   = null;
    private String platform  = null;
    private String resolver  = null;
    private String release   = null;
    private String subSystem = null;
    private String task      = null;

    public CCMCreateTask() {
        super();
        setCcmAction(COMMAND_CREATE_TASK);
    }

        
    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute ccm and then calls Exec's run method
     * to execute the command line.
     * </p>
     */
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();
        Project aProj = getProject();
        int result = 0;

        commandLine.setExecutable(getCcmCommand());
        commandLine.createArgument().setValue(getCcmAction());
        
        checkOptions(commandLine);
        
        result = run(commandLine,this);
        if ( result != 0 ) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }

        Commandline commandLine2 = new Commandline();
        commandLine2.setExecutable(getCcmCommand());
        commandLine2.createArgument().setValue(COMMAND_DEFAULT_TASK);
        commandLine2.createArgument().setValue(getTask());

        log(commandLine.toString(),Project.MSG_DEBUG);

        result = run(commandLine2);
        if ( result != 0 ) {
            String msg = "Failed executing: " + commandLine2.toString();
            throw new BuildException(msg, location);
        }

    }


    /**
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {        
        if (getComment() != null) {
            cmd.createArgument().setValue(FLAG_COMMENT);
            cmd.createArgument().setValue("\""+getComment()+"\"");
        }

        if ( getPlatform() != null) {
            cmd.createArgument().setValue(FLAG_PLATFORM);
            cmd.createArgument().setValue(getPlatform());            

        if ( getResolver() != null) {
            cmd.createArgument().setValue(FLAG_RESOLVER);
            cmd.createArgument().setValue(getResolver());            
        
        if ( getSubSystem() != null) {
            cmd.createArgument().setValue(FLAG_SUBSYSTEM);
            cmd.createArgument().setValue("\""+getSubSystem()+"\"");            
        
        if ( getRelease() != null ) {
            cmd.createArgument().setValue(FLAG_RELEASE);
            cmd.createArgument().setValue(getRelease());            
    }
    
    
    /**
     * Get the value of comment.
     * @return value of comment.
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * Set the value of comment.
     * @param v  Value to assign to comment.
     */
    public void setComment(String  v) {
        this.comment = v;
    }
    
    
    /**
     * Get the value of platform.
     * @return value of platform.
     */
    public String getPlatform() {
        return platform;
    }
    
    /**
     * Set the value of platform.
     * @param v  Value to assign to platform.
     */
    public void setPlatform(String  v) {
        this.platform = v;
    }
    
    
    /**
     * Get the value of resolver.
     * @return value of resolver.
     */
    public String getResolver() {
        return resolver;
    }
    
    /**
     * Set the value of resolver.
     * @param v  Value to assign to resolver.
     */
    public void setResolver(String  v) {
        this.resolver = v;
    }

    
    /**
     * Get the value of release.
     * @return value of release.
     */
    public String getRelease() {
        return release;
    }
    
    /**
     * Set the value of release.
     * @param v  Value to assign to release.
     */
    public void setRelease(String  v) {
        this.release = v;
    }    
    
    /**
     * Get the value of subSystem.
     * @return value of subSystem.
     */
    public String getSubSystem() {
        return subSystem;
    }
    
    /**
     * Set the value of subSystem.
     * @param v  Value to assign to subSystem.
     */
    public void setSubSystem(String  v) {
        this.subSystem = v;
    }
    
    
    /**
     * Get the value of task.
     * @return value of task.
     */
    public String  getTask()    {
        return task;
    }
    
    /**
     * Set the value of task.
     * @param v  Value to assign to task.
     */
    public void setTask(String   v)    {
        this.task = v;
    }
    
    /**
     * /comment -- comments associated to the task
     */
    public static final String FLAG_COMMENT = "/synopsis";
    
    /**
     *  /platform flag -- target platform
     */
    public static final String FLAG_PLATFORM = "/plat";   

    /**
     * /resolver flag
     */
    public static final String FLAG_RESOLVER = "/resolver";   

    /**
     * /release flag
     */
    public static final String FLAG_RELEASE = "/release";   

    /**
     * /release flag
     */
    public static final String FLAG_SUBSYSTEM = "/subsystem";   

    /**
     *  -task flag -- associate checckout task with task
     */
    public static final String FLAG_TASK = "/task";   


    
    /**
     *
     * @exception java.io.IOException <description>
     */
    public void start() throws IOException  {                                
    }

    /**
     *
     */
    public void stop(){     
    }

    /**
     *
     * @param param1 <description>
     * @exception java.io.IOException <description>
     */
    public void setProcessInputStream(OutputStream param1) throws IOException    {    
    }

    /**
     *
     * @param param1 <description>
     * @exception java.io.IOException <description>
     */
    public void setProcessErrorStream(InputStream is) throws IOException    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String s = reader.readLine();
        if ( s != null) 
        {
            log("err "+s,Project.MSG_DEBUG);
    }

    /**
     * read the output stream to retrieve the new task number.
     * @param is InputStream
     * @exception java.io.IOException 
     */
    public void setProcessOutputStream(InputStream is) throws IOException    {

        String buffer = "";
        try           
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            buffer = reader.readLine();
            if ( buffer != null) 
            {
                log("buffer:" +buffer,Project.MSG_DEBUG);             
                String taskstring  = buffer.substring(buffer.indexOf(' ')).trim();
                taskstring = taskstring.substring(0,taskstring.lastIndexOf(' ')).trim();                
                setTask(taskstring);
                log("task is "+getTask(),Project.MSG_DEBUG);             
        }
        catch ( NullPointerException npe) 
        {
            log("error procession stream , null pointer exception",Project.MSG_ERR);
            npe.printStackTrace();
            throw new BuildException(npe.getClass().getName());
        catch (Exception e) 
        {
            log("error procession stream "+e.getMessage(),Project.MSG_ERR);
            throw new BuildException(e.getMessage());
        
    }

}

