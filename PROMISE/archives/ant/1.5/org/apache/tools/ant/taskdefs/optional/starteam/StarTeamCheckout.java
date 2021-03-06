package org.apache.tools.ant.taskdefs.optional.starteam;

import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.Status;
import com.starbase.starteam.TypeNames;
import com.starbase.starteam.View;
import com.starbase.starteam.ViewConfiguration;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Checks out files from a StarTeam project.
 * It also creates all working directories on the
 * local directory if appropriate. Ant Usage:
 * <pre>
 * &lt;taskdef name="starteamcheckout"
 * classname="org.apache.tools.ant.taskdefs.StarTeamCheckout"/&gt;
 * &lt;starteamcheckout username="BuildMaster" password="ant" starteamFolder="Source"
 * starteamurl="servername:portnum/project/view"
 * createworkingdirectories="true"/&gt;
 * </pre>
 *
 * @author Christopher Charlier, ThoughtWorks, Inc. 2001
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Jason Pettiss
 * @author <a href="mailto:stevec@ignitesports.com">Steve Cohen</a>
 * @version 1.1
 *
 * @ant.task name="stcheckout" category="scm"
 */
public class StarTeamCheckout extends TreeBasedTask {

    /**
     * holder for the createDirs attribute
     */
    private boolean createDirs = true;

    /**
     * holder for the deleteUncontrolled attribute.  If true,
     * all local files not in StarTeam will be deleted.
     */
    private boolean deleteUncontrolled = true;

    /**
     * flag (defaults to true) to create all directories
     * that are in the Starteam repository even if they are empty.
     *
     * @param value  the value to set the attribute to.
     */
    public void setCreateWorkingDirs(boolean value) {
        this.createDirs = value;
    }

    /**
     * Whether or not all local files <i>not<i> in StarTeam should be deleted.
     * Optional, defaults to <code>true</code>.
     * @param value  the value to set the attribute to.
     */
    public void setDeleteUncontrolled(boolean value) {
        this.deleteUncontrolled = value;
    }

    /**
     * Sets the label StarTeam is to use for checkout; defaults to the most recent file.
     * The label must exist in starteam or an exception will be thrown. 
     * @param label the label to be used
     */
    public void setLabel(String label) {
        _setLabel(label);
    }

    /**
     * This attribute tells whether to do a locked checkout, an unlocked
     * checkout or to leave the checkout status alone (default).  A locked
     * checkout locks all other users out from making changes.  An unlocked
     * checkout reverts all local files to their previous repository status
     * and removes the lock.
     * @see #setLocked(boolean)
     * @see #setUnlocked(boolean)
     */
    private int lockStatus = Item.LockType.UNCHANGED;

    /**
     * Set to do a locked checkout; optional default is false. 
     * @param v  True to do a locked checkout, false to checkout without
     *           changing status/.
     * @exception BuildException if both locked and unlocked are set true
     */
    public void setLocked(boolean v) throws BuildException {
        setLockStatus(v, Item.LockType.EXCLUSIVE);
    }


    /**
     * Set to do an unlocked checkout. Default is false;
     * @param v  True to do an unlocked checkout, false to checkout without
     *           changing status.
     * @exception BuildException if both locked and unlocked are set true
     */
    public void setUnlocked(boolean v) throws BuildException {
        setLockStatus(v, Item.LockType.UNLOCKED);
    }

    private void setLockStatus(boolean v, int newStatus)
            throws BuildException {
        if (v) {
            if (this.lockStatus == Item.LockType.UNCHANGED) {
                this.lockStatus = newStatus;
            } else if (this.lockStatus != newStatus) {
                throw new BuildException(
                        "Error: cannot set locked and unlocked both true.");
            }
        }
    }

    /**
     * should checked out files get the timestamp from the repository
     * or the time they are checked out.  True means use the repository 
     * timestamp.
     */
    private boolean useRepositoryTimeStamp = false;

    /**
     * sets the useRepositoryTimestmp member.
     * 
     * @param useRepositoryTimeStamp
     *               true means checked out files will get the repository timestamp.
     *               false means the checked out files will be timestamped at the time
     *               of checkout.
     */
    public void setUseRepositoryTimeStamp(boolean useRepositoryTimeStamp)
    {
        this.useRepositoryTimeStamp = useRepositoryTimeStamp;
    }

    /**
     * returns the value of the useRepositoryTimestamp member
     * 
     * @return the value of the useRepositoryTimestamp member
     */
    public boolean getUseRepositoryTimeStamp() {
        return this.useRepositoryTimeStamp;
    }
    /**
     * Override of base-class abstract function creates an
     * appropriately configured view for checkouts - either
     * the current view or a view from this.label or the raw
     * view itself in the case of a revision label.
     * 
     * @param raw    the unconfigured <code>View</code>
     * 
     * @return the snapshot <code>View</code> appropriately configured.
     * @exception BuildException
     */
    protected View createSnapshotView(View raw) 
    throws BuildException
    {

        int labelID = getLabelID(raw);

        if (this.isUsingViewLabel()) {
            return new View(raw, ViewConfiguration.createFromLabel(labelID));
        } 
        else if (this.isUsingRevisionLabel()) {
            return raw;
        }
        else {
            return new View(raw, ViewConfiguration.createTip());
        }
    }

