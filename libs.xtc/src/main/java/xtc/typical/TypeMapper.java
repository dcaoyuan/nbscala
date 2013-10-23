/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 New York University
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
package xtc.typical;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import xtc.util.Runtime;
import xtc.util.Pair;

import xtc.tree.Node;
import xtc.tree.GNode;

/**
 * Type to Java type mapper.
 * 
 * @author Laune Harris
 * @version $Revision: 1.58 $
 */
public class TypeMapper {

  class PatternMatchType {

    /** The type of the left hand side. */
    public Object left;

    /** The type of the right hand side. */
    public Object right;

    /** 
     * Create a new pattern match type. 
     *
     * @param left The type of the left hand side.
     * @param right The type of the right hand side.      
     */
    public PatternMatchType(Object left, Object right) {
      this.left = left;
      this.right = right;
    }
   
  }

  /** The singleton object indicating an object. */
  public static final Object OBJECT = new Object();

  /** The singleton object indicating a pair of objects. */
  public static final Object PAIR_OF_OBJECT = new Object();

  /** 
   * The tuple type of the tuple (name, string, string), 
   *   used in namespace translation. 
   */
  public static final Node nameTupleT =
    GNode.create("Type",
      GNode.create("InstantiatedType",
        GNode.create("TypeInstantiation", "Tuple", null),
        GNode.create("TypeInstantiation", "T3",
          GNode.create("TypeArguments",
            GNode.create("Type",
              GNode.create("QualifiedIdentifier", "Name"),
              null),
            GNode.create("Type",
              GNode.create("QualifiedIdentifier", "String"),
              null),
            GNode.create("Type",
              GNode.create("QualifiedIdentifier", "String"),
              null)))),
      null);

  /** The runtime. */
  protected final Runtime runtime;

  /** The prefix of the types that belong to the xxxTypes.java file*/
  protected final String prefix;

  /** The flag for replacing type with raw_type. */
  protected final boolean replaceType;

  /** The prefix without a dot at the end. */
  protected final String prefixNoDot;

  /** The tree factory. */
  protected final TreeFactory factory;

  /** A list of types that are nodes. */
  Pair<String> nodeTypes;

  /** The set of seen match conditions */
  protected HashMap<String, String> variableNames = 
    new HashMap<String, String>();

  /** Being processing a function type. */
  protected boolean inFunctionType;

  /**
   * Create a new type mapper.
   * 
   * @param runtime The runtime.
   * @param prefix The class name prefix.
   * @param replaceType The flag for replacing type with raw_type.
   */
  public TypeMapper(Runtime runtime, String prefix, 
                    boolean replaceType, Pair<String> nodeTypes) {
    factory = new TreeFactory();
    inFunctionType = false;

    this.runtime = runtime;
    this.prefixNoDot = prefix;
    this.prefix = prefix + ".";    
    this.replaceType = replaceType;
    this.nodeTypes = nodeTypes;
  }

  /**
   * Make a pattern match type.
   *
   * @param left The type of the left hand side.
   * @param right The type of the right hand side.
   */
  public PatternMatchType makePatternMatchType(Object left, Object right) {
    if (null == left) throw new AssertionError("Null left type");
    if (null == right) throw new AssertionError("Null right type");
    return new PatternMatchType(left, right);
  }
  
  /**
   *  Get annotated string from a type.
   *  
   *  @param o The type to get the annotated string from.
   *  @return The annotated string.
   */
  public static String getAnnotatedString(Object o) {
	if (o instanceof TypicalTypes.type) {  
	  TypicalTypes.type t = (TypicalTypes.type)o;
	  @SuppressWarnings("unchecked")
	  TypicalTypes.raw_type<?> rt = (TypicalTypes.raw_type<?>)t.type;
	  return getAnnotatedString(rt);
	} else if (o instanceof TypicalTypes.raw_type) {
	  TypicalTypes.raw_type<?> rt = (TypicalTypes.raw_type<?>)o;
	  if (rt.isStringName()) {
	  	return (String)rt.getTuple().get1();
	  } else throw new AssertionError("unknown type " + rt); 
	} else throw new AssertionError("unknown type " + o);			
  }
  
  /**
   *  Get annotated string from a type.
   *  
   *  @param o The type to get the annotated string from.
   *  @return The annotated string.
   */
  public static Pair<String> getAnnotatedStringList(Object o) {
	if (o instanceof TypicalTypes.type) {  
	  TypicalTypes.type t = (TypicalTypes.type)o;
	  @SuppressWarnings("unchecked")
	  TypicalTypes.raw_type<?> rt = (TypicalTypes.raw_type<?>)t.type;
	  return getAnnotatedStringList(rt);
	} else if (o instanceof TypicalTypes.raw_type) {
	  TypicalTypes.raw_type<?> rt = (TypicalTypes.raw_type<?>)o;
	  if (rt.isStringList()) {
		Pair<String> ob = rt.getTuple().get1();
		return ob;
	  } else throw new AssertionError("unknown type " + rt); 
	} else throw new AssertionError("unknown type " + o);	
  }

