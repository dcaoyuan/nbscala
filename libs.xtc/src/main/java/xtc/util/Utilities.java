/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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
import java.io.Writer;

import java.util.Iterator;
import java.util.List;

import java.util.regex.Pattern;

import xtc.Constants;

/**
 * Implementation of utilities for language processors, focusing on
 * strings and I/O.
 *
 * @author Robert Grimm
 * @version $Revision: 1.46 $
 */
public final class Utilities {

  /** Hide constructor. */
  private Utilities() { /* Nothing to do. */ }

  /** The buffer size for pumping data. */
  private static final int BUFFER_SIZE = 4096;

  /** The buffer for pumping data. */
  private static char[] charBuffer = null;

  /** The regular expression pattern for white space. */
  public static final Pattern WHITE_SPACE = Pattern.compile("\\s+");

  /**
   * The regular expression pattern for newlines surrounded by spaces.
   */
  public static final Pattern SPACE_NEWLINE_SPACE =
    Pattern.compile("[ \t\f]*(?:\r\n|\r|\n)[ \t\f]*");

  /**
   * The regular expression pattern for newlines surrounded by spaces
   * and optionally followed by stars '<code>*</code>'.
   */
  public static final Pattern COMMENT_NEWLINE =
    Pattern.compile("[ \t\f]*(?:\r\n|\r|\n)[ \t\f]*\\**[ \t\f]*");

  /**
   * Flag for escaping the bell '<code>\a</code>' and vertical tab
   * '<code>\v</code>' characters, which may be escaped in standard C
   * but not in Java.
   */
  public static final int ESCAPE_BELL_VT = 0x01;

  /**
   * Flag for using doubled escape sequences.  Doubled escape
   * sequences will still read as an escape sequence, even if they are
   * included in a program source character or string.
   */
  public static final int ESCAPE_DOUBLE = 0x02;

  /**
   * Flag for escaping the '<code>[</code>', '<code>-</code>', and
   * '<code>]</code>' characters used in regular expressions.
   */
  public static final int ESCAPE_REGEX = 0x04;

  /**
   * Flag for escaping all characters that neither are printable ASCII
   * characters nor have named escapes using Unicode escapes.
   */
  public static final int ESCAPE_UNICODE = 0x08;

  /**
   * Flag for escaping less-than '<code>&lt;</code>' and greater-than
   * '<code>&gt;</code>' signs as HTML entities.
   */
  public static final int ESCAPE_HTML = 0x10;

  /** The escape flags for standard C escapes. */
  public static final int C_ESCAPES = ESCAPE_BELL_VT | ESCAPE_UNICODE;

  /** The escape flags for Java escapes. */
  public static final int JAVA_ESCAPES = ESCAPE_UNICODE;

  /** The escape flags for Java and HTML escapes. */
  public static final int JAVA_HTML_ESCAPES = ESCAPE_UNICODE | ESCAPE_HTML;

  /** The escape flags for Java and regex escapes. */
  public static final int FULL_ESCAPES = ESCAPE_UNICODE | ESCAPE_REGEX;

  /** The escape flags for Java, regex, and HTML escapes. */
  public static final int FULL_HTML_ESCAPES = FULL_ESCAPES | ESCAPE_HTML;

  // =======================================================================
  
  /**
   * Determine whether the specified name is qualified.
   *
   * @param name The name.
   * @return <code>true</code> if the name is qualified.
   */
  public static boolean isQualified(String name) {
    final int length = name.length();
    boolean   opaque = false;

    for (int i=0; i<length; i++) {
      char c = name.charAt(i);

      if (opaque) {
        if (Constants.END_OPAQUE == c) opaque = false;
      } else {
        if (Constants.QUALIFIER == c) return true;
        if (Constants.START_OPAQUE == c) opaque = true;
      }
    }

    return false;
  }

  /**
   * Extract the qualifying part from the specified qualified name.
   * For example, the qualifying part for <code>xtc.parser.Rats</code>
   * is <code>xtc.parser</code>.
   *
   * @param qname The qualified name.
   * @return The qualifying part, or <code>null</code> if the
   *   specified name has no qualifying part.
   */
  public static String getQualifier(String qname) {
    boolean opaque = false;

    for (int i=qname.length()-1; i>=0; i--) {
      char c = qname.charAt(i);

      if (opaque) {
        if (Constants.START_OPAQUE == c) opaque = false;
      } else {
        if (Constants.QUALIFIER == c) return qname.substring(0, i);
        if (Constants.END_OPAQUE == c) opaque = true;
      }
    }

    return null;
  }