    /**
     * Implements base-class abstract function to define tests for
     * any preconditons required by the task.
     *
     * @exception BuildException thrown if both rootLocalFolder 
     * and viewRootLocalFolder are defined
     */
    protected void testPreconditions() throws BuildException {
        if (this.isUsingRevisionLabel() && this.createDirs) {
            log("Ignoring createworkingdirs while using a revision label." +
                "  Folders will be created only as needed.",
                Project.MSG_WARN);
            this.createDirs=false;
        }
    }

    /**
     * extenders should emit to the log an entry describing the parameters
     * that will be used by this operation.
     * 
     * @param starteamrootFolder
     *               root folder in StarTeam for the operation
     * @param targetrootFolder
     *               root local folder for the operation (whether specified 
     * by the user or not.
     */

    protected void logOperationDescription(
        Folder starteamrootFolder, java.io.File targetrootFolder)
    {
        log((this.isRecursive() ? "Recursive" : "Non-recursive") + 
            " Checkout from: " + starteamrootFolder.getFolderHierarchy());

        log("  Checking out to" 
            + (null == getRootLocalFolder() ? "(default): " : ": ") 
            + targetrootFolder.getAbsolutePath());


        logLabel();
        logIncludes();
        logExcludes();

        if (this.lockStatus == Item.LockType.EXCLUSIVE) {
            log("  Items will be checked out with Exclusive locks.");
        }
        else if (this.lockStatus == Item.LockType.UNLOCKED) {
            log("  Items will be checked out unlocked (even if presently locked).");
        } 
        else {
            log("  Items will be checked out with no change in lock status.");
        }
        log("  Items will be checked out with " + 
            (this.useRepositoryTimeStamp ? "repository timestamps."  
                                        : "the current timestamp."));
        log("  Items will be checked out " +
            (this.isForced() ? "regardless of" : "in accordance with") +
            " repository status.");
        if (this.deleteUncontrolled) {
            log("  Local items not found in the repository will be deleted.");
        }
        log("  Directories will be created"+
            (this.createDirs ? " wherever they exist in the repository, even if empty." 
                             : " only where needed to check out files."));
        
    }
    /**
     * Implements base-class abstract function to perform the checkout
     * operation on the files in each folder of the tree.
     *
     * @param starteamFolder the StarTeam folder from which files to be
     *                       checked out
     * @param targetFolder the local mapping of rootStarteamFolder
     * @exception BuildException if any error occurs
     */
    protected void visit(Folder starteamFolder, java.io.File targetFolder)
            throws BuildException 
    {  
        try {


            if (null != getRootLocalFolder()) {
                starteamFolder.setAlternatePathFragment(
                    targetFolder.getAbsolutePath());
            }
            
            if (!targetFolder.exists()) {
                if (!this.isUsingRevisionLabel()) {
                    if (this.createDirs) {
                        if (targetFolder.mkdirs()) {
                            log("Creating folder: " + targetFolder);
                        } else {
                            throw new BuildException(
                                "Failed to create local folder " + targetFolder);
                        }
                    }
                }
            }
            
            
            Folder[] foldersList = starteamFolder.getSubFolders();
            Item[] filesList = starteamFolder.getItems(getTypeNames().FILE);

            if (this.isUsingRevisionLabel()) {

                
                Hashtable labelItems = new Hashtable(filesList.length);
                int s = filesList.length;
                int[] ids = new int[s];
                for (int i=0; i < s; i++) {
                    ids[i]=filesList[i].getItemID();
                    labelItems.put(new Integer(ids[i]), new Integer(i));
                }
                int[] foundIds = getLabelInUse().getLabeledItemIDs(ids);
                s = foundIds.length;
                Item[] labeledFiles = new Item[s];
                for (int i=0; i < s; i++) {
                    Integer ID = new Integer(foundIds[i]);
                    labeledFiles[i] = 
                        filesList[((Integer) labelItems.get(ID)).intValue()];
                }
                filesList = labeledFiles;
            }
            
            

            UnmatchedFileMap ufm = 
                new CheckoutMap().
                    init(targetFolder.getAbsoluteFile(), starteamFolder);



            for (int i = 0; i < foldersList.length; i++) {
                Folder stFolder = foldersList[i];

                java.io.File subfolder = 
                     new java.io.File(targetFolder, stFolder.getName());

                 ufm.removeControlledItem(subfolder);

                 if (isRecursive()) {
                         visit(stFolder, subfolder);
                     }
                 }

            for (int i = 0; i < filesList.length; i++) {
                com.starbase.starteam.File stFile = 
                    (com.starbase.starteam.File) filesList[i];
                processFile( stFile, targetFolder);
                
                ufm.removeControlledItem(
                    new java.io.File(targetFolder, stFile.getName()));
            }
            if (this.deleteUncontrolled) {
                ufm.processUncontrolledItems();
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }


    /**
     * provides a string showing from and to full paths for logging
     * 
     * @param remotefile the Star Team file being processed.
     * 
     * @return a string showing from and to full paths
     */
    private String describeCheckout(com.starbase.starteam.File remotefile,
                                    java.io.File localFile)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(getFullRepositoryPath(remotefile))
          .append(" --> ");
        if (null == localFile) {
            sb.append(remotefile.getFullName());
        } else {
            sb.append(localFile);
        }
        return sb.toString();
    }
    private String describeCheckout(com.starbase.starteam.File remotefile) {
        return describeCheckout(remotefile,null);
    }
    /**
     * Processes (checks out) <code>stFiles</code>files from StarTeam folder.
     *
     * @param eachFile repository file to process
     * @param targetFolder a java.io.File (Folder) to work
     * @throws IOException when StarTeam API fails to work with files
     */
    private void processFile(com.starbase.starteam.File eachFile, 
                             java.io.File targetFolder )
    throws IOException 
    {
        String filename = eachFile.getName();

        java.io.File localFile = new java.io.File(targetFolder, filename);

        if (!shouldProcess(filename)) {
            log("Excluding " + getFullRepositoryPath(eachFile), 
                Project.MSG_INFO);
                return;
        }

        if (this.isUsingRevisionLabel()) {
            if (!targetFolder.exists()) {
                if (targetFolder.mkdirs()) {
                    log("Creating folder: " + targetFolder);
                } else {
                    throw new BuildException(
                        "Failed to create local folder " + targetFolder);
                }
            }
            boolean success = eachFile.checkoutByLabelID(
                localFile,
                getIDofLabelInUse(),
                this.lockStatus,
                !this.useRepositoryTimeStamp,
                true,
                false);
            if (success) {
                log("Checked out " + describeCheckout(eachFile, localFile));
            }
        }
        else {
            boolean checkout = true;


            int fileStatus = (eachFile.getStatus());


            if (fileStatus == Status.MERGE || 
                fileStatus == Status.UNKNOWN) 
            {
                eachFile.updateStatus(true, true);
                fileStatus = (eachFile.getStatus());
            }

            log(eachFile.toString() + " has status of " + 
                Status.name(fileStatus), Project.MSG_DEBUG);


            switch (fileStatus) {
            case Status.OUTOFDATE:
            case Status.MISSING:
                log("Checking out: " + describeCheckout(eachFile));
                break;
            default:
                if (isForced()) {
                    log("Forced checkout of " 
                        + describeCheckout(eachFile) 
                        + " over status " + Status.name(fileStatus));
                } else {
                    log("Skipping: " + getFullRepositoryPath(eachFile) + 
                        " - status: " + Status.name(fileStatus));
                    checkout = false;
                }
            }

            if (checkout) {
                if (!targetFolder.exists()) {
                    if (targetFolder.mkdirs()) {
                        log("Creating folder: " + targetFolder);
                    } else {
                        throw new BuildException(
                            "Failed to create local folder " + targetFolder);
                    }
                }
                eachFile.checkout(this.lockStatus, 
                                 !this.useRepositoryTimeStamp, true, true);
            }
        }
    }
    /**
     * handles the deletion of uncontrolled items
     */
    private class CheckoutMap extends UnmatchedFileMap {
        protected boolean isActive() {
            return StarTeamCheckout.this.deleteUncontrolled;
        }

        /**
         * override of the base class init.  It can be much simpler, since
         * the action to be taken is simply to delete the local files.  No
         * further interaction with the repository is necessary.
         * 
         * @param localFolder
         *        the local folder from which the mappings will be made.
         * @param remoteFolder
         *        not used in this implementation
         */
        UnmatchedFileMap init(java.io.File localFolder, Folder remoteFolder) {
            if (!localFolder.exists()) {
                return this;
            }

            String[] localFiles = localFolder.list();
    
            for (int i=0; i < localFiles.length; i++) {
                java.io.File localFile = 
                    new java.io.File(localFolder, localFiles[i]).getAbsoluteFile();
                
                log("adding " + localFile + " to UnmatchedFileMap",
                    Project.MSG_DEBUG);
    
                if (localFile.isDirectory()) {
                    this.put(localFile, "");
                } 
                else {
                    this.put(localFile, "");
                }
            }
            return this;
        }


    
        /**
         * deletes uncontrolled items from the local tree.  It is assumed
         * that this method will not be called until all the items in the
         * corresponding folder have been processed, and that the internal map
         * will contain only uncontrolled items.
         */
        void processUncontrolledItems() throws BuildException {
            if (this.isActive()) {
                Enumeration e = this.keys();
                while (e.hasMoreElements()) {
                    java.io.File local = (java.io.File) e.nextElement();
                    delete(local);
                }
            }
        }
    
        /**
         * deletes all files and if the file is a folder recursively deletes
         * everything in it.
         * 
         * @param local  The local file or folder to be deleted.
         */
        void delete(java.io.File local) {
            if (local.isDirectory() && isRecursive()) {
                String[] contents = local.list();
                for (int i=0; i< contents.length; i++) {
                    java.io.File file = new java.io.File(local, contents[i]);
                    delete(file);
                }
            } 
            local.delete();
            log("Deleted uncontrolled item " + local.getAbsolutePath());
        }
    }


}
