package org.apache.tools.ant.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Vector;
import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * A set of helper methods related to locating executables or checking
 * conditons of a given Java installation.
 *
 * @since Ant 1.5
 */
public final class JavaEnvUtils {

    private JavaEnvUtils() {
    }

    /** Are we on a DOS-based system */
    private static final boolean IS_DOS = Os.isFamily("dos");
    /** Are we on Novell NetWare */
    private static final boolean IS_NETWARE = Os.isName("netware");
    /** Are we on AIX */
    private static final boolean IS_AIX = Os.isName("aix");

    /** shortcut for System.getProperty("java.home") */
    private static final String JAVA_HOME = System.getProperty("java.home");

    /** FileUtils instance for path normalization */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** Version of currently running VM. */
    private static String javaVersion;

    /** floating version of the JVM */
    private static int javaVersionNumber;

    /** Version constant for Java 1.0 */
    public static final String JAVA_1_0 = "1.0";
    /** Number Version constant for Java 1.0 */
    public static final int VERSION_1_0 = 10;

    /** Version constant for Java 1.1 */
    public static final String JAVA_1_1 = "1.1";
    /** Number Version constant for Java 1.1 */
    public static final int VERSION_1_1 = 11;

    /** Version constant for Java 1.2 */
    public static final String JAVA_1_2 = "1.2";
    /** Number Version constant for Java 1.2 */
    public static final int VERSION_1_2 = 12;

    /** Version constant for Java 1.3 */
    public static final String JAVA_1_3 = "1.3";
    /** Number Version constant for Java 1.3 */
    public static final int VERSION_1_3 = 13;

    /** Version constant for Java 1.4 */
    public static final String JAVA_1_4 = "1.4";
    /** Number Version constant for Java 1.4 */
    public static final int VERSION_1_4 = 14;

    /** Version constant for Java 1.5 */
    public static final String JAVA_1_5 = "1.5";
    /** Number Version constant for Java 1.5 */
    public static final int VERSION_1_5 = 15;

    /** Version constant for Java 1.6 */
    public static final String JAVA_1_6 = "1.6";
    /** Number Version constant for Java 1.6 */
    public static final int VERSION_1_6 = 16;

    /** Whether this is the Kaffe VM */
    private static boolean kaffeDetected;

    /** array of packages in the runtime */
    private static Vector jrePackages;


    static {


        try {
            javaVersion = JAVA_1_0;
            javaVersionNumber = VERSION_1_0;
            Class.forName("java.lang.Void");
            javaVersion = JAVA_1_1;
            javaVersionNumber++;
            Class.forName("java.lang.ThreadLocal");
            javaVersion = JAVA_1_2;
            javaVersionNumber++;
            Class.forName("java.lang.StrictMath");
            javaVersion = JAVA_1_3;
            javaVersionNumber++;
            Class.forName("java.lang.CharSequence");
            javaVersion = JAVA_1_4;
            javaVersionNumber++;
            Class.forName("java.net.Proxy");
            javaVersion = JAVA_1_5;
            javaVersionNumber++;
            Class.forName("java.util.ServiceLoader");
            javaVersion = JAVA_1_6;
            javaVersionNumber++;
        } catch (Throwable t) {
        }
        kaffeDetected = false;
        try {
            Class.forName("kaffe.util.NotImplemented");
            kaffeDetected = true;
        } catch (Throwable t) {
        }
    }

    /**
     * Returns the version of Java this class is running under.
     * @return the version of Java as a String, e.g. "1.1"
     */
    public static String getJavaVersion() {
        return javaVersion;
    }


    /**
     * Returns the version of Java this class is running under.
     * This number can be used for comparisions; it will always be
     * @return the version of Java as a number 10x the major/minor,
     * e.g Java1.5 has a value of 15
     */
    public static int getJavaVersionNumber() {
        return javaVersionNumber;
    }

    /**
     * Compares the current Java version to the passed in String -
     * assumes the argument is one of the constants defined in this
     * class.
     * Note that Ant now requires JDK 1.2+ so {@link #JAVA_1_0} and
     * {@link #JAVA_1_1} need no longer be tested for.
     * @param version the version to check against the current version.
     * @return true if the version of Java is the same as the given version.
     * @since Ant 1.5
     */
    public static boolean isJavaVersion(String version) {
        return javaVersion.equals(version);
    }

