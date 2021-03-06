package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/** Synchronize client space to a Perforce depot view.
 *
 *  The API allows additional functionality of the "p4 sync" command
 *
 * <b>Example Usage:</b>
 * <table border="1">
 * <th>Function</th><th>Command</th>
 * <tr><td>Sync to head using P4USER, P4PORT and P4CLIENT settings specified</td>
 * P4User="fbloggs" <br>P4Port="km01:1666" <br>P4Client="fbloggsclient" /&gt;</td></tr>
 * <tr><td>Sync to head using P4USER, P4PORT and P4CLIENT settings defined in environment</td>
 * <tr><td>Force a re-sync to head, refreshing all files</td>
 * <tr><td>Sync to a label</td><td>&lt;P4Sync label="myPerforceLabel" /&gt;</td></tr>
 * </table>
 *
 * @todo Add decent label error handling for non-exsitant labels
 *
 * @ant.task category="scm"
 */
public class P4Sync extends P4Base {

    String label;
    private String syncCmd = "";

    /**
     * Label to sync client to; optional.
     * @param label name of a label against which one want to sync
     * @throws BuildException if label is null or empty string
     */
    public void setLabel(String label) throws BuildException {
        if (label == null || label.equals("")) {
            throw new BuildException("P4Sync: Labels cannot be Null or Empty");
        }

        this.label = label;

    }


    /**
     * force a refresh of files, if this attribute is set; false by default.
     * @param force sync all files, whether they are supposed to be already uptodate or not.
     * @throws BuildException if a label is set and force is null
     */
    public void setForce(String force) throws BuildException {
        if (force == null && !label.equals("")) {
            throw new BuildException("P4Sync: If you want to force, set force to non-null string!");
        }
        P4CmdOpts = "-f";
    }

    /**
     * do the work
     * @throws BuildException if an error occurs during the execution of the Perforce command
     * and failOnError is set to true
     */
    public void execute() throws BuildException {


        if (P4View != null) {
            syncCmd = P4View;
        }


        if (label != null && !label.equals("")) {
            syncCmd = syncCmd + "@" + label;
        }


        log("Execing sync " + P4CmdOpts + " " + syncCmd, Project.MSG_VERBOSE);

        execP4Command("-s sync " + P4CmdOpts + " " + syncCmd, new SimpleP4OutputHandler(this));
    }
}
