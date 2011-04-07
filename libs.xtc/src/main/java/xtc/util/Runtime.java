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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xtc.parser.ParseError;
import xtc.parser.PParser;
import xtc.parser.Result;
import xtc.parser.SemanticValue;

import xtc.tree.Attribute;
import xtc.tree.Node;
import xtc.tree.Printer;

/**
 * A tool's runtime.  This helper class processes command line
 * options, prints errors and warnings, and manages console output.
 *
 * @author Robert Grimm
 * @version $Revision: 1.27 $
 */
public class Runtime {

  /**
   * The internal name for the input directory option.  The option is
   * expected to have multiple directory values.
   */
  public static final String INPUT_DIRECTORY = "inputDirectory";

  /**
   * The internal name for the output directory option.  The option is
   * expected to have a directory value.
   */
  public static final String OUTPUT_DIRECTORY = "outputDirectory";

  /**
   * The internal name for the intput encoding option.  The option is
   * expected to have a word value.
   */
  public static final String INPUT_ENCODING = "inputEncoding";

  /**
   * The internal name for the output encoding option.  The option is
   * expected to have a word value.
   */
  public static final String OUTPUT_ENCODING = "outputEncoding";

  // ========================================================================

  /** The console printer. */
  protected Printer console;

  /** The error console printer. */
  protected Printer errConsole;

  /** The list of command line options. */
  protected final List<Option> optionList;

  /** The map from external names to options. */
  protected final Map<String, Option> externalMap;

  /** The map from internal names to options. */
  protected final Map<String, Option> internalMap;

  /** The actual options. */
  protected final Map<String, Object> options;

  /** The error count. */
  protected int errors;

  /** The warning count. */
  protected int warnings;

  // ========================================================================

  /**
   * Create a new runtime.  Note that the list of input directories is
   * empty, while the output directory is initialized to the current
   * directory.
   */
  public Runtime() {
    console     = new
      Printer(new BufferedWriter(new OutputStreamWriter(System.out)));
    errConsole  = new
      Printer(new BufferedWriter(new OutputStreamWriter(System.err)));
    optionList  = new ArrayList<Option>();
    externalMap = new HashMap<String, Option>();
    internalMap = new HashMap<String, Option>();
    options     = new HashMap<String, Object>();
    errors      = 0;
    warnings    = 0;
  }

  // ========================================================================

  /**
   * Get a printer to the console.
   *
   * @return A printer to the console.
   */
  public Printer console() {
    return console;
  }

  /**
   * Update the printer to the console.  Since the console is used
   * throughout xtc, use this method with caution.
   *
   * @param console The new console.
   */
  public void setConsole(Printer console) {
    this.console = console;
  }

  /**
   * Get a printer to the error console.
   *
   * @return A printer to the error console.
   */
  public Printer errConsole() {
    return errConsole;
  }

  /**
   * Update the printer to the error console.  Since the error console
   * is used throughout xtc, use this method with caution.
   *
   * @param console The new error console.
   */
  public void setErrConsole(Printer console) {
    errConsole = console;
  }

  // ========================================================================

  /**
   * Get an estimate of free memory.
   *
   * @return An estimate of free memory.
   */
  public long freeMemory() {
    return java.lang.Runtime.getRuntime().freeMemory();
  }

  // ========================================================================

  /**
   * Check that no option with the specified names exits.
   *
   * @param external The external name.
   * @param internal The internal name.
   * @throws IllegalArgumentException Signals that an option with
   *   the external or interal name already exists.
   */
  protected void check(String external, String internal) {
    if (externalMap.containsKey(external)) {
      throw new IllegalArgumentException("Option with external name " +
                                         external + " already exists");
    } else if (internalMap.containsKey(internal)) {
      throw new IllegalArgumentException("Option with internal name " +
                                         internal + " already exists");
    }
  }

  /**
   * Add the specified option.  This method adds the specified option
   * to the {@link #optionList}, {@link #externalMap}, and {@link
   * #internalMap} fields.
   *
   * @param option The option.
   */
  protected void add(Option option) {
    optionList.add(option);
    externalMap.put(option.external, option);
    internalMap.put(option.internal, option);
  }