  /**
   * Extract the unqualified name from the specified qualified name.
   * For example, the unqualified name for
   * <code>xtc.parser.Rats</code> is <code>Rats</code>.
   *
   * @param qname The qualified name.
   * @return The unqualified name.
   */
  public static String getName(String qname) {
    boolean opaque = false;

    for (int i=qname.length()-1; i>=0; i--) {
      char c = qname.charAt(i);

      if (opaque) {
        if (Constants.START_OPAQUE == c) opaque = false;
      } else {
        if (Constants.QUALIFIER == c) return qname.substring(i+1);
        if (Constants.END_OPAQUE == c) opaque = true;
      }
    }

    return qname;
  }

  /**
   * Construct an unqualified name.  If the specified name is
   * qualified, this method returns the unqualified name.  Otherwise,
   * it returns the specified name.
   *
   * @param name The name.
   * @return The unqualified name.
   */
  public static String unqualify(String name) {
    return isQualified(name)? getName(name) : name;
  }

  /**
   * Construct a qualified name.  If the specified qualifier is
   * <code>null</code>, this method returns the unqualified name
   *
   * @param qualifier The qualifier.
   * @param name The unqualified name.
   * @return The corresponding qualified name.
   */
  public static String qualify(String qualifier, String name) {
    return null == qualifier ? name : qualifier + Constants.QUALIFIER + name;
  }

  /**
   * Convert the specified qualified name to its components.
   *
   * @param qname The qualified name.
   * @return The corresponding identifier.
   */
  public static String[] toComponents(String qname) {
    final int length = qname.length();
    int       count  = 1;
    boolean   opaque = false;

    // Count the number of components.
    for (int i=0; i<length; i++) {
      char c = qname.charAt(i);

      if (opaque) {
        if (Constants.END_OPAQUE == c) opaque = false;
      } else {
        if (Constants.QUALIFIER == c) count++;
        if (Constants.START_OPAQUE == c) opaque = true;
      }
    }

    // Wrap up with the trivial case of no qualifiers.
    if (1 == count) return new String[] { qname };

    // Set up the component array.
    String[] components = new String[count];
    int      start      = -1;
    count               = 0;

    // Fill in the component array.
    for (int i=0; i<length; i++) {
      char c = qname.charAt(i);

      if (opaque) {
        if (Constants.END_OPAQUE == c) opaque = false;
      } else {
        if (Constants.QUALIFIER == c) {
          components[count] = qname.substring(start+1, i);
          start = i;
          count++;
        } else if (Constants.START_OPAQUE == c) {
          opaque = true;
        }
      }
    }

    // Don't forget the last component.
    components[count] = qname.substring(start+1,length);

    // Done.
    return components;
  }

  /**
   * Replace all occurrences of the qualifier character with the
   * specified character.
   *
   * @param s The string.
   * @param qual The new qualifier.
   * @return The requalified string.
   */
  private static String requalify(String s, char qual) {
    final int     length = s.length();
    StringBuilder buf    = new StringBuilder(length);
    boolean       opaque = false;

    for (int i=0; i<length; i++) {
      char c = s.charAt(i);

      if (opaque) {
        buf.append(c);
        if (Constants.END_OPAQUE == c) opaque = false;

      } else {
        if (Constants.QUALIFIER == c) {
          buf.append(qual);
        } else {
          buf.append(c);
          if (Constants.START_OPAQUE == c) opaque = true;
        }
      }
    }

    return buf.toString();
  }

  /**
   * Convert the specified qualified name into a programming language
   * identifier.
   *
   * @param qname The qualified name.
   * @return The corresponding identifier.
   */
  public static String toIdentifier(String qname) {
    return requalify(qname, '$');
  }

  /**
   * Convert the specified qualified name into a file path.
   *
   * @param qname The qualified name.
   * @return The corresponding file path.
   */
  public static String toPath(String qname) {
    return requalify(qname, File.separatorChar);
  }

  /**
   * Convert the specified qualified name into a file path.
   *
   * @param qname The qualified name.
   * @param ext The file's extension.
   * @return The corresponding file path.
   */
  public static String toPath(String qname, String ext) {
    return toPath(qname) + '.' + ext;
  }

  // =======================================================================

