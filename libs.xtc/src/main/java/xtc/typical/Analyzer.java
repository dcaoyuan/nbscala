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

import java.util.ArrayList;
import java.util.Hashtable;
import xtc.tree.Node;
 
import xtc.util.Runtime;
import xtc.util.SymbolTable;
import xtc.util.Pair;
import xtc.util.Function;

/**
 * The base class of all Typical-generated type checkers.
 *
 * @author Laune Harris, Anh Le
 * @version $Revision: 1.145 $
 */
public abstract class Analyzer {
  
  /** The runtime. */
  protected final Runtime runtime;
  
  /** The symbol table. */
  protected final SymbolTable gamma;

  /** The property to check if enter a scope. */
  protected final String ENTERSCOPE = "enterScope" ;

  /** The property of check if exit a scope. */
  protected final String EXITSCOPE = "exitScope";

  /** 
   * The property to store the times of not entering a new scope because
   *   the new scope is also the current scope.
   */
  protected final String MAGICNUMBER = "magicNumber"; 
  
  /** The hash table */ 
  protected Hashtable<Object, Object> hashTable;
    
  /** The analyzer. */
  protected Function.F1<?, Node> analyzer;

  /** The tree root. */
  protected Node root;

  /** The visited node path.*/ 
  protected final ArrayList<Node> matching_nodes = new ArrayList<Node>();
  
  /** The list of names of the nodes that trigger scope changes. */
  protected final ArrayList<String> processScopeNodes = new ArrayList<String>();
  
  /** Interface for pattern matches. */
  public static interface Match<T> extends Function.F0<T> { /*empty*/ }

  /** Interface for ancestor Matches. */
  public static interface NodeMatch extends Function.F1<Boolean, Node> { /* empty */ } 
                                                    
  /** Interface for require expressions. */
  public static interface Require<T> extends Function.F0<T> { /*empty*/ }

  /** Interface for let expressions. */
  public static interface Let<T> extends Function.F0<T> { /*empty*/ }
  
  /** Interface for guard expressions. */
  public static interface Guard<T> extends Function.F0<T> { /*empty*/ }
  
  /** Get the names of the nodes that trigger scope changes. */
  protected abstract void getScopeNodes();
  
  /** The Typical load function to load predefined values and data types. */
  protected final Function.F3<Void,String,String,Object> load = 
    new Function.F3<Void,String,String,Object>() {
      public Void apply(String s, String ns, Object t) {
        if (null == s || null == ns) return null;
        gamma.current().define(SymbolTable.toNameSpace(s, ns), t);
        return null;
      }  
    };  

  /** The ancestor function.*/
  protected final Function.F1<Node, NodeMatch> ancestor = 
    new Function.F1<Node,NodeMatch>() {
      public final Node apply(NodeMatch pattern) {
        for (int i = matching_nodes.size() - 1; i >=0; i--) {
          final Node node = matching_nodes.get(i);
          if (pattern.apply(node)) return node;
        } 
        return null;
      }
    };
  
  /** The parent function.*/
  protected final Function.F1<Node, NodeMatch> parent = 
    new Function.F1<Node,NodeMatch>() {
      public final Node apply(NodeMatch pattern) {
        final Node node = matching_nodes.get(matching_nodes.size() -1);
        if (pattern.equals(node)) return node;
        return null;
      }
    };

  /** A Typical function to lookup a node, without error messages. */
  protected final Function.F2<Object, Node, Function.F1<?, Node>> lookup2 = 
    new Function.F2<Object, Node, Function.F1<?, Node>>() {
      public final Object apply(Node n, Function.F1<?, Node> getNameSpace) {
        Tuple.T3<Name<?>,String,String> tup = cast(getNameSpace.apply(n));
        assert (null != tup) : "Unable to map namespace for node " + 
                               (null == n ? "Null" : n.getName());
        checkEnterScope(n);

        final Object res = gamma.lookup(tup.get1().mangle(tup.get2()));
        if (null == res) showMessage("error",  "Undefined: " + 
                                     tup.get1().mangle(tup.get2()), n); 
        checkExitScope(n);
        return res;
      }  
    };

  /** A Typical function to lookup a node, with an error message. */
  protected final Function.F4<Object, Node, String, String, 
                              Function.F1<?, Node>> lookup4 = 
    new Function.F4<Object, Node, String, String, Function.F1<?, Node>>() {
      public final Object apply(Node n, String tag, String err, 
                             Function.F1<?, Node> getNameSpace) {
        Tuple.T3<Name<?>,String,String> tup = cast(getNameSpace.apply(n));
        assert (null != tup) : "Unable to map namespace for node " + 
                               (null == n ? "Null" : n.getName());        
        checkEnterScope(n);
        final Object res = gamma.lookup(tup.get1().mangle(tup.get2()));
        if (null == res) showMessage(tag, err, n);
        checkExitScope(n);
        return res;
      } 
    };

