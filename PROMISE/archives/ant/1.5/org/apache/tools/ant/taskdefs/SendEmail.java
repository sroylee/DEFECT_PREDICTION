package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.taskdefs.email.EmailTask;

/**
 * A task to send SMTP email. 
 * This task can send mail using either plain
 * text, UU encoding or Mime format mail depending on what is available.
 * Attachments may be sent using nested FileSet
 * elements.
 
 * @author glenn_twiggs@bmc.com
 * @author Magesh Umasankar
 *
 * @since Ant 1.2
 *
 * @ant.task name="mail" category="network"
 */
public class SendEmail extends EmailTask {
    /**
     * Sets the mailport parameter of this build task.
     * @param value mail port name.
     *
     * @deprecated Use {@link #setMailport(int)} instead.
     */
    public void setMailport(Integer value) {
        setMailport(value.intValue());
    }
}
