package org.apache.tools.ant.types.selectors;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.BuildException;

/**
 * Selector that filters files based on whether they appear in another
 * directory tree. It can contain a mapper element, so isn't available
 * as an ExtendSelector (since those parameters can't hold other
 * elements).
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since 1.5
 */
public class PresentSelector extends BaseSelector {

    private File targetdir = null;
    private Mapper mapperElement = null;
    private FileNameMapper map = null;
    private boolean destmustexist = true;

    public PresentSelector() {
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("{presentselector targetdir: ");
        if (targetdir == null) {
            buf.append("NOT YET SET");
        }
        else {
            buf.append(targetdir.getName());
        }
        buf.append(" present: ");
        if (destmustexist) {
            buf.append("both");
        } else {
            buf.append("srconly");
        }
        if (map != null) {
            buf.append(map.toString());
        }
        else if (mapperElement != null) {
            buf.append(mapperElement.toString());
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * The name of the file or directory which is checked for matching
     * files.
     *
     * @param targetdir the directory to scan looking for matching files.
     */
    public void setTargetdir(File targetdir) {
        this.targetdir = targetdir;
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper");
        }
        mapperElement = new Mapper(project);
        return mapperElement;
    }


    /**
     * This sets whether to select a file if its dest file is present.
     * It could be a <code>negate</code> boolean, but by doing things
     * this way, we get some documentation on how the system works.
     * A user looking at the documentation should clearly understand
     * that the ONLY files whose presence is being tested are those
     * that already exist in the source directory, hence the lack of
     * a <code>destonly</code> option.
     *
     * @param fp An attribute set to either <code>srconly</code or
     *           <code>both</code>.
     */
    public void setPresent(FilePresence fp) {
        if (fp.getIndex() == 0) {
            destmustexist = false;
        }
    }

    /**
     * Checks to make sure all settings are kosher. In this case, it
     * means that the targetdir attribute has been set and we have a mapper.
     */
    public void verifySettings() {
        if (targetdir == null) {
            setError("The targetdir attribute is required.");
        }
        if (mapperElement == null) {
            map = new IdentityMapper();
        }
        else {
            map = mapperElement.getImplementation();
        }
        if (map == null) {
            setError("Could not set <mapper> element.");
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset.
     *
     * @param basedir the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file is a java.io.File object the selector can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {

        validate();

        String[] destfiles = map.mapFileName(filename);
        if (destfiles == null) {
            return false;
        }
        if (destfiles.length != 1 || destfiles[0] == null) {
            throw new BuildException("Invalid destination file results for "
                + targetdir + " with filename " + filename);
        }
        String destname = destfiles[0];
        File destfile = new File(targetdir,destname);
        return destfile.exists() == destmustexist;
    }

    /**
     * Enumerated attribute with the values for indicating where a file's
     * presence is allowed and required.
     */
    public static class FilePresence extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"srconly", "both"};
        }
    }

}