  /**
   * Declare a boolean command line option.
   *
   * @param external The external name.
   * @param internal The internal name.
   * @param value The default value.
   * @param description The description.
   * @return This runtime.
   * @throws IllegalArgumentException Signals that an option with
   *   the external or interal name already exists.
   */
  public Runtime bool(String external, String internal, boolean value,
                      String description) {
    check(external, internal);
    add(new Option(Option.Kind.BOOLEAN, external, internal, value, false,
                   description));
    return this;
  }

  /**
   * Declare a word-valued command line option.
   *
   * @param external The external name.
   * @param internal The internal name.
   * @param multiple The flag for multiple occurrences.
   * @param description The description.
   * @throws IllegalArgumentException Signals that an option with
   *   the external or interal name already exists.
   * @return This runtime.
   */
  public Runtime word(String external, String internal, boolean multiple,
                      String description) {
    check(external, internal);
    add(new Option(Option.Kind.WORD, external, internal, null, multiple,
                   description));
    return this;
  }

  /**
   * Declare an integer-valued command line option.
   *
   * @param external The external name.
   * @param internal The internal name.
   * @param value The default value.
   * @param description The description.
   * @return This runtime.
   * @throws IllegalArgumentException Signals that an option with
   *   the external or interal name already exists.
   */
  public Runtime number(String external, String internal, int value,
                        String description) {
    check(external, internal);
    add(new Option(Option.Kind.INTEGER, external, internal, new Integer(value),
                   false, description));
    return this;
  }

  /**
   * Declare a file-valued command line option.
   *
   * @param external The external name.
   * @param internal The internal name.
   * @param multiple The flag for multiple occurrences.
   * @param description The description.
   * @return This runtime.
   * @throws IllegalArgumentException Signals that an option with
   *   the external or interal name already exists.
   */
  public Runtime file(String external, String internal, boolean multiple,
                      String description) {
    check(external, internal);
    add(new Option(Option.Kind.FILE, external, internal, null, multiple,
                   description));
    return this;
  }

  /**
   * Declare a directory-valued command line option.  The default
   * value is the current directory.
   *
   * @param external The external name.
   * @param internal The internal name.
   * @param multiple The flag for multiple occurrences.
   * @param description The description.
   * @return This runtime.
   * @throws IllegalArgumentException Signals that an option with
   *   the external or interal name already exists.
   */
  public Runtime dir(String external, String internal, boolean multiple,
                     String description) {
    check(external, internal);
    add(new Option(Option.Kind.DIRECTORY, external, internal,
                   new File(System.getProperty("user.dir")), multiple,
                   description));
    return this;
  }

  /**
   * Declare an attribute-valued command line option.
   *
   * @param external The external name.
   * @param internal The internal name.
   * @param multiple The flag for multiple occurrences.
   * @param description The description.
   * @throws IllegalArgumentException Signals that an option with
   *   the external or interal name already exists.
   */
  public Runtime att(String external, String internal, boolean multiple,
                     String description) {
    check(external, internal);
    add(new Option(Option.Kind.ATTRIBUTE, external, internal, null, multiple,
                   description));
    return this;
  }

  // ========================================================================

  /** Print a description of all command line options to the console. */
  public void printOptions() {
    // Determine the alignment across all options.
    int alignment = 0;
    for (Option option : optionList) {
      switch (option.kind) {
      case BOOLEAN:
        alignment = Math.max(alignment, option.external.length() + 5);
        break;
      case WORD:
      case FILE:
        alignment = Math.max(alignment, option.external.length() + 5 + 7);
        break;
      case INTEGER:
      case DIRECTORY:
      case ATTRIBUTE:
        alignment = Math.max(alignment, option.external.length() + 5 + 6);
        break;
      default:
        assert false : "Invalid option " + option;
      }
    }

    // Actually print all options.
    for (Option option : optionList) {
      console.p("  -").p(option.external);
      switch (option.kind) {
      case BOOLEAN:
        break;
      case WORD:
        console.p(" <word>");
        break;
      case INTEGER:
        console.p(" <num>");
        break;
      case FILE:
        console.p(" <file>");
        break;
      case DIRECTORY:
        console.p(" <dir>");
        break;
      case ATTRIBUTE:
        console.p(" <att>");
        break;
      default:
        assert false: "Invalid option " + option;
      }

      console.align(alignment).wrap(alignment, option.description).pln();
    }
    console.flush();
  }