  /**
   * Print a type as a Java generic type.
   *
   * @param o The type object.
   * @return The JavaTypeString.
   */
  public String toTypeString(Object o) {
    if (null == o) throw new AssertionError("Null type");
       
    if (OBJECT == o) {
      return "Object";
    }

    if (PAIR_OF_OBJECT == o) {
      return "Pair<?>";
    } 
    
    if (o instanceof String) return getType((String)o); 
    
    if (o instanceof TypicalTypes.type) {
      TypicalTypes.type t = (TypicalTypes.type)o;
      return toTypeString(t.type);
    } else if (o instanceof TypicalTypes.raw_type) {
      TypicalTypes.raw_type<?> rt = (TypicalTypes.raw_type<?>)o;
      
      if (rt.isBoolT())        return "Boolean";
      if (rt.isIntT())         return "BigInteger";
      if (rt.isFloat32T())     return "Float";
      if (rt.isFloat64T())     return "Double";
      if (rt.isStringT())      return "String";
      if (rt.isAnyT())         return "Object";
      if (rt.isWildcardT())    return "Object";
      
      if (rt.isVariableT()) {
        String str = rt.getTuple().get1();
        if (variableNames.containsKey(str)) {
          return variableNames.get(str);
        } else return "Object";
      }

      if (rt.isConstructorT()) return getType((String)rt.getTuple().get1());
 
      if (rt.isVariantT()) {
        Pair<Object> params = rt.getTuple().get1();
        @SuppressWarnings("unchecked")
        TypicalTypes.raw_type<?> variant = unwrapRawType(params.head());
        return getType((String)variant.getTuple().get1());        
      } 
      
      if (rt.isPolyVariantT()) return getType("Node");
            
      if (rt.isNodeTypeT()) return getType("Node");

      if (rt.isFieldT()) {
        return getType(toTypeString(rt.getTuple().get3()));
      }

      if (rt.isConstructedT()) {
        // Work around
        Object ob = rt;
        @SuppressWarnings("unchecked")
        TypicalTypes.ConstructedT ct = (TypicalTypes.ConstructedT)ob;
        String str = (String)ct.getTuple().get2();
        if ("list".equals(str)) {
          return "Pair<" + getType(toTypeString(getBase(rt))) + ">";
        } else if ("var".equals(str)) {
          return getType(toTypeString(getBase(rt)));
        } else {
          return str;
        }
      }

      if (rt.isTypeName()) return getType((String)rt.getTuple().get1());

      if (rt.isRecordT()) {
        Pair<Object> p = rt.getTuple().get1();
        Object head = p.head(); 
        @SuppressWarnings("unchecked")
        TypicalTypes.raw_type field = unwrapRawType(head);
        if(field.isFieldT()){  
          final String recName = (String)field.getTuple().get1(); 
          return getType(recName);
        } else {
          return toTypeString(field);
        } 
      }
     
      if (rt.isTupleT()) {
        List<String> members = getMembers(rt);
        String res = "Tuple.T" + members.size() + "<";
        for (int i = 0; i < members.size(); i++) {
          res += getType(members.get(i));
          if (i < members.size() -1) {
            res += ",";
          }
        }
        res += ">";
        return res;
      }

      if (rt.isPairOfType()) {
        return toTypeString(rt.getTuple().get1());
      }
      if (rt.isFunctionT()) {
        inFunctionType = true;
        List<String> paramTypes = getParameterTypes(o);
        String ret = getReturnType(o);
        
        StringBuilder str = new StringBuilder("Function.F" + 
                                              paramTypes.size() + "<" + ret);
        for (String s : paramTypes) str.append("," + s);
        str.append(">");
        inFunctionType = false;
        return str.toString();
      }
    } else if (o instanceof PatternMatchType) {
      PatternMatchType po = (PatternMatchType)o;
      return toTypeString(po.left);
    }

    throw new AssertionError("unknown type " + o);
  }

