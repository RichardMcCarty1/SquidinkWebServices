# SquidinkWebServices
Tomcat Servlet Components for Squidink

Servlet Components are built as a .war and included in [SquidInk-Image](https://github.com/RichardMcCarty1/SquidInk-Image) to be incorporated into a docker image for container servicing.

External packages are included / managed in pom.xml by Maven

Servlet Structure and end-point referencing are managed by [src/main/java/com/cirdles](https://github.com/RichardMcCarty1/SquidinkWebServices/tree/master/src/main/java/com/cirdles) and [src/webapp/WEB-INF/web.xml](https://github.com/RichardMcCarty1/SquidinkWebServices/tree/master/src/main/webapp/WEB-INF) respectively

**Filebrowser is referenced by Tomcat as %CATALINA_HOME%/filebrowser/users and should therefore be included in %CATALINA_HOME%**


## Installation for Development

1. Dowload the appropriate version of Apache Tomcat from https://tomcat.apache.org/download-90.cgi

2. Navigate to the directory of the binary distribution that you've downloaded and follow all relevant instructions in RUNNING.txt including setting the CATALINA_HOME environment variable. Note the command used to start Tomcat on your system in section 4.

3. Navigate to the conf directory and open tomcat-users.xml. Add the following to the bottom of the file before the closing tag for tomcat-users: 
```xml
<role rolename="manager-gui"/>
<role rolename="admin-gui"/>
<user username="admin" password="admin" roles="manager-gui"/>
```

4. In the same directory, open server.xml. Modify the block of code starting somewhere around line 69 to be the same as:
```xml
<Connector port="8080" protocol="HTTP/1.1"
            connectionTimeout="20000"
            redirectPort="8443"
            address="0.0.0.0" />
```

5. Download Apache Maven binary distribution from https://maven.apache.org/download.cgi. Follow the instructions in the README. You can check you downloaded this properly by typing
```ssh
mvn --version
```

6. Clone the SquidInkWebServices repo. This could be done from the main repo itself, a fork from the main repo, or an open pull request (if relevant).

7. Navigate to the SquidInkWebServices directory on your system and open a terminal there. Then run:
```ssh
mvn clean package
```

8. Start Tomcat using the command found in step 2.

9. Go to http://localhost:8080/ in your browser. A success screen from Tomcat should appear. Click "Manager App". The username and password should be: "admin". Scroll to "WAR file to deploy" and click "Choose File". Navigate to SquidInkWebServices>target and choose "squid_servlet-1.0.0.war"