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

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.ErrorManager;
import org.openide.execution.ExecutionEngine;
import org.openide.execution.ExecutorTask;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.TaskListener;
import org.openide.util.Utilities;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;


/**
 * <p>An ExecutionService takes an {@link ExecutionDescriptor} and executes it.
 * It will execute the program with an associated I/O window, with stop and
 * restart buttons. It will also obey various descriptor properties such as
 * whether or not to show a progress bar.
 * <p>
 * All launched processes will be killed on exit. Possibly I could make this
 * optional or at least ask the user.
 * </p>
 * 
 * @todo Add a Restart button which accomplishes both a stop and a restart
 *
 * @author Tor Norbye, Caoyuan Deng
 */
public class ExecutionService {

    public static final Logger LOGGER = Logger.getLogger(ExecutionService.class.getName());
    
    static {
        Thread t =
            new Thread() {
                @Override
                public void run() {
                    ExecutionService.killAll();
                }
            };

        Runtime.getRuntime().addShutdownHook(t);
    }

    /** Display names of currently active processes. */
    private static final Set<String> ACTIVE_DISPLAY_NAMES = new HashSet<String>();

    /**
     * All tabs which were used for some process which has now ended.
     * These are closed when you start a fresh process.
     * Map from tab to tab display name.
     * @see "#43001"
     */
    private static final Map<ExecutionService, String> FREE_TABS =
        new WeakHashMap<ExecutionService, String>();
    
    /** Set of currently active processes. */
    private final static Set<ExecutionService> RUNNING_PROCESSES = new HashSet<ExecutionService>();

    private InputOutput io;
    private StopAction stopAction;
    private RerunAction rerunAction;
    protected ExecutionDescriptor descriptor;
    private String displayName; // May be tweaked from descriptor to deal with duplicate running same-name processes

    private boolean rerun;