  /**
   * Locate the specified file.  This method searches for the
   * specified file, relative to each root in the list of
   * <code>java.io.File</code> objects.
   *
   * @param roots The list of file system roots to search.
   * @param path The (relative) file path.
   * @return The corresponding file.
   * @throws FileNotFoundException
   *   Signals that the specified file could not be found.
   */
  public static File locate(List<File> roots, String path)
    throws FileNotFoundException {

    for (File root : roots) {
      File file = new File(root, path);
      if (file.exists() && file.isFile()) {
        return file;
      }
    }

    throw new FileNotFoundException(path + " not found");
  }

  // =======================================================================

  /**
   * Pump all data from the specified reader to the specified writer.
   *
   * @param in The reader.
   * @param out The writer.
   * @throws IOException Signals an exceptinal condition during I/O.
   */
  public static void pump(Reader in, Writer out) throws IOException {
    if (null == charBuffer) charBuffer = new char[BUFFER_SIZE];

    int number = in.read(charBuffer);
    while (-1 != number) {
      out.write(charBuffer, 0, number);
      number = in.read(charBuffer);
    }
  }

  // =======================================================================

  /**
   * Remove all whitespace from the specified string.
   *
   * @param s The string.
   * @return The string without whitespace.
   */
  public static String withoutSpace(String s) {
    return WHITE_SPACE.matcher(s).replaceAll("");
  }

  // =======================================================================

  /**
   * Determine whether the escape bell and vertical tabs flag is set.
   *
   * @see #ESCAPE_BELL_VT
   *
   * @param flags The escape flags.
   * @return <code>true</code> if the escape bell and vertical tabs
   *   flag is set.
   */
  public static boolean useBellVTEscapes(int flags) {
    return (0 != (ESCAPE_BELL_VT & flags));
  }

  /**
   * Determine whether the escape double flag is set.
   *
   * @see #ESCAPE_DOUBLE
   *
   * @param flags The escape flags.
   * @return  <code>true</code> if the escape double flag is set.
   */
  public static boolean useDoubleEscapes(int flags) {
    return (0 != (ESCAPE_DOUBLE & flags));
  }

  /**
   * Determine whether the escape regex flag is set.
   *
   * @see #ESCAPE_REGEX
   *
   * @param flags The escape flags.
   * @return <code>true</code> if the escape regex flag is set.
   */
  public static boolean useRegexEscapes(int flags) {
    return (0 != (ESCAPE_REGEX & flags));
  }

  /**
   * Determine whether the escape Unicode flag is set.
   *
   * @see #ESCAPE_UNICODE
   *
   * @param flags The escape flags.
   * @return <code>true</code> if the escape Unicode flag is set.
   */
  public static boolean useUnicodeEscapes(int flags) {
    return (0 != (ESCAPE_UNICODE & flags));
  }

  /**
   * Determine whether the escape HTML flag is set.
   *
   * @see #ESCAPE_HTML
   *
   * @param flags The escape flags.
   * @return <code>true</code> if the escape HTML flag is set.
   */
  public static boolean useHTMLEscapes(int flags) {
    return (0 != (ESCAPE_HTML & flags));
  }

  // =======================================================================