  // ========================================================================

  /**
   * Process the specified command line arguments.  This method sets
   * all options to their specified values.
   *
   * @param args The arguments.
   * @return The index right after the processed command line options.
   */
  public int process(String args[]) {
    int index = 0;
    options.clear();

    while ((index < args.length) && args[index].startsWith("-")) {
      if (1 >= args[index].length()) {
        error("empty command line option");

      } else {
        String name   = args[index].substring(1);
        Option option = externalMap.get(name);

        if (null == option) {
          error("unrecognized command line option " + name);

        } else if ((! option.multiple) &&
                   (options.containsKey(option.internal))) {
          error("repeated " + name + " option");

        } else if (Option.Kind.BOOLEAN == option.kind) {
          options.put(option.internal, Boolean.TRUE);

        } else if (args.length == index + 1) {
          error(name + " option without argument");

        } else {
          Object value = null;
          index++;

          switch (option.kind) {
          case WORD:
            value = args[index];
            break;

          case INTEGER:
            try {
              value = new Integer(args[index]);
            } catch (NumberFormatException x) {
              error("malformed integer argument to " + name + " option");
            }
            break;

          case FILE:
            File file = new File(args[index]);
            if (file.exists()) {
              value = file;
            } else {
              error("nonexistent file argument to " + name + " option");
            }
            break;

          case DIRECTORY:
            File dir = new File(args[index]);
            if (dir.exists()) {
              if (dir.isDirectory()) {
                value = dir;
              } else {
                error(args[index] + " not a directory");
              }
            } else {
              error("nonexistent directory argument to " + name + " option");
            }
            break;

          case ATTRIBUTE:
            PParser parser = new PParser(new StringReader(args[index]),
                                         "<console>", args[index].length());
            Result  result = null;
            try {
              result       = parser.pAttribute(0);
            } catch (IOException x) {
              error("internal error: " + x);
            }
            if (! result.hasValue()) {
              error("malformed attribute " + args[index] + ": " +
                    ((ParseError)result).msg);

            } else if (result.index != args[index].length()) {
              error("extra characters after " +
                    args[index].substring(0, result.index));

            } else {
              value = ((SemanticValue)result).value;
            }
            break;

          default:
            assert false : "Unrecognized option " + option;
          }

          if (null != value) {
            if (option.multiple) {
              if (options.containsKey(option.internal)) {
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>)options.get(option.internal);
                values.add(value);

              } else {
                List<Object> values = new ArrayList<Object>();
                values.add(value);
                options.put(option.internal, values);
              }

            } else {
              options.put(option.internal, value);
            }
          }
        }
      }

      index++;
    }

