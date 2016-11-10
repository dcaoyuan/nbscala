NetBeans Plugin for Scala
=========================

## Project Informations
This is a plugin of the Netbeans Platform for the [scala language](http://http://www.scala-lang.org/). In the case of a maven scala project, no local installation of scala is needed. It enables syntax checking, syntax highlighting, auto-completion, pretty formatter, occurrences mark, brace matching, indentation, code folding, function navigator, go to declaration, project management and a shell console. It's is specially useful if you are a maven user.

### Notice ###
The Ant based project will be removed soon, which, by far, is not under improving any more. Please use sbt/maven based project.

### Where to start ?
The project central point is [https://github.com/dcaoyuan/nbscala](). There are some other places, where the project was hosted before, but they are abandoned now.

### Community
Until recently, this has mostly been a one man project. Some patches were supplied by different people, but I'm still convinced that it will gather a community soon. For questions and bug reports use the [issue tracker](https://github.com/dcaoyuan/nbscala/issues). If interested in joining the project, you can write me directly or send patches/pull requests.

## Installation

### Installation via NetBeans Update Center
The plugins will be available at [http://plugins.netbeans.org](http://plugins.netbeans.org), thus could be installed via the NetBeans Update Center automatically when it passed verification by NetBeans staffs.

### Manual Installation
Make sure you don't have an old version installed. (Check your netbeans installation for a 'nbscala' directory: if it exists, delete it.)

1. Download the latest release at plugins.netbeans.org.
2. Extract all files into a directory.
3. Start Netbeans.
4. Select Tools -> Plugins -> Downloaded -> Add Plugins...
5. Select all extracted files.
6. Accept the license and the installation of unsigned plugins. 

### Installation Notes:

 * After installation, it's always better to restart NetBeans
 * You may need to delete NetBeans' old cache to get improved features working. To find the cache location, read the netbeans.conf at:

        $NetBeansInstallationPlace/etc/netbeans.conf

## Build Instructions
Cause of the small group of people involved in the project we only supply updates for the latest NetBean version.

### Requirement - Running:
* Java 1.6+
* NetBeans 8.1+

### Requirement - Building:
* Java 1.8 (for master branch)
* Java 1.6 (for 2.9.x branch)
* Maven 2.x/3.x 
* NetBeans 8.1+

### Branches:
* master -- tracking Scala 2.10.x and 2.11.x currently
* 2.9.x  -- for Scala 2.9.x

### Setting nb.installation property for maven
Hint: This is going to be removed in the future. There is already a nbm-application based subproject which can be used to run all modules of the plugin. See the scala.app/pom.xml for more information, what is still missing.

Make a new copy of your installed NetBeans (which will be used to run 'mvn nbm:run-ide' goal), check if there is a directory 'nbscala' under this copy, if yes, delete it. Then set 'nb.installation' property in your maven settings.xml (.m2/settings.xml) to point to this copy:

    <profiles>
        <profile>
            <id>netbeans</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <nb.installation>${user.home}/myapps/netbeans-8.1-fordev</nb.installation>
                <nb.nbmdirectory>${user.home}/myprjs/nbsrc-8.1/nbbuild/nbms</nb.nbmdirectory>
            </properties>
        </profile>
    </profiles>

### Set system environment variable for building.

    MAVEN_OPTS=-Xss8M

or even more:

    MAVEN_OPTS=-Xss8M -Xmx2048M

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

Build-Run-Cycle: (after changed module was successfuly built)
	
    mvn nbm:cluster nbm:run-ide

Build-Debug-Cycle: (after changed module was successfuly built)

    mvn nbm:cluster nbm:run-ide -Pdebug-ide

### Publish to plugins.netbeans.org

Generate keys/keystore (note: The keystore and key password needs to be the same) (only need to create once):

    keytool -genkey -dname "CN=Caoyuan Deng, OU=nbscala, O=inloop.io, L=Richmond, S=BC, C=CA" -alias nbscala -validity 1800
    keytool -list -v
              
Enable signing modules by adding all three keystore related parameters in ~/m2/settings.xml as:

                 <profiles>
                     <profile>
                         <id>sign-nbscala-nbms</id>
                         <activation>
                             <activeByDefault>true</activeByDefault>
                         </activation>
                         <properties>
                             <nbm.sign.keystore>${user.home}/.keystore</nbm.sign.keystore>
                             <nbm.sign.keystorealias>nbscala</nbm.sign.keystorealias>
                             <nbm.sign.keystorepassword>thepassword</nbm.sign.keystorepassword>
                         </properties>
                     </profile>
                 </profiles>

Pack a zip file for plugins.netbeans.org:

    mvn nbm:autoupdate
    cd target/netbeans_site
    zip nbscala-version.zip *.nbm


## Project Details

The Project targets version 2.10.x and 2.11.x of the Scala release.

## Scala Console Integration

### A new Scala shell console was implemented since Feb 27, 2013

### To open it, right click on project, and choose "Open Scala Console"

### Features:

* Be aware of project's classpath that could be imported, new, run under console
* Popup auto-completion when press \<tab\>
* Applied also to Java SE projects and Maven projects

## SBT Integration

### Only Scala-2.10+ is supported under for SBT integration 

* That is, always try to set your project's Scala version to 2.10+ in Build.scala or build.sbt: 

        scalaVersion := "2.10.0"

### Supported features

* Recognize sbt project and open in NetBeans
* Open sbt console in NetBeans (Right click on sbt project, choose "Open Sbt")
* Jump to compile error lines

### How to

* nbsbt-plugin 1.1.2+ has been deployed to repo.scala-sbt.org, that means it will be automatilly resolved when you run sbt):

* Add nbsbt to your plugin definition file. You can use either the global one at  **~/.sbt/0.13/plugins/plugins.sbt** or the project-specific one at **PROJECT_DIR/project/plugins.sbt**

        addSbtPlugin("org.netbeans.nbsbt" % "nbsbt-plugin" % "1.1.4")


## FAQ


**Q**: NetBeans' response becomes slower after a while.

**A**: Edit your NetBeans configuration file (NetBeansInstallationPlace/etc/netbeans.conf), add -J-Xmx2048M (or bigger)


**Q**: How to navigate SBT project's dependency sources.

**A**: You should have SBT download the dependency's sources via sbt command: `sbt updateClassifiers`, please see http://www.scala-sbt.org/0.13.1/docs/Detailed-Topics/Library-Management.html#download-sources. 


**Q**: I got:

    [error] sbt.IncompatiblePluginsException: Binary incompatibility in plugins detected.

**A**: Try to remove published nbsbt plugin from your local .ivy2 repository and sbt plugins cache:

    rm -r ~/.ivy2/local/org.netbeans.nbsbt
    rm -r ~/.sbt/0.13/plugins/target

and redo 'publish-local' for the NetBeans sbt plugin <https://github.com/dcaoyuan/nbsbt>.


**Q**: I got:

    [error] Not a valid command: netbeans
    [error] Expected '/'
    [error] Expected ':'
    [error] Not a valid key: netbeans (similar: test, tags, streams)
    [error] netbeans
    [error]         ^

**A**: Try to remove the project/target folder under your project base directory, there may be something cached here, and was not reflected to the newest condition.


**Q**: What will this plugin do upon my project?

**A**: It will generate a NetBeans project definition file ".classpath_nb" for each project.


**Q**: It seems there are some suspicious error hints displayed on the edited source file, how can I do?

**A**: There may be varies causes, you can try open another source file, then switch back to this one, the error hints may have disappeared. If not, right click in editing window, choose 'Reset Scala Parser', and try the steps mentioned previous again.


**Q**: My project's definition was changed, how to reflect these changes to NetBeans.

**A**: Right click on the root project, choose "Reload Project".


**Q**: Exiting from Scala console leaves terminal unusable.

**A**: Under some unix-like environment, scala interactive console started with some stty setting, but not for NetBeans's integrated one. You can try 'reset' after quit from NetBeans.
