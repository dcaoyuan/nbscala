NetBeans Plugin for Scala
=========================

# Requirement:
* Java 1.6+
* Maven 2.x (If you are using NetBeans 7.0, should change maven home to point to a 2.x version via [Options] -> [Miscellianeous] -> [Maven]) 
* NetBeans 7.0

# Setting nb.home propertity for maven

You should set 'nb.installtion' propertiy in your maven setting.xml (.m2/setting.xml) to point to an existed NetBeans installation home, for example:

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