    return index;
  }

  // ========================================================================

  /**
   * Initialize all options without values to their defaults.  The
   * default value for word, file, and attribute options is
   * <code>null</code> if no multiple occurrences are allowed and the
   * empty list otherwise.
   */
  public void initDefaultValues() {
    for (Option option : optionList) {
      if (! options.containsKey(option.internal)) {
        Object value = null;

        if (null != option.value) {
          if (option.multiple) {
            List<Object> list = new ArrayList<Object>(1);
            list.add(option.value);
            value     = list;
          } else {
            value     = option.value;
          }
        } else if (option.multiple) {
          value = new ArrayList<Object>(0);
        } 

        options.put(option.internal, value);
      }
    }
  }

  /**
   * Initialize all boolean options without values to the specified
   * value.
   *
   * @param value The value.
   */
  public void initFlags(boolean value) {
    for (Option option : optionList) {
      if ((Option.Kind.BOOLEAN == option.kind) &&
          (! options.containsKey(option.internal))) {
        options.put(option.internal, value);
      }
    }
  }

  /**
   * Initialize all boolean options with the specified prefix and
   * without values to the specified value.
   *
   * @param prefix The prefix.
   * @param value The value.
   */
  public void initFlags(String prefix, boolean value) {
    for (Option option : optionList) {
      if ((Option.Kind.BOOLEAN == option.kind) &&
          option.internal.startsWith(prefix) &&
          (! options.containsKey(option.internal))) {
        options.put(option.internal, value);
      }
    }
  }

  /**
   * Determine whether the specified option has a value.
   *
   * @param name The internal name.
   * @return <code>true</code> if the option has a value.
   */
  public boolean hasValue(String name) {
    return options.containsKey(name);
  }

  /**
   * Determine whether any option with the specified prefix has a
   * value.
   *
   * @param prefix The prefix.
   * @return <code>true</code> if any option with the prefix has a
   *   value.
   */
  public boolean hasPrefixValue(String prefix) {
    for (String s : options.keySet()) {
      if (s.startsWith(prefix)) return true;
    }
    return false;
  }

  /**
   * Get the value of the specified option.
   *
   * @param name The internal name.
   * @return The option's value.
   * @throws IllegalArgumentException Signals that the option has no
   *   value.
   */
  public Object getValue(String name) {
    if (options.containsKey(name)) {
      return options.get(name);
    } else {
      throw new IllegalArgumentException("Undefined internal option " + name);
    }
  }

  /**
   * Test the value of the specified boolean option.
   *
   * @param name The internal name.
   * @return The option's boolean value.
   * @throws IllegalArgumentException Signals that the corresponding
   *   option has no value.
   * @throws ClassCastException Signals that the corresponding option
   *   does not have a boolean value.
   */
  public boolean test(String name) {
    if (options.containsKey(name)) {
      return (Boolean)options.get(name);
    } else {
      throw new IllegalArgumentException("Undefined boolean option " + name);
    }
  }

  /**
   * Get the integer value of the specified option.
   *
   * @param name The internal name.
   * @return The option's integer value.
   * @throws IllegalArgumentException Signals that the corresponding
   *   option has no value.
   * @throws ClassCastException Signals that the corresponding option
   *   does not have an integer value.
   */
  public int getInt(String name) {
    if (options.containsKey(name)) {
      return ((Integer)options.get(name)).intValue();
    } else {
      throw new IllegalArgumentException("Undefined integer option " + name);
    }
  }

  /**
   * Get the string value of the specified option.
   *
   * @param name The internal name.
   * @return The option's string value.
   * @throws IllegalArgumentException Signals that the corresponding
   *   option has no value.
   * @throws ClassCastException Signals that the corresponding option
   *   does not have an integer value.
   */
  public String getString(String name) {
    if (options.containsKey(name)) {
      return (String)options.get(name);
    } else {
      throw new IllegalArgumentException("Undefined word option " + name);
    }
  }

  /**
   * Get the file value of the specified option.
   *
   * @param name The internal name.
   * @return The option's file value.
   * @throws IllegalArgumentException Signals that the corresponding
   *   option has no value.
   * @throws ClassCastException Signals that the corresponding option
   *   does not have a file value.
   */
  public File getFile(String name) {
    if (options.containsKey(name)) {
      return (File)options.get(name);
    } else {
      throw new IllegalArgumentException("Undefined file/directory option " +
                                         name);
    }
  }

  /**
   * Get the list value of the specified option.
   *
   * @param name The internal name.
   * @return The option's list value.
   * @throws IllegalArgumentException Signals that the corresponding
   *   option has no value.
   * @throws ClassCastException Signals that the corresponding option
   *   does not have a list value.
   */
  public List<?> getList(String name) {
    if (options.containsKey(name)) {
      return (List)options.get(name);
    } else {
      throw new IllegalArgumentException("Undefined option " + name +
                                         " with multiple values");
    }
  }

  /**
   * Get the attribute list value of the specified option.
   *
   * @param name The internal name.
   * @return The option's attribute list value.
   * @throws IllegalArgumentException Signals that the corresponding
   *   option has no value.
   * @throws ClassCastException Signals that the corresponding option
   *   does not have an attribute list value.
   */
  @SuppressWarnings("unchecked")
  public List<Attribute> getAttributeList(String name) {
    List<?> l = getList(name);
    // Make sure the list actually contains attributes.
    if (0 < l.size()) {
      @SuppressWarnings("unused")
      Attribute a = (Attribute)l.get(0);
    }
    return (List<Attribute>)l;
  }

  /**
   * Get the file list value of the specified option.
   *
   * @param name The internal name.
   * @return The option's file list value.
   * @throws IllegalArgumentException Signals that the corresponding
   *   option has no value.
   * @throws ClassCastException Signals that the corresponding option
   *   does not have a file list value.
   */
  @SuppressWarnings("unchecked")
  public List<File> getFileList(String name) {
    List<?> l = getList(name);
    // Make sure the list actually contains files.
    if (0 < l.size()) {
      @SuppressWarnings("unused")
      File f = (File)l.get(0);
    }
    return (List<File>)l;
  }

  /**
   * Check that the specified value is valid for the specified option.
   *
   * @param option The option.
   * @param value The value.
   * @throws IllegalArgumentException Signals that the value is
   *   invalid.
   */
  protected void check(Option option, Object value) {
    switch (option.kind) {
    case BOOLEAN:
      if (! (value instanceof Boolean)) {
        throw new IllegalArgumentException("Invalid value " + value +
                                           " for boolean option " +
                                           option.internal);
      }
      break;

    case WORD:
      if (! (value instanceof String)) {
        throw new IllegalArgumentException("Invalid value " + value +
                                           " for word option " +
                                           option.internal);
      }
      break;

    case INTEGER:
      if (! (value instanceof Integer)) {
        throw new IllegalArgumentException("Invalid value " + value +
                                           " for number option " +
                                           option.internal);
      }
      break;

    case FILE:
      if ((! (value instanceof File)) ||
          (! ((File)value).exists())) {
        throw new IllegalArgumentException("Invalid value " + value +
                                           " for file option " +
                                           option.internal);
      }
      break;

    case DIRECTORY:
      if ((! (value instanceof File)) ||
          (! ((File)value).isDirectory())) {
        throw new IllegalArgumentException("Invalid value " + value +
                                           " for directory option " +
                                           option.internal);
      }
      break;

    case ATTRIBUTE:
      if (! (value instanceof Attribute)) {
        throw new IllegalArgumentException("Invalid value " + value +
                                           " for attribute option " +
                                           option.internal);
      }
      break;

    default:
      assert false : "Invalid option " + option;
    }
  }

  /**
   * Set the value of the specified option.
   *
   * @param name The internal name.
   * @param value The value.
   * @throws IllegalArgumentException Signals an unrecognized option
   *   or an invalid value.
   */
  public void setValue(String name, Object value) {
    Option option = internalMap.get(name);

    if (null == option) {
      throw new IllegalArgumentException("Undefined option " + name);
    } else {
      check(option, value);

      if (option.multiple) {
        List<Object> list = new ArrayList<Object>(1);
        list.add(value);
        value     = list;
      }

      options.put(name, value);
    }
  }

  /**
   * Set the value of the specified boolean-valued option.
   *
   * @param name The internal name.
   * @param value The value.
   * @throws IllegalArgumentException Signals an unrecognized option
   *   or not a boolean-valued option.
   */
  public void setValue(String name, boolean value) {
    Option option = internalMap.get(name);

    if (null == option) {
      throw new IllegalArgumentException("Undefined option " + name);

    } else if (Option.Kind.BOOLEAN != option.kind) {
      throw new IllegalArgumentException("Not a boolean-valued option " + name);

    } else {
      options.put(name, value);
    }
  }

  // ========================================================================

  /**
   * Locate the specified file.  This method searches this runtime's
   * list of input directories.
   *
   * @see #INPUT_DIRECTORY
   *
   * @param path The (relative) file path.
   * @return The corresponding file.
   * @throws FileNotFoundException
   *   Signals that the specified file could not be found.
   */
  public File locate(String path) throws FileNotFoundException {
    List<File> roots = getFileList(INPUT_DIRECTORY);

    if (null != roots) {
      for (File root : roots) {
        File file = new File(root, path);
        if (file.exists() && file.isFile()) {
          return file;
        }
      }
    }

    throw new FileNotFoundException(path + " not found");
  }

  /**
   * Get a reader for the specified file.  The reader uses this
   * runtime's input encoding and is buffered.
   *
   * @see #INPUT_ENCODING
   *
   * @param file The file.
   * @return The corresponding reader.
   * @throws IOException Signals an I/O error.
   */
  public Reader getReader(File file) throws IOException {
    return getReader(new FileInputStream(file));
  }

  /**
   * Get a reader for the specified input stream.  The reader uses
   * this runtime's input encoding and is buffered.
   *
   * @see #INPUT_ENCODING
   *
   * @param in The input stream.
   * @return The corresponding reader.
   * @throws UnsupportedEncodingException
   *   Signals that this runtime's encoding is not valid.
   */
  public Reader getReader(InputStream in) throws UnsupportedEncodingException {
    String encoding = (String)options.get(INPUT_ENCODING);

    if (null == encoding) {
      return new BufferedReader(new InputStreamReader(in));
    } else {
      return new BufferedReader(new InputStreamReader(in, encoding));
    }
  }

  /**
   * Get this runtime's output directory.
   *
   * @see #OUTPUT_DIRECTORY
   *
   * @return The output directory.
   */
  public File getOutputDirectory() {
    return getFile(OUTPUT_DIRECTORY);
  }

  /**
   * Get a writer for the specified file.  The writer uses this
   * runtime's output encoding and is buffered.
   *
   * @see #OUTPUT_ENCODING
   *
   * @param file The file.
   * @return The corresponding writer.
   * @throws IOException Signals an I/O error.
   */
  public Writer getWriter(File file) throws IOException {
    return getWriter(new FileOutputStream(file));
  }

  /**
   * Get a writer for the specified output stream.  The writer uses
   * this runtime's output encoding and is buffered.
   *
   * @see #OUTPUT_ENCODING
   *
   * @param in The output stream.
   * @return The corresponding writer.
   * @throws UnsupportedEncodingException
   *   Signals that this runtime's encoding is not valid.
   */
  public Writer getWriter(OutputStream in) throws UnsupportedEncodingException {
    String encoding = (String)options.get(OUTPUT_ENCODING);

    if (null == encoding) {
      return new BufferedWriter(new OutputStreamWriter(in));
    } else {
      return new BufferedWriter(new OutputStreamWriter(in, encoding));
    }
  }

  // ========================================================================

  /**
   * Determine whether errors have been reported.
   *
   * @return <code>true</code> if errors have been reported.
   */
  public boolean seenError() {
    return (0 < errors);
  }

  /**
   * Get the current error count.
   *
   * @return The current error count.
   */
  public int errorCount() {
    return errors;
  }

  /** Record an error reported through another means. */
  public void error() {
    errors++;
  }

  /**
   * Print the specified error message.
   *
   * @param msg The error message.
   */
  public void error(String msg) {
    errConsole.p("error: ").pln(msg).flush();
    errors++;
  }
  
  /**
   * Print the specified error message.
   *
   * @param msg The error message.
   * @param n The offending node.
   */
  public void error(String msg, Node n) {
    errConsole.loc(n).p(": ");
    error(msg);
  }

  /** Record a warning reported through another means. */
  public void warning() {
    warnings++;
  }

  /**
   * Print the specified warning message.
   *
   * @param msg The warning message.
   */
  public void warning(String msg) {
    errConsole.p("warning: ").pln(msg).flush();
    warnings++;
  }

  /**
   * Print the specified warning message.
   *
   * @param msg The warning message.
   * @param n The offending node.
   */
  public void warning(String msg, Node n) {
    errConsole.loc(n).p(": ");
    warning(msg);
  }

  // ========================================================================

  /**
   * Exit the tool.  This method terminates the Java virtual machine
   * with the appropriate exit code and a summary of error and warning
   * numbers if any have been reported.
   */
  public void exit() {
    if (0 < errors) {
      if (1 == errors) {
        errConsole.p("1 error");
      } else {
        errConsole.p(errors);
        errConsole.p(" errors");
      }
    }
    if (0 < warnings) {
      if (0 < errors) {
        errConsole.p(", ");
      }
      if (1 == warnings) {
        errConsole.p("1 warning");
      } else {
        errConsole.p(warnings);
        errConsole.p(" warnings");
      }
    }
    if ((0 < errors) || (0 < warnings)) {
      errConsole.pln().flush();
    }

    if (0 < errors) {
      System.exit(1);
    } else {
      System.exit(0);
    }
  }

}
