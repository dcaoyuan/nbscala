/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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

import xtc.tree.Node;

/**
 * The superclass of all grammar elements.
 *
 * @author Robert Grimm
 * @version $Revision: 1.13 $
 */
public abstract class Element extends Node {

  /** An element's tag. */
  public static enum Tag {
    /** An action. */                    ACTION,
    /** A node marker. */                NODE_MARKER,
    /** A nonterminal. */                NONTERMINAL,
    /** A null literal. */               NULL,
    /** An ordered choice. */            CHOICE,
    /** A parse tree node. */            PARSE_TREE_NODE,
    /** A sequence. */                   SEQUENCE,
    /** The any character constant. */   ANY_CHAR,
    /** A character class. */            CHAR_CLASS,
    /** A character literal. */          CHAR_LITERAL,
    /** A character switch. */           CHAR_SWITCH,
    /** A string literal. */             STRING_LITERAL,
    /** A binding. */                    BINDING,
    /** A parser action. */              PARSER_ACTION,
    /** A followed-by predicate. */      FOLLOWED_BY,
    /** A not-followed-by predicate. */  NOT_FOLLOWED_BY,
    /** A semantic predicate. */         SEMANTIC_PREDICATE,
    /** An option. */                    OPTION,
    /** A repetition. */                 REPETITION,
    /** A string match. */               STRING_MATCH,
    /** A voided element. */             VOIDED,
    /** An action base value. */         ACTION_BASE_VALUE,
    /** A binding value. */              BINDING_VALUE,
    /** A generic action value. */       GENERIC_ACTION_VALUE,
    /** A generic recursion value. */    GENERIC_RECURSION_VALUE,
    /** A generic node vlaue. */         GENERIC_NODE_VALUE,
    /** An empty list value. */          EMPTY_LIST_VALUE,
    /** A proper list value. */          PROPER_LIST_VALUE,
    /** A null value. */                 NULL_VALUE,
    /** A string value. */               STRING_VALUE,
    /** A token value. */                TOKEN_VALUE
  }
  
  /** Create a new element. */
  public Element() { /* Nothing to do. */ }

  /**
   * Get this element's tag.
   *
   * @return The tag.
   */
  public abstract Tag tag();

}
