This module is copied from xtc (http://cs.nyu.edu/~rgrimm/xtc/rats.html) source code, will be applied patches.

The modifications will be described here:


I Step 1

1. remove xtc.xform directory
2. remove all under xtc.lang directory except JavaPrint.java
3. remove all junit related source files.
4. ant fixcrlf
5. create/copy Bundle.properties:
OpenIDE-Module-Display-Category=Libraries
OpenIDE-Module-Name=Rats! Packrat Parser
OpenIDE-Module-Short-Description=Extensible parser generator for C-like languages

II Step 2

1. Patch following files to let Location contains offset and endOffset:
xtc/src/xtc/parser/ParserBase.java
xtc/src/xtc/tree/Location.java
xtc/src/xtc/tree/Relocator.java