/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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
package xtc.tree;

/**
 * The interface to objects with a source location.
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public interface Locatable {

  /**
   * Determine whether this object has a location.
   *
   * @return <code>true</code> if this object has a location.
   */
  boolean hasLocation();

  /**
   * Get this object's location.
   *
   * @return This object's location or <code>null</code> if it does
   *   not have a location.
   */
  Location getLocation();

  /**
   * Set this object's location.
   *
   * @param location This object's location.
   */
  void setLocation(Location location);

  /**
   * Set this object's location to the specified locatable's location.
   *
   * @param locatable The locatable object.
   */
  void setLocation(Locatable locatable);

}
