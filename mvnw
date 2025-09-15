#!/bin/sh
# Maven Wrapper Script
# This script allows you to run Maven without having Maven installed

MAVEN_WRAPPER_VERSION="3.9.5"
MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Maven wrapper jar not found. Please run with Maven installed or download the wrapper."
    exit 1
fi

exec java -jar "$MAVEN_WRAPPER_JAR" "$@"