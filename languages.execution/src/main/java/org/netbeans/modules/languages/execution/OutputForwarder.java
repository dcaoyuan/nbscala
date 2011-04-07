/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.languages.execution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.netbeans.modules.languages.execution.OutputRecognizer.ActionText;
import org.netbeans.modules.languages.execution.OutputRecognizer.FileLocation;
import org.netbeans.modules.languages.execution.OutputRecognizer.RecognizedOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

/**
 * <p>An OutputForwarder takes output for example from a child process and
 * pumps it into a Program I/O window.
 * In other words it "redirects" or forwards output from a child process.
 * The forwarder also tokenizes the output into lines, and runs
 * {@link OutputRecognizer} objects on the output, which for example may
 * create hyperlinks or record state regarding the output.
 * <p>
 * Great care is taken to make this forwarding interruptible, such that it
 * won't block if process execution is terminated or interrupted. To do this,
 * it's generally going to sleep for short intervals when there is no available
 * I/O rather than calling a blocking I/O operation.
 * 
 * @author Tor Norbye, Caoyuan Deng
 */
final class OutputForwarder implements Runnable {
    
    private static final Logger LOGGER = Logger.getLogger(OutputForwarder.class.getName());
    
    /** Package private for unit test. */
    static final Pattern RANGE_ERROR_RE = Pattern.compile("#<RangeError: 0x[0-9a-f]+ is recycled object>"); // NOI18N
    
    private StopAction stopAction;
    private InputStream str;
    private PrintWriter writer;
    private FileLocator fileLocator;
    private List<OutputRecognizer> recognizers;
    private String role;

    OutputForwarder(InputStream instream, PrintWriter out, FileLocator fileLocator,
        List<OutputRecognizer> recognizers, StopAction stopAction, String role) {
        str = instream;
        writer = out;
        this.fileLocator = fileLocator;
        this.recognizers = recognizers;
        this.stopAction = stopAction;
        this.role = role;
    }

    /** Package private for unit test. */
    void processLine(String line) throws IOException {
        // TODO: workarounding issue 110763
        if (RANGE_ERROR_RE.matcher(line).matches()) {
            LOGGER.log(Level.FINE, "Filtering line from Output Window (issue #110763): " + line);
            return;
        }

        if (Util.containsAnsiColors(line)) {
            line = Util.stripAnsiColors(line);
        }

        FileLocation location = null;
        boolean handled = false;

        for (OutputRecognizer recognizer : recognizers) {
            RecognizedOutput recognizedOutput = recognizer.processLine(line);

            if (recognizedOutput instanceof FileLocation) {
                location = (FileLocation)recognizedOutput;

                // Keep processing to give all processors a chance
                // to interpret the output even if they are not
                // providing a file location. (The Webrick listener
                // for examples records whether a port conflict message
                // is seen.)
            } else if (recognizedOutput instanceof ActionText) {
                ActionText text = (ActionText)recognizedOutput;

                String[] lines = text.getPlainLinesPre();
                if (lines != null) {
                    for (String l : lines) {
                        writer.println(l);
                    }
                }
                lines = text.getActionLines();
                if (lines != null) {
                    Runnable[] actions = text.getActions();
                    for (int i = 0; i < actions.length; i++) {
                        String l = lines[i];
                        if (writer instanceof OutputWriter) {
                            ((OutputWriter) writer).println(l, new ActionHandler(actions[i]));
                        } else {
                            writer.println(l);
                        }
                    }
                }
                lines = text.getPlainLinesPost();
                if (lines != null) {
                    for (String l : lines) {
                        writer.println(l);
                    }
                }
                // @TODO For interactive console, we need flush it at once, but, always?
                writer.flush();
                handled = true;

            } // TODO: Handle other forms of RecognizedOutput
        }

        if (!handled) {
            if (location != null && writer instanceof OutputWriter) {
                ((OutputWriter) writer).println(line, new OutputProcessor(location.file, location.line, fileLocator));
            } else {
                writer.println(line);
            }
        }
    }

    public void run() {
        for (OutputRecognizer recognizer : recognizers) {
            recognizer.start();
        }
        
        BufferedReader read = new BufferedReader(new InputStreamReader(str), 1200);

        StringBuilder sb = new StringBuilder();

        try {
            while (true) {
                if (!read.ready()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        return;
                    }

                    if (stopAction.process == null) {
                        // process finished
                        return;
                    }
                    
                    if (!read.ready() && sb.length() > 0) {
                        // Some output has been written - not a complete line
                        // and the process seems to be stalling so emit what
                        // we've got.
                        String line = sb.toString();
                        sb.setLength(0);

                        writer.print(line);
                        // @TODO For interactive console, we need flush it at once, but, always?
                        writer.flush();
                    }

                    continue;
                }

                while (read.ready()) {
                    int c = -1;
                    c = read.read();

                    if (c == -1) {
                        String line = sb.toString();
                        sb.setLength(0);
                        
                        processLine(line);

                        return;
                    } else if (c == '\n') {
                        String line = sb.toString();
                        sb.setLength(0);

                        processLine(line);

                        //try {
                        //    writer.flush();
                        //} catch (Exception e) {
                        //    // Happens on kill
                        //    return;
                        //}
                    } else {
                        sb.append((char)c);
                    }
                }

                if (Thread.interrupted()) {
                    return;
                }
            }
        } catch (IOException ioexc) {
            LOGGER.log(Level.INFO, ioexc.getMessage(), ioexc);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, t.getMessage(), t);
        } finally {
            try {
                // TODO: interrupted status is cleared, but some comment "why" would be nice.
                if (Thread.interrupted()) {
                    
                }
                boolean processIsRunning;
                if (stopAction.process == null) {
                    processIsRunning = false;
                } else {
                    try {
                        stopAction.process.exitValue(); // Side effect: throws Exception if running
                        // If we get here the process has exited
                        processIsRunning = false;
                    } catch (IllegalThreadStateException its) {
                        processIsRunning = true;
                    }
                }
                
                // Suck the rest of the I/O out of the process
                
                if (processIsRunning) {
                    // Don't do any blocking I/O calls since they will prevent
                    // Process.destroy() from working (at least on some systems);
                    // UNIXProcess.destroy hangs waiting for sockets to close
                    if (str.available() > 0) {
                        String line = read.readLine();

                        sb.append(line);
                    }

                    if (sb.length() > 0) {
                        processLine(sb.toString());
                    }
                } else {
                    // Process terminated
                    while (true) {
                        String line = read.readLine();
                        if (line == null) {
                            break;
                        }
                        sb.append(line);
                        processLine(sb.toString());
                        sb.setLength(0);
                    }
                }
            } catch (IOException e) {
                // TODO: happens because at the end of the debugging session the
                // underlying process is killed.
                LOGGER.log(Level.INFO, "Process finished unexpectedly: " + e.getMessage());
            } finally {
                try {
                    read.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
                writer.flush();
                writer.close();
                
                for (OutputRecognizer recognizer : recognizers) {
                    recognizer.finish();
                }
            }
        }
    }
    
    private class ActionHandler implements OutputListener {
        final private Runnable runnable;
        
        private ActionHandler(Runnable runnable) {
            this.runnable = runnable;
        }

        public void outputLineSelected(OutputEvent ev) {
        }

        public void outputLineAction(OutputEvent ev) {
            runnable.run();
        }

        public void outputLineCleared(OutputEvent ev) {
        }
    }
}
