package org.apache.tools.ant.taskdefs.optional.j2ee;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;

/**
 *  A generic tool for J2EE server hot deployment.
 *  <p>The simple implementation spawns a JVM with the supplied
 *  class name, jvm args, and arguments.
 *
 *
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.HotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.AbstractHotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.ServerDeploy
 */
public class GenericHotDeploymentTool extends AbstractHotDeploymentTool {
    /** A Java task used to run the deployment tool **/
    private Java java;

    /** The fully qualified class name of the deployment tool **/
    private String className;

    /** List of valid actions **/
    private static final String[] VALID_ACTIONS = {ACTION_DEPLOY};

    /**
     *  Add a nested argument element to hand to the deployment tool; optional.
     *  @return A Commandline.Argument object representing the
     *  command line argument being passed when the deployment
     *  tool is run.  IE: "-user=mark", "-password=venture"...
     */
    public Commandline.Argument createArg() {
        return java.createArg();
    }

    /**
     *  Add a nested argment element to hand to the JVM running the
     *  deployment tool.
     *  Creates a nested arg element.
     *  @return A Commandline.Argument object representing the
     *  JVM command line argument being passed when the deployment
     *  tool is run.  IE: "-ms64m", "-mx128m"...
     */
    public Commandline.Argument createJvmarg() {
        return java.createJvmarg();
    }

    /**
     *  Determines if the "action" attribute defines a valid action.
     *  <p>Subclasses should determine if the action passed in is
     *  supported by the vendor's deployment tool.
     *  For this generic implementation, the only valid action is "deploy"
     *  @return true if the "action" attribute is valid, false if not.
     */
    protected boolean isActionValid() {
        return (getTask().getAction().equals(VALID_ACTIONS[0]));
    }

    /**
     *  Sets the parent task.
     *  @param task An ServerDeploy object representing the parent task.
     *  @ant.attribute ignored="true"
     */
    public void setTask(ServerDeploy task) {
        super.setTask(task);
        java = (Java) task.getProject().createTask("java");
    }

    /**
     *  Perform the actual deployment.
     *  For this generic implementation, a JVM is spawned using the
     *  supplied classpath, classname, JVM args, and command line arguments.
     *  @exception org.apache.tools.ant.BuildException if the attributes are invalid or incomplete.
     */
    public void deploy() throws BuildException {
        java.setClassname(className);
        java.setClasspath(getClasspath());
        java.setFork(true);
        java.setFailonerror(true);
        java.execute();
    }

    /**
     *  Validates the passed in attributes.
     *  Ensures the className and arguments attribute have been set.
     *  @exception org.apache.tools.ant.BuildException if the attributes are invalid or incomplete.
     */
    public void validateAttributes() throws BuildException {
        super.validateAttributes();

        if (className == null) {
            throw new BuildException("The classname attribute must be set");
        }
    }

    /**
     *  The name of the class to execute to perfom
     *  deployment; required.
     *  Example: "com.foobar.tools.deploy.DeployTool"
     *  @param className The fully qualified class name of the class
     *  to perform deployment.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     *
     */
    public Java getJava() {
        return java;
    }

    public String getClassName() {
        return className;
    }
}