    public ExecutionService(ExecutionDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public void setupProcessEnvironment(Map<String,String> env) {
        String path = descriptor.getCmd().getParent();
        if (!Utilities.isWindows()) {
            path = path.replace(" ", "\\ "); // NOI18N
        }

        // Find PATH environment variable - on Windows it can be some other
        // case and we should use whatever it has.
        String pathName = "PATH"; // NOI18N

        if (Utilities.isWindows()) {
            pathName = "Path"; // NOI18N

            for (String key : env.keySet()) {
                if ("PATH".equals(key.toUpperCase())) { // NOI18N
                    pathName = key;

                    break;
                }
            }
        }

        String currentPath = env.get(pathName);

        if (currentPath == null) {
            currentPath = "";
        }

        currentPath = path + File.pathSeparator + currentPath;
        
        if (descriptor.getAppendJdkToPath()) {
            String javaHome = System.getProperty("jruby.java.home"); // NOI18N

            if (javaHome == null) {
                javaHome = System.getProperty("java.home"); // NOI18N
            }
            
            if (javaHome != null) {
                javaHome = javaHome + File.separator + "bin"; // NOI18N
                if (!Utilities.isWindows()) {
                    javaHome = javaHome.replace(" ", "\\ "); // NOI18N
                }
                currentPath = currentPath + File.pathSeparator + javaHome;
            }
        }

        env.put(pathName, currentPath); // NOI18N
    }
    
    public void kill() {
        if (stopAction != null) {
            stopAction.actionPerformed(null);
            if (stopAction.process != null) {
                stopAction.process.destroy();
            }
        }
    }
    
    public static void killAll() {
        for (ExecutionService service : RUNNING_PROCESSES) {
            service.kill();
        }
    }

    Task rerun() {
        try {
            io.getOut().reset();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        rerun = true;
        return run();
    }
    
    /**
     * Retruns list of default arguments and options from the descriptor's
     * <code>initialArgs</code>, <code>script</code> and
     * <code>additionalArgs</code> in that order.
     */
    protected List<? extends String> buildArgs() {
        List<String> argvList = new ArrayList<String>();
        File cmd = descriptor.cmd;
        assert cmd != null;

        if (descriptor.getInitialArgs() != null) {
            argvList.addAll(Arrays.asList(descriptor.getInitialArgs()));
        }

        if (descriptor.script != null) {
            argvList.add(descriptor.script);
        }

        if (descriptor.getAdditionalArgs() != null) {
            argvList.addAll(Arrays.asList(descriptor.getAdditionalArgs()));
        }
        return argvList;
    }

    public Task run() {
        if (!rerun) {
            // try to find free output windows
            synchronized (this) {
                synchronized (FREE_TABS) {
                    for (Iterator<Map.Entry<ExecutionService, String>> it = FREE_TABS.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<ExecutionService, String> entry = it.next();
                        ExecutionService free = entry.getKey();

                        //InputOutput free = entry.getKey();
                        String freeName = entry.getValue();
                        if (free.io.isClosed()) {
                            it.remove();
                            continue;
                        }

                        if ((free != this) && (io == null) && ExecutionService.isAppropriateName(descriptor.getDisplayName(), freeName)) {
                            // Reuse it.
                            displayName = freeName;
                            io = free.io;
                            stopAction = free.stopAction;
                            rerunAction = free.rerunAction;

                            try {
                                io.getOut().reset();
                            } catch (IOException ioe) {
                                Exceptions.printStackTrace(ioe);
                            }

                            // Apparently useless and just prints warning: io.getErr().reset();
                            // useless: io.flushReader();

                            if (descriptor.frontWindow) {
                                io.select();
                            }
                            it.remove();
                        }// else {
                            // if ('auto close tabs' options implemented and checked) { // see #47753
                            //   free.io.closeInputOutput();
                            // }
                        //}
                    }
                }
            }
            
            if ((io == null) || (stopAction == null)) { // free OW wa not found
                displayName = getNonActiveDisplayName(descriptor.getDisplayName());

                stopAction = new StopAction();
                rerunAction = new RerunAction(this, descriptor.getFileObject());

                if (io == null) {
                    io = IOProvider.getDefault()
                                   .getIO(displayName, new Action[] { rerunAction, stopAction });

                    try {
                        io.getOut().reset();
                    } catch (IOException exc) {
                        ErrorManager.getDefault().notify(exc);
                    }

                    // Note - do this AFTER the reset() call above; if not, weird bugs occur
                    io.setErrSeparated(false);

                    // Open I/O window now. This should probably be configurable.
                    if (descriptor.frontWindow) {
                        io.select();
                    }
                }
            }
        }

        ACTIVE_DISPLAY_NAMES.add(displayName);
        io.setInputVisible(descriptor.inputVisible);
        
        //io.getErr().println(NbBundle.getMessage(RubyExecutionService.class, "RunStarting"));
        
        return createTask(io.getIn(), io.getOut(), io.getErr());
    }
    
    public Task run(Reader in, PrintWriter out, PrintWriter err) {
        stopAction = new StopAction();
        rerunAction = new RerunAction(this, descriptor.getFileObject());
        return createTask(in, out, err);
    }
        
    public Task createTask(final Reader in, final PrintWriter out, final PrintWriter err) {
        Runnable runnable =
            new Runnable() {
                public void run() {
                    File cmd = descriptor.cmd;
                    try {
                        Process process = null;
                        if (descriptor.debug) {
                            DebuggerImplementation debugger = Lookup.getDefault().lookup(descriptor.debugger);
                            if (debugger != null) {
                                process = debugger.debug(descriptor);
                            }
                            if (process == null) { 
                                return; 
                            }
                        } else {
                            List<String> commandL = new ArrayList<String>();
                            if (!descriptor.rebuildCmd) {
                                commandL.add(cmd.getPath());
                            }
                            
                            List<? extends String> args = buildArgs();
                            commandL.addAll(args);
                            String[] command = commandL.toArray(new String[commandL.size()]);
                            
                            if ((command != null) && Utilities.isWindows()) {
                                for (int i = 0; i < command.length; i++) {
                                    if ((command[i] != null) && (command[i].indexOf(' ') != -1) &&
                                            (command[i].indexOf('"') == -1)) { // NOI18N
                                        command[i] = '"' + command[i] + '"'; // NOI18N
                                    }
                                }
                            }
                            ProcessBuilder pb = new ProcessBuilder(command);
                            pb.directory(descriptor.pwd);
                            
                            if (descriptor.addBinPath) {
                                Map<String, String> env = pb.environment();
                                setupProcessEnvironment(env);
                            }
                            Util.adjustProxy(pb);
                            ExecutionService.logProcess(pb);
                            process = pb.start();
                        }
                        
                        RUNNING_PROCESSES.add(ExecutionService.this);
                        stopAction.process = process;
                        runIO(stopAction, process, in, out, err,
                                descriptor.getFileLocator(), descriptor.outputRecognizers);
                 
                        process.waitFor();
                    } catch (IOException ex) {
                        ErrorManager.getDefault().notify(ex);
                    } catch (InterruptedException ex) {
                        ErrorManager.getDefault().notify(ex);
                    }
                }
            };

        final ProgressHandle handle;

        if (descriptor.showProgress || descriptor.showSuspended) {
            handle =
                ProgressHandleFactory.createHandle(displayName,
                    new Cancellable() {
                        public boolean cancel() {
                            stopAction.actionPerformed(null);

                            return true;
                        }
                    },
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            if (io != null) {
                                io.select();
                            }
                        }
                    });
            handle.start();
            handle.switchToIndeterminate();

            if (descriptor.showSuspended) {
                handle.suspend(NbBundle.getMessage(ExecutionService.class, "Running"));
            }
        } else {
            handle = null;
        }

        stopAction.setEnabled(true);
        rerunAction.setEnabled(false);

        ExecutorTask task = ExecutionEngine.getDefault().execute(null, runnable, InputOutput.NULL);
        
        task.addTaskListener(new TaskListener() {
                public void taskFinished(Task task) {
                    RUNNING_PROCESSES.remove(ExecutionService.this);

                    if (io != null) {
                        synchronized (FREE_TABS) {
                            FREE_TABS.put(ExecutionService.this, displayName);
                        }
                    }

                    ACTIVE_DISPLAY_NAMES.remove(displayName);

                    //if (task instanceof ExecutorTask) {
                    //    result = ((ExecutorTask)task).result();
                    //}
                    //
                    //                    if (result == 0) {
                    //                        System.err.println(NbBundle.getMessage(RubyExecutionService.class,
                    //                                "RunCompleted"));
                    //                    } else {
                    //                        System.err.println(NbBundle.getMessage(RubyExecutionService.class,
                    //                                "RunFailed", result));
                    //                    }
                    if (descriptor.postBuildAction != null) {
                        descriptor.postBuildAction.run();
                    }

                    if (handle != null) {
                        handle.finish();
                    }

                    stopAction.setEnabled(false);
                    rerunAction.setEnabled(true);

                    if (stopAction.process != null) {
                        stopAction.process.destroy();
                        stopAction.process = null;
                    }
                }
            });

        return task;
        
    }

