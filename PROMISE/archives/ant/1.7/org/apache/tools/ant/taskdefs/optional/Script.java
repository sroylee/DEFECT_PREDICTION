package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.ScriptRunnerHelper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Executes a script.
 *
 * @ant.task name="script"
 */
public class Script extends Task {

    private ScriptRunnerHelper helper = new ScriptRunnerHelper();

    /**
     * Set the project.
     * @param project the project that this task belongs to.
     */
    public void setProject(Project project) {
        super.setProject(project);
        helper.setProjectComponent(this);
    }

    /**
     * Run the script using the helper object.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {
        helper.getScriptRunner().executeScript("ANT");
    }

    /**
     * Defines the manager.
     *
     * @param manager the scripting manager.
     */
    public void setManager(String manager) {
        helper.setManager(manager);
    }

    /**
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        helper.setLanguage(language);
    }

    /**
     * Load the script from an external file ; optional.
     *
     * @param fileName the name of the file containing the script source.
     */
    public void setSrc(String fileName) {
        helper.setSrc(new File(fileName));
    }

    /**
     * Set the script text.
     *
     * @param text a component of the script text to be added.
     */
    public void addText(String text) {
        helper.addText(text);
    }

    /**
     * Set the classpath to be used when searching for classes and resources.
     *
     * @param classpath an Ant Path object containing the search path.
     */
    public void setClasspath(Path classpath) {
        helper.setClasspath(classpath);
    }

    /**
     * Classpath to be used when searching for classes and resources.
     *
     * @return an empty Path instance to be configured by Ant.
     */
    public Path createClasspath() {
        return helper.createClasspath();
    }

    /**
     * Set the classpath by reference.
     *
     * @param r a Reference to a Path instance to be used as the classpath
     *          value.
     */
    public void setClasspathRef(Reference r) {
        helper.setClasspathRef(r);
    }

    /**
     * Set the setbeans attribute.
     * If this is true, &lt;script&gt; will create variables in the
     * script instance for all
     * properties, targets and references of the current project.
     * It this is false, only the project and self variables will
     * be set.
     * The default is true.
     * @param setBeans the value to set.
     */
    public void setSetBeans(boolean setBeans) {
        helper.setSetBeans(setBeans);
    }
}