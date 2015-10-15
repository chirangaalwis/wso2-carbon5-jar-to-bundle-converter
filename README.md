# wso2-carbon5-jar-to-bundle-converter

Goal: converting JAR file(s) to their corresponding OSGi bundle(s)

Change: Previously, this functionality existed as part of https://github.com/wso2/carbon-kernel, in the module https://github.com/wso2/carbon-kernel/tree/master/core/org.wso2.carbon.server. This Java Maven project attempts to reproduce this functionality as a standalone functionality along with new Java 7 platform features.

Target JDK version: 1.8.

Usage:

1.  Clone this project.

2.  Perform a Maven build (maven-clean-install)

3.  To use the product, place the jar-to-bundle-converter-1.0-SNAPSHOT.jar along with uber-jar-to-bundle-converter-1.0-SNAPSHOT.jar which contains the dependencies of the Java Maven project, in the same folder.

    Run the application with the following command:

        java -cp uber-jar-to-bundle-converter-1.0-SNAPSHOT.jar org.wso2.carbon.tool.components.DefaultExecutor