  /**
   * Escape the specified character into the specified appendable.
   *
   * @param c The character.
   * @param out The appendable.
   * @param flags The escape flags.
   * @throws IOException Signals an I/O error.
   */
  public static void escape(char c, Appendable out, int flags)
    throws IOException {

    switch (c) {
    case '\u0007':
      if (useBellVTEscapes(flags)) {
        if (useDoubleEscapes(flags)) {
          out.append("\\\\a");
        } else {
          out.append("\\a");
        }
      }
      // We still need to go through default processing.
      break;
    case '\b':
      if (useDoubleEscapes(flags)) {
        out.append("\\\\b");
      } else {
        out.append("\\b");
      }
      return;
    case '\t':
      if (useDoubleEscapes(flags)) {
        out.append("\\\\t");
      } else {
        out.append("\\t");
      }
      return;
    case '\n':
      if (useDoubleEscapes(flags)) {
        out.append("\\\\n");
      } else {
        out.append("\\n");
      }
      return;
    case '\u000b':
      if (useBellVTEscapes(flags)) {
        if (useDoubleEscapes(flags)) {
          out.append("\\\\v");
        } else {
          out.append("\\v");
        }
      }
      // We still need to go through default processing.
      break;
    case '\f':
      if (useDoubleEscapes(flags)) {
        out.append("\\\\f");
      } else {
        out.append("\\f");
      }
      return;
    case '\r':
      if (useDoubleEscapes(flags)) {
        out.append("\\\\r");
      } else {
        out.append("\\r");
      }
      return;
    case '\"':
      if (useDoubleEscapes(flags)) {
        out.append("\\\\\\\"");
      } else {
        out.append("\\\"");
      }
      return;
    case '\'':
      if (useDoubleEscapes(flags)) {
        out.append("\\\\\\\'");
      } else {
        out.append("\\\'");
      }
      return;
    case '-':
      if (useRegexEscapes(flags)) {
        if (useDoubleEscapes(flags)) {
          out.append("\\\\-");
        } else {
          out.append("\\-");
        }
      } else {
        out.append('-');
      }
      return;
    case '<':
      if (useHTMLEscapes(flags)) {
        out.append("&lt;");
      } else {
        out.append('<');
      }
      return;
    case '>':
      if (useHTMLEscapes(flags)) {
        out.append("&gt;");
      } else {
        out.append('>');
      }
      return;
    case '[':
      if (useRegexEscapes(flags)) {
        if (useDoubleEscapes(flags)) {
          out.append("\\\\[");
        } else {
          out.append("\\[");
        }
      } else {
        out.append('[');
      }
      return;
    case '\\':
      if (useDoubleEscapes(flags)) {
        out.append("\\\\\\\\");
      } else {
        out.append("\\\\");
      }
      return;
    case ']':
      if (useRegexEscapes(flags)) {
        if (useDoubleEscapes(flags)) {
          out.append("\\\\]");
        } else {
          out.append("\\]");
        }
      } else {
        out.append(']');
      }
      return;
    default:
      // The default case is handled below.
    }

    if (useUnicodeEscapes(flags) && ((' ' > c) || ('~' < c))) {
      String hex = Integer.toHexString(c);
      String padding;
      switch (hex.length()) {
      case 1:
        padding = "000";
        break;
      case 2:
        padding = "00";
        break;
      case 3:
        padding = "0";
        break;
      default:
        padding = "";
      }
      if (useDoubleEscapes(flags)) {
        out.append("\\\\");
      } else {
        out.append("\\");
      }
      out.append('u');
      out.append(padding);
      out.append(hex);
      
    } else {
      out.append(c);
    }
  }

  /**
   * Escape the specified string into the specified appendable.
   *
   * @param s The string.
   * @param out The appendable.
   * @param flags The escape flags.
   * @throws IOException Signals an I/O error.
   */
  public static void escape(String s, Appendable out, int flags)
    throws IOException {

    final int length = s.length();
    for (int i=0; i<length; i++) {
      escape(s.charAt(i), out, flags);
    }
  }

  /**
   * Escape the specified character.
   *
   * @param c The character.
   * @param flags The escape flags.
   * @return The escape character as a string.
   */
  public static String escape(char c, int flags) {
    StringBuilder buf = new StringBuilder(1);
    try {
      escape(c, buf, flags);
    } catch (IOException x) {
      assert false;
    }
    return buf.toString();
  }

  /**
   * Escape the specified string.
   *
   * @param s The string.
   * @param flags The escape flags.
   * @return The escaped string.
   */
  public static String escape(String s, int flags) {
    StringBuilder buf = new StringBuilder(s.length());
    try {
      escape(s, buf, flags);
    } catch (IOException x) {
      assert false;
    }
    return buf.toString();
  }

