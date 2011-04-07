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
import java.util.Set;

import xtc.tree.Comment;
import xtc.tree.GNode;
import xtc.tree.Visitor;
import xtc.tree.Node;

import xtc.util.SymbolTable;
import xtc.util.Runtime;
import xtc.util.Pair;

/**  
 * Visitor to translate Typical ASTs into Java ASTs.
 * 
 * @author Laune Harris, Anh Le
 * @version $Revision: 1.377 $
 */
public class Transformer extends Visitor {

  /** A Typical attribute. */
  static class Attribute { 

    /** The name. */
    public String name;

    /** The type (as a type expression node). */
    public Node type;
    
    /**
     * Create a new attribute.
     *
     * @param name The name.
     * @param type The type.
     */

    Attribute(String name, Node type) {
      this.name = name;
      this.type = type;
    }

  } 

  // =========================================================================

  /** A Typical equality definition. */
  static class Equality {

    /** The constructor name */
    public String name;

    /** The positions of the arguments to consider for equality. */
    public List<Integer> positions;

    /**
     * Create a new equality definition.
     *
     * @param name The constructor name.
     * @param positions The relevant positions.
     */
    Equality(String name, List<Integer> positions) {
      this.name      = name;
      this.positions = positions;
    }

  } 

  // =========================================================================
  
  /** A primitive operation instance class. */
  static class PrimitiveInstance {
    
    /** The name of the operation. */
    public String name;

    /** The specific types of the instance. */
    public List<String> types;

    /** The name of the instance. */
    public String instanceName;

    /**
     * Create a new primitive instance.
     *
     * @param name The name of the primitive operation
     * @param types The types of the instance
     */
    public PrimitiveInstance(String name, List<String> types, String instanceName) {
      this.name = name;
      this.types = types;
      this.instanceName = instanceName;
    }
  
  }

  // =========================================================================

  /** Representation of a matching clause. */
  static class Match {
    
    /** The argument type. */
    private Object type;
    
    /** The condition on the argument. */
    private Node condition;

    /** 
     * Create a new match. 
     *
     * @param type The argument type. 
     * @param condition The condition.
     */
    public Match(Object type, Node condition) {
      this.type = type;
      this.condition = condition;
    }
    
