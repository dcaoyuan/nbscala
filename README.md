NetBeans Plugin for Scala
=========================

# Requirement:
* Java 1.6+
* Maven 2.x (If you are using NetBeans 7.0, should change maven home to point to a 2.x version via [Options] -> [Miscellianeous] -> [Maven]) 
* NetBeans 7.0

# Setting nb.installation property for maven

You should set 'nb.installtion' property in your maven setting.xml (.m2/setting.xml) to point to an existed NetBeans installation home, for example:

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