  /** A Typical function to lookup a node locally, without error messages. */
  protected final Function.F2<Object, Node, Function.F1<?, Node>> 
    lookupLocally2 = new Function.F2<Object, Node, Function.F1<?, Node>>() {
      public final Object apply(Node n, Function.F1<?, Node> getNameSpace) {
        Tuple.T3<Name<?>,String,String> tup = cast(getNameSpace.apply(n));
        assert (null != tup) : "Unable to map namespace for node " + 
                               (null == n ? "Null" : n.getName());

        checkEnterScope(n);
        final Object res = gamma.lookup(tup.get1().mangle(tup.get2()));
        if (null == res) showMessage("error", "Undefined in the current scope: " 
                                     + tup.get1().mangle(tup.get2()), n);
        checkExitScope(n);
        return res;
      }  
    };

  /** A Typical function to lookup a node locally, with an error message. */
  protected final Function.F4<Object, Node, String, String, 
                              Function.F1<?, Node>> lookupLocally4 = 
    new Function.F4<Object, Node, String, String, Function.F1<?, Node>>() {
      public final Object apply(Node n, String tag, String err, 
                             Function.F1<?, Node> getNameSpace) {
        Tuple.T3<Name<?>,String,String> tup = cast(getNameSpace.apply(n));
        assert (null != tup) : "Unable to map namespace for node " + 
                               (null == n ? "Null" : n.getName()); 
       
        checkEnterScope(n);
        final Object res = gamma.lookup(tup.get1().mangle(tup.get2()));
        if (null == res) showMessage(tag,err,n);
        checkExitScope(n);
        return res;
      } 
    };

  /** A Typical function to define a node, without error messages. */
  protected final Function.F3<Void, Node, Object, Function.F1<?, Node>> 
    define3 = new Function.F3<Void, Node, Object, Function.F1<?, Node>>() {
      public final Void apply(Node n, Object t, 
                              Function.F1<?, Node> getNameSpace) {
        Tuple.T3<Name<?>,String,String> tup = cast(getNameSpace.apply(n));
      
        assert (null != tup) : "define : no namespace for " + 
                               (null == n ? "Null" : n.getName());      
        checkEnterScope(n);
        gamma.current().define(tup.get1().mangle(tup.get2()), t);
        checkExitScope(n);
        return null;
      }     
    };

  /** A Typical function to define a node, with an error message. */ 
  protected final Function.F5<Void, Node, Object, String, String, 
                              Function.F1<?, Node>> define5 = 
    new Function.F5<Void, Node, Object, String, String, 
                    Function.F1<?, Node>>() {
      public final Void apply(Node n, Object t, String tag,
                         String err, Function.F1<?, Node> getNameSpace) {
        Tuple.T3<Name<?>,String,String> tup = cast(getNameSpace.apply(n));
    
        assert (null != tup) : "define : no namespace for " + 
                               (null == n ? "Null" : n.getName());  
    
        checkEnterScope(n);
        final String newName = tup.get1().mangle(tup.get2());
        if (gamma.current().isDefined(newName)) {
          showMessage(tag,err,n);          
        }
        gamma.current().define(newName, t);
        checkExitScope(n);
        return null;
      }
    };
  
  /** A Typical function to redefine a node. */
  protected final Function.F3<Void, Node, Object, Function.F1<?, Node>> 
    redefine = new Function.F3<Void, Node, Object, Function.F1<?, Node>>() {
      public final Void apply(Node n, Object t, 
                              Function.F1<?, Node> getNameSpace) {
        Tuple.T3<Name<?>,String,String> tup = cast(getNameSpace.apply(n));
    
        assert (null != tup) : "redefine : no namespace for " + 
                               (null == n ? "Null" : n.getName());  
          
        checkEnterScope(n);
        String newName = tup.get1().mangle(tup.get2());
        gamma.current().define(newName, t);
        checkExitScope(n);
        return null;        
      }   
    };

  /** A typical function for checking if a node has been defined */
  protected final Function.F2<Boolean, Node, Function.F1<?,Node>> isDefined = 
    new Function.F2<Boolean, Node, Function.F1<?,Node>>() {
      public final Boolean apply(Node n, Function.F1<?,Node> getNameSpace) {
        Tuple.T3<Name<?>,String,String> tup = cast(getNameSpace.apply(n));
        
        assert (null != tup) : "is_define : no namespace for " + 
                               (null == n ? "Null" : n.getName());
        checkEnterScope(n);
        final boolean res = gamma.isDefined(tup.get1().mangle(tup.get2()));          
        checkExitScope(n);
        return res;
      }       
    };
  
