/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 New York University
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
package xtc.typical;

import xtc.util.Pair;
import xtc.util.Runtime;

import xtc.tree.Node;

import java.util.ArrayList;

/**
 * A Typical Reduction 
 *
 * @author Laune Harris
 * @version $Revision: 1.22 $
 */
@SuppressWarnings("unchecked")
public class Reduction {
 
  /** The runtime */
  protected Runtime runtime;
 
  /** The location node */
  protected Node location;
  
  /** The target list */
  protected Pair<Node> target;

  /** The set of patterns. */
  protected ArrayList<Analyzer.NodeMatch[]> patterns;

  /** The set of results. */
  protected ArrayList<Object> results;

  /** The resulting matches */
  protected Pair<Object> matches = Pair.empty();
    
  /** This flag indicates that the reduction has a 'singleton' constraint */
  protected boolean sing = false;

  /** This flag indicates that the reduction has a 'required' constraint */
  protected boolean req = false;

  /** This flag indicates that the reduction has a 'duplicate' constraint */
  protected boolean dup = false;

  /** This flag indicates that the reduction has a 'noduplicate' constraint */
  protected boolean nodup = false;

  /** This flag indicates that the reduction has a 'set' constraint */
  protected boolean set = false;

  /** This flag indicates that the reduction has a 'optional' constraint */
  protected boolean opt = false;
 
  /** This flag indicates that the reduction has a 'list' constraint */
  protected boolean list = false;
  
  /** Flag indicating that we've seen an error*/
  protected boolean error = false;

  /** This is the string value used for error reporting */
  protected String  tag;
  
  /**
   * Create a new reduction 
   */
  public Reduction(Pair<Node> target, Runtime runtime, Node location) {
    patterns = new ArrayList<Analyzer.NodeMatch[]>();
    results = new ArrayList<Object>();
    this.target = target;
    this.location = location;
    this.runtime = runtime;
   
    //by default don't allow duplicates
    nodup = true;
  }
  
  /**
   * add a pattern to this
   */
  public void addPattern(Object result, Analyzer.NodeMatch... pattern) {
    results.add(result);
    patterns.add(pattern);
  }

  /**
   * set opt to true
   */
  public void setOpt() {
    opt = true;
  }

  /**
   * set list to true
   */
  public void setList() {
    list = true;
  }

  /**
   * set list to true
   */
  public void setSet() {
    set = true;
  }
  
  /**
   * set list to true
   */
  public void setSing() {
    sing = true;
  }

  /**
   * set list to true
   */
  public void setReq() {
    req = true;
  }

  /**
   * set list to true
   */
  public void setNodup() {
    nodup = true;
  }

  /**
   * set list to true
   */
  public void setDup() {
    dup = true;
  }

  /**
   * set the tag
   * @param tag the tag to set
   */
  public void setTag(String tag) {
    this.tag = tag;
  }

  
  /**
   * Apply the reduction.
   * 
   * @return The given result value if the reduction is sucessful,
   *         null otherwise
   */
  public Object apply() {
    /* Here's what should be goin on. We've got a list of arrays of patterns
     * We iterate from the longest array of patterns to the shortets and apply
     * each array on the list. If there's a successful match we tag all the 
     * matches nodes with 'used = yes' to remove them from the available pool.
     * we also record the position of the pattern that matched. At the end
     * we check the constraints against the result and report an error message
     * if necessary. This is probably not the most efficient way to do this
     * right now, but it seems to be correct.
     */
    ArrayList<Integer> positions = new ArrayList<Integer>();
    
    //TODO sort the patterns from longest to shortest

    boolean dupError = false;

    for (Node n : target) {
      n.setProperty("used", "no");
    }

    //iterate over the patterns from longest to shortest
    for (int i = 0; i < patterns.size(); i++) {
      
      //this flag indicates that all sub patterns in a pattern array match
      //the list
      boolean allMatch = true;
      int matchCount = 0;
      
      //apply the pattern to the list
      for (Analyzer.NodeMatch nodeMatch : patterns.get(i)) {
        boolean match = isMatch(nodeMatch);
        if (match) matchCount++; 
        allMatch = allMatch && match;
      }
      
      //if matched tag all the matching nodes with 'used=yes'
      if (allMatch && (matchCount == patterns.get(i).length)) {
        positions.add(i);
        for (Node n : target) {
          if ("maybe".equals(n.getProperty("used"))) {
            n.setProperty("used", "yes");
          }
        }

        if (nodup) {
          //check for duplicates
          //do it all over again if there's another match we give an error
          allMatch = true;
          matchCount = 0;
      
          //apply the pattern to the list
          for (Analyzer.NodeMatch nodeMatch : patterns.get(i)) {
            boolean match = isMatch(nodeMatch);
            if (match) matchCount++; 
            allMatch = allMatch && match;
          }
          
          if (allMatch && (matchCount == patterns.get(i).length)) {
            dupError = true;
          }
        }

      } else {
        //otherwise tag them with 'used=no'
        for (Node n : target) {
          if ("maybe".equals(n.getProperty("used"))) {
            n.setProperty("used", "no");
          }
        }
      }      
    }
    
    int size = positions.size();
    
    
    if (dupError) {
      runtime.error("duplicate " + tag + "s defined");
      return null;
    }

    //constraint and error checks
    if (sing && size > 1) {
      runtime.error("multiple " + tag + "s defined", location);
      return null;
    }

    if (req && size == 0) {
      runtime.error("required " + tag, location);
      return null;
    }
    
    if (list) {
      Pair<Object> values = Pair.EMPTY;      
      for (int i : positions) {
        values = new Pair<Object>(results.get(i), values);
      }
      
      return values.reverse();
    }
   
    if (positions.size() > 0) {
      return results.get(positions.get(0));
    }
    
    return null;  
  }

  /**
   * @return <code>true</code> if this pattern matches a node in the list
   *         <code>false</code> otherwise.
   */
  private boolean isMatch(Analyzer.NodeMatch nm) {
    for (Node n : target) {
      if ("no".equals(n.getProperty("used")) && nm.apply(n)) {
        n.setProperty("used", "maybe");
        return true;
      }
    }
    return false;
  }

  /**
   * Return the node that matches a given pattern. This is useful for
   * as pattern.
   *
   * @return the matching node
   */
  public Node getMatch(Analyzer.NodeMatch nm) {
    for (Object n : target) {
      if (nm.apply((Node)n)) return (Node)n;
    }
    return null;
  }
  
}