  /**
   * Unescape the specified string. This method unescapes basic Java
   * escapes ('<code>\b</code>', '<code>\f</code>', '<code>\n</code>',
   * '<code>\r</code>', '<code>\t</code>', '<code>\"</code>',
   * '<code>\'</code>', and '<code>\\</code>'), the additional
   * standard C escapes ('<code>\a</code>', '<code>\v</code>', and
   * '<code>\?</code>'), standard C's octal escapes, and standard C's
   * and Java's Unicode escapes.  To support regex-like character
   * classes, it also unescapes '<code>\-</code>', '<code>\[</code>',
   * and '<code>\]</code>'.
   *
   * @param s The string to unescape.
   * @return The unescaped string.
   * @throws IllegalArgumentException Signals a malformed string.
   */
  public static String unescape(String s) {
    if (-1 == s.indexOf('\\')) {
      return s;
    }

    final int     length = s.length();
    StringBuilder buf    = new StringBuilder(length);

    for (int i=0; i<length; i++) {
      char c = s.charAt(i);
      if ('\\' != c) {
        buf.append(c);
      } else  {
        i++;
        if (i >= length) {
          throw new IllegalArgumentException("incomplete escape sequence");
        }

        c = s.charAt(i);

        switch (c) {
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
          {
            int val = c;

            if (i+1 < length) {
              c = s.charAt(i+1);

              if (('0' <= c) && (c <= '7')) {
                val = (val * 8) + c;
                i++;

                if (i+1 <length) {
                  c = s.charAt(i+1);

                  if (('0' <= c) && (c <= '7')) {
                    val = (val * 8) + c;
                    i++;
                  }
                }
              }
            }

            buf.append((char)val);
          }
          break;
        case '"':
          buf.append('"');
          break;
        case '\'':
          buf.append('\'');
          break;
        case '-':
          buf.append('-');
          break;
        case '?':
          buf.append('?');
          break;
        case '[':
          buf.append('[');
          break;
        case '\\':
          buf.append('\\');
          break;
        case ']':
          buf.append(']');
          break;
        case 'a':
          buf.append('\u0007');
          break;
        case 'b':
          buf.append('\b');
          break;
        case 'f':
          buf.append('\f');
          break;
        case 'n':
          buf.append('\n');
          break;
        case 'r':
          buf.append('\r');
          break;
        case 't':
          buf.append('\t');
          break;
        case 'u':
          i += 4;
          int n;

          if (i >= length) {
            throw new IllegalArgumentException("incomplete universal character"+
                                               " name " + s.substring(i-3));
          }

          try {
            n = Integer.parseInt(s.substring(i-3, i+1), 16);
          } catch (NumberFormatException x) {
            throw new IllegalArgumentException("malformed universal character" +
                                               " name " + s.substring(i-3,i+1));
          }
          buf.append((char)n);
          break;
        case 'v':
          buf.append('\u000b');
          break;
        default:
          throw new IllegalArgumentException("illegal escaped character \'\\"
                                             + c + "\'");
        }
      }
    }
    return buf.toString();
  }

  // =======================================================================

  /**
   * Convert the specified list to a human-readable representation.
   * This method uses <code>toString()</code> for each element in the
   * specified list to generate a human-readable representation.
   *
   * @param l The list.
   * @return The human-readable representation.
   */
  public static String format(List<?> l) {
    final int     length = l.size();
    StringBuilder buf    = new StringBuilder();
    Iterator      iter   = l.iterator();
    while (iter.hasNext()) {
      String     el      = iter.next().toString();

      if ((1 < length) && (! iter.hasNext())) {
        buf.append("and ");
      }
      buf.append(el);
      if ((2 == length) && (iter.hasNext())) {
        buf.append(' ');
      } else if (iter.hasNext()) {
        buf.append(", ");
      }
    }

    return buf.toString();
  }

  // =======================================================================

  /**
   * Split the specified identifier.  This method splits identifiers
   * using an upper case character for each word component into a
   * string of lower case words separated by the specified separator.
   *
   * @param id The identifier.
   * @param separator The separator.
   * @return The split identifier.
   */
  public static String split(String id, char separator) {
    // Drop any suffixes.
    int idx = id.indexOf('$');
    if (-1 != idx) {
      id = id.substring(0, idx);
    }

    // Count the number of upper case characters.
    final int length      = id.length();
    boolean   startsUpper = false;
    int       upperCount  = 0;

    for (int i=0; i<length; i++) {
      if (Character.isUpperCase(id.charAt(i))) {
        if (0 == i) startsUpper = true;
        upperCount++;
      }
    }

    // No conversion is necessary if all characters are either lower
    // or upper case.
    if ((0 == upperCount) || (length == upperCount)) {
      return id;
    }

    // Do the actual conversion.
    final int     size = startsUpper ? length+upperCount-1 : length+upperCount;
    StringBuilder buf  = new StringBuilder(size);

    for (int i=0; i<length; i++) {
      char c = id.charAt(i);

      if (Character.isUpperCase(c)) {
        if (0 != i) {
          buf.append(separator);
        }
        buf.append(Character.toLowerCase(c));
      } else {
        buf.append(c);
      }
    }

    return buf.toString();
  }

  /**
   * Get the appropriate indefinite article for the specified noun.
   *
   * @param noun The noun.
   * @return The corresponding indefinite article.
   */
  public static String toArticle(String noun) {
    if (noun.startsWith("a") ||
        noun.startsWith("e") ||
        noun.startsWith("i") ||
        noun.startsWith("o") ||
        noun.startsWith("u")) {
      return "an";
    } else {
      return "a";
    }
  }

}