  /** A typical function for checking if a node has been locally defined */
  protected final Function.F2<Boolean, Node, Function.F1<?,Node>> 
    isDefinedLocally = new Function.F2<Boolean, Node, Function.F1<?,Node>>() {
      public final Boolean apply(Node n, Function.F1<?,Node> getNameSpace) {
        Tuple.T3<Name<?>,String,String> tup = cast(getNameSpace.apply(n));
        
        assert (null != tup) : "is_define_locally : no namespace for " + 
                               (null == n ? "Null" : n.getName());
        
        checkEnterScope(n);
        boolean res = gamma.current().isDefinedLocally(tup.get1().mangle(tup.get2()));
        checkExitScope(n);
        return res;
      }    
    };  

  /** The Typical function to print a symbol. */
  protected final Function.F1<Boolean, String> show_symbols = 
    new Function.F1<Boolean, String>() {
      public Boolean apply(String s) {
        if ("local".equals(s)) {
          gamma.current().dump(runtime.console());
          runtime.console().flush();        
        } else if ("all".equals(s)) {
          gamma.root().dump(runtime.console());
          runtime.console().flush();        
        } else {
          runtime.error("local or all required to use show_symbol function");  
        }
        return Boolean.TRUE;
      }  
    };  

  /** The Typical function to get a new name. */
  protected final Function.F1<String, String> freshName  = 
    new Function.F1<String, String>() {
      public String apply(String s){
        return gamma.freshName(s);
      }
    };  

  /** 
   * The Typical function to check if a value if bottom. 
   *   Return <code>true</code> if not bottom, otherwise bottom
   *   (It will be removed once guard is done)
   */
  protected final Function.F1<Boolean, Object> notBottom = 
    new Function.F1<Boolean, Object>() {
      public Boolean apply(Object t) {
        return null == t ? null : Boolean.TRUE;
      }
    }; 

  /**
   * Create a new TypeChecker base.
   *
   * @param runt The runtime.
   */
  public Analyzer(Runtime runt) {
    runtime = runt;
    gamma = new SymbolTable();
    hashTable = new Hashtable<Object, Object>();
    getScopeNodes();    
  }

  /**
   * Check a node and enter a scope.
   *
   * @param n The node to check.
   */
  protected void checkEnterScope(Node n) {   
    if (null != n && n.hasProperty(ENTERSCOPE)) {
      final String scopeName = (String)n.getProperty(ENTERSCOPE);
      if (!scopeName.equals(gamma.current().getName())){
        gamma.enter(scopeName);      
      } else {
        // magicNumber is the number of times not entering a new scope
        //   at this node because the new scope is the current scope
        if (!n.hasProperty(MAGICNUMBER)) {
          n.setProperty(MAGICNUMBER, 1);
        } else {
          final Integer num = (Integer)n.getProperty(MAGICNUMBER);
          n.setProperty(MAGICNUMBER, num + 1);
        }
      }
    }
    
  }
  
  /**
   * Check a node and exit scope.
   * 
   * @param n The node to check.
   */
  protected void checkExitScope(Node n){
    if (null != n && n.hasProperty(EXITSCOPE)) {
      if (!n.hasProperty(MAGICNUMBER)) gamma.exit();
      else {
        final Integer num = (Integer)n.getProperty(MAGICNUMBER);
        if (1 == num) n.removeProperty(MAGICNUMBER);
        else n.setProperty(MAGICNUMBER, num - 1);
      }
    }
    if (null != n && n.hasProperty("deleteScope")) {
      gamma.delete((String)n.getProperty("deleteScope"));
    }
  }
  
  /**
   * Print a error message.
   *
   * @param s The msg.
   * @param n The location.
   * @return null
   */  
  protected Object error(String s, Node n) {
    if (null == n) {
      n = matching_nodes.get(matching_nodes.size() -1);
    }    
    runtime.error(s, n);
    return null;    
  }
  
  /**
   * Print a warning message.
   *
   * @param s The msg.
   * @param n The location.
   * @return null
   */
  protected Object warning(String s, Node n) {
    if (null == n) {
      n = matching_nodes.get(matching_nodes.size() -1);
    }
    runtime.warning(s, n);
    return null;   
  }  

