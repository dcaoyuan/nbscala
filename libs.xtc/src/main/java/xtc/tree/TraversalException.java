/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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
package xtc.tree;

import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * The superclass of exceptions signaled during visitor dispatch.
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public class TraversalException extends RuntimeException {

  /**
   * Create a new traversal exception with the specified detail
   * message.
   *
   * @param message The detail message.
   */
  public TraversalException(String message) {
    super(message);
  }
  
  /**
   * Create a new traversal exception with the specified detail
   * message and cause.
   *
   * @param message The detail message.
   * @param cause The cause.
   */
  public TraversalException(String message, Throwable cause) {
    super(message, cause);
  }

  public Throwable getCause() {
    Throwable t = super.getCause();
    return null == t ? t : clean(t);
  }

  public void printStackTrace(PrintStream s) {
    clean(this);
    super.printStackTrace(s);
  }

  public void printStackTrace(PrintWriter s) {
    clean(this);
    super.printStackTrace(s);
  }

  /**
   * Clean the specified throwable's stack trace.  This method removes
   * any evidence of dynamic visitor dispatch from stack traces.
   *
   * @param t The throwable.
   * @return The throwable.
   */
  private static <T extends Throwable> T clean(T t) {
    StackTraceElement oldTrace[] = t.getStackTrace();

    int size = 0;
    for (StackTraceElement e : oldTrace) {
      if (isClean(e)) size++;
    }

    if (oldTrace.length == size) {
      if (null != t.getCause()) clean(t.getCause());
      return t;
    }

    List<StackTraceElement> newTrace = new ArrayList<StackTraceElement>(size);
    for (StackTraceElement e : oldTrace) {
      if (isClean(e)) newTrace.add(e);
    }
    t.setStackTrace(newTrace.toArray(new StackTraceElement[newTrace.size()]));

    if (null != t.getCause()) clean(t.getCause());
    return t;
  }

  /**
   * Determine whether the specified stack trace element is clean,
   * i.e., does not refer to {@link Visitor#dispatch(Node)} or Java
   * reflection.
   *
   * @param e The stack trace element.
   * @return <code>true</code> if the element is clean.
   */
  private static boolean isClean(StackTraceElement e) {
    String klass  = e.getClassName();
    String method = e.getMethodName();

    return (! ((method.equals("dispatch") &&
                klass.equals("xtc.tree.Visitor")) ||
               (method.startsWith("invoke") &&
                (klass.equals("java.lang.reflect.Method") ||
                 klass.startsWith("sun.reflect.")))));
  }

}
