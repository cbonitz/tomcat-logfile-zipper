# Tomcat Logfile Zip Project
This is a simple war file made to download tomcat logfiles.

## What
* It sends a zip containing all files in `${catalina.base}/logs` to any client requesting its context path.
* The zip file is streamed directly to the client. This make this program memory friendly.
* It is built for trusted environments, i.e. performs no authentication. If you have secrets in your logfiles, *don't use this*

## Why?
It originally was built to easily get logs out of tomcat running in a docker container.

## Prerequisites
* Gradle (tested with 2.4)

## Building
* Run `gradle war` to build. The resulting file is `build/lib/logs.war` and can be dropped right into your webapps directory.
* To deploy via tomcat manager, run `gradle -Pcargo.username=xxx -Pcargo.password=xxx [-Pcargo.port=....] cargoRedeployRemote`