  /**
   * Print a error message.
   * 
   * @param tag The message class.
   * @param msg The message.
   * @param o The node generating the message.
   */
  protected void showMessage(String tag, String msg, Object o){
    Node n = (Node)o;
    if (null == n) {
      if ("error".equals(tag)) {
        if (matching_nodes.size() - 1 < 0) {
          runtime.error(msg);          
        } else {
          Node nod = matching_nodes.get(matching_nodes.size() - 1);
          runtime.error(msg,nod);          
        }        
      } else {
        if (matching_nodes.size() -1 < 0) {
          runtime.warning(msg);          
        } else {
          Node nod = matching_nodes.get(matching_nodes.size() - 1);
          runtime.warning(msg,nod);          
        }
      }
    } else {
      if ("error".equals(tag)) {
        runtime.error(msg,n);
      } else {
        runtime.warning(msg,n); 
      }
    } 
  }  
   
  /**
   * Process scope.
   *
   * @param n The node to process.
   * @param getScope The generated getScope function.
   */
  protected void processScope(Node n, Function.F1<?, Node> getScope) {
    // the function getScope(Node n) will be created 
    // after visiting the scope declaration
    Scope scop = cast(getScope.apply(n));
    if (null == scop) {
      throw new AssertionError("unable to get scope for : " + n.getName());
    }
    // get the scope name
    ScopeKind<?> scopename = scop.getTuple().get1();
    String str;
    if (scopename.isNamed()) {
      // Cast to object first to avoid inconvertible types error with Java 5.
      Name<?> strname = ((ScopeKind.Named)(Object)scopename).getTuple().get1();
      if (strname.isSimpleName()) {
        // Cast to object first to avoid inconvertible types error with Java 5.
        str = ((Name.SimpleName)(Object)strname).getTuple().get1();
      } else {
        str = "QualifiedName";
      }      
    } else if (scopename.isAnonymous()) {
      // Cast to object first to avoid inconvertible types error with Java 5.
      str = gamma.
        freshName(((ScopeKind.Anonymous)(Object)scopename).getTuple().get1());
    } else {
      // Cast to object first to avoid inconvertible types error with Java 5.
      str = gamma.
        freshName(((ScopeKind.Temporary)(Object)scopename).getTuple().get1());
    }  
 
    Pair<Node> nodes = scop.getTuple().get2();
    
    ArrayList<Node> nodeList = (ArrayList<Node>)nodes.list();
    int index;
    int prob = -1;
    // check and remove set scope property in those nodes
    for (index = 0; index < nodeList.size(); index++) {
      Node node = nodeList.get(index);
      if ( node == null) {
        continue;
      }
      if (!node.hasProperty(ENTERSCOPE)) {
        node.setProperty(ENTERSCOPE,str);
        node.setProperty(EXITSCOPE,true);
        prob = index;
      }
    }
    if ((prob!=-1) && (scopename.isTemporary())) {
      Node node = nodeList.get(prob);
      node.setProperty("deleteScope",str);
    }
  }
  
  /**
   * Run this typechecker on the tree rooted at n.
   *
   * @param n The tree root.
   * @return The Symbol table of the ast.
   */
  public SymbolTable run(Node n) {
    if (null == analyzer) {
      runtime.error("Analyzer is null");
      runtime.exit();
    }
    
    if (null == n) {
      runtime.error("Tree root is null");
      runtime.exit();
    } else {
      root = n;
    }

    matching_nodes.add(n);
    analyzer.apply(n);

    return gamma;
  }  

  /**
   * Returns the (possibly annotated and modified) AST root.
   *
   * @return The root of the ast.
   */
  public Node getASTRoot() {
    return root;
  }

  /**
   * Utility function to print a nicely formatted ast for debugging.
   * 
   * @param n The ast root.
   */
  protected void printAST(Node n){
    runtime.console().pln().format(n).pln().flush();
  }

  /**
   * Convert the specified object to a string.
   * 
   * @param o The object.
   * @return The corresponding string.
   */
  public static final String toString(Object o) {
    return null == o ? "?" : o.toString();
  }

   /**
   * Test for equality between two objects.
   * 
   * @param o1 The first object.
   * @param o2 The second object.
   * @return <code>true</code> if both are equal, false otherwise.
   */
  public static Boolean equal(Object o1, Object o2) {
    return null == o1 ? null == o2 : o1.equals(o2);
  }

  /**
   * Test for equality between two objects.
   * 
   * @param o1 The first object.
   * @param o2 The second object.
   * @return <code>true</code> if the o1 and o2 not equal, false otherwise.
   */
  protected static Boolean not_equal(Object o1, Object o2) {
    return null == o1 ? null != o2 : (! o1.equals(o2));
  }

  /**
   * Ignore the specified value.
   *
   * @param o The value to ignore.
   */
  protected static final void discard(Object o) {
    // Nothing to do.
  }
  
  /**
   * Cast an object to type T.
   * 
   * @param arg The object to cast.
   * @return The object cast to T
   */
  @SuppressWarnings("unchecked")
  public static final <T> T cast(Object arg) {
    return (T)arg;
  }
 
}

