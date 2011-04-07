/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2009 Robert Grimm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc;

import xtc.tree.Attribute;

/**
 * Global constants.
 *
 * @author Robert Grimm
 * @version $Revision: 1.123 $
 */
public interface Constants {

  /** Flag for whether to print debugging information. */
  public static final boolean DEBUG = false;

  /** The major version number. */
  public static final int MAJOR = 1;

  /** The minor version number. */
  public static final int MINOR = 14;

  /** The revision number. */
  public static final int REVISION = 3;

  /** The complete version as a string. */
  public static final String VERSION = MAJOR + "." + MINOR + "." + REVISION;

  /** The copyright notice for <em>Rats&#033;</em>. */
  public static final String COPY = "(C) 2004-2009 Robert Grimm";

  /** The copyright notice for all of xtc. */
  public static final String FULL_COPY =
    "(C) 2004-2009 Robert Grimm and New York University";

  /** 
   * The start index for lines.  Note that the same constant is also
   * defined as {@link xtc.parser.ParserBase#FIRST_LINE} to avoid
   * parsers depending on this class.
   */
  public static final int FIRST_LINE = 1;

  /**
   * The start index for columns.  Note that the same constant is also
   * defined as {@link xtc.parser.ParserBase#FIRST_COLUMN} to avoid
   * parsers depending on this class.
   */
  public static final int FIRST_COLUMN = 1;

  /** The number of spaces per indentation when pretty printing sources. */
  public static final int INDENTATION = 2;

  /** The number of characters per line when pretty printing sources. */
  public static final int LINE_LENGTH = 78;

  /** The line separator for the current platform. */
  public static final String LINE_SEPARATOR =
    System.getProperty("line.separator");

  /** The qualification character. */
  public static final char QUALIFIER = '.';

  /** The start character for opaque substrings in qualified strings. */
  public static final char START_OPAQUE = '(';

  /** The end character for opaque substrings in qualified strings. */
  public static final char END_OPAQUE = ')';

  // =========================================================================

  /** A fuzzy boolean. */
  public static enum FuzzyBoolean {
    /** True. */            TRUE,
    /** Not true. */        FALSE,
    /** Indeterminate. */   MAYBE
  };

  // =========================================================================

  /**
   * The property name for unmodified nodes.  The value must be the
   * unmodified {@link xtc.tree.Node node} before any transformations.
   */
  public static final String ORIGINAL = "xtc.Constants.Original";

  /**
   * The property name for synthetic nodes. The value must be
   * a <code>Boolean</code>.
   */
  public static final String SYNTHETIC = "xtc.Constants.Synthetic";

  /**
   * The property name for scopes. The value must be a qualified scope
   * name.
   */
  public static final String SCOPE = "xtc.Constants.Scope";

  /**
   * The property name for types.  The value must be a {@link
   * xtc.type.Type type}.
   */
  public static final String TYPE = "xtc.Constants.Type";

  /**
   * The property name for a modified node's arguments.  The value
   * depends on the applied transformation.
   */
  public static final String ARGUMENTS = "xtc.Constants.Arguments";

  // =========================================================================

  /**
   * The extension (without the separating dot) for files containing
   * grammar modules.
   */
  public static final String EXT_GRAMMAR = "rats";

  // =========================================================================

  /** The attribute name of all factory attributes. */
  public static final String NAME_FACTORY = "factory";

  /** The attribute name of all flag attributes. */
  public static final String NAME_FLAG = "flag";

  /** The attribute name of all gcc attributes. */
  public static final String NAME_GCC = "gcc";

  /** The attribute name of all main attributes. */
  public static final String NAME_MAIN = "main";

  /** The attribute name of all parser attributes. */
  public static final String NAME_PARSER = "parser";

  /** The attribute name of all printer attributes. */
  public static final String NAME_PRINTER = "printer";

  /** The attribute name of all string set attributes. */
  public static final String NAME_STRING_SET = "setOfString";