    /**
     * Compares the current Java version to the passed in String -
     * assumes the argument is one of the constants defined in this
     * class.
     * Note that Ant now requires JDK 1.2+ so {@link #JAVA_1_0} and
     * {@link #JAVA_1_1} need no longer be tested for.
     * @param version the version to check against the current version.
     * @return true if the version of Java is the same or higher than the
     * given version.
     * @since Ant 1.7
     */
    public static boolean isAtLeastJavaVersion(String version) {
        return javaVersion.compareTo(version) >= 0;
    }

    /**
     * Checks whether the current Java VM is Kaffe.
     * @return true if the current Java VM is Kaffe.
     * @since Ant 1.6.3
     */
    public static boolean isKaffe() {
        return kaffeDetected;
    }

    /**
     * Finds an executable that is part of a JRE installation based on
     * the java.home system property.
     *
     * <p><code>java</code>, <code>keytool</code>,
     * <code>policytool</code>, <code>orbd</code>, <code>rmid</code>,
     * <code>rmiregistry</code>, <code>servertool</code> and
     * <code>tnameserv</code> are JRE executables on Sun based
     * JRE's.</p>
     *
     * <p>You typically find them in <code>JAVA_HOME/jre/bin</code> if
     * <code>JAVA_HOME</code> points to your JDK installation.  JDK
     * &lt; 1.2 has them in the same directory as the JDK
     * executables.</p>
     * @param command the java executable to find.
     * @return the path to the command.
     * @since Ant 1.5
     */
    public static String getJreExecutable(String command) {
        if (IS_NETWARE) {
            return command;
        }

        File jExecutable = null;

        if (IS_AIX) {
            jExecutable = findInDir(JAVA_HOME + "/sh", command);
        }

        if (jExecutable == null) {
            jExecutable = findInDir(JAVA_HOME + "/bin", command);
        }

        if (jExecutable != null) {
            return jExecutable.getAbsolutePath();
        } else {
            return addExtension(command);
        }
    }

    /**
     * Finds an executable that is part of a JDK installation based on
     * the java.home system property.
     *
     * <p>You typically find them in <code>JAVA_HOME/bin</code> if
     * <code>JAVA_HOME</code> points to your JDK installation.</p>
     * @param command the java executable to find.
     * @return the path to the command.
     * @since Ant 1.5
     */
    public static String getJdkExecutable(String command) {
        if (IS_NETWARE) {
            return command;
        }

        File jExecutable = null;

        if (IS_AIX) {
            jExecutable = findInDir(JAVA_HOME + "/../sh", command);
        }

        if (jExecutable == null) {
            jExecutable = findInDir(JAVA_HOME + "/../bin", command);
        }

        if (jExecutable != null) {
            return jExecutable.getAbsolutePath();
        } else {
            return getJreExecutable(command);
        }
    }

    /**
     * Adds a system specific extension to the name of an executable.
     *
     * @since Ant 1.5
     */
    private static String addExtension(String command) {
        return command + (IS_DOS ? ".exe" : "");
    }

    /**
     * Look for an executable in a given directory.
     *
     * @return null if the executable cannot be found.
     */
    private static File findInDir(String dirName, String commandName) {
        File dir = FILE_UTILS.normalize(dirName);
        File executable = null;
        if (dir.exists()) {
            executable = new File(dir, addExtension(commandName));
            if (!executable.exists()) {
                executable = null;
            }
        }
        return executable;
    }

    /**
     * demand creation of the package list.
     * When you add a new package, add a new test below.
     */

