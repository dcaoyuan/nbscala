/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import java.text.DateFormat;

import java.util.Date;

import xtc.Constants;

import xtc.parser.ParseException;

import xtc.tree.Printer;
import xtc.tree.Node;
import xtc.tree.VisitingException;

/**
 * The superclass of all tools.
 *
 * @author Robert Grimm
 * @version $Revision: 1.26 $
 */
public abstract class Tool {

  /** The runtime. */
  protected final Runtime runtime;

  /** Create a new tool. */
  public Tool() {
    runtime = new Runtime();
  }

  /**
   * Get this tool's name.
   *
   * @return The name.
   */
  public abstract String getName();

  /**
   * Get this tool's version.  The default implementation returns
   * {@link Constants#VERSION}.
   *
   * @return The version.
   */
  public String getVersion() {
    return Constants.VERSION;
  }

  /**
   * Get this tool's copyright.  The default implementation returns
   * {@link Constants#FULL_COPY}.
   *
   * @return The copyright.
   */
  public String getCopy() {
    return Constants.FULL_COPY;
  }

  /**
   * Get this tool's explanation.  This method should return any text
   * to print after this tool's description of options.  The text is
   * automatically line-wrapped.  The default implementation returns
   * <code>null</code> to indicate that there is no explanation.
   *
   * @return The explanation.
   */
  public String getExplanation() {
    return null;
  }

  /**
   * Initialize this tool.  This method declares this tool's command
   * line options.  The default implementation declares<ul>
   *
   * <li>a boolean option <code>optionSilent</code> for silent
   * operation,</li>
   *
   * <li>a boolean option <code>optionVerbose</code> for verbose
   * operation,</li>
   *
   * <li>a boolean option <code>optionNoExit</code> for not exiting
   * the Java virtual machine,</li>
   *
   * <li>a multiple directory option {@link Runtime#INPUT_DIRECTORY}
   * for the file search path,</li>
   *
   * <li>a directory option {@link Runtime#OUTPUT_DIRECTORY} for the
   * output directory,</li>
   *
   * <li>a word option {@link Runtime#INPUT_ENCODING} for the
   * character encoding when reading files,</li>
   *
   * <li>a word option {@link Runtime#OUTPUT_ENCODING} for the
   * character encoding when writing files,<li>
   *
   * <li>a boolean option <code>optionDiagnostics</code> for printing
   * tool diagnostics,</li>

   * <li>a boolean option <code>optionPerformance</code> for
   * collecting performance statistics,</li>
   *
   * <li>a boolean option <code>optionMeasureParser</code> for
   * measuring parser performance only.</li>
   *
   * <li>a boolean option <code>optionMeasureProcessing</code> for
   * measuring processing performance only.</li>
   *
   * <li>a boolean option <code>optionGC</code> for performing
   * GC,</li>
   *
   * <li>an integer option <code>runsWarmUp</code> with a default of 2
   * for the number of warm-up runs,</li>
   *
   * <li>and an integer option <code>runsTotal</code> with a default
   * of 12 for the total number of runs.</li>
   *
   * </ul>
   */
  public void init() {
    runtime.
      bool("silent", "optionSilent", false,
           "Enable silent operation.").
      bool("verbose", "optionVerbose", false,
           "Enable verbose operation.").
      bool("no-exit", "optionNoExit", false,
           "Do not explicitly exit the Java virtual machine.").
      dir("in", Runtime.INPUT_DIRECTORY, true,
          "Add the specified directory to the file search path.").
      dir("out", Runtime.OUTPUT_DIRECTORY, false,
          "Use the specified directory for output.").
      word("enc-in", Runtime.INPUT_ENCODING, false,
           "Use the specified character encoding for input.").
      word("enc-out", Runtime.OUTPUT_ENCODING, false,
           "Use the specified character encoding for output.").
      bool("diagnostics", "optionDiagnostics", false,
           "Print diagnostics for internal tool state.").
      bool("performance", "optionPerformance", false,
           "Collect and print performance statistics.").
      bool("measureParser", "optionMeasureParser", false,
           "Measure parser performance only.").
      bool("measureProcess", "optionMeasureProcessing", false,
           "Measure processing performance only.").
      bool("gc", "optionGC", false,
           "Perform GC before each operation.").
      number("warmupRuns", "runsWarmUp", 2,
             "Perform the specified number of warm-up runs.  The default is 2.").
      number("totalRuns", "runsTotal", 12,
             "Perform the specified number of total runs.  The default is 12.");
  }

