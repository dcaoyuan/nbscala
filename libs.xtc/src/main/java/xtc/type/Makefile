# Makefile 2 
#***************************************************************************
# 
# Java development root directory.
# 
# (C) 1999 Jacob Dreyer - Geotechnical Software Services
# jacob.dreyer@geosoft.no - http://geosoft.no
#
# Modifications (C) 2001, 2004 Robert Grimm 
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

PACKAGE     = xtc.type
PACKAGE_LOC = xtc/type

SOURCE = \
	Language.java \
	Reference.java \
	NullReference.java \
	StringReference.java \
	VariableReference.java \
	StaticReference.java \
	DynamicReference.java \
	RelativeReference.java\
	CastReference.java \
	IndirectReference.java \
	IndexReference.java \
	FieldReference.java \
	Constant.java \
	Tagged.java \
	Type.java \
	Parameter.java \
	NamedParameter.java \
	InternalParameter.java \
	Wildcard.java \
	WrappedT.java \
	AnnotatedT.java \
	AliasT.java \
	EnumT.java \
	EnumeratorT.java \
	InstantiatedT.java \
	VariableT.java \
	ParameterizedT.java \
	VoidT.java \
	UnitT.java \
	BooleanT.java \
	NumberT.java \
	IntegerT.java \
	FloatT.java \
	LabelT.java \
	DerivedT.java \
	PointerT.java \
	ArrayT.java \
	StructOrUnionT.java \
	StructT.java \
	UnionT.java \
	FunctionOrMethodT.java \
	FunctionT.java \
	MethodT.java \
	ClassOrInterfaceT.java \
	ClassT.java \
	InterfaceT.java \
	PackageT.java \
	InternalT.java \
	TupleT.java \
	VariantT.java \
	ErrorT.java \
	AST.java \
	JavaAST.java \
	C.java \
	CFactory.java \
	TypePrinter.java \
	SourcePrinter.java

JNI_SOURCE =

JAR_EXTRAS = 


#***************************************************************************
#
# Include common part of makefile
#
#***************************************************************************

ifdef JAVA_DEV_ROOT
include $(JAVA_DEV_ROOT)/Makerules
endif