    private static void runIO(final StopAction sa, Process process,
            Reader targetIn, PrintWriter targetOut, PrintWriter targetErr,
            FileLocator fileLocator, List<OutputRecognizer> recognizers) {
        try {
            InputForwarder in = new InputForwarder(process.getOutputStream(), targetIn);
            OutputForwarder out = new OutputForwarder(process.getInputStream(), targetOut, fileLocator,
                    recognizers, sa, "Output"); // NOI18N
            OutputForwarder err = new OutputForwarder(process.getErrorStream(), targetErr, fileLocator,
                    recognizers, sa, "Error"); // NOI18N
            
            RequestProcessor rProcessor = new RequestProcessor(
		    "Process Execution Stream Handler", 3, true); // NOI18N
            
            TaskListener tl =
                    new TaskListener() {
                public void taskFinished(Task task) {
                    sa.notifyDone((RequestProcessor.Task)task);
                }
            };

            
            RequestProcessor.Task outTask = rProcessor.post(out);
            RequestProcessor.Task errTask = rProcessor.post(err);
            RequestProcessor.Task inTask  = rProcessor.post(in);

            
            outTask.addTaskListener(tl);
            errTask.addTaskListener(tl);
            inTask.addTaskListener(tl);
            
            sa.processorTasks.add(outTask);
            sa.processorTasks.add(errTask);
            sa.processorTasks.add(inTask);


            
            process.waitFor();
            sa.process = null;
            
            in.cancel();
            outTask.waitFinished();
            errTask.waitFinished();
            inTask.waitFinished();
            
            rProcessor.stop();
        } catch (InterruptedException exc) {
            // XXX Uhm... why do we log this? Isn't this a good thing?
            // This happens if we try to cancel the process for example
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, exc);
        }
    }
    
    static boolean isAppropriateName(String base, String toMatch) {
        if (!toMatch.startsWith(base)) {
            return false;
        }
        return toMatch.substring(base.length()).matches("^(\\ #[0-9]+)?$"); // NOI18N
    }

    private static String getNonActiveDisplayName(final String displayNameBase) {
        String nonActiveDN = displayNameBase;
        if (ACTIVE_DISPLAY_NAMES.contains(nonActiveDN)) {
            // Uniquify: "prj (targ) #2", "prj (targ) #3", etc.
            int i = 2;
            String testdn;

            do {
                testdn = NbBundle.getMessage(ExecutionService.class, "Uniquified", nonActiveDN, i++);
            } while (ACTIVE_DISPLAY_NAMES.contains(testdn));

            nonActiveDN = testdn;
        }
        assert !ACTIVE_DISPLAY_NAMES.contains(nonActiveDN);
        return nonActiveDN;
    }

    /** Just helper method for logging. */
    public static void logProcess(final ProcessBuilder pb) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Running (basedir: " + pb.directory().getAbsolutePath() + "): \"" + getProcessAsString(pb.command()) + '"');
            LOGGER.fine("Environment: " + pb.environment());
        }
    }

    /** Just helper method for logging. */
    private static String getProcessAsString(List<? extends String> process) {
        
        StringBuilder sb = new StringBuilder();
        for (String arg : process) {
            sb.append(arg).append(' ');
        }
        return sb.toString().trim();
    }
}
