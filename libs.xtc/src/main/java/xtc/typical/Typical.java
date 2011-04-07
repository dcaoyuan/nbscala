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
 
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.PrintWriter;
import java.io.FileWriter;

import xtc.lang.JavaPrinter;

import xtc.parser.ParseException;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

import xtc.util.Tool;
import xtc.util.Runtime;

import xtc.util.SymbolTable;

/**
 * The Typical compiler.
 * 
 * @author Laune Harris, Anh Le
 * @version $Revision: 1.31 $
 */
public class Typical extends Tool {

  /**The Analyzer to run the test*/
  protected Object analyser;

  /**The name of the analyzer*/
  protected String checker;

  /**The name of the typical file */
  protected String typicalName;

  /** Create a new Typical compiler. */
  public Typical() { /* Nothing to do. */ }

  public String getName() {
    return "Typical";
  }
  
  public void init(){
    super.init();
     
    runtime.
      bool("checkOnly", "optionCheckOnly", false,
           "Type check, but do not generate code.").
      bool("typesOnly", "optionTypesOnly", false,
           "Generate external type definitions.").
      word("o", "optionOutput", false,
           "Specify the base name of the generated checker.").
      bool("printAST", "printAST", false, 
           "Print the AST of the of the pre-transformed program.").
      bool("printSymbolTable", "printSymbolTable", false,
           "Print the symbol table.").
      bool("printSource", "printSource", false,
           "Print the java source code of the generated checker.").
      bool("Oswitch", "optimizeMatch", false,
           "Use switch statements and type tags for pattern matches.").
      bool("Ofold-let", "optimizeFoldLet", false,
           "Collapse let expressions where possible.").
      bool("Olet", "optimizeLet", false,
           "Avoid creating a Let object if possible.").     
      bool("Otype", "optimizeType", false,
           "Eliminate type records, if attributes are not defined.").
      word("node", "optionNodeType", false,
           "Specify the name of the node type.");
  }

  public Node parse(Reader in, File file) throws IOException, ParseException {
    typicalName = file.toString();
    TypicalParser parser =
      new TypicalParser(in, file.toString(), (int)file.length());    
    return (Node)parser.value(parser.pModule(0));
  }

  public void process(Node ast) {
    //Check we should print the pre AST
    if(runtime.test("printAST")){
      runtime.console().pln().format(ast).pln().flush();
    }
    
    //It's a compilation run
    GNode result_ast  = null;
    GNode types_ast   = null;
    GNode support_ast = null;

    String output = (String)runtime.getValue("optionOutput");   
    if (null == output) {
      output = typicalName.substring(0, typicalName.length()- 5);
    }

    String nodeType = (String)runtime.getValue("optionNodeType");
    if (null == nodeType) nodeType = "node";
      
    SymbolTable table = null;
    TypicalAnalyzer analyzer = new TypicalAnalyzer(runtime, nodeType);
    table = analyzer.run(ast);
    
    if (0 < runtime.errorCount()) return ;
      
    if (runtime.test("printSymbolTable")) {
      if (null != table){
        Visitor visitor = runtime.console().visitor();
        try {
          table.root().dump(runtime.console());
        } finally {
          runtime.console().register(visitor);
        } 
        runtime.console().flush();
      } else {
        runtime.error("symbol table not initialized");
        runtime.exit();
      }
    }
      
    if (!runtime.test("optionCheckOnly")) {
      Transformer transformer = null;
      transformer = new Transformer((GNode)ast,table,output, runtime);
      transformer.run();
      result_ast = transformer.getCheckerAST();
      types_ast =  transformer.getTypesAST();
      support_ast = transformer.getSupportAST();
      
      
      //write type checker file
      File    dir  = runtime.getFile(Runtime.OUTPUT_DIRECTORY);
      File    file = new File(dir, output + "Analyzer.java" );
      Printer out;
      // Write the xxxAnalyzer.java file
      if (!runtime.test("optionTypesOnly")) {
        try {      
          out = new
            Printer(new PrintWriter(new BufferedWriter(new FileWriter(file))));
        } catch (IOException x) {
          if (null == x.getMessage()) {
            runtime.error(file.toString() + ": I/O error");
          } else {
            runtime.error(file.toString() + ": " + x.getMessage());
          }
          return;
        }      
        printHeader(out);
        new JavaPrinter(out).dispatch(result_ast);
        out.flush();
      }

      // Write the xxxTypes.java
      file = new File(dir, output + "Types.java" );
      try {      
        out = new
          Printer(new PrintWriter(new BufferedWriter(new FileWriter(file))));
      } catch (IOException x) {
        if (null == x.getMessage()) {
          runtime.error(file.toString() + ": I/O error");
        } else {
          runtime.error(file.toString() + ": " + x.getMessage());
        }
        return;
      }      
      printHeader(out);
      new JavaPrinter(out).dispatch(types_ast);
      out.flush();
      
      // Write the xxxSupport.java
      file = new File(dir, output + "Support.java" );
      try {      
        out = new
          Printer(new PrintWriter(new BufferedWriter(new FileWriter(file))));
      } catch (IOException x) {
        if (null == x.getMessage()) {
          runtime.error(file.toString() + ": I/O error");
        } else {
          runtime.error(file.toString() + ": " + x.getMessage());
        }
        return;
      }      
      printHeader(out);
      new JavaPrinter(out).dispatch(support_ast);
      out.flush();
    }

    if (runtime.test("printSource")) {
      new JavaPrinter(runtime.console()).dispatch(result_ast);
      runtime.console().flush();
    }       
  }

  /** Run the compiler with the specified command line arguments. */
  public static void main(String[] args){
    new Typical().run(args);
  }
}