  // =========================================================================

  /** The attribute name of all storage class attributes. */
  public static final String NAME_STORAGE = "storage";

  /** The <code>auto</code> storage class attribute. */
  public static final Attribute ATT_STORAGE_AUTO =
    new Attribute(NAME_STORAGE, "auto");

  /** The <code>extern</code> storage class attribute. */
  public static final Attribute ATT_STORAGE_EXTERN =
    new Attribute(NAME_STORAGE, "extern");

  /** The <code>register</code> storage class attribute. */
  public static final Attribute ATT_STORAGE_REGISTER =
    new Attribute(NAME_STORAGE, "register");

  /** The <code>static</code> storage class attribute. */
  public static final Attribute ATT_STORAGE_STATIC =
    new Attribute(NAME_STORAGE, "static");

  /** The <code>typdef</code> storage class attribute. */
  public static final Attribute ATT_STORAGE_TYPEDEF =
    new Attribute(NAME_STORAGE, "typedef");

  // =========================================================================

  /** The attribute name of all function style declaration attributes. */
  public static final String NAME_STYLE = "style";

  /** The old style attribute. */
  public static final Attribute ATT_STYLE_OLD =
    new Attribute(NAME_STYLE, "old");

  /** The new style attribute. */
  public static final Attribute ATT_STYLE_NEW =
    new Attribute(NAME_STYLE, "new");

  // =========================================================================

  /** The attribute name of all visibility attributes. */
  public static final String NAME_VISIBILITY = "visibility";

  /** The attribute value of the public visibility attribute. */
  public static final String VALUE_PUBLIC = "public";

  /** The attribute value of the protected visibility attribute. */
  public static final String VALUE_PROTECTED = "protected";

  /** The attribute value of the private visibility attribute. */
  public static final String VALUE_PRIVATE = "private";

  /** The public visibility attribute. */
  public static final Attribute ATT_PUBLIC =
    new Attribute(NAME_VISIBILITY, VALUE_PUBLIC);

  /** The protected visibility attribute. */
  public static final Attribute ATT_PROTECTED =
    new Attribute(NAME_VISIBILITY, VALUE_PROTECTED);

  /** The package private visibility attribute. */
  public static final Attribute ATT_PACKAGE_PRIVATE =
    new Attribute(NAME_VISIBILITY, "packagePrivate");

  /** The private visibility attribute. */
  public static final Attribute ATT_PRIVATE =
    new Attribute(NAME_VISIBILITY, VALUE_PRIVATE);

  // =========================================================================

  /** The canonical <code>abstract</code> attribute. */
  public static final Attribute ATT_ABSTRACT = new Attribute("abstract");

  /** The canonical <code>builtin</code> attribute. */
  public static final Attribute ATT_BUILTIN = new Attribute("builtin");

  /** The canonical <code>constant</code> attribute. */
  public static final Attribute ATT_CONSTANT = new Attribute("constant");

  /** The canonical <code>defined</code> attribute. */
  public static final Attribute ATT_DEFINED = new Attribute("defined");

  /** The canonical <code>dump</code> attribute. */
  public static final Attribute ATT_DUMP = new Attribute("dump");

  /** The canonical <code>explicit</code> attribute. */
  public static final Attribute ATT_EXPLICIT = new Attribute("explicit");

  /** The canonical <code>flatten</code> attribute. */
  public static final Attribute ATT_FLATTEN = new Attribute("flatten");

  /** The canonical <code>generic</code> attribute. */
  public static final Attribute ATT_GENERIC = new Attribute("generic");

  /** The canonical <code>genericAsVoid</code> attribute. */
  public static final Attribute ATT_GENERIC_AS_VOID =
    new Attribute("genericAsVoid");

  /** The canonical <code>ignoringCase</code> attribute. */
  public static final Attribute ATT_IGNORING_CASE =
    new Attribute("ignoringCase");

