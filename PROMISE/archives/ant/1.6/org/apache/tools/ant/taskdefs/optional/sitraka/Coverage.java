package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Runs Sitraka JProbe Coverage.
 *
 * Options are pretty numerous, you'd better check the manual for a full
 * descriptions of options. (not that simple since they differ from the online
 * help, from the usage command line and from the examples...)
 * <p>
 *
 *
 * @ant.task name="jpcoverage" category="metrics"
 */
public class Coverage extends CovBase {

    protected Commandline cmdl = new Commandline();

    protected CommandlineJava cmdlJava = new CommandlineJava();

    protected String function = "coverage";

    protected String seedName;

    protected File inputFile;

    protected File javaExe;

    protected String vm;

    protected boolean applet = false;

    /** this is a somewhat annoying thing, set it to never */
    protected String exitPrompt = "never";

    protected Filters filters = new Filters();

    protected Triggers triggers;

    protected String finalSnapshot = "coverage";

    protected String recordFromStart = "coverage";

    protected File snapshotDir;

    protected File workingDir;

    protected boolean trackNatives = false;

    protected Socket socket;

    protected int warnLevel = 0;

    protected Vector filesets = new Vector();


    /** seed name for snapshot file. Can be null, default to snap */
    public void setSeedname(String value) {
        seedName = value;
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setInputfile(File value) {
        inputFile = value;
    }

    /**
     * Path to the java executable.
     */
    public void setJavaexe(File value) {
        javaExe = value;
    }

    public static class Javavm extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{"java2", "jdk118", "jdk117"};
        }
    }

    /**
     * Indicates which virtual machine to run: "jdk117", "jdk118" or "java2".
     * Can be null, default to "java2". */
    public void setVm(Javavm value) {
        vm = value.getValue();
    }

    /**
     * If true, run an applet.
     */
    public void setApplet(boolean value) {
        applet = value;
    }

    /**
     * Toggles display of the console prompt: always, error, never
     */
    public void setExitprompt(String value) {
        exitPrompt = value;
    }

    /**
     * Defines class/method filters based on pattern matching.
     * The syntax is filters is similar to a fileset.
     */
    public Filters createFilters() {
        return filters;
    }

    /**
     * Defines events to use for interacting with the
     * collection of data performed during coverage.
     *
     * For example you may run a whole application but only decide
     * to collect data once it reaches a certain method and once it
     * exits another one.
     */
    public Triggers createTriggers() {
        if (triggers == null) {
            triggers = new Triggers();
        }
        return triggers;
    }

    /**
     * Define a host and port to connect to if you want to do
     * remote viewing.
     */
    public Socket createSocket() {
        if (socket == null) {
            socket = new Socket();
        }
        return socket;
    }

    public static class Finalsnapshot extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{"coverage", "none", "all"};
        }
    }

    /**
     * Type of snapshot to send at program termination: none, coverage, all.
     * Can be null, default to none
     */
    public void setFinalsnapshot(String value) {
        finalSnapshot = value;
    }

    public static class Recordfromstart extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{"coverage", "none", "all"};
        }
    }

    /**
     * "all", "coverage",  or "none".
     */
    public void setRecordfromstart(Recordfromstart value) {
        recordFromStart = value.getValue();
    }

    /**
     * Set warning level (0-3, where 0 is the least amount of warnings).
     */
    public void setWarnlevel(Integer value) {
        warnLevel = value.intValue();
    }

    /**
     * The path to the directory where snapshot files are stored.
     * Choose a directory that is reachable by both the remote
     * and local computers, and enter the same path on the command-line
     * and in the viewer.
     */
    public void setSnapshotdir(File value) {
        snapshotDir = value;
    }

    /**
     * The physical path to the working directory for the VM.
     */
    public void setWorkingdir(File value) {
        workingDir = value;
    }

    /**
     * If true, track native methods.
     */
    public void setTracknatives(boolean value) {
        trackNatives = value;
    }


    /**
     * Adds a JVM argument.
     */
    public Commandline.Argument createJvmarg() {
        return cmdlJava.createVmArgument();
    }

    /**
     * Adds a command argument.
     */
    public Commandline.Argument createArg() {
        return cmdlJava.createArgument();
    }

    /**
     * classpath to run the files.
     */
    public Path createClasspath() {
        return cmdlJava.createClasspath(getProject()).createPath();
    }

    /**
     * classname to run as standalone or runner for filesets.
     */
    public void setClassname(String value) {
        cmdlJava.setClassname(value);
    }

    /**
     * the classnames to execute.
     */
    public void addFileset(FileSet fs) {
        filesets.addElement(fs);
    }



    public Coverage() {
    }

    /** execute the jplauncher by providing a parameter file */
    public void execute() throws BuildException {
        File paramfile = null;
        if (inputFile == null) {
            checkOptions();
            paramfile = createParamFile();
        } else {
            paramfile = inputFile;
        }
        try {
            cmdl.setExecutable(findExecutable("jplauncher"));
            cmdl.createArgument().setValue("-jp_input=" + paramfile.getAbsolutePath());

            LogStreamHandler handler = new CoverageStreamHandler(this);
            Execute exec = new Execute(handler);
            log(cmdl.describeCommand(), Project.MSG_VERBOSE);
            exec.setCommandline(cmdl.getCommandline());
            int exitValue = exec.execute();
            if (Execute.isFailure(exitValue)) {
                throw new BuildException("JProbe Coverage failed (" + exitValue + ")");
            }
        } catch (IOException e) {
            throw new BuildException("Failed to execute JProbe Coverage.", e);
        } finally {
            if (inputFile == null && paramfile != null) {
                paramfile.delete();
            }
        }
    }

    /** wheck what is necessary to check, Coverage will do the job for us */
    protected void checkOptions() throws BuildException {
        if (getHome() == null || !getHome().isDirectory()) {
            throw new BuildException("Invalid home directory. Must point to JProbe home directory");
        }
        File jar = findCoverageJar();
        if (!jar.exists()) {
            throw new BuildException("Cannot find Coverage directory: " + getHome());
        }

        if (snapshotDir == null) {
            snapshotDir = new File(".");
        }
        snapshotDir = getProject().resolveFile(snapshotDir.getPath());
        if (!snapshotDir.isDirectory() || !snapshotDir.exists()) {
            throw new BuildException("Snapshot directory does not exists :" + snapshotDir);
        }
        if (workingDir == null) {
            workingDir = new File(".");
        }
        workingDir = getProject().resolveFile(workingDir.getPath());

        if (javaExe == null && (vm == null || "java2".equals(vm))) {
            if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
                if (vm == null) {
                    vm = "java2";
                }
                javaExe = new File(JavaEnvUtils.getJreExecutable("java"));
            }
        }
    }

    /**
     * return the command line parameters. Parameters can either be passed
     * to the command line and stored to a file (then use the -jp_input=&lt;filename&gt;)
     * if they are too numerous.
     */
    protected String[] getParameters() {
        Vector params = new Vector();
        params.addElement("-jp_function=" + function);
        if (vm != null) {
            params.addElement("-jp_vm=" + vm);
        }
        if (javaExe != null) {
            params.addElement("-jp_java_exe=" + getProject().resolveFile(javaExe.getPath()));
        }
        params.addElement("-jp_working_dir=" + workingDir.getPath());
        params.addElement("-jp_snapshot_dir=" + snapshotDir.getPath());
        params.addElement("-jp_record_from_start=" + recordFromStart);
        params.addElement("-jp_warn=" + warnLevel);
        if (seedName != null) {
            params.addElement("-jp_output_file=" + seedName);
        }
        params.addElement("-jp_filter=" + filters.toString());
        if (triggers != null) {
            params.addElement("-jp_trigger=" + triggers.toString());
        }
        if (finalSnapshot != null) {
            params.addElement("-jp_final_snapshot=" + finalSnapshot);
        }
        params.addElement("-jp_exit_prompt=" + exitPrompt);
        params.addElement("-jp_track_natives=" + trackNatives);
        String[] vmargs = cmdlJava.getVmCommand().getArguments();
        for (int i = 0; i < vmargs.length; i++) {
            params.addElement(vmargs[i]);
        }
        Path classpath = cmdlJava.getClasspath();
        if (classpath != null && classpath.size() > 0) {
            params.addElement("-classpath " + classpath.toString());
        }
        if (cmdlJava.getClassname() != null) {
            params.addElement(cmdlJava.getClassname());
        }
        String[] args = cmdlJava.getJavaCommand().getArguments();
        for (int i = 0; i < args.length; i++) {
            params.addElement(args[i]);
        }

        String[] array = new String[params.size()];
        params.copyInto(array);
        return array;
    }


    /**
     * create the parameter file from the given options. The file is
     * created with a random name in the current directory.
     * @return the file object where are written the configuration to run
     * JProbe Coverage
     * @throws BuildException thrown if something bad happens while writing
     * the arguments to the file.
     */
    protected File createParamFile() throws BuildException {
        File file = createTempFile("jpcov");
        file.deleteOnExit();
        log("Creating parameter file: " + file, Project.MSG_VERBOSE);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] params = getParameters();
        for (int i = 0; i < params.length; i++) {
            pw.println(params[i]);
        }
        pw.flush();
        log("JProbe Coverage parameters:\n" + sw.toString(), Project.MSG_VERBOSE);

        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write(sw.toString());
            fw.flush();
        } catch (IOException e) {
            throw new BuildException("Could not write parameter file " + file, e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ignored) {
                }
            }
        }
        return file;
    }

    /** specific pumper to avoid those nasty stdin issues */
    static class CoverageStreamHandler extends LogStreamHandler {
        CoverageStreamHandler(Task task) {
            super(task, Project.MSG_INFO, Project.MSG_WARN);
        }

        /**
         * there are some issues concerning all JProbe executable
         * In our case a 'Press ENTER to close this window..." will
         * be displayed in the current window waiting for enter.
         * So I'm closing the stream right away to avoid problems.
         */
        public void setProcessInputStream(OutputStream os) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
        }
    }

}
