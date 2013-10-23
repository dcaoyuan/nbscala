# Makefile 2 
#***************************************************************************
# 
# Java development root directory.
# 
# (C) 1999 Jacob Dreyer - Geotechnical Software Services
# jacob.dreyer@geosoft.no - http://geosoft.no
#
# Modifications Copyright (C) 2001, 2004, 2005 Robert Grimm 
# rgrimm@alum.mit.edu
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
#
#***************************************************************************




#***************************************************************************
#
# This section describes the current package.
#
# o PACKAGE     - The complete package name. 
# o PACKAGE_LOC - Same as PACKAGE but with "/"s instead of "."s.
# o SOURCE      - List of the source files. Remember extension.
# o JNI_SOURCE  - Files from SOURCE that are to be built with the JAVAH 
#                 compiler.
# o JAR_EXTRAS  - None-class files and directories that are to be bundled
#                 into the jar archive.
#
#***************************************************************************

PACKAGE     = xtc.parser
PACKAGE_LOC = xtc/parser

SOURCE = \
	Element.java \
	InternalElement.java \
	Action.java \
	NodeMarker.java \
	Terminal.java \
	CharTerminal.java \
	AnyChar.java \
	CharLiteral.java \
	CharRange.java \
	CharClass.java \
	CharCase.java \
	CharSwitch.java \
	StringLiteral.java \
	StringMatch.java \
	NonTerminal.java \
	UnaryOperator.java \
	Predicate.java \
	VoidedElement.java \
	Binding.java \
	FollowedBy.java \
	NotFollowedBy.java \
	SemanticPredicate.java \
	ParserAction.java \
	Name.java \
	SequenceName.java \
	Sequence.java \
	Quantification.java \
	Option.java \
	Repetition.java \
	OrderedChoice.java \
	Production.java \
	FullProduction.java \
	PartialProduction.java \
	AlternativeAddition.java \
	AlternativeRemoval.java \
	ProductionOverride.java \
	ModuleName.java \
	ModuleList.java \
	ModuleMap.java \
	ModuleDependency.java \
	ModuleModification.java \
	ModuleInstantiation.java \
	ModuleImport.java \
	Module.java \
	Grammar.java \
	ParseTreeNode.java \
	Literal.java \
	NullLiteral.java \
	ValueElement.java \
	NullValue.java \
	StringValue.java \
	TokenValue.java \
	BindingValue.java \
	ListValue.java \
	EmptyListValue.java \
	ProperListValue.java \
	ActionBaseValue.java \
	GenericValue.java \
	GenericNodeValue.java \
	GenericActionValue.java \
	GenericRecursionValue.java \
	Properties.java \
	Analyzer.java \
	GrammarVisitor.java \
	Renamer.java \
	Copier.java \
	LeftRecurser.java \
	DirectLeftRecurser.java \
	VariantSorter.java \
	Resolver.java \
	GenericVoider.java \
	Simplifier.java \
	TextTester.java \
	Tokenizer.java \
	Annotator.java \
	TransientMarker.java \
	CostEstimator.java \
	Inliner.java \
	ElementVoider.java \
	Transformer.java \
	ListMaker.java \
	Generifier.java \
	ValueChecker.java \
	ReachabilityChecker.java \
	MetaData.java \
	MetaDataCreator.java \
	MetaDataSetter.java \
	ReferenceCounter.java \
	ChoiceExpander.java \
	TerminalOptimizer.java \
	PrefixFolder.java \
	ProductionVoider.java \
	EquivalenceTester.java \
	DuplicateProductionFolder.java \
	RootFinder.java \
	DeadProductionEliminator.java \
	TreeTyper.java \
	TreeExtractor.java \
	PrettyPrinter.java \
	HtmlPrinter.java \
	CodeGenerator.java \
	Result.java \
	Column.java \
	ParserBase.java \
	SemanticValue.java \
	ParseError.java \
	ParseException.java \
	Rats.java \
	PParserState.java \
	PParser.java

JNI_SOURCE =

JAR_EXTRAS = 


#***************************************************************************
#
# Include common part of makefile
#
#***************************************************************************

ifdef JAVA_DEV_ROOT
include $(JAVA_DEV_ROOT)/Makerules

parsers :
	$(RATS) PGrammar.rats
	$(JAVAC) $(JAVAC_OPTIONS) PParser.java

endif