  /** The canonical <code>implicit</code> attribute. */
  public static final Attribute ATT_IMPLICIT = new Attribute("implicit");

  /** The canonical <code>inline</code> attribute. */
  public static final Attribute ATT_INLINE = new Attribute("inline");

  /** The canonical <code>loop</code> attribute. */
  public static final Attribute ATT_LOOP = new Attribute("loop");

  /** The canonical <code>lvalue</code> attribute. */
  public static final Attribute ATT_LVALUE = new Attribute("lvalue");

  /** The canonical <code>macro</code> attribute. */
  public static final Attribute ATT_MACRO = new Attribute("macro");

  /** The canonical <code>memoized</code> attribute. */
  public static final Attribute ATT_MEMOIZED = new Attribute("memoized");

  /** The canonical <code>native</code> attribute. */
  public static final Attribute ATT_NATIVE = new Attribute("native");

  /** The canonical <code>node</code> attribute. */
  public static final Attribute ATT_NODE = new Attribute("node");

  /** The canonical <code>notAValue</code> attribute. */
  public static final Attribute ATT_NOT_A_VALUE = new Attribute("notAValue");

  /** The canonical <code>noinline</code> attribute. */
  public static final Attribute ATT_NO_INLINE = new Attribute("noinline");

  /** The canonical <code>nowarn</code> attribute. */
  public static final Attribute ATT_NO_WARNINGS = new Attribute("nowarn");

  /** The canonical <code>optional</code> attribute. */
  public static final Attribute ATT_OPTIONAL = new Attribute("optional");

  /** The canonical <code>withParseTree</code> attribute. */
  public static final Attribute ATT_PARSE_TREE = new Attribute("withParseTree");

  /** The canonical <code>profile</code> attribute. */
  public static final Attribute ATT_PROFILE = new Attribute("profile");

  /** The canonical <code>rawTypes</code> attribute. */
  public static final Attribute ATT_RAW_TYPES = new Attribute("rawTypes");

  /** The canonical <code>resetting</code> attribute. */
  public static final Attribute ATT_RESETTING = new Attribute("resetting");

  /** The canonical <code>restrict</code> attribute. */
  public static final Attribute ATT_RESTRICT = new Attribute("restrict");

  /** The canonical <code>stateful</code> attribute. */
  public static final Attribute ATT_STATEFUL = new Attribute("stateful");

  /** The canonical <code>strictfp</code> attribute. */
  public static final Attribute ATT_STRICT_FP = new Attribute("strictfp");

  /** The canonical <code>synchronized</code> attribute. */
  public static final Attribute ATT_SYNCHRONIZED = new Attribute("synchronized");

  /** The canonical <code>threadLocal</code> attribute. */
  public static final Attribute ATT_THREAD_LOCAL = new Attribute("threadLocal");

  /** The canonical <code>transient</code> attribute. */
  public static final Attribute ATT_TRANSIENT = new Attribute("transient");

  /** The canonical <code>uninitialized</code> attribute. */
  public static final Attribute ATT_UNINITIALIZED =
    new Attribute("uninitialized");

  /** The canonical <code>used</code> attribute. */
  public static final Attribute ATT_USED = new Attribute("used");

  /** The canonical <code>variable</code> attribute. */
  public static final Attribute ATT_VARIABLE = new Attribute("variable");

  /** The canonical <code>variant</code> attribute. */
  public static final Attribute ATT_VARIANT = new Attribute("variant");

  /** The canonical <code>verbose</code> attribute. */
  public static final Attribute ATT_VERBOSE = new Attribute("verbose");

  /** The canonical <code>volatile</code> attribute. */
  public static final Attribute ATT_VOLATILE = new Attribute("volatile");

  /** The canonical <code>withLocation</code> attribute. */
  public static final Attribute ATT_WITH_LOCATION =
    new Attribute("withLocation");

}