  /**
   * Prepare for processing.  This method prepares for actually
   * processing files, for example, by performing consistency checks
   * between command line arguments and by initializing all default
   * values not specified on the command line.  The default
   * implementation invokes {@link Runtime#initDefaultValues()}.  It
   * also checks that the <code>optionSilent</code> and
   * <code>optionVerbose</code> flags are not both set at the same
   * time.
   *
   * @see #wrapUp()
   */
  public void prepare() {
    runtime.initDefaultValues();
    if (runtime.test("optionSilent") && runtime.test("optionVerbose")) {
      runtime.error("can't run in silent and verbose mode at the same time");
    }
    if (runtime.test("optionMeasureParser") &&
        runtime.test("optionMeasureProcessing")) {
      runtime.error("can't measure just parsing and just processing at the " +
                    "same time");
    }
    if (runtime.test("optionMeasureParser") &&
        (! runtime.test("optionPerformance"))) {
      runtime.setValue("optionPerformance", true);
    }
    if (runtime.test("optionMeasureProcessing") &&
        (! runtime.test("optionPerformance"))) {
      runtime.setValue("optionPerformance", true);
    }
  }

  /**
   * Print tool diagnostics.  The default implementation of this
   * method does nothing.
   */
  public void diagnose() {
    // Nothing to do.
  }

  /**
   * Locate the file with the specified name.  The default
   * implementation simply looks in the current directory, ignoring
   * any directories in the tool's search path.
   *
   * @see Runtime#locate(String)
   * 
   * @param name The file name.
   * @return The corresponding file.
   * @throws IllegalArgumentException Signals an inappropriate file
   *   (e.g., one that is too large).
   * @throws IOException Signals an I/O error.
   */
  public File locate(String name) throws IOException {
    File file = new File(name);
    if (! file.exists()) {
      throw new FileNotFoundException(name + ": not found");
    }
    return file;
  }

  /**
   * Parse the specified file.
   *
   * @param in The input stream for the file.
   * @param file The corresponding file.
   * @return The AST corresponding to the file's contents, or
   *   <code>null</code> if no tree has been generated.
   * @throws IllegalArgumentException Signals an inappropriate file
   *   (e.g., one that is too large).
   * @throws IOException Signals an I/O error.
   * @throws ParseException Signals a parse error.
   */
  public abstract Node parse(Reader in, File file)
    throws IOException, ParseException;

  /**
   * Process the specified AST node.  This method is only invoked if
   * {@link #parse(Reader,File)} has completed successfuly, has
   * returned a node (and not <code>null</code>), and no errors have
   * been reported through {@link Runtime#error()}, {@link
   * Runtime#error(String)}, or {@link Runtime#error(String,Node)}
   * while parsing.  The default implementation of this method does
   * nothing.
   *
   * @param node The node.
   */
  public void process(Node node) {
    // Nothing to do.
  }

  /**
   * Recursively process the file with the specified name.  This
   * method {@link #locate(String) locates} the specified file, opens
   * it, {@link #parse(Reader,File) parses} it, closes it, and then
   * {@link #process(Node) processes} the resulting AST node.
   *
   * @param name The file name.
   * @throws IllegalArgumentException Signals an inappropriate file
   *   (e.g., one that is too large).
   * @throws FileNotFoundException Signals that the file was not
   *   found.
   * @throws IOException Signals an I/O error while accessing the
   *   file.
   * @throws ParseException Signals a parse error.
   * @throws VisitingException Signals an error while visiting a node.
   */
  public void process(String name) throws IOException, ParseException {
    // Locate the file.
    File file = locate(name);

    // Open the file.
    Reader in = runtime.getReader(file);

    // Parse the file.
    Node root;
    try {
      root = parse(in, file);
    } finally {
      // Close the file.
      try {
        in.close();
      } catch (IOException x) {
        // Ignore.
      }
    }

    // Process the AST.
    process(root);
  }

  /**
   * Print a tool header to the specified printer.  This method prints
   * a header documenting the tool name, version, copyright, and
   * current time.  It also prints a warning not to edit the result.
   *
   * @param printer The printer.
   */
  public void printHeader(Printer printer) {
    printer.sep();
    printer.indent().pln("// This file has been generated by");
    printer.indent().p("// ").p(getName()).p(", version ").
      p(getVersion()).pln(',');
    printer.p("// ").p(getCopy()).pln(',');
    Date now = new Date();
    printer.indent().p("// on ").
      p(DateFormat.getDateInstance(DateFormat.FULL).format(now)).
      p(" at ").
      p(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(now)).pln('.');
    printer.indent().pln("// Edit at your own risk.");
    printer.sep();
    printer.pln();
  }

