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
package xtc;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import xtc.tree.Printer;

/**
 * Utility program to create a platform's configuration header.
 *
 * @author Robert Grimm
 * @version $Revision: 1.1 $
 */
public class Platform {

  /** Hide the constructor. */
  private Platform() { /* Nothing to do. */ }

  /** Generate the current platform's configuration header. */
  public static void main(String[] args) {
    Printer printer = new
      Printer(new BufferedWriter(new OutputStreamWriter(System.out)));

    printer.p("#define OS \"").p(System.getProperty("os.name")).p(' ').
      p(System.getProperty("os.version")).pln('"');
    printer.p("#define ARCH \"").p(System.getProperty("os.arch")).pln('"');
    printer.flush();
  }

}