  /**
   * Get a java type node from a typical type
   *
   * @param o The typical type object
   * @param objectToWild To check if need to change Object to ?
   * @return The corresponding java type node
   */
  public Node toTypeNode(Object o, boolean objectToWild) {
    if (null == o) throw new AssertionError("Null type");
    
    // If this is already a type node, return it
    if(o instanceof Node) return (Node)o;

    // Object type
    if (OBJECT == o) return makeTypeNode("Object", objectToWild);

    // A type name
    if (o instanceof String) return makeTypeNode((String)o, objectToWild);

    // Pair of Object type
    if (PAIR_OF_OBJECT == o) {
      return GNode.create("Type",
               GNode.create("InstantiatedType",
                 GNode.create("TypeInstantiation", 
                   "Pair",
                   GNode.create("TypeArguments",
                     GNode.create("Wildcard", null)))),
               null);
    } 

    // A Typical type
    if (o instanceof TypicalTypes.type) {
      TypicalTypes.type t = (TypicalTypes.type)o;
      return toTypeNode(t.type, objectToWild);
    } // A raw_type
      else if (o instanceof TypicalTypes.raw_type) {
      TypicalTypes.raw_type<?> rt = (TypicalTypes.raw_type<?>)o;
      
      // Basic types
      if (rt.isBoolT())        return makeTypeNode("Boolean", false);
      if (rt.isIntT())         return makeTypeNode("BigInteger", false);
      if (rt.isFloat32T())     return makeTypeNode("Float", false);
      if (rt.isFloat64T())     return makeTypeNode("Double", false);
      if (rt.isStringT())      return makeTypeNode("String",false);
      if (rt.isAnyT())         return makeTypeNode("Object", objectToWild);
      if (rt.isWildcardT())    return makeTypeNode("Object", objectToWild);
      //if (rt.isVariableT())    return makeTypeNode("Object", objectToWild);

      if (rt.isVariableT()) {
        String str = rt.getTuple().get1();
        if (objectToWild) {
          return makeTypeNode("Object", objectToWild);
        }
        if (variableNames.containsKey(str)) {
          return GNode.create("Type",
                   GNode.create("QualifiedIdentifier", variableNames.get(str)),
                   null);
        } else return makeTypeNode("Object", objectToWild);
      }


      // Constructor type
      if (rt.isConstructorT()) {
        return makeTypeNode((String)rt.getTuple().get1(), objectToWild);
      }

      // Variant type
      if (rt.isVariantT()) {
        Pair<Object> params = rt.getTuple().get1();
        @SuppressWarnings("unchecked")
        TypicalTypes.raw_type<?> variant = unwrapRawType(params.head());
        return makeTypeNode((String)variant.getTuple().get1(), objectToWild);        
      }
      
      if (rt.isPolyVariantT()) return makeTypeNode("Node", objectToWild);
      
      if (rt.isNodeTypeT()) return makeTypeNode("Node", objectToWild);
      // Field type
      if (rt.isFieldT()) {
        return toTypeNode(rt.getTuple().get3(), objectToWild);
      }

      // Constructed type
      if (rt.isConstructedT()) {
        // Work around
        Object ob = rt;
        @SuppressWarnings("unchecked")
        TypicalTypes.ConstructedT ct = (TypicalTypes.ConstructedT)ob;
        String str = (String)ct.getTuple().get2();
        Node baseNode = toTypeNode(getBase(rt), objectToWild);
        if ("list".equals(str)) {
          return GNode.create("Type",
                   GNode.create("InstantiatedType",
                     GNode.create("TypeInstantiation", 
                       "Pair",
                       GNode.create("TypeArguments", baseNode))),
                   null);
        } else if ("var".equals(str)) {
          return baseNode;
        } else {
          return makeTypeNode(str, objectToWild);
        }
      }

      // Type name
      if (rt.isTypeName()) {
        return makeTypeNode((String)rt.getTuple().get1(), objectToWild);
      }

      // Record type
      if (rt.isRecordT()) {
        Pair<Object> p = rt.getTuple().get1();
        Object head = p.head(); 
        @SuppressWarnings("unchecked")
        TypicalTypes.raw_type field = unwrapRawType(head);
        if(field.isFieldT()){  
          final String recName = (String)field.getTuple().get1(); 
          return makeTypeNode(recName, objectToWild);
        } else {
          return toTypeNode(field, objectToWild);
        } 
      }

      // Tuple type
      if (rt.isTupleT()) {
       
        //Workaround to fix incompatible type bug on Mac OS
        Object o1 = rt;
        @SuppressWarnings("unchecked")
        TypicalTypes.TupleT tt = (TypicalTypes.TupleT)o1; 
        //End workaround

        List<Object> members = new ArrayList<Object>();
        @SuppressWarnings("unchecked")
        Pair<?> tl  = tt.getTuple().get1();
        for (Iterator<?> iter = tl.iterator(); iter.hasNext(); ) {
          members.add(iter.next());
        }
        // Make type arguments        
        Node typeArgs = GNode.create("TypeArguments");                

        for (Object ob : members) typeArgs.add(toTypeNode(ob, objectToWild));
        
        // Return the tuple type
        return GNode.create("Type",
                 GNode.create("InstantiatedType",
                   GNode.create("TypeInstantiation", "Tuple", null),
                   GNode.create("TypeInstantiation", "T" + members.size(), 
                                typeArgs)),
                 null);
      }

      if (rt.isPairOfType()) {
        return toTypeNode(rt.getTuple().get1(), objectToWild);
      }

      // Function type
      if (rt.isFunctionT()) {
        inFunctionType = true;
        List<Object> paramTypes = new ArrayList<Object>();

        Pair<?> params = cast(rt.getTuple().get1());
        for (Iterator<?> iter = params.iterator(); iter.hasNext();) {
          paramTypes.add(iter.next());
        }

        Node retTypeNode;
        Object retType = rt.getTuple().get2();
        if (retType instanceof TypicalTypes.type) {
          TypicalTypes.type retT = (TypicalTypes.type)retType;
          if (null == retT.type) {
            retTypeNode = makeTypeNode("Void", false) ;
          } else {
            retTypeNode = toTypeNode(retT, objectToWild);
          }
        } else {
          if (null == retType) {
            retTypeNode = makeTypeNode("Void", false) ;
          } else {
            retTypeNode = toTypeNode(retType, objectToWild);
          }
        }
        
        // Make type arguments
        Node typeArgs = GNode.create("TypeArguments");
        typeArgs.add(retTypeNode);
                 
        for (Object ob : paramTypes) typeArgs.add(toTypeNode(ob, objectToWild));
        inFunctionType = false;
        // Return the function type
        return GNode.create("Type",
                 GNode.create("InstantiatedType",
                   GNode.create("TypeInstantiation", "Function", null),
                   GNode.create("TypeInstantiation", "F" + paramTypes.size(), 
                                typeArgs)),
                 null);
      }
    } else if (o instanceof PatternMatchType) {
      PatternMatchType po = (PatternMatchType)o;
      return toTypeNode(po.left, objectToWild);
    }

    throw new AssertionError("unknown type " + o);    
  }
  
