# Tomcat Logfile Zipper
This is a tiny (~ 5kB) Java Servlet enabling easy downloading of Apache Tomcat logfiles via http, e.g. from a Tomcat inside a container.

## What
* It sends a zip called file `logs.zip` containing all files in `${catalina.base}/logs`, including subdirectories, to any client requesting its context path (`/logs` by default).
* The zip file is streamed directly to the client. This make this program memory friendly.
* It is built for trusted environments, i.e. performs no authentication. If you have secrets in your logfiles, *don't use this*
* Zero configuration
* Zero external dependencies

## Why?
It originally was built to easily get logs out of tomcat running in a docker container, and to form a more informed opinion on Gradle vs Maven.

## Prerequisites
* Gradle (tested with 2.4) or Maven (tested with 3.3.3)

## Building with Gradle
* Run `gradle war` to build. The resulting file is `build/lib/logs.war` and can be dropped right into your webapps directory.
* To deploy via tomcat manager, run `gradle -Pcargo.username=<username> -Pcargo.password=<password> -Pcargo.hostname=<hostname> -Pcargo.port=<port> cargoRedeployRemote`

## Building with Maven
* Maven: run `mvn package` to build. The resulting file is `target/logs.war` and can be dropped right into your webapps directory.
* To deploy via tomcat manager, run `mvn tomcat7:redeploy -Dmaven.tomcat.url=http://<host>:<port>/manager/text -Dtomcat.username=<password>-Dtomcat.password=<password>`

## CI
Build Status: ![Travis CI](https://travis-ci.org/cbonitz/tomcat-logfile-zipper.svg)
