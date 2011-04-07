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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.InterruptedException;
import java.util.List;

import org.openide.util.Exceptions;


/**
 * <p>An InputForwarder takes user input in for example a Program I/O window,
 * and sends it to a child process. In other words it "redirects" or forwards
 * the input to the child process. This class doesn't actually care if the I/O
 * is in a child process - it just forwards data to a given output stream.
 * <p>
 * Great care is taken to make this forwarding interruptible, such that it
 * won't block if process execution is terminated or interrupted. To do this,
 * it's generally going to sleep for short intervals when there is no available
 * I/O rather than calling a blocking I/O operation.
 * 
 * 
 * @author Tor Norbye
 */
class InputForwarder implements Runnable {
    private OutputStream out;
    private Reader reader;
    private boolean cancelled;

    public InputForwarder(OutputStream outstream, Reader in) {
        out = outstream;
        reader = in;
    }

    public void cancel() {
        synchronized (this) {
            cancelled = true;
        }
    }

    public void run() {
        BufferedReader input = null;
        BufferedWriter output = null;

        try {
            input = new BufferedReader(reader);
            output = new BufferedWriter(new OutputStreamWriter(out));

            char[] buffer = new char[512];

            while (true) {
                if (Thread.interrupted()) {
                    return;
                }

                synchronized (this) {
                    if (cancelled) {
                        return;
                    }
                }

                // At first I just simply had a loop on input.read() here, but
                // that would sometimes block even after the process had exited,
                // requiring input from the user before the output would be marked
                // as complete. So instead I poll now on ready(), and when not,
                // I just sleep.
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    return;
                }

                int lenRead;

                while ((input.ready()) && ((lenRead = input.read(buffer, 0, buffer.length)) > 0)) {
                    output.write(buffer, 0, lenRead);
                    output.flush();
                }
            }
        } catch (Exception ioe) {
            Exceptions.printStackTrace(ioe);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }

                if (output != null) {
                    output.close();
                }
            } catch (Exception ioe) {
                Exceptions.printStackTrace(ioe);
            }
        }
    }
}