  /**
   * Check if a typical type contains type variables.
   *
   * @param o The typical type to check.
   * @return <code> true </code> if this type contains type variables,
   *    <code> false </code> otherwise 
   */
  public boolean hasTypeVariables(Object o) {
    if (null == o) return false;
    if (o.equals(nameTupleT)) return false;

    if (isTGType(o)) {
      // Get the type field
      TypicalTypes.raw_type<?> rt = unwrapRawType(o);
     
      if (null == rt) throw new AssertionError("Null raw type");
 
      if (rt.isVariableT()) {
        String str = (String)rt.getTuple().get1();
        if (variableNames.containsKey(str)) return true;
        return false;        
      }

      // Check with each kind of field type
      if (rt.isConstructorT()) {
        return hasTypeVariables(rt.getTuple().get3());
      } 
      
      if (rt.isVariantT()) {
        Pair<Object> tl = rt.getTuple().get1();
        for (Iterator<?> iter = tl.iterator(); iter.hasNext(); ) {
          if (hasTypeVariables(iter.next())) return true;
        }
        return false;
      }

      if (rt.isFieldT()) {
        return hasTypeVariables(rt.getTuple().get3());
      }

      if (rt.isConstructedT()) {
        // FIXME HERE - Support other constructed types
        return hasTypeVariables(getBase(rt));
      }

      if (rt.isRecordT()) {
        Pair<Object> tl = rt.getTuple().get1();
        for (Iterator<?> iter = tl.iterator(); iter.hasNext(); ) {
          if (hasTypeVariables(iter.next())) return true;
        }
        return false;        
      }

      if (rt.isTupleT()) {
        //Workaround to fix incompatible type bug on Mac OS
        Object o1 = rt;
        @SuppressWarnings("unchecked")
        TypicalTypes.TupleT tt = (TypicalTypes.TupleT)o1; 
        //End workaround

        @SuppressWarnings("unchecked")
        Pair<?> tl  = tt.getTuple().get1();
        for (Iterator<?> iter = tl.iterator(); iter.hasNext(); ) {
          if (hasTypeVariables(iter.next())) return true;
        }
        return false;
      }

      if (rt.isPairOfType()) {
        return hasTypeVariables(rt.getTuple().get1()) ||
               hasTypeVariables(rt.getTuple().get2());        
      }

      if (rt.isFunctionT()) {
        Object retType = rt.getTuple().get2();
        if (hasTypeVariables(retType)) return true;
        
        Pair<?> params = cast(rt.getTuple().get1());
        for (Iterator<?> iter = params.iterator(); iter.hasNext();) {
          if (hasTypeVariables(iter.next())) return true;          
        }
        return false;        
      }
      return false;

    } else if (o instanceof PatternMatchType) {
      PatternMatchType po = (PatternMatchType)o; 
      return hasTypeVariables(po.left) || hasTypeVariables(po.right);    

    } else { // Not a typical type
      throw new AssertionError("unknown type " + o);
    }    
  }

  /**
   * Get type variables from a typical type
   *
   * @param o The type to get type variables from
   * @return A list of type variables
   */
  public List<String> getTypeVariables(Object o) {
    List<String> res = new ArrayList<String>();
    if (null == o) return res;

    if (isTGType(o)) {
      // Get the type field
      TypicalTypes.raw_type<?> rt = unwrapRawType(o);
     
      if (null == rt) return res;
 
      if (rt.isVariableT()) {
        res.add((String)rt.getTuple().get1());
        return res;
      }

      // Check with each kind of field type
      if (rt.isConstructorT()) {
        return getTypeVariables(rt.getTuple().get3());
      } 
      
      if (rt.isVariantT()) {
        Pair<Object> tl = rt.getTuple().get1();
        for (Iterator<?> iter = tl.iterator(); iter.hasNext(); ) {
          List<String> varList = getTypeVariables(iter.next());
          for (String mem : varList) if (-1 == res.indexOf(mem)) res.add(mem);
        }
        return res;
      }

      if (rt.isFieldT()) {
        return getTypeVariables(rt.getTuple().get3());
      }

      if (rt.isConstructedT()) {
        // FIXME HERE - Support other constructed types
        return getTypeVariables(getBase(rt));
      }

      if (rt.isRecordT()) {
        Pair<Object> tl = rt.getTuple().get1();
        for (Iterator<?> iter = tl.iterator(); iter.hasNext(); ) {
          List<String> varList = getTypeVariables(iter.next());
          for (String mem : varList) if (-1 == res.indexOf(mem)) res.add(mem);
        }
        return res;        
      }

      if (rt.isTupleT()) {
        //Workaround to fix incompatible type bug on Mac OS
        Object o1 = rt;
        @SuppressWarnings("unchecked")
        TypicalTypes.TupleT tt = (TypicalTypes.TupleT)o1; 
        //End workaround

        @SuppressWarnings("unchecked")
        Pair<?> tl  = tt.getTuple().get1();
        for (Iterator<?> iter = tl.iterator(); iter.hasNext(); ) {
          List<String> varList = getTypeVariables(iter.next());
          for (String mem : varList) if (-1 == res.indexOf(mem)) res.add(mem);
        }
        return res;
      }

      if (rt.isPairOfType()) {
        List<String> varList = getTypeVariables(rt.getTuple().get1());
        res = varList;
        varList = getTypeVariables(rt.getTuple().get2());
        for (String mem : varList) if (-1 == res.indexOf(mem)) res.add(mem);
        return res;        
      }


      if (rt.isFunctionT()) {
        Pair<?> params = cast(rt.getTuple().get1());
        List<String> varList;
        for (Iterator<?> iter = params.iterator(); iter.hasNext();) {
          varList = getTypeVariables(iter.next());
          for (String mem : varList) if (-1 == res.indexOf(mem)) res.add(mem);
        }

        Object retType = rt.getTuple().get2();
        varList = getTypeVariables(retType);
        for (String mem : varList) if (-1 == res.indexOf(mem)) res.add(mem);
        return res;        
      }
      return res;

    } else if (o instanceof PatternMatchType) {
      PatternMatchType po = (PatternMatchType)o; 
      List<String> varList = getTypeVariables(po.left);
      res = varList;
      varList = getTypeVariables(po.right);
      for (String mem : varList) if (-1 == res.indexOf(mem)) res.add(mem);
      return res;

    } else { // Not a typical type
      throw new AssertionError("unknown type " + o);
    }    
  }
  
