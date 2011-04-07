package xtc.tree;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import xtc.Constants;
import xtc.lang.JavaPrinter;

/**
 * A printer that guarantees that each Node is printed at the <em>exact</em>
 * file, line, and column specified by its location. Where necessary, this
 * printer prints line markers (similar to those used by the C preprocessor:<br>
 * 
 * <tt>#</tt> <i>line</i> <tt>"</tt><i>file</i><tt>"</tt><br>
 * 
 * Reproducing exact source locations helps downstream tools track symbolic
 * information for error messages, debugging, and exception backtraces. Even
 * though this printer makes a best effort to print nodes without locations 
 * nicely, experience shows that the generated code is usually harder to read
 * for humans (less "pretty") than with the superclass xtc.tree.Printer.
 */
public class LineupPrinter extends Printer {
  /**
   * Is the current line blank? If yes, the printer delays any indentation until
   * it encounters the first non-blank character on the line.
   */  
  protected boolean blankLine = true;
  
  /** Does the node currently being visited have a location? */
  protected boolean currentNodeHasLocation = true;
  
  /** File name specified by the last node that had a location. */
  protected String lastFile = null;
  
  /** Line number specified by the last node that had a location. */
  protected long lastLine = 0;
  
  /** File name specified by the last emitted line marker. */
  protected String markedFile = null;

  /** Line number specified by the last emitted line marker. */
  protected long markedLine = 0;
  
  /** Line number in the actual output after the last emitted line marker. */
  protected long markedPhysical = 0;
  
  protected boolean showFilePaths = true;
  
  public LineupPrinter(final OutputStream out, final boolean showFilePaths) {
    this(new PrintWriter(out, false), showFilePaths);
  }
  
  public LineupPrinter(final PrintWriter out, final boolean showFilePaths) {
    super(out);
    this.showFilePaths = showFilePaths;
  }

  public LineupPrinter(final Writer out, final boolean showFilePaths) {
    this(new PrintWriter(out, false), showFilePaths);
  }
  
  public Printer align(final int desiredColumn) {
    if (blankLine) {
      indent = desiredColumn;
      return this;
    }
    return super.align(desiredColumn);
  }

  /** Current line in the output adjusted for line markers. */
  protected long effectiveLine() {
    return markedLine + line - markedPhysical;
  }

  /** If blankLine is true, print any deferred line breaks and blank characters. */
  protected void endBlank() {
    if (blankLine) {
      if (Constants.FIRST_COLUMN < column) {
        if (!currentNodeHasLocation && null != lastFile)
          /* make it appear to debugger as if line+file stand still */
          printLineMarker(lastFile, lastLine);
        else
          super.pln();
      }
      blankLine = false;
      super.indent();
    }
  }

  /**
   * If the given node has a location, ensure that the output file, line, and
   * column is at exactly that location, by emitting some combination of line
   * markers, line breaks, and blanks.
   */
  protected void ensureLocation(final Node n) {
    if (null == n || !n.hasLocation())
      return;
    final Location nl = n.getLocation();
    /* Note: columns in locations are 1-based, whereas printer columns are based
     * on Constants.FIRST_COLUMN. Therefore, this code adjusts nl.column. */
    final int nlColumn = nl.column + Constants.FIRST_COLUMN - 1;
    if (null == markedFile || !markedFile.equals(nl.file)
        || nl.line < effectiveLine() || effectiveLine() + 2 < nl.line
        || nl.line == effectiveLine() && nlColumn < column) {
      markedFile = nl.file;
      markedLine = nl.line;
      printLineMarker(markedFile, markedLine);
      markedPhysical = line();
      assert effectiveLine() == nl.line;
    }
    assert markedFile.equals(nl.file) && effectiveLine() <= nl.line;
    while (effectiveLine() < nl.line)
      super.pln();
    if (blankLine) {
      if (Constants.FIRST_COLUMN == column)
        indent = nlColumn;
      blankLine = false;
    }
    assert column <= nlColumn;
    while (column < nlColumn)
      super.p(' ');
    assert effectiveLine() == nl.line && column == nlColumn;
  }

  public Printer indent() {
    if (indent < 0)
      indent = 0;
    return this;
  }

  public Printer indentLess() {
    if (indent < Constants.INDENTATION)
      indent = Constants.INDENTATION;
    return this;
  }
  
  public Printer indentMore() {
    if (indent < 0)
      indent = 0;
    return this;
  }

  public Printer p(final Attribute a) {
    return printNode(a);
  }
  
  public Printer p(final char c) {
    if (blankLine && ' ' == c) {
      indent++;
      return this;
    }
    endBlank();
    return super.p(c);
  }
  
  public Printer p(final Comment c) {
    return printNode(c);
  }
  
  public Printer p(final double d) {
    endBlank();
    return super.p(d);
  }
  
  public Printer p(final int i) {
    endBlank();
    return super.p(i);
  }
  
  public Printer p(final long l) {
    endBlank();
    return super.p(l);
  }
  
  public Printer p(final Node n) {
    return printNode(n);
  }
  
  public Printer p(final String s) {
    endBlank();
    return super.p(s);
  }
  
  public Printer pln() {
    blankLine = true;
    return this;
  }
  
  public Printer pln(final char c) {
    return p(c).pln();
  }
  
  public Printer pln(final double d) {
    return p(d).pln();
  }
  
  public Printer pln(final int i) {
    return p(i).pln();
  }

  public Printer pln(final long l) {
    return p(l).pln();
  }

  public Printer pln(final String s) {
    return p(s).pln();
  }

  public void printLineMarker(final String file, final long line) {
    if (Constants.FIRST_COLUMN < column)
      super.pln();
    final boolean isJava = visitor instanceof JavaPrinter;
    super.p(isJava ? "//#line " : "# ");
    final int sepIndex = file.lastIndexOf('/');
    final boolean keepFile = !isJava || showFilePaths || -1 == sepIndex;
    final String fileName = keepFile ? file : file.substring(sepIndex + 1);
    super.p(line + " \"" + fileName + "\"");
    super.pln();
    blankLine = true;
  }

  protected Printer printNode(final Node n) {
    final boolean outerNodeHasLocation = currentNodeHasLocation;
    currentNodeHasLocation = null != n && n.hasLocation();
    if (currentNodeHasLocation) {
      lastFile = n.getLocation().file;
      lastLine = n.getLocation().line;
    }
    if (n instanceof LineMarker) {
      final LineMarker m = (LineMarker) n;
      assert n.getLocation().file.equals(m.file) && n.getLocation().line == m.line - 1;
      if (Constants.FIRST_COLUMN < column)
        super.pln();
      indent = 0;
      markedFile = m.file;
      markedLine = m.line;
      markedPhysical = line + 2;
    } else {
      ensureLocation(n);
    }
    super.p(n);
    currentNodeHasLocation = outerNodeHasLocation;
    return this;
  }
  
  public Printer reset() {
    blankLine = true;
    return super.reset();
  }
  
  public Printer sep() {
    blankLine = true;
    return super.sep();
  }
}
