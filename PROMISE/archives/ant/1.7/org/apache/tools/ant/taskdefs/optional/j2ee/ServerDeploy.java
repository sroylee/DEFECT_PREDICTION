package org.apache.tools.ant.taskdefs.optional.j2ee;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *  Controls hot deployment tools for J2EE servers.
 *
 *  This class is used as a framework for the creation of vendor specific
 *  hot deployment tools.
 *
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.HotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.AbstractHotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.GenericHotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.WebLogicHotDeploymentTool
 */
public class ServerDeploy extends Task {
    /** The action to be performed.  IE: "deploy", "delete", etc... **/
    private String action;

    /** The source (fully-qualified path) to the component being deployed **/
    private File source;

    /** The vendor specific tool for deploying the component **/
    private Vector vendorTools = new Vector();


    /**
     *  Creates a generic deployment tool.
     *  <p>Ant calls this method on creation to handle embedded "generic" elements
     *  in the ServerDeploy task.
     *  @param tool An instance of GenericHotDeployment tool, passed in by Ant.
     */
    public void addGeneric(GenericHotDeploymentTool tool) {
        tool.setTask(this);
        vendorTools.addElement(tool);
    }

    /**
     *  Creates a WebLogic deployment tool, for deployment to WebLogic servers.
     *  <p>Ant calls this method on creation to handle embedded "weblogic" elements
     *  in the ServerDeploy task.
     *  @param tool An instance of WebLogicHotDeployment tool, passed in by Ant.
     */
    public void addWeblogic(WebLogicHotDeploymentTool tool) {
        tool.setTask(this);
        vendorTools.addElement(tool);
    }

    /**
     *  Creates a JOnAS deployment tool, for deployment to JOnAS servers.
     *  <p>Ant calls this method on creation to handle embedded "jonas" elements
     *  in the ServerDeploy task.
     *  @param tool An instance of JonasHotDeployment tool, passed in by Ant.
     */
    public void addJonas(JonasHotDeploymentTool tool) {
        tool.setTask(this);
        vendorTools.addElement(tool);
    }



    /**
     *  Execute the task.
     *  <p>This method calls the deploy() method on each of the vendor-specific tools
     *  in the <code>vendorTools</code> collection.  This performs the actual
     *  process of deployment on each tool.
     *  @exception org.apache.tools.ant.BuildException if the attributes
     *  are invalid or incomplete, or a failure occurs in the deployment process.
     */
    public void execute() throws BuildException {
        for (Enumeration e = vendorTools.elements();
             e.hasMoreElements();) {
            HotDeploymentTool tool = (HotDeploymentTool) e.nextElement();
            tool.validateAttributes();
            tool.deploy();
        }
    }


    /**
     *  Returns the action field.
     *  @return A string representing the "action" attribute.
     */
    public String getAction() {
        return action;
    }

    /**
     *  The action to be performed, usually "deploy"; required.
     *   Some tools support additional actions, such as "delete", "list", "undeploy", "update"...
     *  @param action A String representing the "action" attribute.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     *  Returns the source field (the path/filename of the component to be
     *  deployed.
     *  @return A File object representing the "source" attribute.
     */
    public File getSource() {
        return source;
    }

    /**
     *  The filename of the component to be deployed; optional
     *  depending upon the tool and the action.
     *  @param source String representing the "source" attribute.
     */
    public void setSource(File source) {
        this.source = source;
    }
}