  /**
   * Preprocess type variables in function type
   * 
   * @param o The function type to process
   * @param index The starting index
   * @return the number of type parameters
   */
  public int processTypeVariables(Object o, int index) {
    if (null == o) throw new AssertionError("Null type");
    
    if (isTGType(o)) {
      // Get the type field
      TypicalTypes.raw_type<?> rt = unwrapRawType(o);
      if (rt.isFunctionT()) {
        Pair<?> params = cast(rt.getTuple().get1());
        Object retType = rt.getTuple().get2();

        // Get type variables from return type
        List<String> retTypeVariables = getTypeVariables(retType);
        
        // Get all type variables from parameter types
        List<List<String>> sll = new ArrayList<List<String>>();
        for (Iterator<?> iter = params.iterator(); iter.hasNext();) {
          List<String> varList = getTypeVariables(iter.next());
          sll.add(varList);
        }

        // If a type variables is in return type, assign a new name and
        //   put in the hash map, otherwise put "Object"
        int res = 0;
        
        for (int li = 0; li < sll.size(); li++) {
          List<String> sl = sll.get(li);
          for (String mem : sl) {
            // Check if this type variabl appears more than once or
            //   it appears in the return type  
            if (((sl.indexOf(mem) != sl.lastIndexOf(mem)) || 
                (-1 != retTypeVariables.indexOf(mem))) &&
                !variableNames.containsKey(mem)) {
              variableNames.put(mem, "T" + index);
              index++;
              res++;    
            } else {
              // Check if this type variable appear in other parameter types
              for (int ld = 0; ld < sll.size(); ld++) {
                if (li != ld) {
                  List<String> strl = sll.get(ld);
                  if ((-1 != strl.indexOf(mem)) &&
                      !variableNames.containsKey(mem)) {
                    variableNames.put(mem, "T" + index);
                    index++;
                    res++;      
                  } 
                }
              }
            }
          }
        }        
        if ((null != retType) && isFunctionType(retType)) {
          res = res + processTypeVariables(retType, index);          
        }
        return res;
      } else {
        throw new AssertionError("function type expected, found " + o);
      }
    } else {
      throw new AssertionError("unknown type " + o);
    }  
  }

  /**
   * Get the type of the left side of a pattern matching.
   *
   * @param o The pattern match type.
   * @return The left type. 
   */
  public Object getPatternMatchLeftType(Object o) {
    if (null == o) throw new AssertionError("Null type");
   
    TypicalTypes.raw_type<?> pm = null;
    if (isTGType(o)) {
      pm = unwrapRawType(o);
      if (pm.isPairOfType()) return pm.getTuple().get1();
      else {
        throw new AssertionError("PairOfType is expected, found " + o);
      }
    } else if (o instanceof PatternMatchType) {
      PatternMatchType po = (PatternMatchType)o;
      return po.left;
    } else {
      throw new AssertionError("unknown type " + o);
    }
  }

  /**
   * Get the type of the left side of a pattern matching.
   *
   * @param o The pattern match type.
   * @return The left type. 
   */
  public Object getPatternMatchRightType(Object o) {
    if (null == o) throw new AssertionError("Null type");
   
    TypicalTypes.raw_type<?> pm = null;
    if (isTGType(o)) {
      pm = unwrapRawType(o);
      if (pm.isPairOfType()) return pm.getTuple().get2();
      else {
        throw new AssertionError("PairOfType is expected, found " + o);
      }
    } else if (o instanceof PatternMatchType) {
      PatternMatchType po = (PatternMatchType)o;
      return po.left;
    } else {
      throw new AssertionError("unknown type " + o);
    }   
  }
  
  /**
   * Unwrapp a raw_type.
   *
   * @param o The type object.
   * @return Unwrapped raw_type.
   */
  private TypicalTypes.raw_type<?> unwrapRawType(Object o) {
    if (null == o) throw new AssertionError("Null type");

    if (o instanceof TypicalTypes.raw_type) return (TypicalTypes.raw_type<?>)o;

    if (o instanceof TypicalTypes.type) {
      TypicalTypes.type t = (TypicalTypes.type)o;
      return t.type;
    }
    
    throw new IllegalArgumentException("can't unwrap non type");
  }
  
  /**
   * Test if a type is a list type.
   *
   * @param o The type to test.
   * @return <code> true </code> if this type is a list type,
   *    <code> false </code> otherwise.
   */
  public boolean isList(Object o) {
    if (null == o) throw new AssertionError("Null type");

    // FIXME check the constructor.
    if (PAIR_OF_OBJECT == o) return true;
   
    TypicalTypes.raw_type<?> rt = null;

    if (isTGType(o)) {
      rt = unwrapRawType(o);
    } else {
      throw new AssertionError("unknown type " + o);
    }
    if (!rt.isConstructedT()) return false;
    else {
      // Work around
      Object ob = rt;
      @SuppressWarnings("unchecked")
      TypicalTypes.ConstructedT ct = (TypicalTypes.ConstructedT)ob;
      String str = (String)ct.getTuple().get2();
      return "list".equals(str);
    }    
  }

  /**
   * Test if a type is a variable type.
   *
   * @param o The type to test.
   * @return <code> true </code> if this type is a variable type,
   *    <code> false </code> otherwise.
   */
  public boolean isVariable(Object o) {
    if (null == o) throw new AssertionError("Null type");

    // FIXME check the constructor.
    if (PAIR_OF_OBJECT == o) return true;
   
    TypicalTypes.raw_type<?> rt = null;

    if (isTGType(o)) {
      rt = unwrapRawType(o);
    } else {
      throw new AssertionError("unknown type " + o);
    }
    if (!rt.isConstructedT()) return false;
    else {
      // Work around
      Object ob = rt;
      @SuppressWarnings("unchecked")
      TypicalTypes.ConstructedT ct = (TypicalTypes.ConstructedT)ob;
      String str = (String)ct.getTuple().get2();
      return "var".equals(str);
    }    
  }

