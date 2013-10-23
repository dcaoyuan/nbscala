# Makefile 2 
#***************************************************************************
# 
# Java development root directory.
# 
# (C) 1999 Jacob Dreyer - Geotechnical Software Services
# jacob.dreyer@geosoft.no - http://geosoft.no
#
# Modifications Copyright (C) 2001, 2004 Robert Grimm 
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

PACKAGE     = xtc.typical
PACKAGE_LOC = xtc/typical

SOURCE = \
	Name.java \
	ScopeKind.java \
	Scope.java \
	TypicalParser.java \
	Tuple.java \
	Primitives.java \
	Record.java \
	Reduction.java \
	Analyzer.java \
	TypicalTypes.java \
	TypicalSupport.java \
	TypicalAnalyzer.java \
	TypeMapper.java \
	TreeFactory.java \
	TypeCollector.java \
	FreeVariableCollector.java \
	FlagSetter.java \
	LetFolder.java \
	Transformer.java \
	Typical.java 

JNI_SOURCE = 

JAR_EXTRAS = 



#***************************************************************************
#
# Include common part of makefile
#
#***************************************************************************

ifdef JAVA_DEV_ROOT
include $(JAVA_DEV_ROOT)/Makerules

JAVAC_OPTIONS  = -Xlint:unchecked -d $(CLASS_DIR) -sourcepath $(SOURCE_DIR)

parsers :
	$(RATS) Typical.rats
	$(JAVAC) $(JAVAC_OPTIONS) TypicalParser.java

analyzers :
	$(TYPICAL) -Otype -Ofold-let -Oswitch Typical.tpcl
	$(JAVAC) $(JAVAC_OPTIONS) TypicalTypes.java
	$(JAVAC) $(JAVAC_OPTIONS) TypicalSupport.java
	$(JAVAC) $(JAVAC_OPTIONS) TypicalAnalyzer.java
	$(TYPICAL) -checkOnly Typical.tpcl # Sanity check

factories :
	$(FACTORY) TreeFactory.ffj
	$(JAVAC) $(JAVAC_OPTIONS) TreeFactory.java

.PHONY	: check check-typical 


endif
