package org.apache.tools.ant;

import java.io.PrintStream;

/**
 * Interface used by Ant to log the build output.
 *
 * A build logger is a build listener which has the 'right' to send output to
 * the ant log, which is usually <code>System.out</code> unless redirected by
 * the <code>-logfile</code> option.
 *
 */
public interface BuildLogger extends BuildListener {

    /**
     * Sets the highest level of message this logger should respond to.
     *
     * Only messages with a message level lower than or equal to the
     * given level should be written to the log.
     * <P>
     * Constants for the message levels are in the
     * {@link Project Project} class. The order of the levels, from least
     * to most verbose, is <code>MSG_ERR</code>, <code>MSG_WARN</code>,
     * <code>MSG_INFO</code>, <code>MSG_VERBOSE</code>,
     * <code>MSG_DEBUG</code>.
     *
     * @param level the logging level for the logger.
     */
    void setMessageOutputLevel(int level);

    /**
     * Sets the output stream to which this logger is to send its output.
     *
     * @param output The output stream for the logger.
     *               Must not be <code>null</code>.
     */
    void setOutputPrintStream(PrintStream output);

    /**
     * Sets this logger to produce emacs (and other editor) friendly output.
     *
     * @param emacsMode <code>true</code> if output is to be unadorned so that
     *                  emacs and other editors can parse files names, etc.
     */
    void setEmacsMode(boolean emacsMode);

    /**
     * Sets the output stream to which this logger is to send error messages.
     *
     * @param err The error stream for the logger.
     *            Must not be <code>null</code>.
     */
    void setErrorPrintStream(PrintStream err);
}