  /**
   * Check if this type contains an any type.
   *
   * @param o The type to test.
   * @return <code> true </code> if this type contains an any type,
   *    <code> false </code> otherwise.
   */
  public boolean containsAny(Object o){
    if (null == o) throw new AssertionError("Null type");

    List<String> members = getMembers(o);
    
    if (members.contains("AnyT")) return true;

    return false;
  }

  /**
   * Test if a type is a node.
   *
   * The type object.
   *
   * @return <code>true</code> if node <code>false</code> otherwise.
   */
  public boolean isNode(Object o) {
    return "Node".equals(toTypeString(o));
  }
  
  /**
   * Get the base of a contstructed type.
   *
   * @param o The type object.
   * @return The base type.
   */
  public Object getBase(Object o) {
    if (null == o) throw new AssertionError("Null type");

    if (PAIR_OF_OBJECT == o) return OBJECT;
    
    TypicalTypes.raw_type<?> rt = null;

    if (isTGType(o)) rt = unwrapRawType(o);
    else throw new AssertionError("Not a type " + o);    
    
    if (rt.isConstructedT()) {
      Pair<Object> p = cast(rt.getTuple().get1());
      return p.head();
    } else {
      throw new AssertionError("ConstructedT is expected, found " + o);
    }
  }

  /**
   * Test if this type object is a function type.
   *
   * @param o The type to test.
   * @return <code>true</code> if function type, false otherwise.
   */
  public boolean isFunctionType(Object o) {
    if (null == o) throw new AssertionError("Null type");

    if (isTGType(o)) {
      TypicalTypes.raw_type<?> rt =  unwrapRawType(o);
      return (null != rt) && rt.isFunctionT();
    }
    return false;
  }

  /**
   * Test if this object is a generated type.
   *
   * @param o The type object to test.
   * @return <code> true</code> if o is a generated type, false otherwise.
   */
  private boolean isTGType(Object o){
    return (o instanceof TypicalTypes.type || 
            o instanceof TypicalTypes.raw_type);
  }

  /**
   * Get the the return type from a type.
   *
   * @param o The type object.
   * @return The Java Type of this return type. 
   */
  public String getReturnType(Object o) {
    if (null == o) throw new AssertionError("Null type");

    TypicalTypes.raw_type<?> rt = null;
    if (isTGType(o)) rt =  unwrapRawType(o);
    else throw new AssertionError("Not a type " + o);
    
    if (rt.isFunctionT()) {
      inFunctionType = true;
      Object ob = rt.getTuple().get2();

      if (null == ob) return "Void";

      TypicalTypes.raw_type<?> retType = unwrapRawType(ob);

      if (null == retType) return "Void";
      String res = getType(toTypeString(retType));
      inFunctionType = false; 
      return res;
    } else {
      return getType(toTypeString(rt)); 
    }       
  }

  /**
   * Get the the return type object from a type.
   *
   * @param o The type object.
   * @return The return type object of this type. 
   */
  public Object getReturnTypeObject(Object o) {
    if (null == o) throw new AssertionError("Null type");

    TypicalTypes.raw_type<?> rt = null;
    if (isTGType(o)) rt =  unwrapRawType(o);
    else throw new AssertionError("Not a type " + o);
    
    if (rt.isFunctionT()) {
      return rt.getTuple().get2();      
    } else {
      return o; 
    }       
  }

  /**
   * Get the the return type node from a type.
   *
   * @param o The type object.
   * @return The Java type node of this return type. 
   */
  public Node getReturnTypeNode(Object o) {
    if (null == o) throw new AssertionError("Null type");

    TypicalTypes.raw_type<?> rt = null;
    if (isTGType(o)) rt =  unwrapRawType(o);
    else throw new AssertionError("Not a type " + o);
    
    if (rt.isFunctionT()) {
      inFunctionType = true;
      Object ob = rt.getTuple().get2();
      if (null == ob) return makeTypeNode("Void", false);

      TypicalTypes.raw_type<?> retType = unwrapRawType(ob);
      if (null == retType) return makeTypeNode("Void", false);
      Node res = toTypeNode(retType, false);
      inFunctionType = false; 
      return res;
    } else {
      return toTypeNode(rt, false); 
    }       
  }
 
  /**
   * Get the list of java types from a types parameter list.
   *
   * @param o The type object
   * @return The list of java types.
   */
  public List<String> getParameterTypes(Object o) {
    if (null == o) throw new AssertionError("Null type");

    List<String> pts = new ArrayList<String>();

    if (isTGType(o)) {
      inFunctionType = true;
      TypicalTypes.raw_type<?> pm =  unwrapRawType(o);

      if (!pm.isFunctionT()) {
        throw new AssertionError("FunctionT is expected, found " + o);
      }
      
      Pair<?> params = cast(pm.getTuple().get1());
      for (Iterator<?> iter = params.iterator(); iter.hasNext();) {
        Object x = iter.next();
        pts.add(getType(toTypeString(x)));
      }
      inFunctionType = false;
      return pts;
    }    
    return null;
  }

  /**
   * Get the list of java type nodes from a types parameter list.
   *
   * @param o The type object
   * @return The list of java type nodes.
   */
  public List<Node> getParameterTypeNodes(Object o) {
    if (null == o) throw new AssertionError("Null type");

    List<Node> pts = new ArrayList<Node>();

    if (isTGType(o)) {
      inFunctionType = true;

      TypicalTypes.raw_type<?> pm =  unwrapRawType(o);

      if (!pm.isFunctionT()) {
        throw new AssertionError("FunctionT is expected, found " + o);
      }
      
      Pair<?> params = cast(pm.getTuple().get1());
      for (Iterator<?> iter = params.iterator(); iter.hasNext();) {
        pts.add(toTypeNode(iter.next(), false));
      }
      inFunctionType = false;
      return pts;
    }    
    return null;
  }

