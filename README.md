NetBeans Plugin for Scala
=========================

# Requirement:
* Java 1.6+
* Maven 2.x/3.x 
* NetBeans 7.0

# Setting nb.installation property for maven

You should set 'nb.installation' property in your maven settings.xml (.m2/settings.xml) to point to an existed NetBeans installation home, for example:

    <profiles>
        <profile>
            <id>nb-installation</id>
            <properties>
                <nb.installation>${user.home}/myapps/netbeans-7</nb.installation>
             </properties>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>nb-installation</activeProfile>
    </activeProfiles>

# Build all nbms

    cd nbscala
    mvn clean install

# Generate auto-update site:
    cd nbscala
    mvn nbm:autoupdate

the nbms and update site can be found at nbscala/target/netbeans_site

# Run/Debug ide:
    cd nbscala
    mvn nbm:cluster

To run:

    mvn nbm:run-ide

To debug:

    mvn nbm:run-ide -Pdebug-ide

# Download built package

    http://sourceforge.net/projects/erlybird/files/nb-scala/