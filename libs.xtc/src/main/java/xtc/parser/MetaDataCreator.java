/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2006 Robert Grimm
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
 * Visitor to create the production meta-data.  Note that this visitor
 * only creates the meta-data records, but does not fill in
 * appropriate values.  Further note that this visitor assumes that
 * the entire grammar is contained in a single module.
 *
 * @see DeadProductionEliminator
 * @see MetaDataSetter
 *
 * @author Robert Grimm
 * @version $Revision: 1.11 $
 */
public class MetaDataCreator extends xtc.tree.Visitor {

  /** Create a new meta-data creator. */
  public MetaDataCreator() { /* Nothing to do. */ }

  /** Visit the specified grammar. */
  public void visit(Module m) {
    for (Production p : m.productions) dispatch(p);
  }

  /** Visit the specified production. */
  public void visit(Production p) {
    if (! p.hasProperty(Properties.META_DATA)) {
      p.setProperty(Properties.META_DATA, new MetaData());
    }
  }
 
}