  /**
   * Get the field types from a record.
   *
   * @param o The type object.
   * @return The list of field types.
   */
  public List<String> getFieldTypes(Object o) {
    if (null == o) throw new AssertionError("Null type");

    List<String> fieldTypes = new ArrayList<String>();
    
    TypicalTypes.raw_type<?> pm = null;
    if (!isTGType(o)) throw new AssertionError("Not a type " + o);

    pm = unwrapRawType(o);
    if (!pm.isRecordT()) {
      throw new AssertionError("RecordT is expected, found " + o);
    }
   
    Pair<Object> mems = cast(pm.getTuple().get1());
    
    for (Iterator<Object> iter = mems.iterator(); iter.hasNext();) {
      // Cast to object first to avoid inconvertible types error with Java 5.
      TypicalTypes.FieldT ft =
        (TypicalTypes.FieldT)(Object)(unwrapRawType(iter.next()));
      fieldTypes.add(getType(toTypeString(ft.getTuple().get3())));
    } 
    
    return fieldTypes;
  }

  /**
   * Get the field type nodes from a record.
   *
   * @param o The type object.
   * @return The list of field type nodes.
   */
  public List<Node> getFieldTypeNodes(Object o) {
    if (null == o) throw new AssertionError("Null type");

    List<Node> fieldTypes = new ArrayList<Node>();
    
    TypicalTypes.raw_type<?> pm = null;
    if (!isTGType(o)) throw new AssertionError("Not a type " + o);

    pm = unwrapRawType(o);
    if (!pm.isRecordT()) {
      throw new AssertionError("RecordT is expected, found " + o);
    }

    Pair<Object> mems = cast(pm.getTuple().get1());
    
    for (Iterator<Object> iter = mems.iterator(); iter.hasNext();) {
      // Cast to object first to avoid inconvertible types error with Java 5.
      TypicalTypes.FieldT ft =
        (TypicalTypes.FieldT)(Object)(unwrapRawType(iter.next()));
      fieldTypes.add(toTypeNode(ft.getTuple().get3(), false));
    }     
    return fieldTypes;
  }
  
  /**
   * Get the list of field names from a record.
   *
   * @param o The type object.
   * @return The list of field names.
   */
  public List<String> getFieldNames(Object o) {
    if (null == o) throw new AssertionError("Null type");

    List<String> fieldNames = new ArrayList<String>();
    
    TypicalTypes.raw_type<?> pm = null;
    if (!isTGType(o)) throw new AssertionError("Not a type " + o);

    pm = unwrapRawType(o);
    if (!pm.isRecordT()) {
      throw new AssertionError("RecordT is expected, found " + o);
    }

    Pair<Object> mems = cast(pm.getTuple().get1());
    
    for (Iterator<Object> iter = mems.iterator(); iter.hasNext();) {
      Object j = unwrapRawType(iter.next());
      TypicalTypes.FieldT ft = (TypicalTypes.FieldT)j;
      String fn = ft.getTuple().get2();
      fieldNames.add(fn);
    } 
    return fieldNames;    
  }

   
  /**
   * Get the members from a constructor type.
   *
   * @param o The type object.
   * @return The list of members.
   */
  @SuppressWarnings("unchecked")
  public List<String> getMembers(Object o) {
    List<String> members = new ArrayList<String>();

    List<Object> memberObjects = getMemberObjects(o);

    for (Object object : memberObjects) {
      members.add(toTypeString(object));
    }

    return members;
  }  

  /**
   * Get the members from a constructor type.
   *
   * @param o The type object.
   * @return The list of members.
   */
  @SuppressWarnings("unchecked")
  public List<Object> getMemberObjects(Object o) {
    if (null == o) throw new AssertionError("Null type");
    if (!isTGType(o)) throw new AssertionError("Not a type " + o);

    List<Object> members = new ArrayList<Object>();
    
    TypicalTypes.raw_type rt = unwrapRawType(o);    
    
    if (rt.isConstructorT()) {
      TypicalTypes.ConstructorT c = (TypicalTypes.ConstructorT)rt;
      if(null == c.getTuple().get3()) {
        return members;
      }
      TypicalTypes.raw_type type = unwrapRawType(c.getTuple().get3());
      
      if (type.isTupleT()) {
        TypicalTypes.TupleT tt = (TypicalTypes.TupleT)type;
        
        @SuppressWarnings("unchecked")
        Pair<?> tl  = tt.getTuple().get1();
        for (Iterator<?> iter = tl.iterator(); iter.hasNext(); ) {
          members.add(iter.next());
        }
        return members;
      } else {
        members.add(type);
        return members;
      } 
    }
    
    if (null != rt && rt.isTupleT()){
      @SuppressWarnings("unchecked")
      Pair<Object> tl  = (Pair<Object>)rt.getTuple().get1();
      for (Iterator<Object> iter = tl.iterator(); iter.hasNext(); ) {
        members.add(toTypeString(iter.next()));
      }
      return members;
    } 
    throw new AssertionError("unknown type " + o);
  }  