    public int hashCode() {
      return 0;
      
    }
    
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Match)) return false;
      
      Match other = (Match)o;
      return (type.equals(other.type) && condition.equals(other.condition));    
    }
    
  }
  
  // =========================================================================
  
  /** Representation of a variable binding in a let expression. */
  static class LetBinding {
  
    /** The name of the variable. */
    public String name;

    public Object typeObject;
  
    /** The type node of the variable. */  
    public Node type;

    /** The value node of the variable. */
    public Node value;

    /**
     * Create a new let binding.
     *
     * @param name The name of the variable.
     * @param type The type of the variable.
     * @param value The value of the variable.
     */  
    public LetBinding(String name, Object typeObject, Node type, Node value){
      this.name = name;
      this.typeObject = typeObject;
      this.type = type;
      this.value = value;
    }  

  }

  // =========================================================================

  /** The type annotation name. */
  protected static final String TYPE = "__type";
  
  /** The property indicating the top level of a pattern. */
  protected static final String TOP = "top";

  /** The property indicating the right hand side of a binding. */
  protected static final String RHS = "rhs";

  /** The property representing the match argument. */
  protected static final String MATCHARG = "match_arg";
  
  /** The property representing the block contains the bindings argument. */
  protected static final String BINDINGS = "bindings";

  /** The annotation name to remember it is currently in analyze function. */
  protected static final String INANALYZE = "inAnalyze";

  /** The annotation name to indicate it is needed to process scope. */
  protected static final String SCOPE = "process_scope";

  /** The annotation name to indicate it is needed to annotate the node. */
  protected static final String ANNOTATE = "annotate_node";

  /** The property indicating the variables from upper levels of binding. */
  protected static final String UPVARS = "up_variables";

  /** The property indicating the variable bindings from an expression. */
  protected static final String LETBINDINGS = "let_bindings";

  /** The property indicating the body of a function definition. */
  protected static final String BODY = "body";

  /** The property indicating a new instance of Let must be created. */
  protected static final String NEWLET = "new_let";

  /** The tree factory. */
  protected final TreeFactory factory;
   
  /** The symbol table for this typical file. */
  protected final SymbolTable table;

  /** The root of the incoming typical abstract syntax tree. */
  protected final Node typical;

  /** The name of the node type. */
  protected final String nodeTypeName;
  
  /** The name of the checker to be output. */
  protected final String output;

  /** The root of the generated java ast for the type checker. */
  protected GNode transformed; 

  /** The root of the types file. */
  protected GNode typesAST;

  /** The root of the support file. */
  protected GNode supportAST; 

  /** The class body of the generated typechecker. */
  protected Node cbody; 

  /** The class body of the types file. */
  protected Node tbody;

  /** The class body of the support file. */
  protected Node sbody;
  
  /** The type enumerations. */
  protected Node tags;

  /** This Transformers runtime. */ 
  protected final Runtime runtime; 

  /** The cached variables. */
  protected final 
    HashMap<Integer, String> typicalVars = new HashMap<Integer,String>();

  /** The Type mapper. */
  protected TypeMapper mapper;

  /** Flag indicating that a scope definition has been seen. */  
  protected boolean seenScope = false;

  /** The set of seen match conditions */
  protected HashMap<Match, String> matches = 
    new HashMap<Match, String>();

  /** The set of seen match conditions */
  protected HashMap<Node, String> nodeMatches = 
    new HashMap<Node, String>();

  /** Empty modifier node. */
  final protected Node  mod = GNode.create("Modifiers");

  /** Final modifier node. */
  final protected Node fmod = toModifiers("final");

  /** Public modifier node. */
  final protected Node pmod = toModifiers("public");

  /** Nullliteral node. */
  final protected Node nullNode = GNode.create("NullLiteral");
 
  /** Node type. */
  final protected Node nodeType = GNode.create("Type",
                            GNode.create("QualifiedIdentifier", "Node"),
                            null);
  /** GNode type. */
  final protected Node gnodeType = GNode.create("Type",
                             GNode.create("QualifiedIdentifier", "GNode"),
                             null);  
  
  /** The list of field that go into the. */
  final protected List<Node> staticFields = new ArrayList<Node>();

  /** The list of function definitions. */
  final protected List<Node> functionDefinitions = new ArrayList<Node>();

  /** A spare variable used in binding to a wildcard. */
  final protected String spareVar;

  /** Flag indicating that a namespace declaration has been seen. */
  protected boolean seenNameSpace = false;  
  
  /** A list to store all node names in scope definition. */
  protected ArrayList<String> processScopeNodes = new ArrayList<String>();
  
  /** List of equal structure. */
  protected ArrayList<Equality> equalities = new ArrayList<Equality>();

  /** The package name. */
  protected String packageName;

  /** The package node. */
  protected Node packageNode;

  /** A list to store all defined attributes. */
  protected List<Attribute> attributeList = new ArrayList<Attribute>();

  /** A list to store all defined equal attributes. */
  protected List<Attribute> eqAttributeList = new ArrayList<Attribute>();

  /** A variable to remember if type is optimized. */
  protected boolean replaceType;

  /** A variable to remember if let is optimized. */
  protected boolean optimizeLet;

  /** A variable to check if List is used. */
  private boolean isListUsed;

  /** A variable to check if ArrayList is used. */
  private boolean isArrayListUsed;  

  /** A variable to check if BigInteger is used. */
  private boolean isBigIntegerUsed;

  /** A variable to check if Pair is used. */
  private boolean isPairUsed;

  /** A variable to check if Node is used. */
  private boolean isNodeUsed;

  /** A variable to check if GNode is used. */
  private boolean isGNodeUsed;

  /** A variable to check if Primitives is used. */
  private boolean isPrimitivesUsed;

  /** A variable to check if Record is used. */
  private boolean isRecordUsed;

  /** A variable to check if Variant is used. */
  private boolean isVariantUsed;

  /** A variable to check if Tuple is used. */
  private boolean isTupleUsed;

  /** A variable to check if Reduction is used. */
  private boolean isReductionUsed;

  /** A variable to check if Name is used. */
  private boolean isNameUsed;

  /** A variable to check if Scope is used. */
  private boolean isScopeUsed;

  /** A variable to check if ScopeKind is used. */
  private boolean isScopeKindUsed;

  /** A variable to check if Analyzer is used. */
  private boolean isAnalyzerUsed;
  
  /** 
   * A list to store all declarations of 
   *   instances of primitive list operations. 
   */
  protected List<Node> primitiveDeclList = new ArrayList<Node>();

  /** A list to store all primitive instances. */
  protected List<PrimitiveInstance> primitiveInsList = 
    new ArrayList<PrimitiveInstance>(); 

  /** A list of types that are nodes. */
  protected Pair<String> nodeTypes = null;

  /**
   * Initialise this transformer.
   *
   * @param ast the root of the typical ast
   * @param s the name of the checker to be generated
   * @param runt the runtime
   */
  public Transformer(GNode ast, SymbolTable st, String s, Runtime runt) {
    factory = new TreeFactory();
    typical = ast; 
    output = s;
    table = st;
    runtime = runt;

    // Set replaceType
    if (runtime.test("optimizeType")) replaceType = true;

    // Set optimizeLet
    if (runtime.test("optimizeLet")) optimizeLet = true;
    else optimizeLet = false;

    spareVar = table.freshJavaId("spareVar");

    // Get the name of the node type
    String tempName = (String)runtime.getValue("optionNodeType"); 
    if (null == tempName) nodeTypeName = "node";
    else nodeTypeName = tempName;

    /* Process the module declaration. */
    dispatch(typical.getGeneric(0));
    cbody = makeClassBody();
    cbody = GNode.ensureVariable((GNode)cbody);
    // Add spare variable declaration
    if (optimizeLet) cbody.add(factory.fieldDecl3(spareVar));

    tbody = GNode.create("ClassBody");
    tbody = GNode.ensureVariable((GNode)tbody);

    sbody = GNode.create("ClassBody");
    sbody = GNode.ensureVariable((GNode)sbody);
  }
  
  /**
   * Process the module.
   *
   * @param n The module node.
   */
  public void visitModule(GNode n) {
    boolean hasAttributes = false;
    
    final int size = n.size();
    for (int i = 0; i < size; i++) {
      Node node = n.getGeneric(i);
      if (node.hasName("AttributeDefinition") ||
          node.hasName("EqualAttributeDefinition")) {
        hasAttributes = true;
        new TypeCollector().dispatch(node);
      } else if (node.hasName("NameSpaceDefinition") ||
                 (node.hasName("TypeDefinition") &&
                    "raw_type".equals(node.getString(1)))) {
        new TypeCollector().dispatch(node);
      }                          
    }

    if (hasAttributes && replaceType) {
      replaceType = false;
    }

    // Get the list of types that are nodes
    @SuppressWarnings("unchecked")
    Object ob = n.getProperty("__node_types");
    Pair<String> nodeTypes = TypeMapper.getAnnotatedStringList(ob);
    
    mapper = new TypeMapper(runtime, output + "Types", replaceType, nodeTypes);
    
    /* Process attribute, equality, and namespaced  definitions first. */
    for (int j = 0; j < size; j++) {
      Node node = n.getGeneric(j);    
      if(node.hasName("AttributeDefinition") ||
         node.hasName("EqualAttributeDefinition") ||
         node.hasName("EqualityDefinition") ||
         node.hasName("NameSpaceDefinition")) {
        dispatch(node);        
      } 
    }

    for (int i = 0; i < size; i++) {
      Node node = n.getGeneric(i);
      
      /* Skip the node, attribute, equality and namespace declarations. */
      if ((node.hasName("TypeDefinition") && 
           nodeTypes.contains(node.getString(1))) ||
          node.hasName("AttributeDefinition") || 
          node.hasName("EqualAttributeDefinition") ||
          node.hasName("EqualityDefinition") ||
          node.hasName("NameSpaceDefinition")) {
        continue;        
      }       
      
      dispatch(node); 
      
      if (node.hasName("TypeDefinition") &&
          "raw_type".equals(node.getString(1))) {
        dispatch(processRawTypeDefinition());           
      } 
    }
    
    /* Add code to process scopes. */
    processScopeSpace();
    cbody.addAll(functionDefinitions);

    sbody.addAll(primitiveDeclList);
    sbody.addAll(staticFields);
    tbody.add(factory.typesConstr(output + "Types"));
    sbody.add(factory.typesConstr(output + "Support"));
    
    // Make skeletons of generated files
    transformed = GNode.cast(makeSkeleton());   
    typesAST = GNode.cast(makeTypesSkeleton());
    supportAST = GNode.cast(makeSupportSkeleton());

  }
  
  /**
   * Process a module declaration.
   *
   * @param n The ModuleDeclaration node.
   */
  public void visitModuleDeclaration(GNode n) {
    Node qid = GNode.create("QualifiedIdentifier");

    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < n.size() - 1; i++) {
      qid.add(n.getString(i));
      buf.append(n.getString(i));
      if (i < n.size() - 2) buf.append('.');
    }
    
    packageName = buf.toString();
    packageNode = GNode.create("PackageDeclaration", null, qid);
  }
  
  /**
   * Process a fun expression.
   * 
   * @param n The fun expression node.
   * @return The java code for the fun expression.
   */
  public Node visitFunExpression(GNode n) { 
    Object t = n.getProperty(TYPE);
    if (null == t) throw new AssertionError("Null type");
    
    if (mapper.isFunctionType(t)) {
      Node params = n.getGeneric(0);
      Node value = n.getGeneric(1);

      if (n.hasProperty(INANALYZE)) {
        params.setProperty(INANALYZE, Boolean.TRUE);
        value.setProperty(INANALYZE, Boolean.TRUE);
      }  

      final String scopename = (String)n.getProperty("enterScope");
      boolean didEnter = false;
      if (n.hasProperty("enterScope")) {     
        if (!table.current().getName().equals(scopename)) {
          enterScope(scopename);
          didEnter = true;
        }
      }
    
      // Get the number of type variables from the function type
      final int typeVarNumber = mapper.processTypeVariables(t, 0); 
      // Check if this function is generic
      final boolean isGeneric = typeVarNumber > 0;
      
      Node returnTypeNode = mapper.getReturnTypeNode(t);      
      List<Node> paramTypeNodes = mapper.getParameterTypeNodes(t);      
      Node functionTypeNode = mapper.toTypeNode(t,false);
      
      value.setProperty(TYPE, returnTypeNode);
      
      //populate the formal parameters for the "apply" method
      Node formalParameters = GNode.create("FormalParameters", params.size());      
      for (int i = 0; i < params.size(); i++) {
        formalParameters.add(GNode.create("FormalParameter", fmod, 
          paramTypeNodes.get(i), null,
          params.getGeneric(i).getString(0), null));
      }
      
      // Type parameters node
      Node typeParas = null;
      if (isGeneric) {
        typeParas = GNode.create("TypeParameters");
        for (int i = 0; i < typeVarNumber; i ++) {
          typeParas.add(GNode.create("TypeParameter", "T" + i, null));
        }
      }

      // Make a new let for the body if necessary
      Object returnType = mapper.getReturnTypeObject(t);
      Node valueExpr = (Node)dispatch(value);      
      valueExpr = checkToLet(valueExpr, returnType);
      

     
      Node classBody = GNode.create("ClassBody", 
        GNode.create("MethodDeclaration", pmod, typeParas,
        returnTypeNode, "apply", formalParameters, null, null,
         GNode.create("Block", factory.ret(valueExpr))));
      
      if (didEnter) exitScope(scopename);
      
      return toNewExpression2(functionTypeNode,null,classBody);
        
    } else throw new AssertionError("Function type is required");
  }

  /**
   * Process a value definition.
   * 
   * @param n The value binding node.
   * @return The java code for the funciton
   */
  public Node visitValueDefinition(GNode n) {

    String name = n.getString(0);
    Node params = n.getGeneric(1);
    Node value = n.getGeneric(2);

    String nsname = SymbolTable.toNameSpace(name, "value");
    Object t = n.getProperty(TYPE);
    
    if (null == t) t = table.current().lookup(nsname);

    if (mapper.isFunctionType(t)) {
      // Get the number of type variables from the function type
      final int typeVarNumber = mapper.processTypeVariables(t, 0); 
      // Check if this function is generic
      final boolean isGeneric = typeVarNumber > 0;

      Node returnTypeNode = mapper.getReturnTypeNode(t);
            
      List<Node> paramTypeNodes = mapper.getParameterTypeNodes(t);

      Node functionTypeNode = mapper.toTypeNode(t, false);
      //desugar function expressions
      if (value.hasName("FunctionExpression")) {
        String arg = table.freshJavaId("arg");        
        value = GNode.create("MatchExpression", 
          GNode.create("LowerID", arg), value.get(0));
        value.setProperty("__arg_type",paramTypeNodes.get(0));
       

        n.set(1, GNode.create("Parameters",
          GNode.create("Parameter", arg , null)));
        params = n.getGeneric(1);        
      } else if (value.hasName("ReduceExpression")) {
        String arg = "lst";
        n.set(1, GNode.create("Parameters",
          GNode.create("Parameter", arg , null)));
        params = n.getGeneric(1);        
      }
      

      // Set property to remember value is in analyze function
      if ("analyze".equals(name)) value.setProperty(INANALYZE, Boolean.TRUE); 
      
      value.setProperty(TYPE, returnTypeNode);
      
      //populate the formal parameters for the "apply" method
      Node formalParameters = GNode.create("FormalParameters", params.size());
      
      for (int i = 0; i < params.size(); i++) {
        formalParameters.add(GNode.create("FormalParameter", fmod, 
          paramTypeNodes.get(i), null,
          params.getGeneric(i).getString(0), null));
      }
       
      //enter the scope and process this bindings value     
      if (!"getNameSpace".equals(name)) enterScope(name);

      // Type parameters node
      Node typeParas = null;
      if (isGeneric) {
        typeParas = GNode.create("TypeParameters");
        for (int i = 0; i < typeVarNumber; i ++) {
          typeParas.add(GNode.create("TypeParameter", "T" + i, null));
        }
      }

      // The body of the function
      Node block;
      if (!optimizeLet) {
        block = GNode.create("Block", factory.ret((Node)dispatch(value))); 
      } else {
        block = GNode.create("Block");
        block = GNode.ensureVariable(GNode.cast(block));
        Node valueExpr = (Node)dispatch(value);

        List<LetBinding> bindList = getBindings(valueExpr);
        if (null != bindList) {
          for (LetBinding bind : bindList) {
            if (!bind.name.equals(spareVar)) {
              if (mapper.hasTypeVariables(bind.typeObject)) {
                block.add(factory.fieldDecl2(bind.type, bind.name, bind.value));
              } else {
                block.add(factory.fieldDecl2(bind.type, bind.name, 
                                             factory.cast(bind.value)));
              }
            } else {
              if (mapper.hasTypeVariables(bind.typeObject)) {
                block.add(factory.assign(toIdentifier(bind.name), bind.value));
              } else {
                block.add(factory.assign(toIdentifier(bind.name), 
                                         factory.cast(bind.value)));
              }
            }
          }
        }
        block.add(factory.castReturn(valueExpr));
      } 
     
      Node classBody = GNode.create("ClassBody", 
        GNode.create("MethodDeclaration", pmod, typeParas,
        returnTypeNode, "apply", formalParameters, null, null, block)); 
      
      if (!"getNameSpace".equals(name)) exitScope(name);
          
      //create and add the fielddeclaration to the typechecker
      if ("getNameSpace".equals(name) || "getScope".equals(name)) {
        return makeVarDec2(name,functionTypeNode,
                          toNewExpression2(functionTypeNode,null,classBody));
      } else {
        // If not a generic function, create a field declaration
        if (!isGeneric){
          functionDefinitions.add(makeVarDec2(name,functionTypeNode,
            toNewExpression2(functionTypeNode,null,classBody)));
        } else { // create a class
          Node classNode = factory.classDecl3(name);
          classNode.set(5, classBody);
          functionDefinitions.add(classNode);
          functionDefinitions.add(
            factory.instanceDecl(GNode.create("Type",
                                   GNode.create("QualifiedIdentifier",name),null),
                                 name,
                                 GNode.create("QualifiedIdentifier",name)));         
        }
      }
    } else {
      //no function needed, just field declaration
      //value.setProperty(NEWLET, Boolean.TRUE);
      Node valueExpr = (Node)dispatch(value);
      valueExpr = checkToLet(valueExpr, t);
      // fix cast here
      if (mapper.hasTypeVariables(t)) {
        functionDefinitions.add(makeVarDec2(name, mapper.toTypeNode(t, false),
                                            valueExpr));
      } else {      
        functionDefinitions.add(makeVarDec2(name, mapper.toTypeNode(t, false),
          factory.cast(valueExpr)));
      }     
    }

    // Note: in general, we modify the AST in place and are done.  For
    // getNameSpace() and getScope(), however, we need to do further
    // processing; so they are returned above.
    
    return null;
  }

  /**
   * Process a function expression
   *
   * @param n The function expression node
   * @return The translated node
   */
  public Node visitFunctionExpression(GNode n) {
    // Get the function type
    final Object t = n.getProperty(TYPE);
    final Node returnTypeNode = mapper.getReturnTypeNode(t);
         
    final List<Node> paramTypeNodes = mapper.getParameterTypeNodes(t);

    // Get the return type from the pattern match type
    final Object retType =
      mapper.getPatternMatchRightType(n.getGeneric(0).getProperty(TYPE));

    // Create a Typical match expression node
    Node value = GNode.create("MatchExpression", 
                   GNode.create("LowerID", "var"), n.getGeneric(0));
    value.setProperty("__arg_type", paramTypeNodes.get(0));
    value.setProperty(TYPE, retType);
    return factory.functionExpression(
      returnTypeNode, paramTypeNodes.get(0), (Node)dispatch(value));      
  }
  
  /**  
   * Process a type definition.
   *
   * @param n The type definition node.
   */
  public void visitTypeDefinition(GNode n) {
    n.getGeneric(2).setProperty("name", n.getString(1));
    dispatch(n.getGeneric(2));
  }
  
  /**
   * Process a string literal.
   *
   * @param n The string literal node.
   * @return n.
   */
  public GNode visitStringLiteral(GNode n) {
    return n;
  }

  /**
   * Process an integer literal.
   * 
   * @param n the integer literal node.
   * @return The BigInteger node.
   */
  public Node visitIntegerLiteral(GNode n) {
    return factory.createInteger(toLiteral("IntegerLiteral", n.getString(0)));
  }

  /**
   * Process a float literal.
   * 
   * @param n the float literal node.
   * @return The 'new Double' node.
   */
  public Node visitFloatingLiteral(GNode n) {
    return
      factory.createFloat(toLiteral("FloatingPointLiteral", n.getString(0)));
  }

  /**
   * Process a bottom node.
   *
   * @param n The bottom node.
   * @return A null literal.
   */
  public Node visitBottom(GNode n) {
    return GNode.create("NullLiteral");
  }

  /**
   * Process a bottom pattern.
   *
   * @param n The bottom pattern node.
   * @return A null literal.
   */
  public Node visitBottomPattern(GNode n) {
    return 
      factory.equalsBottom(toIdentifier((String)n.getProperty(MATCHARG)));
  }
  
  /**
   * Process a boolean literal.
   * 
   * @param n the boolean literal node.
   */
  public Node visitBooleanLiteral(GNode n) {
   return ("true".equals(n.getString(0))) ? toIdentifier("Boolean.TRUE") 
                                          : toIdentifier("Boolean.FALSE");
  }

  /**
   * Process a cons expression.
   *
   * @param n The cons expression node.
   * @return The java equivalent.
   */
  public Node visitConsExpression(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(1).setProperty(INANALYZE, Boolean.TRUE);
    }
    if (!optimizeLet) { 
      return factory.consWrapper((Node)dispatch(n.getGeneric(0)),
                                 (Node)dispatch(n.getGeneric(1)));
    } else {
      List<String> upVars = getUpVariables(n);
      Node left = n.getGeneric(0);
      Node right = n.getGeneric(1);

      if (null != upVars) left.setProperty(UPVARS, upVars);
      Node leftExpr = (Node)dispatch(left);
      List<LetBinding> bl1 = getBindings(leftExpr);      
      List<String> leftVars = extractVariables(bl1);
      List<String> upVars2 = groupList(upVars,leftVars);

      if (null != upVars2) right.setProperty(UPVARS, upVars2);
      Node rightExpr = (Node)dispatch(right);

      List<LetBinding> bl2 = getBindings(rightExpr);
      List<LetBinding> bl = groupList(bl1,bl2);

      Node ret = factory.consWrapper(leftExpr, rightExpr);
      if (null != bl) ret.setProperty(LETBINDINGS, bl);
      return ret;      
    }      
  }
  
  /**
   * Transform a wildcard.
   *
   * @param n The wildcard node.
   * @return A wildcard primary identifier.
   */
  public Node visitWildCard(GNode n) {
    return toLiteral("BooleanLiteral", "true");
  }
  
  /**
   * Process a field expression.
   *
   * @param n The field expression node.
   * @return A java field access node (with null pointer checks inserted).
   */
  public Node visitFieldExpression(GNode n) {
    Node expr = n.getGeneric(0);

    if (n.hasProperty(INANALYZE)) {
      expr.setProperty(INANALYZE, Boolean.TRUE);
    }   
    if ("TupleConstructor".equals(expr.getName())) {
      String convertedName = Primitives.convertName(n.getString(1)); 	
      if ("Limits".equals(expr.getString(0))) {
    	if (Primitives.hasIntegerType(convertedName)) {
    	  return factory.createInteger(toIdentifier(
    			                         "xtc.Limits." + convertedName));
    	}   
    	return toIdentifier("xtc.Limits." + convertedName);
      }	
      return toIdentifier("Primitives." + convertedName);
    } 

    passVariables(n, expr);
    
    // Check replaceType and the field name
    if (replaceType && "type".equals(n.getString(1))) {
      return (Node)dispatch(expr);
    }
 
    Node exprNode = (Node)dispatch(expr);
    Node ret = factory.fieldExpression(exprNode, n.getString(1));
    passBindings(exprNode, ret);
    return ret;
  }
  
  /**
   * Process a tuple literal.
   *
   * @param n The tuple literal node.
   * @return The java equivalent.
   */
  public Node visitTupleLiteral(GNode n) {
    return makeTuple(n);
  }  

  /**
   * Process a tuple pattern.
   *
   * @param n The tuple pattern node.
   * @return The java equivalent.
   */ 
  public Node visitTuplePattern(GNode n) {
    String matchArg = (String)n.getProperty(MATCHARG);
    
    Node condition = 
      factory.jand(factory.notEqualsBottom(toIdentifier(matchArg)),
        factory.sizeEqual(toIdentifier(matchArg), toLiteral("IntegerLiteral",
          Integer.toString(n.size()))));
   
    for (int i = 0; i < n.size(); i++) {
      Node node = n.getGeneric(i);
      
      if (node.hasName("WildCard")) continue;
      
      if (node.hasName("Variable")) {
        //make the binding
        node.setProperty(RHS, matchArg +  ".get" + (i + 1) + "()");
        node.setProperty(BINDINGS, n.getProperty(BINDINGS));
        dispatch(node);
        continue;
      }
      
      if (isLiteral(node)) {
        condition = factory.jand(condition, 
         factory.jequals2((Node)dispatch(node), 
         toIdentifier(matchArg + ".get" + (i + 1) + "()")));
        continue;
      }
        
      node.setProperty(RHS, matchArg +  ".get" + (i + 1) + "()");  
      node.setProperty(BINDINGS, (n.getProperty(BINDINGS)));
      node.setProperty(MATCHARG, (String)n.getProperty(MATCHARG) + 
                       ".get" + (i + 1) + "()");
      condition = factory.jand(condition, (Node)dispatch(node));      
    } 
    
    if (n.hasProperty(TOP)) {
      String matchName = table.freshJavaId("match");
      condition = replaceMatchArg(condition, matchArg);

      Match ms = new Match(mapper.toTypeNode(n.getProperty(TYPE), false),
                           condition);
      
      if (matches.containsKey(ms)) {
        matchName = matches.get(ms);
      } else {
        matches.put(ms, matchName);
        Object t = n.getProperty(TYPE);
        Node tNode = null;
        if (mapper.hasTypeVariables(t)) {
          tNode = mapper.toTypeNode(t, true); 
        } else {
          tNode = mapper.toTypeNode(t, false);
        }
        Node matchFunction = 
          factory.matchFunction(matchName, tNode, condition);
        
        matchFunction.getGeneric(4).getGeneric(0).set(3, "m");
        staticFields.add(matchFunction);
      }    
      
      return factory.matchCall(toIdentifier(output + "Support"), matchName, 
        toIdentifier((String)n.getProperty(MATCHARG)));
    } else {
      return condition;
    }
  }
  
  /**
   * Process a List literal.
   *
   * @param n The list literal node.
   * @return The java equivalent.
   */
  public Node visitListLiteral(GNode n) {
    return makeList(n);
  }

  /**
   * Make conditions identical with respect to argument name so we can generate
   * unique match functions;
   *
   * @param n The node to process.
   * @param id The name of the id to replace.
   * @return The processed node.
   */
  private Node replaceMatchArg(Node n, String id) {

    for (int i = 0; i < n.size(); i++) {
      Object o = n.get(i);
      if (o instanceof Node) {
        Node node = (Node)o;

        if (node.hasName("PrimaryIdentifier") &&
            node.getString(0).equals(id)) {
          node.set(0, node.getString(0).replace(id,"m"));
        } else if (node.hasName("PrimaryIdentifier") &&
                   node.getString(0).startsWith(id + ".")) {             
          node.set(0, node.getString(0).replace(id + ".", "m."));
        } else {
          replaceMatchArg(node, id);
        }       
      } else if ((o instanceof String) && id.equals(o)) {
        n.set(i, id);
      }      
    }

    return n;
  }

  /**
   * Process a List pattern.
   *
   * @param n The list pattern node.
   * @return The java equivalent.
   */
  public Node visitListPattern(GNode n) {
    checkTypeAnnotation(n);
   
    String matchArg = (String)n.getProperty(MATCHARG);
    
    Node condition = (0 == n.size()) 
      ? factory.isEmptyCall(toIdentifier(matchArg)) 
      : factory.sizeEqual(toIdentifier(matchArg), 
          toLiteral("IntegerLiteral", Integer.toString(n.size())));
    
    for (int i = 0; i < n.size(); i++) {
      Node node = n.getGeneric(i);
      
      if (node.hasName("WildCard")) continue;
      
      if (node.hasName("Variable")) {
        // fix cast here
        Object t = n.getProperty(TYPE);
        if (mapper.hasTypeVariables(t)) {
          ((Node)n.getProperty(BINDINGS)).add(makeVarDec2(node.getString(0), 
          mapper.toTypeNode(mapper.getBase(t), false), 
          toIdentifier(matchArg + ".get(" + i + ")")));
        } else {
          ((Node)n.getProperty(BINDINGS)).add(makeVarDec2(node.getString(0), 
          mapper.toTypeNode(mapper.getBase(t), false), 
          factory.cast(toIdentifier(matchArg + ".get(" + i + ")"))));
        }
        continue;
      }
      
      if (isLiteral(node)) {
        condition = factory.jand(condition, 
         factory.jequals2((Node)dispatch(node), 
                          toIdentifier(matchArg + ".get(" + i + ")")));
        continue;
      }

      node.setProperty(BINDINGS, n.getProperty(BINDINGS));
      node.setProperty(MATCHARG, matchArg + ".get(" + i + ")");
      condition = factory.jand(condition, (Node)dispatch(node));
    } 

    if (n.hasProperty(TOP)) {
      Object t = n.getProperty(TYPE);
      Node listTypeNode = null;
      if (mapper.hasTypeVariables(t)) {
        listTypeNode = mapper.toTypeNode(t, true);
      } else {
        listTypeNode = mapper.toTypeNode(t, false);
      }  

      condition = replaceMatchArg(condition, matchArg);
      
      String matchName = table.freshJavaId("match");
      Match ms = null;
      
      ms = new Match(listTypeNode, condition);
            
      if (matches.containsKey(ms)) {
        matchName = matches.get(ms);        
      } else {
        matches.put(ms, matchName);
        Node matchFunction = 
          factory.matchFunction(matchName, listTypeNode, condition);
     
        matchFunction.getGeneric(4).getGeneric(0).set(3, "m");
        staticFields.add(matchFunction);
      } 
      
      return factory.matchCall(toIdentifier(output + "Support"), matchName, 
        toIdentifier((String)n.getProperty(MATCHARG)));
    } else {
      return condition;
    }
  }

  /**
   * Transform a predicate expression.
   *
   * @param n The predicate expression.
   * @return The java equivalent.
   */
  public Node visitPredicateExpression(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(1).setProperty(INANALYZE, Boolean.TRUE);
    }
 
    Node arg = n.getGeneric(0).getGeneric(0);
    
    for (int i = 0; i < arg.size(); i++) {
      Node node = arg.getGeneric(i);
      node.setProperty(RHS, RHS);
      node.setProperty(BINDINGS, GNode.create("Block"));
      node.setProperty("enterScope", "scope" + i);
    }

    Node wild = GNode.create("WildCard");
    wild.setProperty("enterScope", "ihavenoscope");
   
    Node last = GNode.create("PatternMatch", 
      GNode.create("Patterns",wild),GNode.create("BooleanLiteral", "false"));
    Node first = GNode.create("PatternMatch", n.getGeneric(0).getGeneric(0), 
                              GNode.create("BooleanLiteral", "true"));
    
    Node pmatching = GNode.create("PatternMatching", first, last);
    
    Object pt = mapper.makePatternMatchType(arg.getProperty(TYPE),
                                            n.getProperty(TYPE));

    first.setProperty(TYPE, pt);
    last.setProperty(TYPE, pt);
    pmatching.setProperty(TYPE, pt);
    first.setProperty("enterScope", "predicatescope");
    last.setProperty("enterScope", "predicatescope");
    

    Node match = GNode.create("MatchExpression", n.getGeneric(1), pmatching);
    match.setProperty("__arg_type", n.getProperty("__arg_type"));
    
    match.setProperty(TYPE, n.getProperty(TYPE));
       
    return 
    factory.jequals2((Node)dispatch(match),toLiteral("BooleanLiteral","true"));
  }
  
  /**
   * Transform a guard expression.
   * 
   * @param n
   */
  public Node visitGuardExpression(GNode n) {
    //we need to collect all the variables and test for nullity
    Set<String> variables = new FreeVariableCollector(n).getIdentifiers();
    
    List<Node> statements = new ArrayList<Node>();
    
    Object t = n.getGeneric(0).getProperty(TYPE);
    
    Node typeNode = mapper.toTypeNode(t, false);

    String name = table.freshJavaId("result");
    
    for (String variable : variables) {
      statements.add(factory.
        ifStatement(factory.isNull(toIdentifier(variable)),
                    factory.ret(toIdentifier("null"))));
      
    }

    Node no = n.getGeneric(0);
    if (n.hasProperty(INANALYZE)) {
      no.setProperty(INANALYZE, Boolean.TRUE);      
    }    
    passVariables(n, no);
    Node expr = (Node)dispatch(no);
    statements.add(factory.fieldDecl2(typeNode, name,expr));
    // fix cast here
    if (mapper.hasTypeVariables(t)) {                                      
      statements.add(factory.ifStatement(factory.isNull(toIdentifier(name)),
              factory.ret((Node)dispatch(n.getGeneric(1)))));
    } else {
      statements.add(factory.ifStatement(factory.isNull(toIdentifier(name)),
              factory.ret(factory.cast((Node)dispatch(n.getGeneric(1))))));
    }        
    statements.add(factory.ret(toIdentifier(name)));

    Node ret = factory.guardExpression(typeNode, statements);
    passBindings(expr, ret); 
    return ret;
  }
  
  /**
   * Transform a reduce expression.
   *
   * @param n The predicate expression.
   * @return The java equivalent.
   */
  public Node visitReduceExpression(GNode n) {

    Node arg = toIdentifier("lst");
    Node runtimeNode = toIdentifier("runtime");

    List<Node> initList = new ArrayList<Node>();
       
    Node options = n.getGeneric(0);
    
    for (int i = 0; i < options.size(); i++) {
      String opt = options.getString(i);

      if ("required".equals(opt)) {
        initList.add(factory.reduceReq());
      } else if ("optional".equals(opt)) {
        initList.add(factory.reduceOpt());
      } else if ("list".equals(opt)) {
        initList.add(factory.reduceList());
      } else if ("set".equals(opt)) {
        initList.add(factory.reduceSet());
      } else if ("dup".equals(opt)) {
        initList.add(factory.reduceDup());
      } else if ("nodup".equals(opt)) {
        initList.add(factory.reduceNodup());
      } else if ("singleton".equals(opt)) {
        initList.add(factory.reduceSing());
      }
    }
    
    //set the tag
    initList.add(factory.reduceTag(n.getGeneric(1)));
                 
    Node patternMatching = n.getGeneric(2);
                 
    for (int i = 0; i < patternMatching.size(); i++) {
      Node patternMatch = patternMatching.getGeneric(i);
      
      ArrayList<Node> addArgs = new ArrayList<Node>();

      ArrayList<Node> block = new ArrayList<Node>();

      addArgs.add((Node)dispatch(patternMatch.getGeneric(1)));
      Node lpatterns = patternMatch.getGeneric(0).getGeneric(0);
      for (int j = 0; j < lpatterns.size(); j++) {
        Node node = lpatterns.getGeneric(j);
        
        if (node.hasName("AsPattern")) {
          String binding = node.getString(1);
          
          node = node.getGeneric(0);
          node.setProperty("ancestor", true);
          node.setProperty("top", true);
          node.setProperty(MATCHARG, table.freshJavaId("arg"));
          
          block.add(factory.reduceGetMatch(binding, (Node)dispatch(node)));
        }

        node.setProperty("ancestor", true);
        node.setProperty("top", true);
        node.setProperty(MATCHARG, table.freshJavaId("arg"));
        addArgs.add((Node)dispatch(node));
      }     
      
      block.add(factory.reduceAddPatterns(addArgs));
      initList.add(factory.block(block));
   } 
    
    return factory.cast(factory.reduceExpression(arg, runtimeNode, initList));
  }

  /**
   * Process a function application. 
   *
   * @param n The function application node.
   * @return The java equivalent of the function application node.
   */
  public Node visitFunctionApplication(GNode n) {
    // Get function arguments
    Node funcArgs = n.getGeneric(n.size() - 1);
    
    Node id = n.getGeneric(0);

    if ("ancestor".equals(id.getString(0)) || 
        "parent".equals(id.getString(0))) {
      
      Node arg = funcArgs.getGeneric(0);
      arg.setProperty("ancestor", true);
      arg.setProperty(MATCHARG, table.freshJavaId("arg"));
      arg = (Node)dispatch(arg);
      
      List<Node> nlist = new ArrayList<Node>(1);
      nlist.add(arg);

      return factory.apply(toIdentifier(id.getString(0)),nlist);
    }
    //special case

    // Annotate if this function application is in analyze function
    if (n.hasProperty(INANALYZE)) {
      for (int i = 0; i < funcArgs.size(); i++) {
        funcArgs.getGeneric(i).setProperty(INANALYZE, Boolean.TRUE);
      }      
    }

    //process arguments
    Node args = GNode.create("Arguments");
    
    List<LetBinding> bl = null;
    List<String> upVars = getUpVariables(n);
        
    for (int i = 0; i < funcArgs.size(); i++) {
      Node arg = funcArgs.getGeneric(i);
      if (null != upVars) arg.setProperty(UPVARS, upVars);
      Node expr = (Node)dispatch(arg);
      List<LetBinding> l = getBindings(expr);
      List<String> vars = extractVariables(l);
      upVars = groupList(upVars, vars);
      bl = groupList(bl,l); 
      args.add(expr);      
    }      

    // Name of this function
    final String funcName;
    // Name is this function in Java's style
    final String newName;
    
    // Type of this function
    final Object funcType;
    
    // Parameter types of this function
    final List<String> paramTypes;
    final List<Node> paramTypeNodes;
   
    // Return type of this function
    final Node retTypeNode;

    Node ret = null;

    // Process function application of the form Name.name ...
    if (3 == n.size()) {
      // All functions here are in Primitives
      // Module name
      String moduleName = n.getGeneric(0).getString(0);

      if ("Prelude".equals(moduleName)) {
        // Functions of this form: Prelude.name
        funcName = n.getGeneric(1).getString(0);
      } else {        
        funcName = moduleName + "." + n.getGeneric(1).getString(0);
      }
    
      // Get type of this function
      funcType = table.current().lookup("value(" + funcName + ")");
      // Get parameter type list
      paramTypes = mapper.getParameterTypes(funcType);
      paramTypeNodes = mapper.getParameterTypeNodes(funcType);
      // Get return type
      retTypeNode = mapper.getReturnTypeNode(funcType);
      
      // Java's style of the function name
      newName = Primitives.convertName(n.getGeneric(1).getString(0)); 
      
      // Process get and put here
      if ("Map".equals(moduleName)) {
        List<Node> args1 = new ArrayList<Node>();
        if ("get".equals(newName)) {
          // This must be a complete function application
          args1.add(args.getGeneric(0));  
          args1.add(toIdentifier("hashTable"));
          ret = factory.castInvocation(toIdentifier("Primitives.get"),
                                        "apply", args1); 
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        } else {
          if (2 == args.size()) {
            // complete application
            args1.add(args.getGeneric(0)); 
            args1.add(args.getGeneric(1));   
            args1.add(toIdentifier("hashTable"));
            ret = factory.castInvocation(toIdentifier("Primitives.put"),
                                          "apply", args1); 
            if (null != bl) ret.setProperty(LETBINDINGS, bl);
            return ret;
          } else {
            // incomplete application
            String tempVar = table.freshJavaId("var");
            ret = factory.curryingPut(toIdentifier(tempVar), args.getGeneric(0));
            if (null != bl) ret.setProperty(LETBINDINGS, bl);
            return ret;
          }
        }
        
      }      
      
      //check if this is a complete or imcomplete function application
      if (args.size() == paramTypes.size()) {
        // This is a complete function application
        // Special case the pair ops are generic
        final String newInstance;
        Node newIns = null;
        
        // Instances with one type parameter
        if ("head".equals(newName) || "tail".equals(newName) ||
            "append".equals(newName) || "union".equals(newName) ||
            "cons".equals(newName) || "nth".equals(newName) ||
            "intersection".equals(newName) || "subtraction".equals(newName)) {
          
          final Object t = 
            mapper.getBase(funcArgs.getGeneric(0).getProperty(TYPE));                    
          final String typeName = mapper.toTypeString(t);
          final Node typeNode = mapper.toTypeNode(t, false);

          // Check for type variables
          if (mapper.hasTypeVariables(t)) {
            if ("head".equals(newName)) {
              ret = factory.newApplyHead(typeNode, makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret;
            } else if ("tail".equals(newName)) {
              ret = factory.newApplyHead(typeNode, makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret; 
            } else if ("append".equals(newName)) {
              ret = factory.newApplyAppend(typeNode, makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret; 
            } else if ("union".equals(newName)) {
              ret = factory.newApplyUnion(typeNode, makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret;
            } else if ("cons".equals(newName)) {
              ret = factory.newApplyCons(typeNode, makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret;
            } else if ("nth".equals(newName)) {
              ret = factory.newApplyNth(typeNode, makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret;
            } else if ("intersection".equals(newName)) {
              ret = factory.newApplyIntersection(typeNode, 
                                                  makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret;
            } else if ("subtraction".equals(newName)) {
              ret = factory.newApplySubtraction(typeNode, 
                                                 makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret;
            }
          }
          
          List<String> instanceTypes = new ArrayList<String>();
          instanceTypes.add(typeName);

          String instanceName = getInstanceName(newName, instanceTypes);

          if (null == instanceName) {
            newInstance = table.freshJavaId(newName);
            primitiveInsList.add(new PrimitiveInstance(newName, instanceTypes,
                                                       newInstance));
          } else {
            newInstance = instanceName;
          }

          // Create a new instances with one type parameter
          
          if (null == instanceName && "head".equals(newName)) {
            newIns = factory.newHead(typeNode, newInstance);
          } else if (null == instanceName && "tail".equals(newName)) {
            newIns = factory.newTail(typeNode, newInstance);
          } else if (null == instanceName && "append".equals(newName)){
            newIns = factory.newAppend(typeNode, newInstance);
          } else if (null == instanceName && "union".equals(newName)) {
            newIns = factory.newUnion(typeNode, newInstance);
          } else if (null == instanceName && "cons".equals(newName)) {
            newIns = factory.newCons(typeNode, newInstance);
          } else if (null == instanceName && "nth".equals(newName)) {
            newIns = factory.newNth(typeNode, newInstance);
          } else if (null == instanceName && "intersection".equals(newName)) {
            newIns = factory.newIntersection(typeNode, newInstance);
          } else if (null == instanceName && "subtraction".equals(newName)) {
            newIns = factory.newSubtraction(typeNode, newInstance);
          }
   
          if (null == instanceName) primitiveDeclList.add(newIns);

          ret = factory.apply(toIdentifier(output + "Support." + newInstance),
                               makeArgumentList(args));
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        }  

        if ("exists".equals(newName)) {
          final Object t = 
            mapper.getBase(funcArgs.getGeneric(1).getProperty(TYPE));
          final String typeName = mapper.toTypeString(t);
          final Node typeNode = mapper.toTypeNode(t, false);
          // check for type variables
          if (mapper.hasTypeVariables(t)) {
            ret = factory.newApplyExists(typeNode, makeArgumentList(args)); 
            if (null != bl) ret.setProperty(LETBINDINGS, bl);
            return ret;
          }
          
          List<String> instanceTypes = new ArrayList<String>();
          instanceTypes.add(typeName);
          
          String instanceName = getInstanceName(newName, instanceTypes);
  
          if (null == instanceName) {
            newInstance = table.freshJavaId(newName);
            primitiveInsList.add(new PrimitiveInstance(newName, instanceTypes,
                                                       newInstance));
            newIns = factory.newExists(typeNode, newInstance);
            primitiveDeclList.add(newIns);
          } else {
            newInstance = instanceName;
          }                    
          ret = factory.apply(toIdentifier(output + "Support." + newInstance),
                               makeArgumentList(args));
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        }
        
        // Instances with two type parameters
        if ("map".equals(newName) || "iter".equals(newName)) {
          final Object t1 = funcArgs.getGeneric(0).getProperty(TYPE);
          final String typeName1 = mapper.getReturnType(t1);
          final Node typeNode1 = mapper.getReturnTypeNode(t1);
          final Object t2 = 
            mapper.getBase(funcArgs.getGeneric(1).getProperty(TYPE));
          final String typeName2 = mapper.toTypeString(t2);
          final Node typeNode2 = mapper.toTypeNode(t2, false);
          
          // Check for type variables
          if (mapper.hasTypeVariables(t1) || mapper.hasTypeVariables(t2)) {
            if ("map".equals(newName)) {
              ret = factory.newApplyMap(typeNode1, typeNode2, 
                                         makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret;
            } else if ("iter".equals(newName)) {
              ret = factory.newApplyIter(typeNode1, typeNode2, 
                                         makeArgumentList(args));
              if (null != bl) ret.setProperty(LETBINDINGS, bl);
              return ret;
            } 
          }
          
          List<String> instanceTypes = new ArrayList<String>();
          instanceTypes.add(typeName1);
          instanceTypes.add(typeName2);

          String instanceName = getInstanceName(newName, instanceTypes);
          if (null == instanceName) {
            newInstance = table.freshJavaId(newName);
            primitiveInsList.add(new PrimitiveInstance(newName, instanceTypes,
                                                       newInstance));
          } else {
            newInstance = instanceName;
          }
          
          if (null == instanceName && "map".equals(newName)) {
            newIns = factory.newMap(typeNode1, typeNode2, newInstance);
          } else if (null == instanceName && "iter".equals(newName)) {
            newIns = factory.newIter(typeNode1, typeNode2, newInstance);
          } 
          if (null == instanceName) primitiveDeclList.add(newIns);
          ret = factory.apply(toIdentifier(output + "Support." + newInstance),
                               makeArgumentList(args));
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        }  

        if ("foldl".equals(newName)) {
          final Object t1 = funcArgs.getGeneric(2).getProperty(TYPE);
          final String typeName1 = mapper.toTypeString(t1);
          final Node typeNode1 = mapper.toTypeNode(t1, false);
          final Object t2 = 
            mapper.getBase(funcArgs.getGeneric(1).getProperty(TYPE));
          final String typeName2 = mapper.toTypeString(t2);
          final Node typeNode2 = mapper.toTypeNode(t2, false);
          
          // Check for type variables
          if (mapper.hasTypeVariables(t1) || mapper.hasTypeVariables(t2)) {
            ret = factory.newApplyFoldl(typeNode1, typeNode2,
                                         makeArgumentList(args)); 
            if (null != bl) ret.setProperty(LETBINDINGS, bl);
            return ret;
          }
          List<String> instanceTypes = new ArrayList<String>();
          instanceTypes.add(typeName1);
          instanceTypes.add(typeName2);

          String instanceName = getInstanceName(newName, instanceTypes);
          if (null == instanceName) {
            newInstance = table.freshJavaId(newName);
            primitiveInsList.add(new PrimitiveInstance(newName, instanceTypes,
                                                       newInstance));
            newIns = factory.newFoldl(typeNode1, typeNode2, newInstance);
            primitiveDeclList.add(newIns);
          } else {
            newInstance = instanceName;
          }
          ret = factory.apply(toIdentifier(output + "Support." + newInstance),
                               makeArgumentList(args));
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        }
        // Other primitive functions
        ret = factory.applyPrimitive(newName, makeArgumentList(args));  
        if (null != bl) ret.setProperty(LETBINDINGS, bl);
        return ret;
      } else {
        // This is an incomplete function application, process currying
        ret = makeCurry("Primitives." + newName, args, 
                         paramTypeNodes, retTypeNode, funcType);
        if (null != bl) ret.setProperty(LETBINDINGS, bl);
        return ret;
      }

    } else { // Process function application of this form: name ...  
     
      funcName = n.getGeneric(0).getString(0);
      // Java's style of the function name
      newName = Primitives.convertName(n.getGeneric(0).getString(0));
 
      List<Node> args1 = new ArrayList<Node>();  

      // Process symbol table function applications, they must be complete.
      if ("lookup".equals(newName)||"lookupLocally".equals(newName)) {
        if (1 == funcArgs.size()) {
          // If it has only one child, that must be a node
          args1.add((Node)dispatch(funcArgs.getGeneric(0)));
          args1.add(toIdentifier("getNameSpace"));        
          ret = factory.castInvocation(toIdentifier(newName + "2"),
                                        "apply", args1);
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
  
        } else if(2 == funcArgs.size()) {
          Node arg = (Node)dispatch(funcArgs.getGeneric(0));
                  
          if ("ErrorClause".equals(funcArgs.getGeneric(1).getName())) {
            // If the second child is a error clause.
            String tag = funcArgs.getGeneric(1).getGeneric(0).getString(0);
            args1.add(arg);
            args1.add(toLiteral("StringLiteral",  "\"" + tag + "\""));
            args1.add((Node)dispatch(funcArgs.getGeneric(1).getGeneric(1)));
            args1.add(toIdentifier("getNameSpace"));
            ret = factory.castInvocation(toIdentifier(newName + "4"),
                                      "apply", args1);
            if (null != bl) ret.setProperty(LETBINDINGS, bl);
            return ret;
          } else { 
            // The second child is a tag.
            args1.add(arg);
            args1.add(toIdentifier("getNameSpace"));
            ret = factory.castInvocation(toIdentifier(newName + "2"),
                                      "apply", args1);
            if (null != bl) ret.setProperty(LETBINDINGS, bl);
            return ret;
          }
        } else {
          // The arguments include: node, tag and error clause in that order.
          String tag = funcArgs.getGeneric(2).getGeneric(0).getString(0);
          
          args1.add((Node)dispatch(funcArgs.getGeneric(0)));
          args1.add(toLiteral("StringLiteral",  "\"" + tag + "\""));
          args1.add((Node)dispatch(funcArgs.getGeneric(2).getGeneric(1)));
          args1.add(toIdentifier("getNameSpace"));
          ret = factory.castInvocation(toIdentifier(newName + "4"),
                                        "apply", args1);        
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        } 
      }  
        // Process define.
      if ("define".equals(newName)) {
        if (2 == funcArgs.size()) {
          //  It is has 2 children, they must be a node and a type.
          args1.add((Node)dispatch(funcArgs.getGeneric(0)));
          args1.add((Node)dispatch(funcArgs.getGeneric(1)));
          args1.add(toIdentifier("getNameSpace"));
          
          ret = factory.apply(toIdentifier("define3"), args1);
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        } else {
          // It has 3 children: node, type and and error clause in that order.
          String tag = funcArgs.getGeneric(2).getGeneric(0).getString(0);
        
          args1.add((Node)dispatch(funcArgs.getGeneric(0)));
          args1.add((Node)dispatch(funcArgs.getGeneric(1)));
          args1.add(toLiteral("StringLiteral",  "\"" + tag + "\""));
          args1.add((Node)dispatch(funcArgs.getGeneric(2).getGeneric(1)));
          args1.add(toIdentifier("getNameSpace"));
          ret = factory.invocation(toIdentifier("define5"),
                                    "apply", args1);                    
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        } 
      }
    
      //process redefine, isDefined, and isDefinedLocally
      if ("isDefined".equals(newName) || "isDefinedLocally".equals(newName) 
          || "redefine".equals(newName)) {
        args.add(toIdentifier("getNameSpace"));
        ret = factory.apply(toIdentifier(newName),makeArgumentList(args));  
        if (null != bl) ret.setProperty(LETBINDINGS, bl);
        return ret;
      }
 
      // Get the type of this function
      funcType = table.current().lookup("value(" + funcName + ")");
      // Get parameter type
      paramTypes = mapper.getParameterTypes(funcType);
      paramTypeNodes = mapper.getParameterTypeNodes(funcType);
      // Get return type
      retTypeNode = mapper.getReturnTypeNode(funcType);

      // check for complete and incomplete function applications
      if (args.size() == paramTypes.size()) {
        // Complete function application
        if (Primitives.isPrimitive(newName)) {
          ret = factory.applyPrimitive(newName, makeArgumentList(args));  
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        } else {
          ret = factory.apply(toIdentifier(newName),makeArgumentList(args));          
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        }
      } else {
        // incomplete function application
        if (Primitives.isPrimitive(newName)) {
          ret = makeCurry("Primitives." + newName, args, 
                            paramTypeNodes, retTypeNode, funcType);  
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        } else {
          ret = makeCurry(newName, args, paramTypeNodes, 
                           retTypeNode, funcType);
          if (null != bl) ret.setProperty(LETBINDINGS, bl);
          return ret;
        }
      }             
    }       
  }    
  
  /**
   * Make a java expresson to represent a tuple.
   *
   * @param n The typical tuple node.
   * @return The java representation.
   */
  private Node makeTuple(GNode n) {    
    GNode args = GNode.create("Arguments");
    List<String> upVars = getUpVariables(n);
    List<LetBinding> bl = null;
 
    for (int i = 0; i < n.size(); i++) {
      Node no = n.getGeneric(i); 
      if (n.hasProperty(INANALYZE)) {
        no.setProperty(INANALYZE, Boolean.TRUE);
      }
      if (null != upVars) no.setProperty(UPVARS,upVars);
      Node expr = (Node)dispatch(no); 
      args.add(expr);
      List<LetBinding> l = getBindings(expr);
      List<String> vars = extractVariables(l);
      upVars = groupList(upVars, vars);
      bl = groupList(bl,l);      
    }  

    final List<Node> members = mapper.getMemberNodes(n.getProperty(TYPE));
    // Make type arguments        
    Node typeArgs = GNode.create("TypeArguments");                
    final Node typeNode;
    for (Node ob : members) typeArgs.add(ob);

    if (!members.isEmpty()) {        
      typeNode = GNode.create("Type",
                 GNode.create("InstantiatedType",
                   GNode.create("TypeInstantiation", "Tuple", null),
                   GNode.create("TypeInstantiation", "T" + members.size(), 
                                typeArgs)),
                 null);
    } else {
      typeNode = GNode.create("Type",
                   GNode.create("QualifiedIdentifier", "Tuple", "T0"),
                   null);  
    }

    Node ret = toNewExpression2(typeNode, args,null);
    if (null != bl) ret.setProperty(LETBINDINGS,bl);       
    return ret;
      
  }
   
  /** 
   * Create java code for a list literal.
   *
   * @param n The list expression or pattern node.
   * @return A Pair representing this list.
   */ 
  private Node makeList(GNode n) {
    List<Node> arguments = new ArrayList<Node>(n.size());

    List<LetBinding> bl = null;
    List<String> upVars = getUpVariables(n);
    
    //process each list element
    for (int i = 0; i < n.size(); i++) {
      Node no = n.getGeneric(i);
      if (n.hasProperty(INANALYZE)) {
        no.setProperty(INANALYZE, Boolean.TRUE);
      }
      if (null != upVars) no.setProperty(UPVARS, upVars);
      Node expr = (Node)dispatch(no);
      arguments.add(expr);
      List<LetBinding> l = getBindings(expr);
      List<String> vars = extractVariables(l);
      upVars = groupList(upVars, vars);
      bl = groupList(bl,l);      
    }
    
    if (arguments.isEmpty()) {
      final String typeName = getType(mapper.getBase(n.getProperty(TYPE)));
      final Node typeNode = mapper.toTypeNode(
                              mapper.getBase(n.getProperty(TYPE)), false);
      if ("Object".equals(typeName)) {
        return factory.invocation(toIdentifier("Pair"), "empty", arguments);
      } else {
        return factory.pairEmpty(typeNode);
      }
    }
    
    //cons up the list
    Node newPair = factory.newPair(
      mapper.toTypeNode(n.getProperty(TYPE), false), arguments.get(0));
       
    for (int i = 1; i < arguments.size(); i++) {
      newPair = factory.appendPair(newPair, 
      toNewExpression2(mapper.toTypeNode(n.getProperty(TYPE), false),
                      GNode.create("Arguments", arguments.get(i)), null));
    }
    if (null != bl) newPair.setProperty(LETBINDINGS, bl);
    return newPair;
  }


  /**
   * Process a cons pattern.
   *
   * @param n The cons pattern node.
   * @return The java equivalent.
   */
  public Node visitConsPattern(GNode n) {
    String name = table.freshJavaId("list");
    String rhs = (String)n.getProperty(RHS);
    
    String matchArg = (String)n.getProperty(MATCHARG); 
      
    // fix cast here
    Object t = n.getProperty(TYPE);
    if (mapper.hasTypeVariables(t)) {
      ((Node)n.getProperty(BINDINGS)).add(makeVarDec2(name, 
        mapper.toTypeNode(t, false),
        toIdentifier(rhs)));
    } else {
      ((Node)n.getProperty(BINDINGS)).add(makeVarDec2(name, 
        mapper.toTypeNode(t, false),
        factory.cast(toIdentifier(rhs))));
    }  

    Node head = n.getGeneric(0);
    head.setProperty(MATCHARG, "Primitives.wrapHead(" + matchArg + ")");
    head.setProperty(RHS, "Primitives.wrapHead(" + name + ")");
    head.setProperty(BINDINGS, n.getProperty(BINDINGS));
    
    Node tail = n.getGeneric(1); 
    tail.setProperty(MATCHARG,"Primitives.wrapTail(" + matchArg + ".tail()");
    tail.setProperty(RHS, "Primitives.wrapTail(" + name + ")");
    tail.setProperty(BINDINGS, n.getProperty(BINDINGS));
       
    
    if ((head.hasName("WildCard") || head.hasName("Variable")) &&
        (tail.hasName("WildCard") || tail.hasName("Variable"))) {
      dispatch(head);
      dispatch(tail);
      return factory.isNotEmptyCall(toIdentifier(matchArg));
    } else if (tail.hasName("WildCard") || tail.hasName("Variable")) {
      head = (Node)dispatch(head);
      dispatch(tail);
      return factory.jequals(head,factory.headWrapper(toIdentifier(matchArg)));
    } else if (head.hasName("WildCard") || head.hasName("Variable")) {
      tail = (Node)dispatch(tail);
      dispatch(head);
      return factory.jequals(tail,factory.tailWrapper(toIdentifier(matchArg)));
    }
        
    return factory.jand(factory.jequals((Node)dispatch(head),
                           factory.headWrapper(toIdentifier(matchArg))), 
                        factory.jequals((Node)dispatch(tail), 
                                factory.tailWrapper(toIdentifier(matchArg))));
  }
      
  /**
   * Process a match expression type.
   * 
   * @param n The match expression node.
   * @return The java equivalent.
   */
  public Node visitMatchExpression(GNode n) {
    
    Object argType =
      mapper.getPatternMatchLeftType(n.getGeneric(1).getProperty(TYPE));
   
    String matchArg = table.freshJavaId("arg");

    n.getGeneric(1).setProperty(TOP, null);
    n.getGeneric(1).setProperty(MATCHARG, matchArg);
    n.getGeneric(1).setProperty(RHS, matchArg); 
    
    List<Node> nodes = new ArrayList<Node>();
    
    final Node argTypeNode = mapper.toTypeNode(argType, false);

    if (nodeType.equals(argTypeNode) ||
        gnodeType.equals(argTypeNode)) {
      nodes.add(makeVarDec2(matchArg, argTypeNode,
                           factory.gnodeCast((Node)dispatch(n.getGeneric(0)))));
      if (n.hasProperty(INANALYZE)) {
        n.getGeneric(1).setProperty(ANNOTATE, Boolean.TRUE);
      } else {
        n.getGeneric(1).setProperty(SCOPE, Boolean.TRUE);
      }
    } else {
      // fix cast here
      if (mapper.hasTypeVariables(argType)) {
        nodes.add(makeVarDec2(matchArg, argTypeNode,
                           (Node)dispatch(n.getGeneric(0))));
      } else {
        nodes.add(makeVarDec2(matchArg, argTypeNode,
                           factory.cast((Node)dispatch(n.getGeneric(0)))));
      }                  
    }
    
    // Checking and return null
    if (!containsBottomMatch(n.getGeneric(1))) {
      nodes.add(factory.ifStatement4(toIdentifier(matchArg)));                   
    }
    
    @SuppressWarnings("unchecked")
    List<Node> matches = (List<Node>)dispatch(n.getGeneric(1));
   
    nodes.addAll(matches);
    nodes.add(factory.ret(nullNode));
        
    Node match = factory.matchExpression(
      mapper.toTypeNode(n.getProperty(TYPE), false), nodes);  
    
    return match; 
  } 
  
  /** 
   * Check if a pattern matching contains an explicit match for bottom.
   *
   * @param n The pattern matching node.
   * @return <code> true </code> if it contains a match for bottom pattern. 
   *   <code> false</code> otherwise.   
   */
  private boolean containsBottomMatch(Node n) {
    for (int i = 0; i < n.size(); i++) {
      Node patterns = n.getGeneric(i).getGeneric(0);      
      for(int j= 0; j < patterns.size(); j++) { 
        Node pat = patterns.getGeneric(j);
        if (pat.hasName("BottomPattern")) return true;
      }
    }
    return false;  
  } 
  
  /**
   * Check if this pattern match is on non-node type constructors.
   * 
   * @param n the pattern match node.
   * @return <code> true </code> if type constructor match. <code> false</code>
   * otherwise.
   */
  private boolean isTypeConstructorMatch(Node n) {
    for (int i = 0; i < n.size(); i++) {
      if (n.getGeneric(i).getGeneric(0).getGeneric(0)
          .hasName("TypeConstructorPattern") &&
          !mapper.isNode(n.getGeneric(i).getGeneric(0).getGeneric(0).
                         getProperty(TYPE))) {
        return true;
      }      
    }
    return false;
  }
  
  /**
   * Process a pattern matching expression.
   *
   * @param n The pattern matching node.
   * @return A list with the unified types of the left and right sides
   *         of the pattern matching.
   */
  public List<Node> visitPatternMatching(GNode n) {

    int size = n.size();
    ArrayList<Node> nodes = new ArrayList<Node>();
    
    boolean tcmatch = isTypeConstructorMatch(n);
    
    for (int i = 0; i < size; i++) {
      Node patterns = n.getGeneric(i).getGeneric(0);
      
      for(int j= 0; j < patterns.size(); j++) {
        
        String foo = (String)n.getGeneric(i).getProperty("enterScope");
        if (null== foo) foo = (String)n.getGeneric(i).getProperty("enterScope");
        
        enterScope(foo);
        
        Node pmatch2 = GNode.create("PatternMatch",
          GNode.create("Patterns",patterns.getGeneric(j)),
                                    n.getGeneric(i).getGeneric(1));
        pmatch2.setProperty(TOP, null);
        pmatch2.setProperty(MATCHARG, n.getProperty(MATCHARG));
        pmatch2.setProperty(RHS, n.getProperty(MATCHARG));
        pmatch2.setProperty(TYPE,n.getProperty(TYPE));

        if (n.hasProperty(ANNOTATE)) { 
          pmatch2.setProperty(ANNOTATE, Boolean.TRUE);
        }  
        if (n.hasProperty(SCOPE)) { 
          pmatch2.setProperty(SCOPE, Boolean.TRUE);
        }  
        
        if (tcmatch) pmatch2.setProperty("TCMatch", null);
        
        nodes.add((Node)dispatch(pmatch2));
        
        exitScope(foo);
      }      
    }
 

    if (runtime.test("optimizeMatch")) {
      
      HashMap<String, ArrayList<Node>> bins =
        new HashMap<String, ArrayList<Node>>();
      
      bins.put("default", new ArrayList<Node>());
      
      
      for (int i = 0; i < nodes.size(); i++) {
        Node node = nodes.get(i);
        if (node.hasProperty("TName")) {
          String name = (String)node.getProperty("TName");
          
          if (bins.containsKey(name)) {
            bins.get(name).add(node);
          } else {
            ArrayList<Node> temp = new ArrayList<Node>();
            temp.add(node);
            bins.put(name, temp);        
          }
        } else {
          bins.get("default").add(node);
        }
      }
      
      ArrayList<Node> nodes2 = new ArrayList<Node>();
      Node switchn = 
        factory.switchStmnt(toIdentifier((String)n.getProperty(MATCHARG)));
      
      switchn = GNode.ensureVariable(GNode.cast(switchn));
      
      String defname = table.freshJavaId("default");
      
      for (String key : bins.keySet()) {
        if ("default".equals(key)) continue;
        switchn.add(makeCase(GNode.create("PrimaryIdentifier", key), 
                             bins.get(key),defname));
      }
      switchn.add(GNode.create("DefaultClause", 
                               GNode.create("BreakStatement", null)));
      
      nodes2.add(factory.switchWrap(
        toIdentifier((String)n.getProperty(MATCHARG)),switchn));
      
      if (bins.get("default").size() > 0) {
        nodes2.addAll(bins.get("default"));
      }
      
      if (tcmatch) {
        return nodes2;
      }
    }
    
    return nodes;
  }
        
  /**
   * Transform a typed pattern.
   *
   * @param n The typed pattern node.
   * @return The java code for the typed pattern.
   */
  public Node visitTypedPattern(GNode n) {
    return (Node)dispatch(n.getGeneric(0));
  }

  /**
   * Generate an as pattern.
   *
   * @param n The as pattern node.
   * @return The java as pattern node.
   */
  public Node visitAsPattern(GNode n) {
    Node pat = n.getGeneric(0);
    String rhs = (String)n.getProperty(RHS);
    String matchArg = (String)n.getProperty(MATCHARG);
    
    if (n.hasProperty(TOP)) pat.setProperty(TOP, null);

    pat.setProperty(MATCHARG, n.getProperty(MATCHARG));
    pat.setProperty(RHS, rhs);
    pat.setProperty(BINDINGS, n.getProperty(BINDINGS));
    pat.setProperty(TYPE,pat.getProperty(TYPE));
     
    // fix cast here
    Object t = pat.getProperty(TYPE);
    if (mapper.hasTypeVariables(t)) {               
      ((Node)n.getProperty(BINDINGS)).add(makeVarDec2(n.getString(1), 
        mapper.toTypeNode(t, false), 
        toIdentifier(matchArg)));
    } else {
      ((Node)n.getProperty(BINDINGS)).add(makeVarDec2(n.getString(1), 
        mapper.toTypeNode(t, false), 
        factory.cast(toIdentifier(matchArg))));
    } 
   
    if (isLiteral(n))
      return factory.jequals((Node)dispatch(pat),toIdentifier(matchArg));
    return (Node)dispatch(pat);
  }  

  /**
   * Generate a when pattern.
   *
   * @param n The when pattern node.
   * @return An if statement represeing the when expression.
   */
  public Node visitWhenPattern(GNode n) {
    Node pat = n.getGeneric(0);
    
    pat.setProperty(TOP, null);
    pat.setProperty(RHS, n.getProperty(RHS));
    pat.setProperty(BINDINGS, n.getProperty(BINDINGS));
    pat.setProperty(TYPE, n.getProperty(TYPE));
    pat.setProperty(MATCHARG, n.getProperty(MATCHARG));
    
    if (n.hasProperty(TOP)) pat.setProperty(TOP, null);
  
    //n.getGeneric(1).setProperty(NEWLET, Boolean.TRUE);
    Node expr = (Node)dispatch(n.getGeneric(1));
    expr = checkToLet(expr, n.getGeneric(1).getProperty(TYPE));
    
    return factory.ifStatement(expr,(Node)dispatch(pat));
  }


  //additional conditions that need to be met after the basic pattern
  //is matched and bound.
  //For example, in the pattern Foo(a, a) -> e, both a's are wildcards.
  //however the expression is only executed if the a's are equal.
  //we therefor translate that pattern match as follows
  // if ( Constructor.make("Foo", wild1, wild2).equals(arg)) {
  //   a = arg.get(1);
  //   if (a.equals(arg.get(2))) { ***conditions***
  //     return e; 
  //   }
  // }
  Node conditions = null;

  /**
   * Process a pattern match.
   *
   * @param n The pattern match node.
   * @return The if statement representing this pattern match.
   */
  public Node visitPatternMatch(GNode n) {
    Node savedConditions = conditions;

    String rhs = (String)n.getProperty(RHS);
    Node pattern = n.getGeneric(0);   
    
    Node expr    = n.getGeneric(1);
    String matchArg = (String)n.getProperty(MATCHARG);

    Node ifStmnt = toIfStatement(null, null);

    if (n.hasProperty(INANALYZE)) expr.setProperty(INANALYZE, Boolean.TRUE);

    pattern.setProperty(TOP, null);
    pattern.setProperty(BINDINGS, ifStmnt.getGeneric(1));
    pattern.setProperty(MATCHARG, matchArg);
    pattern.setProperty(RHS, rhs);
       
    if ("WhenPattern".equals(pattern.getGeneric(0).getName())) {
      Node when = (GNode)dispatch(pattern);
            
      if (pattern.getGeneric(0).getGeneric(0).hasName("WildCard") ||
          pattern.getGeneric(0).getGeneric(0).hasName("Variable")) {
        ifStmnt.set(0, toLiteral("BooleanLiteral", "true"));
      } else {
        ifStmnt.set(0, GNode.create(when.getGeneric(1).getGeneric(0)));
      }
    
      if (optimizeLet) {
        List<String> vars = getPatternVariables(pattern.getGeneric(0));
        if (null != vars && !vars.isEmpty()) expr.setProperty(UPVARS, vars);
      }
     
      Node exprNode = (Node)dispatch(expr);
      
      when.getGeneric(1).set(0, factory.ret(exprNode));
      ifStmnt.getGeneric(1).add(when);
   
      if (n.hasProperty(ANNOTATE) || n.hasProperty(SCOPE)) {        
        return augmentIfStatement(ifStmnt, matchArg, n, exprNode);
      }
      return addToIf(ifStmnt, exprNode);
    }
    
    if (pattern.hasName("WildCard") || pattern.hasName("Variable")) {
      ifStmnt.set(0, toLiteral("BooleanLiteral", "true"));
      dispatch(pattern);
    } else {
      ifStmnt.set(0, dispatch(pattern));
    }
    
    if (optimizeLet) {
      List<String> vars = getPatternVariables(pattern.getGeneric(0));
      if (null != vars && !vars.isEmpty()) expr.setProperty(UPVARS, vars);
    }

    Node res = (Node)dispatch(expr);
    
    // fix cast
    Object t = mapper.getPatternMatchRightType(n.getProperty(TYPE));    
    if(res.hasName("Block")) {
      ifStmnt.getGeneric(1).add(res);
    } else {  
      if(conditions == null) {
        if (mapper.hasTypeVariables(t)) {
          ifStmnt.getGeneric(1).
            add(factory.ret(res));
        } else {
          ifStmnt.getGeneric(1).
            add(factory.ret(factory.cast(res)));
        } 
      } else { 
        Node b = GNode.create("Block");
        Node newIf = toIfStatement(conditions, b);
        if (mapper.hasTypeVariables(t)) {
          b.add(factory.ret(res));
        } else {
          b.add(factory.ret(factory.cast(res)));
        }
        ifStmnt.getGeneric(1).add(newIf);
      }
    }
    conditions = savedConditions;
    
    if (n.hasProperty(ANNOTATE) || n.hasProperty(SCOPE)) {
      return augmentIfStatement(ifStmnt, matchArg, n, res);
    }
    
    if (n.hasProperty("TCMatch")) {
      if (n.getGeneric(0).getGeneric(0).hasName("TypeConstructorPattern")) {
        ifStmnt.setProperty("TName", 
                            n.getGeneric(0).getGeneric(0).getString(0));
      }
      if (n.getGeneric(0).getGeneric(0).hasName("AsPattern") &&
          n.getGeneric(0).getGeneric(0).getGeneric(0).hasName("TypeConstructorPattern")) {
        ifStmnt.setProperty("TName", 
           n.getGeneric(0).getGeneric(0).getGeneric(0).getString(0));
      }
    }

    return addToIf(ifStmnt, res);
  }
   

  /**
   * Process a type constructor pattern.
   *
   * @param n The type constructor node.
   * @return The java equivalent.
   */
  public Node visitTypeConstructorPattern(GNode n) {
 
    final String typeName = n.getString(0);
    final Object type = n.getProperty(TYPE);
    final Node typeNode = mapper.toTypeNode(type, false); 

    String matchArg = (String)n.getProperty(MATCHARG);
    
    
    Node matchArgNode = toIdentifier(matchArg);
    Node condition = null;
    Node inner = (Node)n.getProperty(BINDINGS);
    
    final int size = n.size();
 
    if (nodeType.equals(typeNode) || gnodeType.equals(typeNode)) {
      condition = factory.hasNameCall(toIdentifier(matchArg), 
        toLiteral("StringLiteral", "\"" + typeName + "\""));

      if ((size == 1) || n.getGeneric(1).hasName("WildCard")) {
        //do nothing since the initial condition is sufficient
      } else {
        Node params = n.getGeneric(1);
        List<String> members = mapper.getMembers(type);
        List<Object> memberObjects = mapper.getMemberObjects(type);
        List<Node> memberNodes = mapper.getMemberNodes(type);
        
        boolean hasVar = false;
        for (Object o : memberObjects) {
          if (mapper.isVariable(o)) hasVar = true;
        }
        
        //check the last item to see if it's a list variable
        //need to also check the last item to see if it's a node var
        
        if (members.get(members.size() -1).startsWith("Pair") || hasVar) {
          condition = factory.jand(condition, 
            factory.sizeGreaterEqual(matchArgNode, 
            toLiteral("IntegerLiteral", Integer.toString((params.size()- 1)))));
        } else {
          condition = factory.jand(condition, 
            factory.sizeEqual(matchArgNode, 
            toLiteral("IntegerLiteral", Integer.toString(params.size()))));
        } 
                
        for (int i = 0; i < params.size(); i++) {
          
          String tname = members.get(i);
          Node tNode = memberNodes.get(i);
          
          Node node = params.getGeneric(i);
          if (node.hasName("Variable")) {
            if ("String".equals(tname)) {
              inner.add(factory.makeNodeBinding2(node.getString(0),
                          toIdentifier(matchArg),
                          toLiteral("IntegerLiteral", Integer.toString(i))));
            } else if ( "Node".equals(members.get(i)) || 
                      "GNode".equals(members.get(i))) {
              inner.add(factory.makeNodeBinding(node.getString(0),
                          toIdentifier(matchArg),
                          toLiteral("IntegerLiteral", Integer.toString(i))));
            } else if (tname.startsWith("Pair")) {
              inner.add(makeVarDec2(node.getString(0), tNode,
                factory.cast(toIdentifier("Primitives.getChildren(" + matchArg +
                    ", " + i + ", " + matchArg + ".size())"))));                  
            }
            
            continue;
          } 
          
          if (node.hasName("WildCard")) continue;
          
          if (isLiteral(node)) {
            condition = factory.jand(condition, 
              factory.jequals2((Node)dispatch(node), 
              toIdentifier(matchArg + ".getString(" + i + ")")));
            continue;
          }
          
          node.setProperty(BINDINGS, inner);
          
          if ("String".equals(members.get(i))) {
            node.setProperty(MATCHARG, matchArg + ".getString(" + i + ")");
          } else if ( "Node".equals(members.get(i)) || 
                      "GNode".equals(members.get(i))) {
            node.setProperty(MATCHARG, matchArg + ".getGeneric(" + i + ")");
          } else if (members.get(i).startsWith("Pair")) {
            
            node.setProperty(MATCHARG, "Primitives.getChildren(" + matchArg +
                             ", " + i + ", " + matchArg + ".size())");         
          }
          
          condition = factory.jand(condition,(Node)dispatch(node));
        }
      }
    } else {
      condition = 
        factory.isMethodCall(matchArgNode, "is" + typeName);

      if (n.size() == 1 || n.getGeneric(1).hasName("WildCard")) {
        //do nothing since the initial condition is sufficient
      } else {
        Node params = n.getGeneric(1);
        
        List<Node> memberNodes = mapper.getMemberNodes(type);
                
        for (int i = 0; i < params.size(); i++) {
          Node node = params.getGeneric(i);
          
          if (node.hasName("WildCard")) {
            continue;
          }
          if (node.hasName("Variable")) {
            inner.add(makeVarDec2(node.getString(0), memberNodes.get(i),
                factory.cast(toIdentifier(matchArg + ".getTuple().get" + 
                                          (i + 1) + "()"))));
            continue;
          }

          if (isLiteral(node)) {
            condition = factory.jand(condition, 
              factory.jequals2((Node)dispatch(node), 
               toIdentifier(matchArg + ".getTuple().get" + (i + 1) + "()")));
            continue;
          }

          node.setProperty(MATCHARG, matchArg + ".getTuple().get" + (i + 1) +
                           "()");
          condition = factory.jand(condition,(Node)dispatch(node));
        }
      }    
    }  
    
   
    
    if (n.hasProperty(TOP) || n.hasProperty("ancestor")) {
      
   
      condition = replaceMatchArg(condition, matchArg);
      
      if (n.hasProperty("ancestor")) {
        Node node = factory.ancestorExpression(factory.ret(condition));
        
        if (nodeMatches.containsKey(node)) {
          return factory.support(toIdentifier(output + "Support"),
                                 nodeMatches.get(node));
        } else {
          String arg = table.freshJavaId("nodeMatch");
          staticFields.add(factory.supportNodeMatch(arg, node));
          nodeMatches.put(node, arg);
          return factory.support(toIdentifier(output + "Support"),arg);
        }
      }

      String matchName = table.freshJavaId("match");
      Match ms = new Match(mapper.toTypeNode(n.getProperty(TYPE), false),
                           condition);
      if (matches.containsKey(ms)) {
        matchName = matches.get(ms);
      } else {
        matches.put(ms, matchName);
        Node tNode = null;
        if (mapper.hasTypeVariables(type)) {
          tNode = mapper.toTypeNode(type, true);
        } else {
          tNode = mapper.toTypeNode(type, false);
        }
        Node matchFunction = 
          factory.matchFunction(matchName, tNode , 
                                condition);   
       
        matchFunction.getGeneric(4).getGeneric(0).set(3, "m");
        staticFields.add(matchFunction);
      } 
      
      

      return factory.matchCall(toIdentifier(output + "Support"), matchName, 
        toIdentifier((String)n.getProperty(MATCHARG)));
    } else {
      return condition;
    }
  } 

  /**
   * Process pattern parameters.
   *
   * @param n The pattern parameters node.
   * @return A new tuple expression.
   */
  public Node visitPatternParameters(GNode n) {
    return makeTuple(n);
  }
  
  /**
   * Process a variant type definition.
   *
   * @param n The variant type node.
   */
  public void visitVariantDeclaration(GNode n) {
    String baseName = (String)n.getProperty("name");
    Node baseBody = GNode.create("ClassBody");
   
    Node enums = GNode.create("EnumConstants");
    Node tag = GNode.create("EnumDeclaration", GNode.create("Modifiers", 
      GNode.create("Modifier", "public"), GNode.create("Modifier", "static")), 
      "Tag", null, enums, null);    
    
    baseBody.add(tag);
    baseBody.add(factory.defaultConstr(baseName));
    baseBody.add(factory.getTagAbstract());
    
    for (int i = 0; i < n.size(); i++) {
      Node tc = n.getGeneric(i);
      addEnum(enums, tc.getString(0));
      Object tcType = tc.getProperty(TYPE);
      
      List<String> types = mapper.getMembers(tcType);
      List<Node> typeNodes = mapper.getMemberNodes(tcType);
         
      int size = types.size();
      
      Node fparams = GNode.create("FormalParameters");
      Node classBody = GNode.create("ClassBody");

      //create a constructor
      Node createArgs = GNode.create("Arguments");
      for (int j = 0; j < size; j++) {
        Node fptype = typeNodes.get(j);
        String fpname = "member" + (j + 1);
        
        createArgs.add(toIdentifier(fpname));
        fparams.add(GNode.create("FormalParameter", null, fptype, null,
            fpname, null));
      }

      Node constrblock1  = GNode.create("Block");
      Node constr1 = GNode.create("ConstructorDeclaration", pmod, null,
                     tc.getString(0), fparams, null, constrblock1);

      // Make type arguments        
      Node typeArgs = GNode.create("TypeArguments");                

      final Node typeNode;
      for (Node ob : typeNodes) typeArgs.add(ob);

      if (!typeNodes.isEmpty()) {        
        typeNode = GNode.create("Type",
                   GNode.create("InstantiatedType",
                     GNode.create("TypeInstantiation", "Tuple", null),
                     GNode.create("TypeInstantiation", "T" + typeNodes.size(), 
                                  typeArgs)),
                   null);
      } else {
        typeNode = GNode.create("Type",
                     GNode.create("QualifiedIdentifier", "Tuple", "T0"),
                     null);  
      }        

      constrblock1.add(factory.assign(toIdentifier("tuple"),
         factory.newExpr(typeNode, makeArgumentList(createArgs))));
      
      classBody.add(constr1);
      classBody.add(factory.getTag(tc.getString(0)));
      
      //add an isXXX method to the base class
      Node isBase = factory.isMethod();
      isBase.set(3, "is" + tc.getString(0));
      baseBody.add(isBase);
      
      //add an isXXX method to the variant type class
      Node isVariant = factory.isMethod();
      isVariant.set(3, "is" + tc.getString(0));
      isVariant.getGeneric(7).getGeneric(0).getGeneric(0).set(0, "true");
      classBody.add(isVariant);
      
      // Add getName() method
      classBody.add(factory.getNameMethod(GNode.create("StringLiteral","\"" + 
                                                       tc.getString(0)+ "\"")));
      // ADD equals methods
      for (Equality e : equalities) {
        if (e.name.equals(tc.getString(0))) {
          classBody.add(createVariantEqualsMethod(tc.getString(0)));
          break;
        }
      }
      
      // Add toString()
      String toString = "\"" + tc.getString(0);
      if (size > 0) {
        toString += " of \" + tuple.toString()";
      } else {
        toString += "\"";
      }
      
      Node toStringNode = factory.toStringMethod();
      toStringNode.set(7, GNode.create("Block", 
        factory.ret(toLiteral("StringLiteral", toString))));
      
      classBody.add(toStringNode);

      final Node baseNameNode = GNode.create("Type",
                                  GNode.create("InstantiatedType",
                                    GNode.create("TypeInstantiation", baseName,
                                      GNode.create("TypeArguments", typeNode))),
                                  null);

      tbody.add(comment(makeExtendedClassDecl(tc.getString(0),
                                              baseNameNode, classBody),
                        "Implementation of constructor '"+ tc.getString(0) +
                        "' in variant '"+ baseName + "'."));
    }
    
    final String typeName = baseName + "<T extends Tuple>";
    final Node vNode = GNode.create("Type",
                         GNode.create("InstantiatedType",
                           GNode.create("TypeInstantiation", "Variant",
                             GNode.create("TypeArguments",
                               GNode.create("Type",
                                 GNode.create("QualifiedIdentifier", "T"),
                                 null)))),
                         null);
 
    tbody.add(comment(makeExtendedClassDecl2(typeName, vNode, baseBody),
                      "Superclass of all constructors in variant '" +
                      baseName + "'."));
  } 

  /**
   * Create an extends class declaration.
   *
   * @param n The name of the class.
   * @param b The base name node (the class that is extended).
   * @param classBody The class body.
   */
  private Node makeExtendedClassDecl(String n, Node b, Node classBody) {
    Node foo = factory.extendsDecl();
    foo.set(1, n);
    foo.getGeneric(3).set(0, b);
    foo.set(5, classBody);
    return foo;
  }
  
  /**
   * Create an abstract extends class declaration.
   *
   * @param n The name of the class.
   * @param b The base name node (the class that is extended).
   * @param classBody The class body.
   */
  private Node makeExtendedClassDecl2(String n, Node b, Node classBody) {
    Node foo = factory.extendsDecl2();
    foo.set(1, n);
    foo.getGeneric(3).set(0, b);
    foo.set(5, classBody);
    return foo;
  }

  /**
   * Create an implements class declaration.
   *
   * @param n The name of the class.
   * @param b The base name (the interface that is implemented).
   * @param classBody The class body.
   */
  private Node makeImplementedClassDecl(String n, String b, Node classBody) {
    Node foo = factory.implementsDecl();
    foo.set(1, n);
    foo.getGeneric(4).set(0, toType(b));
    foo.set(5, classBody);
    return foo;
  }
  
  /**
   * Process a record type definition.
   *
   * @param n The type information.
   */
  public void visitRecordDeclaration(GNode n) {
    String name = (String)n.getProperty("name");
    Object t = n.getProperty(TYPE);
    
    List<Node> fieldTypeNodes = mapper.getFieldTypeNodes(t);
    List<String> fieldNames = mapper.getFieldNames(t);
    Node classBody = GNode.create("ClassBody");
    StringBuilder toString = new StringBuilder("\"{\"");
    
    //ADD equals method for type record
    Node equals = null;
    Node eqBlock = GNode.create("Block");
    
    if("type".equals(name)) {
      equals = createTypeRecordEquals();
    } else {
      equals = factory.equalsMethod();
      equals.set(7, eqBlock);
      eqBlock.add(toIfStatement(factory.jnot(toInstanceOf(toIdentifier("o"),
        toType(name))), factory.ret(toLiteral("BooleanLiteral", "true"))));
      eqBlock.add(makeVarDec("r", name, factory.cast(toIdentifier("o"))));      
    }
    
    Node constrblk = GNode.create("Block");
    Node constr  = GNode.create("ConstructorDeclaration", pmod, null, name,
      GNode.create("FormalParameters"), null, constrblk);

    Node fps = constr.getGeneric(3);

    for (int i = 0; i < fieldNames.size(); i++) {
      String fname = fieldNames.get(i);
      Node ftype = fieldTypeNodes.get(i);
      toString.append(" + (null == "+fname+ " ? \"?\" : " +fname
                      + ".toString())");

      fps.add(GNode.create("FormalParameter", null, ftype, 
                           null, fname, null));
      constrblk.add(factory.assign(factory.thisExpr(fname), 
                                     toIdentifier(fname)));
       
      if (i < fieldNames.size() - 1) toString.append(" + \",\" ");
       
      classBody.add(factory.publicFieldDecl(ftype, fname));

      eqBlock.add(toIfStatement(
        factory.jnot(factory.jequals(toIdentifier(fname),
          factory.fieldExpression(toIdentifier("r"),
            fname))),
            factory.ret(toLiteral("BooleanLiteral", "false"))));
    }
    eqBlock.add(factory.ret(toLiteral("BooleanLiteral", "true")));

    toString.append(" + " + "\"}\"");
    
    //create the toString method for this record
    Node toStringNode = factory.toStringMethod();
    toStringNode.getGeneric(7).set(0, 
      factory.ret(toLiteral("StringLiteral",toString.toString())));
      
    classBody.add(constr);
    classBody.add(equals);
    classBody.add(toStringNode);
    
    tbody.add(comment(makeImplementedClassDecl(name, "Record", classBody),
                      "Implementation of record '" + name + "'."));
  }  
  
  /**
   * Create a new named tuple.
   *
   * @param n The tuple constructor node.
   * @return The java equivalent.
   */
  public Node visitTupleConstructor(GNode n) {
    Node args = GNode.create("Arguments");  

    if (n.hasProperty(INANALYZE)) {
      for (int j = 1; j < n.size(); j++) {
        n.getGeneric(j).setProperty(INANALYZE, Boolean.TRUE);
      }
    } 

    List<LetBinding> bl = null;
    List<String> upVars = getUpVariables(n);
    Node ret; 

    if (n.getProperty(TYPE)  != null && 
        gnodeType.equals(mapper.toTypeNode(n.getProperty(TYPE),false)) || 
        nodeType.equals(mapper.toTypeNode(n.getProperty(TYPE), false))) {      
      List<Node> args2 = new ArrayList<Node>(n.size());
      args2.add(toLiteral("StringLiteral", "\"" + n.getString(0) + "\"" ));
                
      for (int i = 1; i < n.size(); i++) {
        Node no = n.getGeneric(i);
        if (null != upVars) no.setProperty(UPVARS, upVars);
        Node expr = (Node)dispatch(no);
        List<LetBinding> l = getBindings(expr);
        List<String> vars = extractVariables(l);
        upVars = groupList(upVars, vars);
        bl = groupList(bl,l); 
        args2.add(expr);
      }
      
      ret = factory.gnodeCreate(args2);
      if (null != bl) ret.setProperty(LETBINDINGS, bl);
      return ret;
    }
    
    for (int i = 1; i < n.size(); i++) {
      Node no = n.getGeneric(i);
      if (null != upVars) no.setProperty(UPVARS, upVars);
      Node expr = (Node)dispatch(no);
      List<LetBinding> l = getBindings(expr);
      List<String> vars = extractVariables(l);
      upVars = groupList(upVars, vars);
      bl = groupList(bl,l); 
      args.add(expr);
    }

    ret = toNewExpression2(mapper.toTypeNode(n.getString(0), false), 
                            args,null);
    if (null != bl) ret.setProperty(LETBINDINGS, bl);
    return ret;
  }
  
  /**
   * Create an instanceof node.
   *
   * @param n1 The object node.
   * @param n2 The type node.
   * @return The instanceof expression
   */
  private Node toInstanceOf(Node n1, Node n2) {
    return GNode.create("InstanceOfExpression", n1, n2);
  }

  /**
   * Process a LowerID node.
   *
   * @return The primary identifier corresponding to this node.
   */
  public Node visitLowerID(GNode n) {
    final String newName = Primitives.convertName(n.getString(0));
    if (Primitives.isPrimitive(newName)) {
      if ("nonce".equals(newName)) {
        return factory.apply2(toIdentifier("Primitives." + newName));
      }
      return toIdentifier("Primitives." + newName);
    }    
    else return toIdentifier(newName);
  }
  
  /**
   * Process a variable.
   *
   * @param n The variable node.
   */
  public void visitVariable(GNode n) {
    String name = n.getString(0);
    //make bindings for this variable if necessary
    Object type = n.getProperty(TYPE);

    final Node typeNode = mapper.toTypeNode(type, false);     
    
    String rhs = (String)n.getProperty(RHS);
    
    if (table.current().isDefinedLocally(
        SymbolTable.toNameSpace(name, "value"))) {
      
      if (nodeType.equals(typeNode) || gnodeType.equals(typeNode)) {
        ((GNode)n.getProperty(BINDINGS)).add(makeVarDec2(name, typeNode,
                             factory.gnodeCast(toIdentifier(rhs))));
      } else {
        // fix cast here
        if (mapper.hasTypeVariables(type)) {
          ((GNode)n.getProperty(BINDINGS)).add(makeVarDec2(name, typeNode,
                               toIdentifier(rhs)));
        } else {
          ((GNode)n.getProperty(BINDINGS)).add(makeVarDec2(name, typeNode,
                               factory.cast(toIdentifier(rhs))));
        }                    
      }        
    } else {
      Node cond = factory.jequals(toIdentifier(rhs),toIdentifier(name));
      if (conditions == null) {
        conditions = cond;
      } else {
        conditions = factory.jand(conditions, cond);
      }
    }
  }
  
  /**
   * Transform a record expression. 
   *
   * @param n The record expression node.
   * @return A java equivalent.
   */
  public Node visitRecordExpression(GNode n) {
    
    boolean bottomWith = (null != n.getGeneric(0)) && 
      n.getGeneric(0).getGeneric(0).hasName("Bottom"); 

    if (n.hasProperty(INANALYZE)) {
      for (int j = 0; j < n.size(); j++) {
        n.getGeneric(j).setProperty(INANALYZE, Boolean.TRUE);
      }
    }  

    // Check the first field assign, field name and replaceType variable
    if (replaceType && "type".equals(n.getGeneric(1).getString(0))) {
      return (Node)dispatch(n.getGeneric(1).getGeneric(1));
    }
    
    Object rt = n.getProperty(TYPE);
       
    List<String> fieldNames = mapper.getFieldNames(rt);
    Node firstNode = n.getGeneric(0);

    Node args = GNode.create("Arguments");
    List<LetBinding> bl = null;
    List<String> upVars = getUpVariables(n);

    for (String s : fieldNames) {
      boolean found = false;
      for (int i = 1; i < n.size(); i++) {
        if (s.equals(n.getGeneric(i).getString(0))) {
          found = true;
          Node no = n.getGeneric(i).getNode(1);
          if (null != upVars) no.setProperty(UPVARS, upVars);
          Node expr = (Node)dispatch(no);
          List<LetBinding> l = getBindings(expr);
          List<String> vars = extractVariables(l);
          upVars = groupList(upVars, vars);
          bl = groupList(bl,l); 
          args.add(expr);
        }
      }
      if (!found && ("WithExpression".equals(firstNode.getName()))) {
        if (!bottomWith) {
          // expression with
          //firstNode.getGeneric(0).setProperty(NEWLET, Boolean.TRUE);
          Node letNode = (Node)dispatch(firstNode.getGeneric(0));
          letNode = checkToLet(letNode, firstNode.getGeneric(0).getProperty(TYPE)); 
          args.add(factory.fieldExpression(letNode, s)); 
        } else {
          args.add(GNode.create("NullLiteral"));
        }
      } else if (!found) {
        assert found : "cannot determine field value";
      }
    }
    Node ret = toNewExpression2(mapper.toTypeNode(rt,false), args, null);
    if (null != bl) ret.setProperty(LETBINDINGS, bl);
    return ret;
  }
  
  /**
   * Transform a field assignment.
   *
   * @param n The field assignment node.
   * @return The java assignment expression.
   */
  public Node visitFieldAssign(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(1).setProperty(INANALYZE, Boolean.TRUE);
    } 
    if (!optimizeLet) { 
      return factory.assignField(toIdentifier(n.getString(0)),
                               (Node)dispatch(n.getNode(1)));
    } else {
      passVariables(n, n.getGeneric(1));
      Node expr = (Node)dispatch(n.getNode(1));
      Node ret = factory.assignField(toIdentifier(n.getString(0)), expr);
      passBindings(expr, ret);
      return ret;
    }
  }

  /**
   * Process a let expression.
   *
   * @param n The let expression node.
   * @return An anonymous Let instance
   */
  public Node visitLetExpression(GNode n) {
    
    if (runtime.test("optimizeFoldLet")) {
      new LetFolder().collapseLet(n, table);      
    }
    
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(1).setProperty(INANALYZE, Boolean.TRUE);
    }  

    final String scopename = (String)n.getProperty("enterScope");

    boolean didEnter = false;
    if (n.hasProperty("enterScope")) {     
      if (!table.current().getName().equals(scopename)) {
        enterScope(scopename);
        didEnter = true;
      }
    }
    
    Node bindings = n.getGeneric(0);
    Node value = n.getGeneric(1);
    Object resultType = n.getProperty(TYPE);
    int size = bindings.size();
    Node block = null;
    Node res = null; 
    
    Object t = value.getProperty(TYPE);

    if (!optimizeLet) {
      res = (GNode)dispatch(value);
      if ("Block".equals(res.getName())) {
        block = GNode.create("Block", res);
      } else {
        // fix cast here
        if (mapper.hasTypeVariables(t)) {
          block = GNode.create("Block", factory.ret(res));
        } else {
          block = GNode.create("Block", factory.ret(factory.cast(res)));
        }
      }
 
      Node letclass = GNode.create("ClassBody");
      letclass = GNode.ensureVariable((GNode)letclass);
      GNode letbody = GNode.create("Block");
    
      for (int i = 0; i < size; i++) {
        Node binding = bindings.getGeneric(i);
        Node left = binding.getGeneric(0);
        Node right = binding.getGeneric(1);

        if (n.hasProperty(INANALYZE)) right.setProperty(INANALYZE, Boolean.TRUE); 
     
        String bname;
        Object btype;
  
        right = (Node)dispatch(right);
  
        if (left.hasName("Variable")) {
          bname = left.getString(0);
          btype = left.getProperty(TYPE);
          letclass.add(makeVarDec2(bname, mapper.toTypeNode(btype, false) , null));
          // fix cast here
          if (mapper.hasTypeVariables(btype)) {
            letbody.add(factory.assign(toIdentifier(bname), right));
          } else {
            letbody.add(factory.assign(toIdentifier(bname), factory.cast(right)));
          }
        
        } else if ("TypedPattern".equals(left.getName()) &&
                   "Variable".equals(left.getGeneric(0).getName())) {
          bname = left.getGeneric(0).getString(0);        
          btype = left.getProperty(TYPE);
          letclass.add(makeVarDec2(bname, mapper.toTypeNode(btype, false), null));
          // fix cast here
          if (mapper.hasTypeVariables(btype)) {
            letbody.add(factory.assign(toIdentifier(bname),right));
          } else {
            letbody.add(factory.assign(toIdentifier(bname),factory.cast(right)));
          }
        
        } else {
          if (right.hasName("ConditionalExpression")) {
            letbody.add(factory.discard(right));
          } else {
            letbody.add(factory.expressionStmnt(right));
          }
        }
      }       

      if (letbody.size() > 0) {
        letclass.add(letbody);
      }
    
      letclass.add(GNode.create("MethodDeclaration",
        toModifiers("public"),null, mapper.toTypeNode(resultType, false),
        "apply", GNode.create("FormalParameters"), null, null, block));
    
      Node let = factory.letExpression(mapper.toTypeNode(resultType, false));
      let.getGeneric(0).set(4, letclass);

      if (didEnter) exitScope(scopename);
      return let;

    } else { // optimizing Let
      List<String> upVars = getUpVariables(n);
      List<LetBinding> bl = null;

      // Get variables in let bindings
      List<String> vars = new ArrayList<String>();
      for (int i = 0; i < size; i++) {
        Node binding = bindings.getGeneric(i);
        Node left = binding.getGeneric(0);
        if (left.hasName("Variable")) {
          vars.add(left.getString(0));          
        
        } else if ("TypedPattern".equals(left.getName()) &&
                   "Variable".equals(left.getGeneric(0).getName())) {
          vars.add(left.getGeneric(0).getString(0));
        }  
      }
      // check for redefinitions
      boolean check = false;
      for (String v : vars) {
        if (null != upVars && upVars.contains(v)) {
          check = true;
          continue;
        }
      }

      if (check) upVars = vars;
      else upVars = groupList(upVars, vars);

      for (int i = 0; i < size; i++) {
        Node binding = bindings.getGeneric(i);
        Node left = binding.getGeneric(0);
        Node right = binding.getGeneric(1);
        String name;
        Object type;
        if (left.hasName("Variable")) {
          name = left.getString(0);
          type = left.getProperty(TYPE);        
        } else if ("TypedPattern".equals(left.getName()) &&
                   "Variable".equals(left.getGeneric(0).getName())) {
          name = left.getGeneric(0).getString(0);
          type = left.getProperty(TYPE);        
        } else {
          name = spareVar;
          type = right.getProperty(TYPE);        
        } 
        // process the value of a binding
        right.setProperty(UPVARS, upVars);
        Node expr = (Node)dispatch(right);
        List<LetBinding> l = getBindings(expr);
        List<String> vs = extractVariables(l);
        bl = groupList(bl,l);
        if (null == bl) {
          bl = new ArrayList<LetBinding>();
          bl.add(new LetBinding(name, type, mapper.toTypeNode(type, false), expr));
        } else {
          bl.add(new LetBinding(name, type, mapper.toTypeNode(type, false), expr));
        }
        upVars = groupList(upVars, vs);
      }
      
      // Process the expression
      value.setProperty(UPVARS, upVars);
      res = (Node)dispatch(value);
      List<LetBinding> l = getBindings(res);
      bl = groupList(bl,l);
      Node let = null; 
      if (check || n.hasProperty(NEWLET)) {
        let = convertToLet(res, bl, t);
      } else {
        res.setProperty(LETBINDINGS, bl);
        let = res;
      } 
      
      if (didEnter) exitScope(scopename);
      return let;
    }    
  }

  /**
   * Check and convert an expression to let expression if it contains bindings.
   *
   * @param n The expression node.
   * @param retType The type of that expression.
   * @return the converted node.
   */
  private Node checkToLet(Node n, Object retType) {
    List<LetBinding> bl = getBindings(n);
    if (null == bl || bl.isEmpty()) return n;

    Node block = null;
    if ("Block".equals(n.getName())) {
      block = GNode.create("Block", n);
    } else {
      // fix cast here
      if (mapper.hasTypeVariables(retType)) {
        block = GNode.create("Block", factory.ret(n));
      } else {
        block = GNode.create("Block", factory.ret(factory.cast(n)));
      }
    }
 
    Node letclass = GNode.create("ClassBody");
    letclass = GNode.ensureVariable((GNode)letclass);
    GNode letbody = GNode.create("Block");

    for (LetBinding bind : bl) {
      if (!bind.name.equals(spareVar)) {
        letclass.add(makeVarDec2(bind.name, bind.type , null));
      }
      if (mapper.hasTypeVariables(bind.typeObject)) {
        letbody.add(factory.assign(toIdentifier(bind.name), bind.value));
      } else {
        letbody.add(factory.assign(toIdentifier(bind.name), 
                                   factory.cast(bind.value)));
      }
    }

    if (letbody.size() > 0) {
      letclass.add(letbody);
    }
    
    letclass.add(GNode.create("MethodDeclaration",
      toModifiers("public"),null, mapper.toTypeNode(retType, false),
      "apply", GNode.create("FormalParameters"), null, null, block));
    
    Node let = factory.letExpression(mapper.toTypeNode(retType, false));
    let.getGeneric(0).set(4, letclass);
    return let;    
  }

  /**
   * Convert an expression to let expression if it contains bindings.
   *
   * @param n The expression node.
   * @param bl The list of variable bindings.
   * @param retType The type of that expression.
   * @return the converted node.
   */
  private Node convertToLet(Node n, List<LetBinding> bl, Object retType) {
    Node block = null;
    if ("Block".equals(n.getName())) {
      block = GNode.create("Block", n);
    } else {
      // fix cast here
      if (mapper.hasTypeVariables(retType)) {
        block = GNode.create("Block", factory.ret(n));
      } else {
        block = GNode.create("Block", factory.ret(factory.cast(n)));
      }
    }
 
    Node letclass = GNode.create("ClassBody");
    letclass = GNode.ensureVariable((GNode)letclass);
    GNode letbody = GNode.create("Block");

    for (LetBinding bind : bl) {
      if (!bind.name.equals(spareVar)) {
        letclass.add(makeVarDec2(bind.name, bind.type , null));
      }
      if (mapper.hasTypeVariables(bind.typeObject)) {
        letbody.add(factory.assign(toIdentifier(bind.name), bind.value));
      } else {
        letbody.add(factory.assign(toIdentifier(bind.name), 
                                   factory.cast(bind.value)));
      }
    }

    if (letbody.size() > 0) {
      letclass.add(letbody);
    }
    
    letclass.add(GNode.create("MethodDeclaration",
      toModifiers("public"),null, mapper.toTypeNode(retType, false),
      "apply", GNode.create("FormalParameters"), null, null, block));
    
    Node let = factory.letExpression(mapper.toTypeNode(retType, false));
    let.getGeneric(0).set(4, letclass);
    return let;    
  }
 
  /**
   * Process a Patterns node. 
   *
   * @return The patterns node corresponding to this node.
   */
  public Node visitPatterns(GNode n) {
    Node pat = n.getGeneric(0);
   
    if (n.hasProperty(TOP)) pat.setProperty(TOP, null);

    if (isLiteral(pat)) {
      return factory.jequals((Node)dispatch(pat), 
                             toIdentifier((String)n.getProperty(MATCHARG)));
    } else {
      pat.setProperty(MATCHARG, n.getProperty(MATCHARG));
      pat.setProperty(BINDINGS, n.getProperty(BINDINGS));
      pat.setProperty(RHS, n.getProperty(RHS));
      return (GNode)dispatch(pat);
    }
  }  
  
  /**
   * Test if a node is a literal.
   *
   * @param n The node to test.
   * @return True if literal false otherwise.
   */
  private boolean isLiteral(Node n) {
    if (null == n) {
      runtime.error("Calling isLiteral on null node", n);
    } else {
      String name = n.getName();
      return ("StringLiteral".equals(name) || "BooleanLiteral".equals(name) ||
              "FloatingLiteral".equals(name) || "IntegerLiteral".equals(name));
    }
      return false;
   }  

  /**
   * Process an additive expression.
   *
   * @param n The additive expression node.
   * @return The java additive expression.
   */
  public Node visitAdditiveExpression(GNode n) {
    String op = n.getString(1);
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(2).setProperty(INANALYZE, Boolean.TRUE);
    }  

    List<String> upVars = getUpVariables(n);
    if (null != upVars) n.getGeneric(0).setProperty(UPVARS, upVars); 
    Node left = (Node)dispatch(n.getGeneric(0));
    List<LetBinding> bl1 = getBindings(left);
    List<String> vars = extractVariables(bl1);
    upVars = groupList(upVars,vars);

    if (null != upVars) n.getGeneric(2).setProperty(UPVARS, upVars);
    Node right = (Node)dispatch(n.getGeneric(2));
    List<LetBinding> bl2 = getBindings(right);
    bl1 = groupList(bl1, bl2);
    Node ret;    
    
    if ("+".equals(op))  ret = factory.addInt(left, right);
    else if ("-".equals(op))  ret = factory.subtractInt(left, right);
    else if ("+.".equals(op)) ret = factory.addFloat64(left, right);
    else ret = factory.subtractFloat64(left, right);
    if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
    return ret;
  }

  /**
   * Process a concatenation expression.
   *
   * @param n The concatenation expression node.
   * @return The java equivalent.
   */
  public Node visitConcatenationExpression(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(2).setProperty(INANALYZE, Boolean.TRUE);
    }  

    List<String> upVars = getUpVariables(n);
    if (null != upVars) n.getGeneric(0).setProperty(UPVARS, upVars); 
    Node left = (Node)dispatch(n.getGeneric(0));
    List<LetBinding> bl1 = getBindings(left);
    List<String> vars = extractVariables(bl1);
    upVars = groupList(upVars,vars);

    if (null != upVars) n.getGeneric(2).setProperty(UPVARS, upVars);
    Node right = (Node)dispatch(n.getGeneric(2));
    List<LetBinding> bl2 = getBindings(right);
    bl1 = groupList(bl1, bl2);
    Node ret;        
    
    if ("^".equals(n.getString(1))) ret = factory.concatStrings(left, right);
    else ret = factory.concatLists(left, right);

    if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
    return ret;
  }

  /**
   * Process a multiplicative expression.
   *
   * @param n The multiplicative expression node.
   * @return The java multiplicatiee expression.
   */
  public Node visitMultiplicativeExpression(GNode n) {
    String op = n.getString(1);
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(2).setProperty(INANALYZE, Boolean.TRUE);
    }  

    List<String> upVars = getUpVariables(n);
    if (null != upVars) n.getGeneric(0).setProperty(UPVARS, upVars); 
    Node left = (Node)dispatch(n.getGeneric(0));
    List<LetBinding> bl1 = getBindings(left);
    List<String> vars = extractVariables(bl1);
    upVars = groupList(upVars,vars);

    if (null != upVars) n.getGeneric(2).setProperty(UPVARS, upVars);
    Node right = (Node)dispatch(n.getGeneric(2));
    List<LetBinding> bl2 = getBindings(right);
    bl1 = groupList(bl1, bl2);
    Node ret;            
    
    if ("*".equals(op))  ret = factory.multiplyInt(left, right);
    else if ("/".equals(op))  ret = factory.divideInt(left, right);
    else if ("%".equals(op))  ret = factory.modInt(left, right);
    else if ("*.".equals(op)) ret = factory.multiplyFloat64(left, right);
    else ret = factory.divideFloat64(left, right);

    if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
    return ret;
  }

  /**
   * Process a relational expression.
   *
   * @param n The relational expression node.
   * @return The java relational expression
   */
  public Node visitRelationalExpression(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(2).setProperty(INANALYZE, Boolean.TRUE);
    }  

    List<String> upVars = getUpVariables(n);
    if (null != upVars) n.getGeneric(0).setProperty(UPVARS, upVars); 
    Node left = (Node)dispatch(n.getGeneric(0));
    List<LetBinding> bl1 = getBindings(left);
    List<String> vars = extractVariables(bl1);
    upVars = groupList(upVars,vars);

    if (null != upVars) n.getGeneric(2).setProperty(UPVARS, upVars);
    Node right = (Node)dispatch(n.getGeneric(2));
    List<LetBinding> bl2 = getBindings(right);
    bl1 = groupList(bl1, bl2);
    Node ret;                
  
    String op = n.getString(1);
    String internal = null;
    
    if ("<".equals(op))   internal = "lessInt";
    if ("<=".equals(op))  internal = "lessEqualInt";
    if (">".equals(op))   internal = "greaterInt";
    if (">=".equals(op))  internal = "greaterEqualInt";
    if ("<.".equals(op))  internal = "lessFloat64";
    if ("<=.".equals(op)) internal = "lessEqualFloat64";
    if (">.".equals(op))  internal = "greaterFloat64";
    if (">=.".equals(op)) internal = "greaterEqualFloat64";
    assert null != internal : "undefined relational operator " + op;
     
    ret = 
      factory.relationalExpr(toIdentifier("Primitives." + internal),left,right);
    if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
    return ret;
  }

  /**
   * Process a logical or  expression.
   *
   * @param n The logical or expression node.
   * @return The java expression.
   */
  public Node visitLogicalOrExpression(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(1).setProperty(INANALYZE, Boolean.TRUE);
    }  

    List<String> upVars = getUpVariables(n);
    if (null != upVars) n.getGeneric(0).setProperty(UPVARS, upVars); 
    Node left = (Node)dispatch(n.getGeneric(0));
    List<LetBinding> bl1 = getBindings(left);
    List<String> vars = extractVariables(bl1);
    upVars = groupList(upVars,vars);

    if (null != upVars) n.getGeneric(1).setProperty(UPVARS, upVars);
    Node right = (Node)dispatch(n.getGeneric(1));
    List<LetBinding> bl2 = getBindings(right);
    bl1 = groupList(bl1, bl2);
    Node ret = factory.or(left, right);
    if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
    return ret;
  }
  
  /** 
   * Process a logical and expression.
   *
   * @param n The logical and expression node.
   * @return The java expression.
   */
  public Node visitLogicalAndExpression(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(1).setProperty(INANALYZE, Boolean.TRUE);
    }  

    List<String> upVars = getUpVariables(n);
    if (null != upVars) n.getGeneric(0).setProperty(UPVARS, upVars); 
    Node left = (Node)dispatch(n.getGeneric(0));
    List<LetBinding> bl1 = getBindings(left);
    List<String> vars = extractVariables(bl1);
    upVars = groupList(upVars,vars);

    if (null != upVars) n.getGeneric(1).setProperty(UPVARS, upVars);
    Node right = (Node)dispatch(n.getGeneric(1));
    List<LetBinding> bl2 = getBindings(right);
    bl1 = groupList(bl1, bl2);
    Node ret = factory.and(left, right);
    if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
    return ret;
  }
  
  /**
   * Process a logical negation expression.
   *
   * @param n The logical negation expression node.
   * @return n The java equivalent.
   */  
  public Node visitLogicalNegationExpression(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);      
    }  
    passVariables(n, n.getGeneric(0));
    Node expr = (Node)dispatch(n.getGeneric(0));
    Node ret = factory.not(expr);
    passBindings(expr, ret);
    return ret;
  }
  
  /**
   * Process an equality expression.
   *
   * @param n The equality expression node.
   * @return The java equivalent.
   */
  public Node visitEqualityExpression(GNode n) {
    String op = n.getString(1);
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(2).setProperty(INANALYZE, Boolean.TRUE);
    }  

    List<String> upVars = getUpVariables(n);
    if (null != upVars) n.getGeneric(0).setProperty(UPVARS, upVars); 
    Node left = (Node)dispatch(n.getGeneric(0));
    List<LetBinding> bl1 = getBindings(left);
    List<String> vars = extractVariables(bl1);
    upVars = groupList(upVars,vars);

    if (null != upVars) n.getGeneric(2).setProperty(UPVARS, upVars);
    Node right = (Node)dispatch(n.getGeneric(2));
    List<LetBinding> bl2 = getBindings(right);
    bl1 = groupList(bl1, bl2);
    Node ret;            
    
    if ("=".equals(op)) {
      if ("Bottom".equals(n.getGeneric(0).getName())) {    
        ret = factory.equalsBottom(right);
        if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
        return ret;      
      } else if ("Bottom".equals(n.getGeneric(2).getName())) {
        ret = factory.equalsBottom(left);
        if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
        return ret;    
      }
    } else {
       if ("Bottom".equals(n.getGeneric(0).getName())) {    
        ret = factory.notEqualsBottom(right);
        if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
        return ret;      
      } else if ("Bottom".equals(n.getGeneric(2).getName())) {
        ret = factory.notEqualsBottom(left);
        if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
        return ret;    
      }
    }

    ret = 
      ("=".equals(n.getString(1))) ? factory.equal(left, right)
                                   : factory.not(factory.equal(left, right));
    if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
    return ret;
  }

  /**
   * Process an attribute declaration.
   *
   * @param n the attribute declaration node to process.
   */
  public void visitAttributeDefinition(GNode n) {
    Node typeNode = n.getGeneric(1);
    if ("ConstraintType".equals(typeNode.getName())) {
      typeNode = GNode.create("UserDefinedType","Node");
    }
    attributeList.add(new Attribute(n.getString(0), typeNode)); 
  }
  
  /**
   * Process an equal attribute definition.
   *
   * @param n the equal attribute definition node to process.
   */
  public void visitEqualAttributeDefinition(GNode n) {
    Node typeNode = n.getGeneric(1);
    if ("ConstraintType".equals(typeNode.getName())) {
      typeNode = GNode.create("UserDefinedType","Node");
    }
    eqAttributeList.add(new Attribute(n.getString(0), typeNode));     
  }

  /**
   * Process raw_type definition to create "type" record.
   *
   * @return The node of generated code for "type" record definition.
   */
  public Node processRawTypeDefinition() {
    //Create type
    Node typeInfo = 
      GNode.create("RecordDeclaration",GNode.create("FieldType", "type",
        GNode.create("UserDefinedType", "raw_type<?>")));

    typeInfo = GNode.ensureVariable((GNode)typeInfo);
      
    typeInfo.setProperty(TYPE, table.current().lookup("type(type)"));
    
    //Create equal attributes
    for (Attribute att : eqAttributeList) {
      typeInfo.add(GNode.create("FieldType", att.name, att.type));
    } 
   
    //Create attributes
    for (Attribute att : attributeList) {
      typeInfo.add(GNode.create("FieldType", att.name, att.type));
    } 
    
    return GNode.create("TypeDefinition", null, "type", typeInfo);   
  }
  
  /**
   * Process an equality definition.
   *
   * @param n The equality definition node.
   */
  public void visitEqualityDefinition(GNode n) {
    // process equal structure 
    for (int index = 1; index < n.size(); index ++) {
      boolean seenLowerID = false;
      Node child = (GNode)n.get(index);
      ArrayList<Integer> list = new ArrayList<Integer>();

      for (int i = 1; i < child.size(); i++) {
        if (!("WildCard".equals(child.getGeneric(i).getName()))) {
          list.add(i);
          seenLowerID = true;          
        }         
      }
      
      if (!seenLowerID) {
        throw new AssertionError("At least one identifier required");        
      } 
      equalities.add(new Equality((child.getGeneric(0)).getString(0),list));
    } 
  } 
  
  /**
   * Get node name from a pattern
   *
   * @param n The pattern node
   */
  protected String getNodeName(Node n) {
    return ("TypeConstructorPattern".equals(n.getName())) 
      ? n.getString(0) : getNodeName(n.getGeneric(0));
  }

  /**
   * Process scope definition
   *
   * @param n The scope definition node
   */  
  public void visitScopeDefinition(GNode n) {   
    // Adding code to build the list of node names that need to process scope
    Node pat = n.getGeneric(0);
    for (int i = 0; i < pat.size(); i++) {
      Node patterns = pat.getGeneric(i).getGeneric(0);
      for (int j = 0; j < patterns.size(); j++) {
        processScopeNodes.add(getNodeName(patterns.getGeneric(j)));         
      }
    } 
  
    Node match = GNode.create("MatchExpression",
       GNode.create("LowerID","n"), n.getGeneric(0));
    match.setProperty("__arg_type", n.getProperty("__arg_type"));
    match.setProperty(TYPE, 
      mapper.getPatternMatchRightType(n.getProperty(TYPE)));
    
    seenScope = true;

    Node pmatch = n.getGeneric(0);
    pmatch.setProperty(MATCHARG, "n");

    @SuppressWarnings("unchecked")
    List<Node> nodes = (List<Node>)dispatch(pmatch);
    
    nodes.add(factory.ret(GNode.create("NullLiteral")));
   
    // Create typical AST
    Node top = GNode.create("ValueDefinition",
                 "getScope",
                 GNode.create("Parameters",
                   GNode.create("Parameter",
                     "n",
                     GNode.create("UserDefinedType",
                       "node")
                   ) 
                 ),
                 GNode.create("BooleanLiteral", "true")
               );
            
    top.setProperty(TYPE, table.current().lookup("value(getScope)"));
    top.setProperty("__isfunction", null);
       
    Node block = GNode.create("Block");

    for (Node node : nodes) block.add(node);
    
    Node scopefunc = (Node)dispatch(top);

    scopefunc.getGeneric(2).getGeneric(0).getGeneric(2).getGeneric(4).
      getGeneric(0).set(7, block);

    functionDefinitions.add(scopefunc);
    return;
  }
 
  /**
   * Process  namespace definition.
   *
   * @param n The name space definition node.
   */  
  public void visitNameSpaceDefinition(GNode n) {   
    seenNameSpace = true;       
    Node pat = GNode.create("PatternMatching");
    pat.setProperty(TYPE, n.getProperty(TYPE));
    pat.setProperty(MATCHARG, "n");

    // Create the pattern matching node
    for(int i = 0; i < n.size(); i++) {
      Node child = n.getGeneric(i);
     
      for(int j = 0; j < child.getGeneric(2).size(); j++) {        
        Node grand = child.getGeneric(2).getGeneric(j);
        Node pattern = GNode.create("PatternMatch");
        
        pattern.add(grand.getGeneric(0));
        // Create Typical tuple
        Node tup = GNode.create("TupleLiteral", grand.getGeneric(1),
          GNode.create("StringLiteral", "\"" + child.getString(0) + "\""), 
          GNode.create("StringLiteral", "\"" + child.getString(1) + "\""));
        tup.setProperty(TYPE, TypeMapper.nameTupleT);
        pattern.add(tup);
      
        pattern.setProperty("enterScope",grand.getProperty("enterScope"));
        
        Node thepatterns = pattern.getGeneric(0);
        
        for (int k = 0; k < thepatterns.size(); k++) {
        thepatterns.getGeneric(k).setProperty("enterScope", 
          pattern.getProperty("enterScope"));
        }
        pat.add(pattern);
      }      
    }

    pat.setProperty(MATCHARG, "n");
    Node match = 
      GNode.create("MatchExpression", GNode.create("LowerID","n"),pat);
    
    // create the value definition node
    Node top = GNode.create("ValueDefinition",
      "getNameSpace", GNode.create("Parameters", GNode.create("Parameter", "n",
       GNode.create("UserDefinedType", "node"))), match);
    top.setProperty("__isfunction", null);
    match.setProperty("__arg_type", table.current().lookup("type(node)"));
    match.setProperty(TYPE, TypeMapper.nameTupleT);
    top.setProperty("__arg_type", n.getProperty("__arg_type"));
   
    top.setProperty(TYPE, table.current().lookup("value(getNameSpace)"));
    functionDefinitions.add((Node)dispatch(top));
  }

  /**
   * Transform an error clause into a function call to either or warning.
   *
   * @param n The expression node.
   * @return The function call java node.
   */
  public Node visitErrorClause(GNode n) {
    Node loc = (Node)dispatch(n.getGeneric(2));
    if (null == loc) loc = nullNode;
    //n.getGeneric(1).setProperty(NEWLET, Boolean.TRUE);
    Node letNode = (Node)dispatch(n.getGeneric(1));
    letNode = checkToLet(letNode, n.getGeneric(1).getProperty(TYPE));
    return factory.errorClause(n.getGeneric(0).getString(0), letNode,loc);
  }

  /**
   * Transform an assert clause into a function call to assert.
   *
   * @param n The assert clause node.
   * @return The function call java node.
   */
  public Node visitAssertClause(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      if (null != n.getGeneric(1)) {
        n.getGeneric(1).setProperty(INANALYZE, Boolean.TRUE);
      }
    }
    //n.getGeneric(0).setProperty(NEWLET, Boolean.TRUE);
    //n.getGeneric(1).setProperty(NEWLET, Boolean.TRUE);

    Node exp0 = (Node)dispatch(n.getGeneric(0));
    exp0 = checkToLet(exp0, n.getGeneric(0).getProperty(TYPE)); 

    Node exp1 = (Node)dispatch(n.getGeneric(1));
    exp1 = checkToLet(exp1, n.getGeneric(1).getProperty(TYPE)); 

    return (null == n.getGeneric(1)) 
      ? GNode.create("PostfixExpression", toIdentifier("assertion"),
          GNode.create("Arguments", exp0))
      : GNode.create("PostfixExpression", toIdentifier("assertion"),
          GNode.create("Arguments", exp0, exp1));
  }

  /**
   * Transform an if expression into a java conditional expression.
   *
   * @param n The if expression node.
   * @return The java conditional expression.
   */
  public Node visitIfExpression(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(1).setProperty(INANALYZE, Boolean.TRUE);
    }  

    List<String> upVars = getUpVariables(n);
    if (null != upVars) n.getGeneric(0).setProperty(UPVARS, upVars); 
    Node left = (Node)dispatch(n.getGeneric(0));
    List<LetBinding> bl1 = getBindings(left);
        
    Node right = (Node)dispatch(n.getGeneric(1));
    right = checkToLet(right, n.getGeneric(1).getProperty(TYPE));
    Node ret = factory.ifExpression(left, right);
    if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
    return ret;
  }
  
  /**
   * Transform an if expression into a java conditional expression.
   *
   * @param n The if expression node.
   * @return The java conditional expression.
   */
  public Node visitIfElseExpression(GNode n) {
    if (n.hasProperty(INANALYZE)) {
      n.getGeneric(0).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(1).setProperty(INANALYZE, Boolean.TRUE);
      n.getGeneric(2).setProperty(INANALYZE, Boolean.TRUE);
    }

    List<String> upVars = getUpVariables(n);
    if (null != upVars) n.getGeneric(0).setProperty(UPVARS, upVars); 
    Node left = (Node)dispatch(n.getGeneric(0));
    List<LetBinding> bl1 = getBindings(left);
    

    Node middle = (Node)dispatch(n.getGeneric(1));
    middle = checkToLet(middle, n.getGeneric(1).getProperty(TYPE));

    Node right = (Node)dispatch(n.getGeneric(2));
    right = checkToLet(right, n.getGeneric(2).getProperty(TYPE));

    Node ret = factory.ifElseExpression(left, middle, right);
    if (null != bl1) ret.setProperty(LETBINDINGS, bl1);
    return ret;
  }
  
  /**
   * Process require expression.
   *
   * @param n the require expression node
   * @return The java code for the require expression.
   */  
  public Node visitRequireExpression(GNode n) {
    
    int nodeSize = n.size();
 
    // get the expression
    Node expr = n.getGeneric(nodeSize -1);
    passVariables(n, expr);

    if (n.hasProperty(INANALYZE)) expr.setProperty(INANALYZE, Boolean.TRUE);
 
    //dispatch this node to translate
    Node valExpr = (Node)dispatch(expr);

    final Node genericType;
    if (expr.hasProperty(TYPE)) {
      genericType = mapper.toTypeNode(expr.getProperty(TYPE), false);
    } else { 
      genericType = GNode.create("Type",
                      GNode.create("QualifiedIdentifier", "Object"),
                      null);
    }
    
    ArrayList<Node> list = new ArrayList<Node>();
    
    for (int id = 0; id < nodeSize - 1; id++) {
      Node no = n.getGeneric(id);
      list.add(no);
    }

    // Create require block
    List<Node> requireBlock = new ArrayList<Node>();

    /* A list of translated expressions */
    ArrayList<Node> listExpr = new ArrayList<Node>();
    
    // Add statement
    for (Node node: list) {      
      Node boolNode = node.getGeneric(0);
      //boolNode.setProperty(NEWLET, Boolean.TRUE);
      if (n.hasProperty(INANALYZE)) {
        boolNode.setProperty(INANALYZE, Boolean.TRUE);
      }
      Node boolExpr = (Node)dispatch(boolNode);
      boolExpr = checkToLet(boolExpr, boolNode.getProperty(TYPE));

      final String newVar = table.freshJavaId("var");

      requireBlock.add(factory.boolVar(newVar, boolExpr));
      
      listExpr.add(toIdentifier(newVar));

      Node tagNode = node.getGeneric(1);
      String tag = tagNode.getString(0);

      Node msgNode = node.getGeneric(2);
      Node msgExpr = (Node)dispatch(msgNode);
  
      Node atNode = node.getGeneric(3);
      Node atExpr;
      if (null == atNode) {
        atExpr = nullNode;
      } else {
        atExpr = (Node)dispatch(atNode);
      }  
    
      Node ifNode = factory.ifStatement3(toIdentifier(newVar), 
                                         GNode.create("StringLiteral",
                                           "\"" + tag + "\""), msgExpr,atExpr); 
      requireBlock.add(ifNode);
    }

    // check for returning null (bottom)    
    if (1 == listExpr.size()) {
      Node checkNode = factory.ifStatement4(listExpr.get(0));
      
      requireBlock.add(checkNode);
    } else {
      Node logicalOr = GNode.create("LogicalOrExpression");
      logicalOr.add(factory.isNull(listExpr.get(0)));
      logicalOr.add(factory.isNull(listExpr.get(1)));
      
      for (int i = 2; i < listExpr.size(); i++ ) {
        logicalOr = GNode.create("LogicalOrExpression", logicalOr, 
                                  factory.isNull(listExpr.get(i)));
      }
      
      Node checkNode = factory.ifStatement5(logicalOr);
      requireBlock.add(checkNode);
    }
    
    // Check to return expr

    if (1 == listExpr.size()) {
      Node checkNode;
      if(!"Block".equals(valExpr.getName())) {
        checkNode = factory.ifStatement(listExpr.get(0), factory.ret(valExpr)); 
      } else {
        checkNode = toIfStatement(listExpr.get(0), valExpr);
      }
      requireBlock.add(checkNode);
    } else {
      Node logicalAnd = GNode.create("LogicalAndExpression");
      logicalAnd.add(listExpr.get(0));
      logicalAnd.add(listExpr.get(1));
      
      for(int i = 2; i < listExpr.size(); i++) {
        logicalAnd = GNode.create("LogicalAndExpression", logicalAnd,
                                  listExpr.get(i));
      }
      Node checkNode;
      if (!"Block".equals(valExpr.getName())) {
        checkNode = factory.ifStatement(logicalAnd, factory.ret(valExpr));     
      } else {
        checkNode = toIfStatement(logicalAnd, valExpr);
      }
      requireBlock.add(checkNode);
    }
   
    // return null at the end
    requireBlock.add(factory.ret(nullNode));
    Node ret = factory.requireExpression(genericType, requireBlock);
    passBindings(valExpr, ret);
    return ret;
  }
  
  /** Store nodes that need to process scopes. */
  public void processScopeSpace() {
    Node block = GNode.create("Block");
    
    for (int i = 0; i < processScopeNodes.size(); i++) {
      Node add = factory.addScopeNode(toIdentifier(
                                      "\"" + processScopeNodes.get(i) + "\""));
      block.add(add);

    }   
  
    Node getNodes = factory.getScopeNodesMethod();
    getNodes.set(7, block);
    
    cbody.add(getNodes);

    // if scope definition is not used
    if(!seenScope) {
      cbody.add(factory.getScopeClass());
      cbody.add(factory.getScopeObject()); 
     
    } 
    if(!seenNameSpace) {
      throw new AssertionError("Name space must be defined");      
    }
  }
   
  /**
   * Create equals method for the record "type".
   * 
   * @return The equals method for the type record.
   */
  public Node createTypeRecordEquals() {

    Node block = GNode.create("Block");

    block.add(toIfStatement(factory.equality(toIdentifier("o"), nullNode),
      factory.ret(GNode.create("BooleanLiteral","false"))));
                        
    block.add(toIfStatement(GNode.create("LogicalNegationExpression",
      GNode.create("InstanceOfExpression", toIdentifier("o"), toType("type"))),
        factory.ret(toLiteral("BooleanLiteral","false"))) );
 
    block.add(factory.recordFieldEqual());
    
    block.add(factory.recordEqualReturn());

    block.add(factory.compareTypes());
    
    // Add equal attribute comparisons.
    for (Attribute att : eqAttributeList) {
      block.add(factory.compareAttributes(toIdentifier(att.name)));      
    }
    block.add(factory.ret(toIdentifier("res")));

    Node retNode = factory.equalsMethod();
    retNode.set(7, block);
    return retNode;
  }

  /**
   * Create equals method for a constructor that is in equality definition.
   *
   * @param variantName The name of the contructor.
   * @return The equals method for the variant.
   */
  public Node createVariantEqualsMethod(String variantName) {
    Equality constr = null;
    for (Equality e : equalities) {
      if (variantName.equals(e.name)) {
        constr = e;
        break;
      }
    }
    
    assert null != constr : "null value for constr";  
         
    Node block = GNode.create("Block");
    block.add(factory.ifStatement1(toIdentifier("o")));

    Node secondIf = factory.ifStatement2(toIdentifier("o"));
    secondIf.getGeneric(0).getGeneric(0).set(1, toType(variantName));
    block.add(secondIf);
    block.add(makeDec("other", variantName, GNode.create("CastExpression",
         toType(variantName), toIdentifier("o"))));
    block.add(makeDec("res", "boolean", toLiteral("BooleanLiteral","true")));
       
    for (Integer pos : constr.positions) {
      block.add(factory.compareMembers(
                  "getTuple().get" + pos + "()"));      
    }
    block.add(factory.ret(toIdentifier("res")));
    Node foo = factory.equalsMethod();
    foo.set(7, block);
    return foo;
  }

  /**
   * Augment a translated IfStatement of a pattern match to process scope.
   *
   * @param n The IfStatement node.
   * @param matchArg The argument of the match expression.
   * @param no The pattern match node.
   * @param bn The node to get bindings from (if exists).
   * @return The augmented IfStatement. 
   */
  public Node augmentIfStatement(Node n, String matchArg, GNode no, Node bn) {
    Node block = n.getGeneric(1);
    GNode pattern = no.getGeneric(0).getGeneric(0);
    List<Integer> indexList = getIndexList(pattern);

    int size = block.size();
    // Get the last return statement of the block
    Node retNode = block.getGeneric(size - 1);

    assert ("ReturnStatement" == retNode.getName()) : 
      "The last statement of the if block is not a return statement";

    // Update matching_nodes.
    block.set(size - 1, factory.matchingNodesAdd(toIdentifier(matchArg)));
    // Check if this node needs to process scope
    block.add(factory.processScope(toIdentifier(matchArg)));
    // Check if enter a new scope
    block.add(factory.checkEnterScope(toIdentifier(matchArg)));
    // Check if needed to process scope of the offsprings
    String nodeName = table.freshJavaId("nodeName");
    String listName = table.freshJavaId("listName");
    if (null != indexList && indexList.size() > 1) {
      block.add(factory.spOffspringList(listName));
      block.add(factory.spRunNode(nodeName, toIdentifier(matchArg)));
      int index;
      for (int ind = 0; ind < indexList.size() - 1; ind++){
        index = indexList.get(ind);
        block.add(factory.spGetGeneric(toIdentifier(nodeName),
          toLiteral("IntegerLiteral", "" + index)));
        block.add(factory.processScope(toIdentifier(nodeName)));
        block.add(factory.checkEnterScope(toIdentifier(nodeName)));
        block.add(factory.spOffspringListAdd(toIdentifier(listName), 
                                             toIdentifier(nodeName)));
      }
    }
    // Add bindings if exists
    List<LetBinding> bl = getBindings(bn);
      
    if (null != bl) {
      for (LetBinding bind : bl) {
        if (!bind.name.equals(spareVar)) {
          if (mapper.hasTypeVariables(bind.typeObject)) { 
            block.add(factory.fieldDecl2(bind.type, bind.name, bind.value));
          } else {
            block.add(factory.fieldDecl2(bind.type, bind.name, 
                      factory.cast(bind.value)));
          }
        } else {
          if (mapper.hasTypeVariables(bind.typeObject)) {
            block.add(factory.assign(toIdentifier(bind.name), bind.value));
          } else {
            block.add(factory.assign(toIdentifier(bind.name), 
                      factory.cast(bind.value)));
          }
        }
      }
    }

    // Store the return value
    String freshId = table.freshJavaId("retValue");
    block.add(factory.storeValue(freshId, retNode.getGeneric(0)));
    // Check to exit scope
    if (null != indexList && indexList.size() > 1) {
      block.add(factory.spForLoop(toIdentifier(listName)));
    }
    block.add(factory.checkExitScope(toIdentifier(matchArg)));
    // Update matching_nodes
    block.add(factory.matchingNodesRemove());
    // Annotate this node with the type
    if (no.hasProperty(ANNOTATE)) {
      block.add(factory.annotateType(toIdentifier(matchArg), 
                                     toIdentifier(freshId)));
    }
    // return
    Object t = mapper.getPatternMatchRightType(no.getProperty(TYPE));
    if (mapper.hasTypeVariables(t)) {
      block.add(factory.castReturn(toIdentifier(freshId)));
    } else {
      block.add(factory.castReturn(toIdentifier(freshId)));
    }
    return n;    
  } 

  /**
   * Add bindings to an if statement
   *
   * @param n The if statement node
   * @param no The node annotated with bindings
   * @return The if statement with bindings added
   */
  public Node addToIf(Node n, Node no) {
    Node block = n.getGeneric(1);
    int size = block.size();
    // Get the last return statement of the block
    Node retNode = block.getGeneric(size - 1);
    block.set(size - 1, null);

    List<LetBinding> bl = getBindings(no);

    if (null != bl) {
      for (LetBinding bind : bl) {
        if (!bind.name.equals(spareVar)) {
          if (mapper.hasTypeVariables(bind.typeObject)) { 
            block.add(factory.fieldDecl2(bind.type, bind.name, bind.value));
          } else {
            block.add(factory.fieldDecl2(bind.type, bind.name, 
                      factory.cast(bind.value)));
          }
        } else {
          if (mapper.hasTypeVariables(bind.typeObject)) {
            block.add(factory.assign(toIdentifier(bind.name), bind.value));
          } else {
            block.add(factory.assign(toIdentifier(bind.name), 
                      factory.cast(bind.value)));
          }
        }
      }
    }	
    block.add(retNode);
    return n; 
  } 

  /**
   * Get a list of indice that corresspond to nodes needed to process scope
   *
   * @param node The pattern node
   * @return The list of indice
   */
  public static List<Integer> getIndexList(GNode node){
    List<Integer> res;
    final String nodeName = node.getName();
    if ("WhenPattern".equals(nodeName) ||
        "AsPattern".equals(nodeName) ||
        "TypedPattern".equals(nodeName)) {
      return getIndexList(node.getGeneric(0));
      
    } else if ("TypeConstructorPattern".equals(nodeName)) {
      if (1 == node.size() || "WidlCard".equals(node.getGeneric(1).getName())) {
        return null;
      } else {
        Node paras = node.getGeneric(1);
        boolean hasBinding = false;

        for (int index = 0; index < paras.size(); index++) {
          res = getIndexList(paras.getGeneric(index));
          if (null != res && !res.isEmpty()) {
            if ("ConsPattern".equals(paras.getGeneric(index).getName())||
                "ListPattern".equals(paras.getGeneric(index).getName())) {
              int pos = res.remove(0);
              res.add(0, pos + index);
              return res;
            } else {
              res.add(0, index);
              return res;
            }
          } else if (null != res) {
            hasBinding = true;
          }          
        }
        if (hasBinding) {
          res = new ArrayList<Integer>();
          res.add(0);
          return res;
        } else return null;
      }  
    } else if ("ConsPattern".equals(nodeName)) {
      res = getIndexList(node.getGeneric(0));
      if (null == res) {
        res = getIndexList(node.getGeneric(1));
        if (null == res || res.isEmpty()) return res;
        else {
          int pos = res.remove(0);
          res.add(0, pos + 1);
          return res;
        }
      } else if (res.isEmpty()) {
        return res;        
      } else {
        res.add(0,0);
        return res;
      }

    } else if ("ListPattern".equals(nodeName)) {
      boolean hasBinding = false;
      for (int index = 0; index < node.size(); index++){
        res = getIndexList(node.getGeneric(index));
        if (null != res && !res.isEmpty()) {
          res.add(0, index);
          return res;
        } else if (null != res){
          hasBinding = true;
        }       
      }
      return hasBinding? new ArrayList<Integer>() : null;
    } else if ("Variable".equals(nodeName)) {
      return new ArrayList<Integer>();
    } else if ("WildCard".equals(nodeName) ||
               "BottomPattern".equals(nodeName)) {
      return null;
    } 
    return null;
  }

  /**
   * Process currying in function applications
   *
   * @param funcName The name of the function
   * @param args The node that contains all arguments
   * @param paramTypes The list of parameter type nodes of the function
   * @param retType The return type node of the function
   * @param funcType The whole function type
   * @return A node that is a new function
   */
  public Node makeCurry(String funcName, Node args, 
                        List<Node> paramTypes, Node retType, Object funcType) {
      
    //create the function name and parameterise it with the return type
    //and the argument types
    final int paraSize = paramTypes.size() - args.size();

    Node typeArgs = GNode.create("TypeArguments");
    typeArgs.add(retType);

    for (int i = args.size(); i < paramTypes.size(); i++) {
      typeArgs.add(paramTypes.get(i));
    }      
    
    Node functionTypeNode = GNode.create("Type",
                 GNode.create("InstantiatedType",
                   GNode.create("TypeInstantiation", "Function", null),
                   GNode.create("TypeInstantiation", "F" + paraSize, 
                                typeArgs)),
                 null);

    // Populate the formal parameters for the "apply" method
    Node formalParameters = GNode.create("FormalParameters", paraSize);
    // Names of formal parameters
    List<String> formalParams = new ArrayList<String>();
  
    for (int i = args.size(); i < paramTypes.size(); i++) {
      String newPara = table.freshJavaId("para");
      formalParams.add(newPara); 
      formalParameters.add(GNode.create("FormalParameter", fmod, 
        paramTypes.get(i), null, newPara, null));
    }
    
    // Create block for apply method
    Node block = GNode.create("Block");
    // Names of temporary variables
    List<String> tempVars = new ArrayList<String>();
    
    for (int i = 0; i < args.size(); i++) {
      // Assign to a temporary variable
      String tempVar = table.freshJavaId("var");
      tempVars.add(tempVar);
      block.add(factory.fieldDecl2(paramTypes.get(i),
                                   tempVar, args.getGeneric(i)));      
    }
  
    // Node of new arguments
    Node newArgs = GNode.create("Arguments");
    
    for (String s : tempVars) newArgs.add(toIdentifier(s));
    for (String s : formalParams) newArgs.add(toIdentifier(s));
    
    // Create return statement
    Node ret = factory.ret(factory.apply(toIdentifier(funcName),
                                         makeArgumentList(newArgs)));
    block.add(ret);

    // Create body of the class
    Node classBody = GNode.create("ClassBody", 
      GNode.create("MethodDeclaration", pmod, null,
      retType, "apply", formalParameters, null, null,
      block));
 
    return toNewExpression2(functionTypeNode,null,classBody);        
  }

  /** 
   * Get the index of a primitive instance in the list
   * 
   * @param name The name of the instance
   * @param types The types of the instance
   * @return The index of the instance, -1 if not exists
   */
  public String getInstanceName(String name, List<String> types) {
    for (PrimitiveInstance ins : primitiveInsList) {
      if (!name.equals(ins.name) || types.size() != ins.types.size()) continue;
      boolean check = true;
      for (int j = 0; j < types.size(); j++) {
        if (!types.get(j).equals(ins.types.get(j))) {
          check = false;
          break;
        }
      }
      if (check) return ins.instanceName;
    }
    return null;
  }

  /**
   * Make a variable declaration.
   *
   * @param s The variable name.
   * @param t The type.
   * @param d The declarator value.
   * @return The variable declaration node.
   */
  private Node makeVarDec(String s, String t, Node d) {  
    Node decl = factory.fieldDecl(toType(t), d);
    //set the name
    decl.getGeneric(2).getGeneric(0).set(0, s);
    return decl;
  }

  /**
   * Make a variable declaration with type is a node.
   *
   * @param s The variable name.
   * @param t The type node.
   * @param d The declarator value.
   * @return The variable declaration node.
   */
  private Node makeVarDec2(String s, Node t, Node d) {  
    Node decl = factory.fieldDecl(t, d);
    //set the name
    decl.getGeneric(2).getGeneric(0).set(0, s);
    return decl;
  }

  /**
   * Make a variable declaration.
   *
   * @param s The variable name.
   * @param t The type.
   * @param d The declarator value.
   * @return The variable declaration node.
   */
  @SuppressWarnings ("unused")
  private Node makeStaticVarDec(String s, String t, Node d) {  
    Node decl = factory.staticFieldDecl(toType(t), d);
    //set the name
    decl.getGeneric(2).getGeneric(0).set(0, s);
    return decl;
  }
  
  /**
   * Make a variable declaration.
   *
   * @param s The variable name.
   * @param t The type.
   * @param d The declarator value.
   * @return The variable declaration node.
   */
  private Node makeDec(String s, String t, Node d) {  
    Node decl = factory.fieldDecl1(toType(t), d);
    //set the name
    decl.getGeneric(2).getGeneric(0).set(0, s);
    return decl;
  }

  /**
   * Get variale bindings from a node.
   * 
   * @param n The node to get variable bindings.
   * @return A list of bindings.
   */
  @SuppressWarnings("unchecked")
  private List<LetBinding> getBindings(Node n) {    
    return (List<LetBinding>)n.getProperty(LETBINDINGS);
  }

  /**
   * Get annotated variales from a node.
   * 
   * @param n The node to get variables bindings.
   * @return A list of variables (string).
   */
  @SuppressWarnings("unchecked")
  private List<String> getUpVariables(Node n) {
    return (List<String>)n.getProperty(UPVARS);
  }

  /**
   * Get variables from a list of bindings.
   *
   * @param l The list of bindings.
   * @return A list of variables.
   */
  private List<String> extractVariables(List<LetBinding> l) {
    if (null == l) return null;
    List<String> ret = new ArrayList<String>();
    for (LetBinding bind : l) {
      ret.add(bind.name);
    }
    return ret;
  }

  /**
   * Group variables from 2 lists of variables.
   * @param l1 The first list.
   * @param l2 The second list.
   * @return The resulted list.
   */
  private <T0> List<T0> groupList(List<T0> l1, List<T0> l2) {
    if (null == l1) return l2;
    if (null == l2) return l1;
    List<T0> ret = l1;
    ret.addAll(l2);
    return ret;
  }

  /**
   * Pass annotated variables from a node to another.
   *
   * @param from The node to get annotated variables.
   * @param to The node to pass variables to.
   */
  private void passVariables(Node from, Node to) {
    List<String> vars = getUpVariables(from);
    if (null != vars) to.setProperty(UPVARS, vars);
  }

  /**
   * Pass annotated bindings from a node to another.
   *
   * @param from The node to get annotated bindings.
   * @param to The node to pass bindings to.
   */
  private void passBindings(Node from, Node to) {
    List<LetBinding> bl = getBindings(from);
    if (null != bl) to.setProperty(LETBINDINGS, bl);
  }

  /** 
   * Get variables from a pattern.
   *
   * @param n The pattern node.
   * @return A list of variables.
   */ 
  private List<String> getPatternVariables(Node n) {
    String name = n.getName();
    
    if ("TuplePattern".equals(name)) {
      List<String> res = new ArrayList<String>();
      for (int i = 0; i < n.size(); i++) {
        List<String> vars = getPatternVariables(n.getGeneric(i));
        if (null != vars) res.addAll(vars);
      }
      return res;
      
    } else if ("WhenPattern".equals(name)) {
      return getPatternVariables(n.getGeneric(0));

    } else if ("AsPattern".equals(name)) {
      List<String> vars = getPatternVariables(n.getGeneric(0));
      vars.add(n.getString(1));
      return vars; 

    } else if ("TypedPattern".equals(name)) {
      return getPatternVariables(n.getGeneric(0));

    } else if ("ConsPattern".equals(name)) {
      List<String> res = getPatternVariables(n.getGeneric(0));
      res.addAll(getPatternVariables(n.getGeneric(1)));
      return res;

    } else if ("Variable".equals(name)) {
      List<String> res = new ArrayList<String>(); 
      res.add(n.getString(0));
      return res;

    } else if ("TypeConstructorPattern".equals(name)) {
      if (1 < n.size()) return getPatternVariables(n.getGeneric(1));
      else return new ArrayList<String>(); 

    } else if ("PatternParameters".equals(name)) {
      List<String> res = new ArrayList<String>();
      for (int i = 0; i < n.size(); i++) {
        List<String> vars = getPatternVariables(n.getGeneric(i));
        if (null != vars) res.addAll(vars);
      }
      return res;

    } else if ("ListPattern".equals(name)) {
      List<String> res = new ArrayList<String>();
      for (int i = 0; i < n.size(); i++) {
        List<String> vars = getPatternVariables(n.getGeneric(i));
        if (null != vars) res.addAll(vars);
      }
      return res;

    } else if ("RecordPattern".equals(name)) {
      List<String> res = new ArrayList<String>();
      for (int i = 0; i < n.size(); i++) {
        List<String> vars = getPatternVariables(n.getGeneric(i));
        if (null != vars) res.addAll(vars);
      }
      return res;

    } else if ("FieldPattern".equals(name)) {
      return getPatternVariables(n.getGeneric(1));

    } else return new ArrayList<String>();
  }

  /** Run this transformer. */
  public void run() {     
    dispatch(typical);
  }

  /**
   * Return the ast of the generated program.
   *
   * @return The ast of the output type checker.
   */
  public GNode getCheckerAST() {
    return transformed;
  }
   
  /**
   * Return the ast of the generated types file.
   *
   * @return The ast of the output types file.
   */
  public GNode getTypesAST() {
    return typesAST;
  }  

  /**
   * Return the ast of the generated support file.
   *
   * @return The ast of the output support file.
   */
  public GNode getSupportAST() {
    return supportAST;
  } 

  /**
   * Check which libraries are used, set the corresponding flag variables.
   *
   * @param n The root of the generated code to check.
   */
  private void setFlagVariables(Node n) {
        
    FlagSetter set = new FlagSetter((GNode)n);
        
    isListUsed       = set.isListUsed;
    isArrayListUsed  = set.isArrayListUsed;
    isBigIntegerUsed = set.isBigIntegerUsed;
    isPairUsed       = set.isPairUsed;
    isNodeUsed       = set.isNodeUsed;
    isGNodeUsed      = set.isGNodeUsed;
    isPrimitivesUsed = set.isPrimitivesUsed;
    isRecordUsed     = set.isRecordUsed;
    isVariantUsed    = set.isVariantUsed;
    isTupleUsed      = set.isTupleUsed;
    isReductionUsed  = set.isReductionUsed;
    isNameUsed       = set.isNameUsed;
    isScopeUsed      = set.isScopeUsed;
    isScopeKindUsed  = set.isScopeKindUsed; 
    isAnalyzerUsed   = set.isAnalyzerUsed;    
  }

  /**
   * Import libraries that are used.
   *
   * @param compUnit The CompilationUnit node.
   * @param n The node to check which libraries are used.
   * @param fileName The importing file, XXXAnalyzer, XXXTypes or XXXSuppport
   */
  private void addImports(Node compUnit, Node n, String fileName) {

    setFlagVariables(n);
    Node importNode; 
    
    if (isBigIntegerUsed) {
      importNode = GNode.create("ImportDeclaration", null,
        GNode.create("QualifiedIdentifier", "java", "math", "BigInteger"),
        null);
      compUnit.add(importNode);
    }
    
    if (isListUsed) {
      importNode = GNode.create("ImportDeclaration", null,
        GNode.create("QualifiedIdentifier", "java", "util", "List"),null);
      compUnit.add(importNode);
    }
   
    if (isArrayListUsed) { 
      importNode = GNode.create("ImportDeclaration", null,
        GNode.create("QualifiedIdentifier", "java", "util", "ArrayList"),
        null);
      compUnit.add(importNode); 
    } 
    
    if (isPairUsed) {
      importNode = GNode.create("ImportDeclaration", null,
        GNode.create("QualifiedIdentifier", "xtc", "util", "Pair"), null);
      compUnit.add(importNode);
    }
    
    if ("Analyzer".equals(fileName)) {
      importNode = GNode.create("ImportDeclaration", null,
        GNode.create("QualifiedIdentifier", "xtc", "util", "Runtime"), null);
      compUnit.add(importNode);
     
      importNode = GNode.create("ImportDeclaration", null,
       GNode.create("QualifiedIdentifier", "xtc", "util", "Function"), null);
      compUnit.add(importNode);
    }

    if (isNodeUsed) {
      importNode = GNode.create("ImportDeclaration", null,
        GNode.create("QualifiedIdentifier", "xtc", "tree", "Node"), null);
      compUnit.add(importNode);
    }
    
    if (isGNodeUsed) {
      importNode = GNode.create("ImportDeclaration", null,
        GNode.create("QualifiedIdentifier", "xtc", "tree", "GNode"), null);
      compUnit.add(importNode);
    } 
    
    // Check and import xtc.typical.XXX
    if (!"xtc.typical".equals(packageName)) {
      if ("Analyzer".equals(fileName) || isAnalyzerUsed) {
        importNode = GNode.create("ImportDeclaration", null,
          GNode.create("QualifiedIdentifier", "xtc", "typical", "Analyzer"), 
          (GNode)null);
        compUnit.add(importNode);
      }
     
      if (isPrimitivesUsed) {
        importNode = GNode.create("ImportDeclaration", null,
          GNode.create("QualifiedIdentifier", "xtc", "typical", "Primitives"), 
          (GNode)null);
        compUnit.add(importNode);
      }

      if (isRecordUsed) {
        importNode = GNode.create("ImportDeclaration", null,
          GNode.create("QualifiedIdentifier", "xtc", "typical", "Record"), 
          (GNode)null);
        compUnit.add(importNode);
      }

      if (isVariantUsed) {    
        importNode = GNode.create("ImportDeclaration", null,
          GNode.create("QualifiedIdentifier", "xtc", "typical", "Variant"), 
          (GNode)null);
        compUnit.add(importNode);
      }
  
      if (isTupleUsed) {
        importNode = GNode.create("ImportDeclaration", null,
          GNode.create("QualifiedIdentifier", "xtc", "typical", "Tuple"), 
          (GNode)null);
        compUnit.add(importNode);
      }
    
      if (isReductionUsed) {
        importNode = GNode.create("ImportDeclaration", null,
          GNode.create("QualifiedIdentifier", "xtc", "typical", "Reduction"),
          (GNode)null);
        compUnit.add(importNode);
      }
   
      if (isNameUsed) {
        importNode = GNode.create("ImportDeclaration", null,
          GNode.create("QualifiedIdentifier", "xtc", "typical", "Name"),
          (GNode)null);
        compUnit.add(importNode);
      }
    
      if (isScopeUsed) {
        importNode = GNode.create("ImportDeclaration", null,
          GNode.create("QualifiedIdentifier", "xtc", "typical", "Scope"),
          (GNode)null);
        compUnit.add(importNode);
      }
 
      if (isScopeKindUsed) {
        importNode = GNode.create("ImportDeclaration", null,
          GNode.create("QualifiedIdentifier", "xtc", "typical", "ScopeKind"),
          (GNode)null);
        compUnit.add(importNode);
      }
    }
  }

  /**
   * Make the class body for the XXXAnalyzer
   *
   * @return The node of the class body
   */
  private Node makeClassBody() {
    // Make the constructor
    Node constructor;
    if ("Typical".equals(output)) {
      constructor = factory.makeConstructor(output + "Analyzer"); 
      Node fieldDecl = factory.nodeTypeDecl();
      // return the class body node
      return GNode.create("ClassBody", fieldDecl, constructor);
    } else {
      constructor = factory.makeConstructor2(output + "Analyzer");
      // return the class body node
      return GNode.create("ClassBody", constructor); 
    }    
  } 

  /**
   * Create the typechecker skeleton.
   *
   * @return The root of the type checker ast.
   */
  private Node makeSkeleton() {    
    Node classDec = GNode.create("ClassDeclaration",
      toModifiers("public"),
      output + "Analyzer",
      null,
      GNode.create("Extension", GNode.create("Type",
        GNode.create("QualifiedIdentifier", "Analyzer"),null)),null, 
      cbody);

    Node compUnit = GNode.create("CompilationUnit", 8);
    compUnit.add(packageNode);
    addImports(compUnit, cbody, "Analyzer");      
    compUnit.add(comment(classDec,
                         "Type checker for " + output + "."));
    return compUnit; 
  }  

  /**
   * Create the skeleton for the xxxTypes.java file.
   *
   * @return The root of the XXXTypes.java ast.
   */
  private Node makeTypesSkeleton() {
    Node compUnit = GNode.create("CompilationUnit", 8);
    compUnit.add(packageNode);
    addImports(compUnit, tbody, "Types");
   
    Node classDecl = factory.classDecl2(output + "Types");
    classDecl.set(5, tbody);

    compUnit.add(comment(classDecl,
                         "Types for " + output + "."));
    return compUnit;    
  }

  /**
   * Create the skeleton for the xxxSupport.java file.
   *
   * @return The root of the XXXSupport.java ast.
   */
  private Node makeSupportSkeleton() {
    Node compUnit = GNode.create("CompilationUnit", 8);
    compUnit.add(packageNode);
    addImports(compUnit, sbody, "Support");
    
    Node classDecl = factory.classDecl2(output + "Support");
    classDecl.set(5, sbody);

    compUnit.add(comment(classDecl,
                         "Helper functionality for " + output + "."));
    return compUnit;    
  }

  /** 
   * Add an enumeration constant. 
   * 
   * @param s The name of the enum.
   */
  private void addEnum(Node enums, String s) {
    enums.add(GNode.create("EnumConstant", null, s, null, null));
  }
   
  /**
   * Enter the scoped named.
   *
   * @param n The scope name.
   */
  private void enterScope(String n) {
    table.enter(n);
  }

  /**
   * Exit the scope named n.
   *
   * @param n The scope name.
   */
  private void exitScope(String n) {
    if (!(n.equals(table.current().getName()))) {
      throw new AssertionError("mismatched scope exit " + n);      
    } else {
      table.exit();
    }
  }
  
  /**
   * Debug function to check for type annotations.
   *
   * @param n The node to check.
   */
  private void checkTypeAnnotation(GNode n) {
    if (!n.hasProperty(TYPE)) {
      throw new AssertionError("no type annotation for " + n.getName());      
    } else if (n.getProperty(TYPE) == null) {
      throw new AssertionError(n.getName() + " has null type");
    }
  }  
 
  /**
   * Utility function to print a nicely formatted ast for debugging.
   * 
   * @param n The ast root.
   */
  @SuppressWarnings("unused")
  private final void printAST(Node n) {
    runtime.console().pln().format(n).pln().flush();
  }
  
  /** Print the symbol table. */
  @SuppressWarnings("unused")
  private final void printSymbolTable() {
    if (null != table) {
      Visitor visitor = runtime.console().visitor();
      try {
        table.root().dump(runtime.console());
      } finally {
        runtime.console().register(visitor);
      } 
      runtime.console().flush();
    } else {
      throw new AssertionError("Symbol table not initialized");      
    }
  }

  /**
   * Return the qualified name of a type object.
   *
   * @param o The type object.
   * @return The (possibly prefixed) type.
   */
  private String getType(Object o) {
    return mapper.toTypeString(o);    
  }

  /**
   * Wrap the specified node in a documentation comment.
   *
   * @param node The node.
   * @param text The comment's text.
   * @return The commented node.
   */
  public static Node comment(Node node, String... text) {
    final List<String> l = new ArrayList<String>(text.length);
    for (String s : text) l.add(s);
    return new Comment(Comment.Kind.DOCUMENTATION, l, node);
  }

  /**
   * Convert the specified string to a modifier in a list of modifiers.
   *
   * @param mod The modifier
   * @return The corresponding node.
   */
  public static Node toModifiers(String mod) {
    return GNode.create("Modifiers", GNode.create("Modifier", mod));
  }

  /**
   * Convert the specified string to a literal node.
   *
   * @param name The node name.
   * @param literal The literal as a string.
   * @return The corresponding literal node.
   */
  public static Node toLiteral(String name, String literal) {
    return GNode.create(name, literal);
  }
  
  /**
   * Create an identifier node with the specified name.
   *
   * @param name The name.
   * @return The corresponding identifier node.
   */
  public static Node toIdentifier(String name) {
    if (null == name) throw new AssertionError("null name in toIdentifier");
    return GNode.create("PrimaryIdentifier", name);
  }

  /**
   * Create a conditional statement node.
   *
   * @param condition The condition node.
   * @param action The action statement or block.
   * @return The conditional node.
   */  
  public Node toIfStatement(Node condition, Node action) {
    
    action = (null == action) ? GNode.create("Block") : action;
    
    Node block = (action.hasName("Block")) 
        ? GNode.ensureVariable(GNode.cast(action))
        : GNode.ensureVariable(GNode.create("Block", action));
    
    return GNode.create("ConditionalStatement", condition, block, null);
  }
  
  /**
   * Create a case statemnt
   * 
   * @param clause
   * @param action
   */
  private Node makeCase(Node clause, List<Node> actions, String s) {
    Node c = factory.caseStmnt(clause, actions).getGeneric(1);    
    return c;
  }

  /**
   * Create a type node.
   *
   * @param s The name of the type.
   * @return The type node.
   */
  private static Node toType(String s) {
    assert null != s :"null string"; 
    return 
      GNode.create("Type",GNode.create("QualifiedIdentifier",s), null); 
  }

  /**
   * Create new expression with type is a node.
   *   If args is null empty arguments will be used.
   *
   * @param name The type node.
   * @param args The arguments.
   * @param body The option anonymous class body.
   */
  private Node toNewExpression2(Node name, Node args, Node body) {
    if (null == args) args = GNode.create("Arguments");
 
    List<Object> arglisto = new ArrayList<Object>(args.size());
    args.addAllTo(arglisto);
    
    Object o = arglisto; // Hack to enable cast from List<Object> to
                         // List<Node>.

    @SuppressWarnings("unchecked")
    List<Node> arglistn = (List<Node>)o;
    Node newNode = factory.newExpr(name, arglistn);
    newNode.set(4, body);
    return newNode;
  }  

  /**
   * Extract the list of arguments from an Arguments node.
   *
   * @param args The arguments node.
   * @return The list of arguments.
   */
  @SuppressWarnings("unchecked")
  private static List<Node> makeArgumentList(Node args) {
    if (null == args) args = GNode.create("Arguments");
    List<Object> arguments = new ArrayList<Object>(args.size());
    args.addAllTo(arguments);

    Object o = arguments; // Hack to enable cast from List<Object> to
                          // List<Node>.
    return (List<Node>)(o);
  }
    
}
