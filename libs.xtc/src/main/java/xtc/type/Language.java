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
package xtc.type;

/**
 * A language tag
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public class Language {

  /** The canonical language tag for C. */
  public static final Language C = new Language("C");

  /** The canonical language tag for Java. */
  public static final Language JAVA = new Language("Java");

  /** The language's name. */
  private String name;

  /**
   * Create a new language tag.
   *
   * @param name The language's name.
   */
  public Language(String name) {
    this.name = name;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (! (o instanceof Language)) return false;
    return name.equals(((Language)o).name);
  }

  public String toString() {
    return name;
  }

}