  /**
   * Get the member nodes from a constructor type.
   *
   * @param o The type object.
   * @return The list of member nodes.
   */
  @SuppressWarnings("unchecked")
  public List<Node> getMemberNodes(Object o) {
    if (null == o) throw new AssertionError("Null type");
    
    List<Node> members = new ArrayList<Node>();

    if (o instanceof Node) {
      Node n = (Node)o;
      Node no = n.getGeneric(0).getGeneric(1).getGeneric(1);
      for (int i = 0; i < no.size(); i++) {
        members.add(no.getGeneric(i));
      }
      return members;
    }

    if (!isTGType(o)) throw new AssertionError("Not a type " + o);
    TypicalTypes.raw_type rt = unwrapRawType(o);    
    
    if (rt.isConstructorT()) {
      TypicalTypes.ConstructorT c = (TypicalTypes.ConstructorT)rt;
      if(null == c.getTuple().get3()) {
        return members;
      }
      TypicalTypes.raw_type type = unwrapRawType(c.getTuple().get3());
      
      if (type.isTupleT()) {
        TypicalTypes.TupleT tt = (TypicalTypes.TupleT)type;
        
        @SuppressWarnings("unchecked")
        Pair<?> tl  = tt.getTuple().get1();
        for (Iterator<?> iter = tl.iterator(); iter.hasNext(); ) {
          members.add(toTypeNode(iter.next(), false));
        }
        return members;
      } else {
        members.add(toTypeNode(type, false));
        return members;
      } 
    }
    
    if (null != rt && rt.isTupleT()){
      @SuppressWarnings("unchecked")
      Pair<Object> tl  = (Pair<Object>)rt.getTuple().get1();
      for (Iterator<Object> iter = tl.iterator(); iter.hasNext(); ) {
        members.add(toTypeNode(iter.next(), false));
      }
      return members;
    } 
    throw new AssertionError("unknown type " + o);
  }      
  
  /** MagiCast (TM).  Use at the peril of your eternal soul. */
  @SuppressWarnings("unchecked")
  private <T> Pair<T> cast(T o) {
    return (Pair<T>)o;
  }

  /**
   * Add the prefix to types that need it
   * 
   * @param t The type.
   * @return The possibly prefixed type.
   */
  private String getType(String t) {
    String t1;
    
    t1 = "raw_type".equals(t) ? "raw_type<?>" : t;
    t1 = "Pair".equals(t1) ? "Pair<?>" : t1;
   
    if (replaceType && "type".equals(t1)) {
      t1 = "raw_type<?>";
    }

    // if ("node".equals(t1)) t1 = "Node" ;
    if (nodeTypes.contains(t1)) t1 = "Node";
    else if ("name".equals(t1)) t1 = "Name";
    else if ("scope_kind".equals(t1)) t1 = "ScopeKind";
    else if ("scopeT".equals(t1)) t1 = "Scope";   
    
    if ("Named".equals(t1)) return "ScopeKind.Named";
    else if ("Anonymous".equals(t1)) return "ScopeKind.Anonymous";
    else if ("Temporary".equals(t1)) return "ScopeKind.Temporary";
    else if ("SimpleName".equals(t1)) return "Name.SimpleName";
    else if ("QualifiedName".equals(t1)) return "Name.QualifiedName";

    else if ("Object".equals(t1) || "String".equals(t1) || 
             "BigInteger".equals(t1) || "Float".equals(t1) || 
             "Double".equals(t1) || "Node".equals(t1) || 
             "GNode".equals(t1) || "Boolean".equals(t1) || 
             t1.startsWith("Pair<") || t1.startsWith("Tuple.T") || 
             t1.startsWith("Function.F") || t1.startsWith(prefix) || 
             "Name".equals(t1) || "Scope".equals(t1) || 
             "ScopeKind".equals(t1) || "Void".equals(t1)) return t1;
    else return prefix + t1 ;
  } 

  /**
   * Make a type node from a string
   *
   * @param str The name of the type
   * @param objectToWild To check if need to change Object to ?
   * @return The type node
   */
  private Node makeTypeNode(String str, boolean objectToWild) {
    String s;

    s = "raw_type".equals(str) ? "raw_type<?>" : str;
   
    if (replaceType && "type".equals(str)) {
      s = "raw_type<?>";
    }

    if (objectToWild && "Object".equals(str)) { 
      return GNode.create("Wildcard", null);
    }

    //if ("node".equals(s)) s = "Node" ;
    if (nodeTypes.contains(s)) s = "Node";
    else if ("name".equals(s)) s = "Name";
    else if ("scope_kind".equals(s)) s = "ScopeKind";
    else if ("scopeT".equals(s)) s = "Scope";

    if ("Named".equals(s)) {
      return GNode.create("Type",
               GNode.create("QualifiedIdentifier", "ScopeKind","Named"),
               null);
    }
    else if ("Anonymous".equals(s)) {
      return GNode.create("Type",
               GNode.create("QualifiedIdentifier", "ScopeKind","Anonymous"),
               null);
    }
    else if ("Temporary".equals(s)) {
      return GNode.create("Type",
               GNode.create("QualifiedIdentifier", "ScopeKind","Temporary"),
               null);
    }
    else if ("SimpleName".equals(s)) {
      return GNode.create("Type",
               GNode.create("QualifiedIdentifier", "Name","SimpleName"),
               null);
    }
    else if ("QualifiedName".equals(s)) {
      return GNode.create("Type",
               GNode.create("QualifiedIdentifier", "Name","QualifiedName"),
               null);
    }

    else if ("Object".equals(s) || "String".equals(s) || 
             "BigInteger".equals(s) || "Float".equals(s) || 
             "Double".equals(s) || "Node".equals(s) || 
             "GNode".equals(s) || "Boolean".equals(s) || 
             "Name".equals(s) || "Scope".equals(s) || 
             "ScopeKind".equals(s) || "Void".equals(s)) {
      return GNode.create("Type",
               GNode.create("QualifiedIdentifier", s),
               null);
    }
    else return GNode.create("Type",
                  GNode.create("QualifiedIdentifier", prefixNoDot, s),
                  null);    
  } 

}
