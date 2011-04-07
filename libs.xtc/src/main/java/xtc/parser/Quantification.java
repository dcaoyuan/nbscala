/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006 Robert Grimm
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
package xtc.parser;

/**
 * The superclass of repetitions and options.
 *
 * @author Robert Grimm
 * @version $Revision: 1.1 $
 */
public abstract class Quantification extends UnaryOperator {

  /**
   * Create a new quantified element.
   *
   * @param element The element.
   */
  public Quantification(Element element) {
    super(element);
  }

}