    private static void buildJrePackages() {
        jrePackages = new Vector();
        switch(javaVersionNumber) {
            case VERSION_1_6:
            case VERSION_1_5:
                jrePackages.addElement("com.sun.org.apache");
            case VERSION_1_4:
                if (javaVersionNumber == VERSION_1_4) {
                    jrePackages.addElement("org.apache.crimson");
                    jrePackages.addElement("org.apache.xalan");
                    jrePackages.addElement("org.apache.xml");
                    jrePackages.addElement("org.apache.xpath");
                }
                jrePackages.addElement("org.ietf.jgss");
                jrePackages.addElement("org.w3c.dom");
                jrePackages.addElement("org.xml.sax");
            case VERSION_1_3:
                jrePackages.addElement("org.omg");
                jrePackages.addElement("com.sun.corba");
                jrePackages.addElement("com.sun.jndi");
                jrePackages.addElement("com.sun.media");
                jrePackages.addElement("com.sun.naming");
                jrePackages.addElement("com.sun.org.omg");
                jrePackages.addElement("com.sun.rmi");
                jrePackages.addElement("sunw.io");
                jrePackages.addElement("sunw.util");
            case VERSION_1_2:
                jrePackages.addElement("com.sun.java");
                jrePackages.addElement("com.sun.image");
            case VERSION_1_1:
            default:
                jrePackages.addElement("sun");
                jrePackages.addElement("java");
                jrePackages.addElement("javax");
                break;
        }
    }

    /**
     * Testing helper method; kept here for unification of changes.
     * @return a list of test classes depending on the java version.
     */
    public static Vector getJrePackageTestCases() {
        Vector tests = new Vector();
        tests.addElement("java.lang.Object");
        switch(javaVersionNumber) {
            case VERSION_1_6:
            case VERSION_1_5:
                tests.addElement(
                    "com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl ");
            case VERSION_1_4:
                tests.addElement("sun.audio.AudioPlayer");
                if (javaVersionNumber == VERSION_1_4) {
                    tests.addElement("org.apache.crimson.parser.ContentModel");
                    tests.addElement("org.apache.xalan.processor.ProcessorImport");
                    tests.addElement("org.apache.xml.utils.URI");
                    tests.addElement("org.apache.xpath.XPathFactory");
                }
                tests.addElement("org.ietf.jgss.Oid");
                tests.addElement("org.w3c.dom.Attr");
                tests.addElement("org.xml.sax.XMLReader");
            case VERSION_1_3:
                tests.addElement("org.omg.CORBA.Any");
                tests.addElement("com.sun.corba.se.internal.corba.AnyImpl");
                tests.addElement("com.sun.jndi.ldap.LdapURL");
                tests.addElement("com.sun.media.sound.Printer");
                tests.addElement("com.sun.naming.internal.VersionHelper");
                tests.addElement("com.sun.org.omg.CORBA.Initializer");
                tests.addElement("sunw.io.Serializable");
                tests.addElement("sunw.util.EventListener");
            case VERSION_1_2:
                tests.addElement("javax.accessibility.Accessible");
                tests.addElement("sun.misc.BASE64Encoder");
                tests.addElement("com.sun.image.codec.jpeg.JPEGCodec");
            case VERSION_1_1:
            default:
                tests.addElement("sun.reflect.SerializationConstructorAccessorImpl");
                tests.addElement("sun.net.www.http.HttpClient");
                tests.addElement("sun.audio.AudioPlayer");
                break;
        }
        return tests;
    }
    /**
     * get a vector of strings of packages built into
     * that platforms runtime jar(s)
     * @return list of packages.
     */
    public static Vector getJrePackages() {
        if (jrePackages == null) {
            buildJrePackages();
        }
        return jrePackages;
    }

    /**
     *
     * Writes the command into a temporary DCL script and returns the
     * corresponding File object.
     * It is the job of the caller to delete the file on exit.
     * @param cmd the command.
     * @return the file containing the command.
     * @throws IOException if there is an error writing to the file.
     */
    public static File createVmsJavaOptionFile(String[] cmd)
            throws IOException {
        File script = FILE_UTILS.createTempFile("ANT", ".JAVA_OPTS", null, false, true);
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(script)));
            for (int i = 0; i < cmd.length; i++) {
                out.println(cmd[i]);
            }
        } finally {
            FileUtils.close(out);
        }
        return script;
    }

    /**
     * Return the value of ${java.home}
     * @return the java home value.
     */
    public static String getJavaHome() {
        return JAVA_HOME;
    }
}