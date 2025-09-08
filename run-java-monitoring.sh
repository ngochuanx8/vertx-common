#!/bin/bash

# Alternative script to run with Java directly (without Maven)
# Requires pre-compiled classes and dependencies

# JVM arguments for monitoring and profiling
JVM_OPTS="-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-Xmx2g \
-Xms1g \
-XX:+UnlockExperimentalVMOptions \
-XX:+UseStringDeduplication \
-Djava.rmi.server.hostname=localhost \
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=9999 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.local.only=false \
-XX:+FlightRecorder \
-XX:StartFlightRecording=duration=60m,filename=vertx-app.jfr \
-XX:FlightRecorderOptions=samplethreads=true \
-Djava.awt.headless=true"

echo "Starting Vert.x application with Java directly..."
echo "JMX Port: 9999 (for VisualVM connection)"
echo "Flight Recorder will save to: vertx-app.jfr"
echo ""

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Java not found in PATH"
    echo "Please install Java JDK 21 or set JAVA_HOME"
    exit 1
fi

# Check for compiled classes
if [ ! -d "target/classes" ]; then
    echo "❌ Compiled classes not found in target/classes"
    echo ""
    echo "Please compile the project first:"
    echo "1. Using Maven: mvn compile"
    echo "2. Using your IDE: Build/Compile the project"
    echo "3. Using Gradle: ./gradlew compileJava"
    exit 1
fi

# Build classpath - include compiled classes and all JARs from target/dependency
CLASSPATH="target/classes"

# Add dependency JARs if they exist
if [ -d "target/dependency" ]; then
    for jar in target/dependency/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
        fi
    done
fi

# If no dependency directory, try to find Maven repository JARs
if [ ! -d "target/dependency" ]; then
    echo "⚠️  Dependencies not found in target/dependency"
    echo "Run 'mvn dependency:copy-dependencies' to copy dependencies"
    echo "Or use your IDE's classpath configuration"
    echo ""
    echo "Attempting to run with minimal classpath..."
fi

echo "Classpath: $CLASSPATH"
echo ""
echo "Starting application..."
echo "Connect VisualVM to: localhost:9999"
echo "Press Ctrl+C to stop the application"
echo ""

# Run the application
java $JVM_OPTS -cp "$CLASSPATH" org.example.App