  /**
   * Wrap up this tool.  This method is invoked after all files have
   * been processed.  The default implementation does nothing.
   */
  public void wrapUp() {
    // Nothing to do.
  }

  /**
   * Run this tool with the specified command line arguments.  This
   * method works as following:<ol>
   *
   * <li>It calls {@link #init()} to initialize this tool.</li>
   *
   * <li>It prints the {@link #getName() name}, {@link #getVersion()
   * version}, and {@link #getCopy() copyright} to the {@link
   * Runtime#console() console}.</li>
   *
   * <li>If this tool has been invoked without arguments, it prints a
   * description of all command line {@link Runtime#printOptions()
   * options} and, optionally, an {@link #getExplanation()
   * explanation}.  It then exits.</li>
   *
   * <li>It {@link Runtime#process(String[]) processes} the specified
   * command line arguments and {@link #prepare() prepares} for
   * processing the files.  If any errors have been {@link
   * Runtime#seenError() reported} during the two method calls, it
   * exits.</li>
   *
   * <li>For each file name specified on the command line, it {@link
   * #locate(String) locates} the file, {@link #parse(Reader,File)
   * parses} the contents, and {@link #process(Node) processes} the
   * resulting AST.  If the <code>-performance</code> command line
   * option has been specified, it repeatedly parses and processes
   * each file, measuring both latency and heap utilization.  It then
   * exits.</li>
   *
   * </ol>
   *
   * @param args The command line arguments.
   */
  public void run(String[] args) {
    // Initialize this tool.
    init();

    // Print the tool description and exit if there are no arguments.
    if (0 == args.length) {
      runtime.console().p(getName()).p(", v. ").p(getVersion()).p(", ").
        pln(getCopy());
      runtime.console().pln().pln("Usage: <option>* <file-name>+").pln().
        pln("Options are:");
      runtime.printOptions();

      final String explanation = getExplanation();
      if (null != explanation) {
        runtime.console().pln().wrap(0, explanation).pln();
      }
      runtime.console().pln().flush();
      if (runtime.hasValue("optionNoExit") && runtime.test("optionNoExit")) {
        return;
      } else {
        runtime.exit();
      }
    }

    // Process the command line arguments and print tool name.
    int index = runtime.process(args);

    if ((! runtime.hasValue("optionSilent")) ||
        (! runtime.test("optionSilent"))) {
      runtime.console().p(getName()).p(", v. ").p(getVersion()).p(", ").
        pln(getCopy()).flush();
    }

    final boolean diagnose =
      runtime.hasValue("optionDiagnostics") && runtime.test("optionDiagnostics");

    if (index >= args.length && ! diagnose) {
      runtime.error("no file names specified");
    }

    // Prepare for processing the files.
    prepare();

    // Print diagnostics.
    if (diagnose) diagnose();

    // Stop if there have been errors already.
    if (runtime.seenError()) {
      if (runtime.test("optionNoExit")) {
        return;
      } else {
        runtime.exit();
      }
    }

    // Process the files.
    final boolean    silent      = runtime.test("optionSilent");
    final boolean    doGC        = runtime.test("optionGC");
    final boolean    measure     = runtime.test("optionPerformance");
    final boolean    parserOnly  = runtime.test("optionMeasureParser");
    final boolean    processOnly = runtime.test("optionMeasureProcessing");
    final int        warmUp      = measure? runtime.getInt("runsWarmUp") : 0;
    final int        total       = measure? runtime.getInt("runsTotal")  : 1;
    final Statistics time        = measure? new Statistics() : null;
    final Statistics memory      = measure? new Statistics() : null;
    final Statistics fileSizes   = measure? new Statistics() : null;
    final Statistics latencies   = measure? new Statistics() : null;
    final Statistics heapSizes   = measure? new Statistics() : null;

    // If measuring, we need to print a legend.
    if (measure) {
      runtime.console().p("Legend: file, size, time (ave, med, stdev), ").
        pln("memory (ave, med, stdev)").pln().flush();
    }

    while (index < args.length) {
      // If we are neither silent nor measuring, report on activity.
      if ((! silent) && (! measure)) {
        runtime.console().p("Processing ").p(args[index]).pln(" ...").flush();
      }

      // Locate the file.
      File file = null;

      try {
        file = locate(args[index]);

      } catch (IllegalArgumentException x) {
        runtime.error(x.getMessage());

      } catch (FileNotFoundException x) {
        runtime.error(x.getMessage());

      } catch (IOException x) {
        if (null == x.getMessage()) {
          runtime.error(args[index] + ": I/O error");
        } else {
          runtime.error(args[index] + ": " + x.getMessage());
        }

      } catch (Throwable x) {
        runtime.error();
        x.printStackTrace();
      }

      // Parse and process the file.
      if (null != file) {
        if (measure) {
          time.reset();
          memory.reset();
        }

        for (int i=0; i<total; i++) {
          Node    ast     = null;
          boolean success = false;
          
          // Perform GC if requested.
          if (doGC) {
            System.gc();
          }
          
          // Measure performance if requested.
          long startTime   = 0;
          long startMemory = 0;
          if (measure && (! processOnly)) {
            startMemory = java.lang.Runtime.getRuntime().freeMemory();
            startTime   = System.currentTimeMillis();
          }

          // Parse the input.
          Reader in = null;
          try {
            in      = runtime.getReader(file);
            ast     = parse(in, file);
            success = true;

          } catch (IllegalArgumentException x) {
            runtime.error(x.getMessage());

          } catch (FileNotFoundException x) {
            runtime.error(x.getMessage());

          } catch (UnsupportedEncodingException x) {
            runtime.error(x.getMessage());

          } catch (IOException x) {
            if (null == x.getMessage()) {
              runtime.error(args[index] + ": I/O error");
            } else {
              runtime.error(args[index] + ": " + x.getMessage());
            }

          } catch (ParseException x) {
            runtime.error();
            System.err.print(x.getMessage());

          } catch (Throwable x) {
            runtime.error();
            x.printStackTrace();

          } finally {
            if (null != in) {
              try {
                in.close();
              } catch (IOException x) {
                // Nothing to see here. Move on.
              }
            }
          }

          if (success && (null != ast) && (! parserOnly)) {
            // Measure processing only if requested.
            if (measure && processOnly) {
              startMemory = java.lang.Runtime.getRuntime().freeMemory();
              startTime   = System.currentTimeMillis();
            }

            // Process the AST.
            try {
              process(ast);
            } catch (VisitingException x) {
              runtime.error();
              x.getCause().printStackTrace();
            } catch (Throwable x) {
              runtime.error();
              x.printStackTrace();
            }
          }

          // Collect performance data for this run if requested.
          if (measure) {
            final long endTime   = System.currentTimeMillis();
            final long endMemory = java.lang.Runtime.getRuntime().freeMemory();
            
            if (i >= warmUp) {
              time.add(endTime - startTime);
              memory.add(startMemory - endMemory);
            }
          }
        }

        // Collect performance data for all the file's runs if
        // requested.
        if (measure) {
          final long   fileSize = file.length();
          final double latency  = time.mean();
          final double heapSize = memory.mean();

          fileSizes.add(fileSize / 1024.0);
          latencies.add(latency);
          heapSizes.add(heapSize / 1024.0);

          runtime.console().p(args[index]).p(' ').
            p(fileSize).p(' ').
            p(Statistics.round(latency)).p(' ').
            p(time.median()).p(' ').
            p(Statistics.round(time.stdev())).p(' ').
            p(Statistics.round(heapSize)).p(' ').
            p(memory.median()).p(' ').
            pln(Statistics.round(memory.stdev())).flush();
        }
      }

      // Next file.
      index++;
    }

    // Wrap up.
    wrapUp();

    // Print overall statistics, if requested.
    if (measure) {
      final double totalTime   = latencies.sum();
      final double totalMemory = heapSizes.sum();
      final double throughput  = 1000.0/Statistics.fitSlope(fileSizes,latencies);
      final double heapUtil    = Statistics.fitSlope(fileSizes, heapSizes);

      runtime.console().pln().
        p("Total time               : ").p(Statistics.round(totalTime)).
        pln(" ms").
        p("Total memory             : ").p(Statistics.round(totalMemory)).
        pln(" KB").
        p("Average throughput       : ").p(Statistics.round(throughput)).
        pln(" KB/s").
        p("Average heap utilization : ").p(Statistics.round(heapUtil)).
        pln(":1").flush();
    }

    // Done.
    if (! runtime.test("optionNoExit")) runtime.exit();
  }

}
