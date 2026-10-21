#!/bin/sh
# Maven Wrapper script for Unix/macOS/Git Bash
# Downloads Maven if not present and runs it

set -e

MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"
MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"

# Find project base directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Read distribution URL from properties
if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    DIST_URL=$(grep "distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2- | tr -d '\r')
    WRAPPER_URL=$(grep "wrapperUrl" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2- | tr -d '\r')
else
    echo "Error: $MAVEN_WRAPPER_PROPERTIES not found"
    exit 1
fi

# Determine Maven home
MAVEN_HOME="$HOME/.m2/wrapper/dists/$(basename "$DIST_URL" .zip)"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven from $DIST_URL ..."
    mkdir -p "$HOME/.m2/wrapper/dists"
    TMPFILE="$(mktemp)"
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL -o "$TMPFILE" "$DIST_URL"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "$TMPFILE" "$DIST_URL"
    else
        echo "Error: curl or wget required to download Maven"
        exit 1
    fi
    unzip -q -o "$TMPFILE" -d "$HOME/.m2/wrapper/dists"
    rm -f "$TMPFILE"
    # The zip extracts to a subdirectory like apache-maven-3.9.9
    EXTRACTED_DIR=$(ls -d "$HOME/.m2/wrapper/dists/apache-maven-"* 2>/dev/null | head -1)
    if [ -d "$EXTRACTED_DIR" ] && [ "$EXTRACTED_DIR" != "$MAVEN_HOME" ]; then
        mv "$EXTRACTED_DIR" "$MAVEN_HOME" 2>/dev/null || MAVEN_HOME="$EXTRACTED_DIR"
    fi
    echo "Maven installed to $MAVEN_HOME"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
