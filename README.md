NetBeans Plugin for Scala
=========================

## Index
1. Common Informations
 1. Where to start?
 2. Community
 3. Snapshot Builds
2. Build Instructions
3. Project Details

## Common Informations
This is a plugin of the Netbeans Platform for the [scala language](http://http://www.scala-lang.org/). It targets version 2.9.x. In the case of a maven scala project, no local installation of scala is needed. It enables syntax checking, syntax highlighting, auto-completion, pretty formatter, occurrences mark, brace matching, indentation, code folding, function navigator, go to declaration, project management and a shell console. It's is specially useful if you are a maven user.

### Where to start ?
The project central point is [https://github.com/dcaoyuan/nbscala](). There are some other places, where the project was hosted before, but they are abandoned now.

### Community
Until recently, this has mostly been a one man project. Some patches were supplied by different people, but I'm still convinced that it will gather a community soon. For questions and bug reports use the [issue tracker](https://github.com/dcaoyuan/nbscala/issues). If interested in joining the project, you can write me directly or send patches/pull requests.

### Snapshot Builds
Snapshot builds can be found here [http://sourceforge.net/projects/erlybird](). I'm using sourceforge because of their supply of higher bandwidth.

### Installation
Make sure you don't have an old version installed. (Check your netbeans installation for a 'nbscala' directory: if it exists, delete it.)
 1. Download the latest release at sourceforge.
 1. Extract all files into a directory.
 1. Start Netbeans.
 1. Select Tools -> Plugins -> Downloaded -> Add Plugins...
 1. Select all extracted files.
 1. Accept the licence and the installation of unsigned plugins. 

## Build Instructions

### Requirement:
* Java 1.6 (Java 1.7 is not supported yet)
* Maven 2.x/3.x 
* NetBeans 7.1

### Setting nb.installation property for maven

Make a new copy of your installed NetBeans (which will be used to run 'mvn nbm:run-ide' goal), check if there is a directory 'nbscala' under this copy, if yes, delete it. Then set 'nb.installation' property in your maven settings.xml (.m2/settings.xml) to point to this copy:

    <profiles>
        <profile>
            <id>netbeans</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <nb.installation>${user.home}/myapps/netbeans-71</nb.installation>
            </properties>
        </profile>
    </profiles>

### Build all nbms

    cd nbscala
    mvn clean install

### Generate auto-update site:
    cd nbscala
    mvn nbm:autoupdate

the nbms and update site can be found at nbscala/target/netbeans_site

### Run/Debug ide:
    cd nbscala
    mvn nbm:cluster

To run:

    mvn nbm:run-ide

To debug:

    mvn nbm:run-ide -Pdebug-ide

## Project Details

The Project targets version 2.9.x of the scala release.

